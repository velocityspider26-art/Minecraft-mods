package com.velocityspider.crashphysics.effects;

import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.material.FractureMode;
import com.velocityspider.crashphysics.network.CameraShakePayload;
import com.velocityspider.crashphysics.network.CrashFxPayload;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sounds, particles, waves and camera shake for crashes.
 * <p>
 * Impacts are added up per vehicle over a tick and played once at the end of it, scaled by how much energy the crash
 * dissipated, so a long scrape doesn't turn into hundreds of overlapping sounds. Players with the mod get the full
 * effects (waves, sparks, dust rings, smoke, sound that takes time to arrive) as one packet per tick; players without it
 * get plain particles and sounds.
 */
public final class CrashEffects {

    /**
     * Below this energy [J] an impact makes no extra noise beyond the blocks breaking
     */
    private static final double MIN_ENERGY = 4_000.0;

    /**
     * How far away the visual effects are sent [blocks]
     */
    private static final double EFFECT_RANGE = 160.0;
    private static final double WAKE_RANGE = 112.0;
    private static final int MAX_WAKES_PER_VEHICLE = 10;

    private final ServerLevel level;
    private final RandomSource random = RandomSource.create();
    private final Reference2ObjectOpenHashMap<ServerSubLevel, Impact> impacts = new Reference2ObjectOpenHashMap<>();
    private final Reference2LongOpenHashMap<ServerSubLevel> lastSound = new Reference2LongOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<ServerSubLevel, List<CrashFxPayload.Fx>> wakes = new Reference2ObjectOpenHashMap<>();
    private final Map<ServerPlayer, List<CrashFxPayload.Fx>> outgoing = new HashMap<>();
    private long tick;

    private static final class Impact {
        double energy;
        double weightedX;
        double weightedY;
        double weightedZ;
        double velocityX;
        double velocityY;
        double velocityZ;
        // The velocity bounced off the surfaces hit: the way sparks and chips fly
        double reboundX;
        double reboundY;
        double reboundZ;
        final Map<FractureMode, Double> vehicleMaterials = new EnumMap<>(FractureMode.class);
        final Map<FractureMode, Double> hitMaterials = new EnumMap<>(FractureMode.class);
        BlockState dustState;
        double dustEnergy;
    }

    public CrashEffects(final ServerLevel level) {
        this.level = level;
    }

    /**
     * Adds an impact to this tick's effects.
     *
     * @param energy energy dissipated [J]
     * @param vx     velocity of the vehicle against what it hit [m/s]
     * @param nx     the contact normal
     */
    public void recordImpact(final ServerSubLevel vehicle, final double x, final double y, final double z, final double energy,
                             final FractureMode vehicleMaterial, final FractureMode hitMaterial, final BlockState hitState,
                             final double vx, final double vy, final double vz, final double nx, final double ny, final double nz) {
        if (!(energy > 0.0)) {
            return;
        }

        final Impact impact = this.impacts.computeIfAbsent(vehicle, key -> new Impact());
        impact.energy += energy;
        impact.weightedX += x * energy;
        impact.weightedY += y * energy;
        impact.weightedZ += z * energy;
        impact.velocityX += vx * energy;
        impact.velocityY += vy * energy;
        impact.velocityZ += vz * energy;
        final double normal = vx * nx + vy * ny + vz * nz;
        impact.reboundX += (vx - 2.0 * normal * nx) * energy;
        impact.reboundY += (vy - 2.0 * normal * ny) * energy;
        impact.reboundZ += (vz - 2.0 * normal * nz) * energy;
        impact.vehicleMaterials.merge(vehicleMaterial, energy, Double::sum);
        impact.hitMaterials.merge(hitMaterial, energy, Double::sum);
        if (energy > impact.dustEnergy && !hitState.isAir()) {
            impact.dustEnergy = energy;
            impact.dustState = hitState;
        }
    }

    /**
     * A vehicle's hull is moving through the water surface here: it makes a wake.
     *
     * @param width how wide the part of the hull here is [m]
     */
    public void wake(final ServerSubLevel vehicle, final double x, final double y, final double z, final double vx, final double vz, final double width) {
        final List<CrashFxPayload.Fx> list = this.wakes.computeIfAbsent(vehicle, key -> new ArrayList<>());
        if (list.size() < MAX_WAKES_PER_VEHICLE * 2) {
            list.add(new CrashFxPayload.Fx(CrashFxPayload.WAKE, x, y, z, (float) width, (float) vx, 0.0f, (float) vz, 0));
        }
    }

    /**
     * Air coming out of a vehicle moving under water.
     */
    public void bubbles(final ServerSubLevel vehicle, final double x, final double y, final double z, final int count) {
        final List<CrashFxPayload.Fx> list = this.wakes.computeIfAbsent(vehicle, key -> new ArrayList<>());
        if (list.size() < MAX_WAKES_PER_VEHICLE * 2) {
            list.add(new CrashFxPayload.Fx(CrashFxPayload.BUBBLES, x, y, z, count, 0.0f, 0.0f, 0.0f, 0));
        }
    }

    /**
     * Metal sliding over rock or metal: a stream of sparks, and the grind of it.
     *
     * @param vx sliding velocity [m/s]
     */
    public void scrape(final ServerSubLevel vehicle, final double x, final double y, final double z, final double vx, final double vy, final double vz) {
        final double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        final List<CrashFxPayload.Fx> list = this.wakes.computeIfAbsent(vehicle, key -> new ArrayList<>());
        // Thrown along the slide and up off the surface
        list.add(new CrashFxPayload.Fx(CrashFxPayload.SPARKS, x, y, z, (float) Mth.clamp(speed / 12.0, 0.4, 3.0),
                (float) vx, (float) (Math.abs(vy) + speed * 0.4), (float) vz, 1));
        if (this.tick % 6 == 0 && this.lastScrapeSound != this.tick) {
            this.lastScrapeSound = this.tick;
            this.play(SoundEvents.GRINDSTONE_USE, x, y, z, (float) Mth.clamp(0.4 + speed * 0.03, 0.4, 1.5), (float) Mth.clamp(0.6 + speed * 0.02, 0.6, 1.4));
        }
    }

    private long lastScrapeSound = -1;

    /**
     * Water pouring into a hull through a hole.
     *
     * @param dx   the way it flows in
     * @param rate how much flows in [m³/s]
     */
    public void breach(final ServerSubLevel vehicle, final double x, final double y, final double z, final double dx, final double dy, final double dz,
                       final double rate) {
        final List<CrashFxPayload.Fx> list = this.wakes.computeIfAbsent(vehicle, key -> new ArrayList<>());
        list.add(new CrashFxPayload.Fx(CrashFxPayload.BREACH, x, y, z, (float) rate, (float) dx, (float) dy, (float) dz, 0));
        if (this.tick % 15 == 0) {
            // The gurgle and rush of water flooding in
            this.play(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, x, y, z, (float) Mth.clamp(0.4 + rate * 0.1, 0.4, 2.0), 0.7f);
        }
    }

    /**
     * Something caught fire or blew up in a crash: it burns and smokes for a while.
     *
     * @param strength how big the fire is, about 1 for a burning block, up to 3 for a big explosion
     */
    public void fire(final double x, final double y, final double z, final float strength) {
        for (final ServerPlayer player : this.level.players()) {
            if (player.distanceToSqr(x, y, z) <= EFFECT_RANGE * EFFECT_RANGE && this.modded(player)) {
                final float duration = 300.0f + 200.0f * strength;
                this.send(player, new CrashFxPayload.Fx(CrashFxPayload.SMOKE, x, y, z, strength, duration, 1.0f + strength * 0.5f, 0.0f, 0));
            }
        }
    }

    public void flush() {
        this.tick++;
        if (!this.impacts.isEmpty()) {
            this.flushImpacts();
        }
        if (!this.wakes.isEmpty()) {
            this.flushWakes();
        }
        for (final Map.Entry<ServerPlayer, List<CrashFxPayload.Fx>> entry : this.outgoing.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                PacketDistributor.sendToPlayer(entry.getKey(), new CrashFxPayload(List.copyOf(entry.getValue())));
            }
        }
        this.outgoing.clear();
    }

    private void flushImpacts() {
        final double volumeScale = CrashConfig.SOUND_VOLUME.getAsDouble();
        final boolean shake = CrashConfig.CAMERA_SHAKE.getAsBoolean();

        for (final Map.Entry<ServerSubLevel, Impact> entry : this.impacts.entrySet()) {
            final Impact impact = entry.getValue();
            if (impact.energy < MIN_ENERGY) {
                continue;
            }

            final double x = impact.weightedX / impact.energy;
            final double y = impact.weightedY / impact.energy;
            final double z = impact.weightedZ / impact.energy;
            final double vx = impact.velocityX / impact.energy;
            final double vy = impact.velocityY / impact.energy;
            final double vz = impact.velocityZ / impact.energy;
            final double magnitude = Math.log10(impact.energy / 1000.0);

            // Don't stack sounds every tick of a long slide unless the crash gets much worse
            final boolean playedRecently = this.lastSound.containsKey(entry.getKey()) && this.tick - this.lastSound.getLong(entry.getKey()) < 3;
            if (!playedRecently || magnitude > 2.5) {
                this.lastSound.put(entry.getKey(), this.tick);
                this.playSounds(impact, x, y, z, magnitude, volumeScale, Math.sqrt(vx * vx + vy * vy + vz * vz));
            }

            final double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            this.spawnEffects(entry.getKey(), impact, x, y, z, vx, vy, vz, magnitude, speed);

            if (shake) {
                this.shakeCameras(x, y, z, impact.energy);
            }
        }

        this.impacts.clear();
        if (this.lastSound.size() > 512) {
            this.lastSound.reference2LongEntrySet().removeIf(entry -> entry.getKey().isRemoved());
        }
    }

    private void flushWakes() {
        for (final Map.Entry<ServerSubLevel, List<CrashFxPayload.Fx>> entry : this.wakes.entrySet()) {
            final List<CrashFxPayload.Fx> list = entry.getValue();
            if (list.isEmpty()) {
                continue;
            }
            // Spread the ones that are sent along the hull
            final int step = Math.max(1, list.size() / MAX_WAKES_PER_VEHICLE);
            final CrashFxPayload.Fx first = list.getFirst();
            for (final ServerPlayer player : this.level.players()) {
                if (player.distanceToSqr(first.x(), first.y(), first.z()) > WAKE_RANGE * WAKE_RANGE) {
                    continue;
                }
                if (this.modded(player)) {
                    final List<CrashFxPayload.Fx> out = this.outgoing.computeIfAbsent(player, key -> new ArrayList<>());
                    for (int i = 0; i < list.size(); i += step) {
                        out.add(list.get(i));
                    }
                } else if (this.tick % 2 == 0) {
                    // Plain spray off the bow for players without the mod
                    for (int i = 0; i < list.size(); i += step) {
                        final CrashFxPayload.Fx fx = list.get(i);
                        final double speed = Math.sqrt(fx.vx() * fx.vx() + fx.vz() * fx.vz());
                        if (fx.kind() == CrashFxPayload.WAKE && speed > 3.0) {
                            this.level.sendParticles(player, ParticleTypes.SPLASH, false, fx.x(), fx.y() + 0.1, fx.z(), 3, 0.3, 0.1, 0.3, 0.1);
                        } else if (fx.kind() == CrashFxPayload.SPARKS) {
                            this.level.sendParticles(player, ParticleTypes.ELECTRIC_SPARK, false, fx.x(), fx.y() + 0.1, fx.z(), 3, 0.2, 0.1, 0.2, 0.3);
                        }
                    }
                }
            }
            list.clear();
        }
        if (this.tick % 200 == 0) {
            this.wakes.keySet().removeIf(ServerSubLevel::isRemoved);
        }
    }

    private boolean modded(final ServerPlayer player) {
        return player.connection.hasChannel(CrashFxPayload.TYPE);
    }

    private void send(final ServerPlayer player, final CrashFxPayload.Fx fx) {
        this.outgoing.computeIfAbsent(player, key -> new ArrayList<>()).add(fx);
    }

    private void playSounds(final Impact impact, final double x, final double y, final double z, final double magnitude, final double volumeScale,
                            final double speed) {
        final float volume = (float) (Mth.clamp(0.4 + 0.45 * magnitude, 0.3, 4.0) * volumeScale);
        final float pitch = (float) (Mth.clamp(1.25 - 0.13 * magnitude, 0.45, 1.3) * (0.9 + this.random.nextDouble() * 0.2));
        if (volume <= 0.0f) {
            return;
        }

        final FractureMode vehicle = dominant(impact.vehicleMaterials);
        final FractureMode hit = dominant(impact.hitMaterials);

        this.play(vehicleSound(vehicle), x, y, z, volume, pitch);
        if (hit != vehicle) {
            this.play(hitSound(hit), x, y, z, volume * 0.8f, pitch);
        }

        // The deep thump of something big hitting hard, carrying a long way
        if (magnitude > 2.5 && speed > 6.0) {
            this.play(SoundEvents.GENERIC_EXPLODE.value(), x, y, z, (float) (Mth.clamp(0.4 + 0.5 * (magnitude - 2.5), 0.4, 4.0) * volumeScale),
                    (float) Mth.clamp(0.75 - 0.08 * (magnitude - 2.5), 0.4, 0.75));
        }
        if (magnitude > 3.0 && speed > 10.0) {
            // A low rumble heard from far off
            this.play(SoundEvents.GENERIC_EXPLODE.value(), x, y, z, (float) (Mth.clamp(2.0 + 3.0 * (magnitude - 3.0), 2.0, 12.0) * volumeScale), 0.45f);
        }
    }

    /**
     * Plays a sound to everyone in earshot. Players with the mod hear it once the sound has had time to reach them.
     */
    private void play(final SoundEvent sound, final double x, final double y, final double z, final float volume, final float pitch) {
        final double range = Math.max(1.0f, volume) * 16.0;
        final int id = BuiltInRegistries.SOUND_EVENT.getId(sound);
        for (final ServerPlayer player : this.level.players()) {
            if (player.distanceToSqr(x, y, z) > range * range) {
                continue;
            }
            if (this.modded(player)) {
                this.send(player, new CrashFxPayload.Fx(CrashFxPayload.SOUND, x, y, z, volume, pitch, 0.0f, 0.0f, id));
            } else {
                player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.BLOCKS, x, y, z,
                        volume, pitch, this.random.nextLong()));
            }
        }
    }

    private void spawnEffects(final ServerSubLevel vehicleBody, final Impact impact, final double x, final double y, final double z,
                              final double vx, final double vy, final double vz, final double magnitude, final double speed) {
        // Where the vehicle is: dust rolls out from under all of it, and a wreck smokes from its top, not from the
        // contact point hidden underneath
        final double[] bounds = worldBounds(vehicleBody);
        final double centerX = bounds != null ? 0.5 * (bounds[0] + bounds[3]) : x;
        final double centerZ = bounds != null ? 0.5 * (bounds[2] + bounds[5]) : z;
        final float halfWidth = bounds != null ? (float) (0.5 * Math.hypot(bounds[3] - bounds[0], bounds[5] - bounds[2])) : 1.0f;
        final double top = bounds != null ? bounds[4] : y + 1.0;

        final FractureMode vehicle = dominant(impact.vehicleMaterials);
        final FractureMode hit = dominant(impact.hitMaterials);
        final boolean liquid = impact.dustState != null && !impact.dustState.getFluidState().isEmpty();
        final boolean loose = hit == FractureMode.GRANULAR || hit == FractureMode.SOFT || hit == FractureMode.PLANT;
        final boolean metal = vehicle == FractureMode.DUCTILE || hit == FractureMode.DUCTILE
                || impact.vehicleMaterials.getOrDefault(FractureMode.DUCTILE, 0.0) > impact.energy * 0.2;
        final boolean hard = hit == FractureMode.BRITTLE || hit == FractureMode.DUCTILE || hit == FractureMode.GLASS;

        for (final ServerPlayer player : this.level.players()) {
            if (player.distanceToSqr(x, y, z) > EFFECT_RANGE * EFFECT_RANGE) {
                continue;
            }
            if (this.modded(player)) {
                if (liquid) {
                    this.send(player, new CrashFxPayload.Fx(CrashFxPayload.SPLASH, x, y, z, (float) magnitude, (float) vx, (float) vy, (float) vz, 0));
                } else if (impact.dustState != null) {
                    final int stateId = Block.getId(impact.dustState);
                    if (loose && magnitude > 1.5 && speed > 6.0) {
                        this.send(player, new CrashFxPayload.Fx(CrashFxPayload.DUST_RING, centerX, y, centerZ, (float) (magnitude - 1.0),
                                Math.min(halfWidth, 24.0f), 0.0f, 0.0f, stateId));
                    } else {
                        this.send(player, new CrashFxPayload.Fx(CrashFxPayload.DEBRIS_DUST, x, y, z, (float) magnitude, (float) vx, (float) vy, (float) vz, stateId));
                    }
                }
                if (metal && hard && magnitude > 0.8 && speed > 5.0) {
                    this.send(player, new CrashFxPayload.Fx(CrashFxPayload.SPARKS, x, y, z, (float) magnitude, (float) (impact.reboundX / impact.energy),
                            (float) (impact.reboundY / impact.energy), (float) (impact.reboundZ / impact.energy), 0));
                }
            } else {
                this.vanillaParticles(player, impact, x, y, z, magnitude, liquid, metal && hard && speed > 5.0);
            }
        }

        if (magnitude > 3.5 && speed > 15.0) {
            this.level.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.5, z, (int) Mth.clamp((magnitude - 3.5) * 4.0, 1, 12), 1.5, 1.0, 1.5, 0.0);
        }
    }

    /**
     * @return the vehicle's bounds in the world (min x, y, z, max x, y, z), or null if unknown
     */
    @org.jetbrains.annotations.Nullable
    private static double[] worldBounds(final ServerSubLevel vehicle) {
        if (vehicle.isRemoved()) {
            return null;
        }
        final dev.ryanhcode.sable.companion.math.BoundingBox3ic plot = vehicle.getPlot().getBoundingBox();
        if (plot == null) {
            return null;
        }
        final double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        final org.joml.Vector3d corner = new org.joml.Vector3d();
        for (int n = 0; n < 8; n++) {
            vehicle.logicalPose().transformPosition(corner.set(
                    (n & 1) == 0 ? plot.minX() : plot.maxX() + 1,
                    (n & 2) == 0 ? plot.minY() : plot.maxY() + 1,
                    (n & 4) == 0 ? plot.minZ() : plot.maxZ() + 1));
            bounds[0] = Math.min(bounds[0], corner.x);
            bounds[1] = Math.min(bounds[1], corner.y);
            bounds[2] = Math.min(bounds[2], corner.z);
            bounds[3] = Math.max(bounds[3], corner.x);
            bounds[4] = Math.max(bounds[4], corner.y);
            bounds[5] = Math.max(bounds[5], corner.z);
        }
        return bounds;
    }

    private void vanillaParticles(final ServerPlayer player, final Impact impact, final double x, final double y, final double z, final double magnitude,
                                  final boolean liquid, final boolean sparks) {
        if (liquid) {
            // A wall of spray thrown up and out, then falling back
            final int count = (int) Mth.clamp(magnitude * 25.0, 10, 300);
            final double spread = Mth.clamp(0.5 + magnitude * 0.6, 0.5, 4.0);
            this.particles(player, ParticleTypes.SPLASH, x, y + 0.6, z, count, spread, 0.4, spread, 0.6);
            this.particles(player, ParticleTypes.BUBBLE, x, y - 0.5, z, count / 2, spread * 0.7, 0.6, spread * 0.7, 0.2);
            if (magnitude > 2.0) {
                this.particles(player, ParticleTypes.CLOUD, x, y + 1.0, z, (int) Mth.clamp((magnitude - 2.0) * 15.0, 4, 60), spread, 1.0, spread, 0.05);
            }
            return;
        }
        if (impact.dustState != null) {
            final int count = (int) Mth.clamp(magnitude * 10.0, 4, 120);
            final double spread = Mth.clamp(0.3 + magnitude * 0.4, 0.3, 3.0);
            this.particles(player, new BlockParticleOption(ParticleTypes.BLOCK, impact.dustState), x, y + 0.3, z, count, spread, spread * 0.5, spread, 0.15);
        }
        if (sparks && magnitude > 0.8) {
            this.particles(player, ParticleTypes.ELECTRIC_SPARK, x, y + 0.2, z, (int) Mth.clamp(magnitude * 8.0, 4, 60), 0.5, 0.3, 0.5, 0.4);
        }
        if (magnitude > 2.0) {
            final int smoke = (int) Mth.clamp((magnitude - 2.0) * 12.0, 3, 60);
            this.particles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.5, z, smoke, 1.2, 0.6, 1.2, 0.02);
        }
    }

    private void particles(final ServerPlayer player, final ParticleOptions type, final double x, final double y, final double z, final int count,
                           final double dx, final double dy, final double dz, final double speed) {
        this.level.sendParticles(player, type, false, x, y, z, count, dx, dy, dz, speed);
    }

    private void shakeCameras(final double x, final double y, final double z, final double energy) {
        final double intensity = Math.min(1.5, Math.sqrt(energy / 4.0e6));
        if (intensity < 0.03) {
            return;
        }

        final double radius = 24.0 + 80.0 * Math.min(1.0, intensity);
        for (final ServerPlayer player : this.level.players()) {
            final double distance = Math.sqrt(player.distanceToSqr(x, y, z));
            if (distance > radius) {
                continue;
            }
            final double falloff = (1.0 - distance / radius);
            final float strength = (float) (intensity * falloff * falloff);
            if (strength > 0.02f && player.connection.hasChannel(CameraShakePayload.TYPE)) {
                PacketDistributor.sendToPlayer(player, new CameraShakePayload(strength));
            }
        }
    }

    private static FractureMode dominant(final Map<FractureMode, Double> energies) {
        FractureMode best = FractureMode.BRITTLE;
        double bestEnergy = -1.0;
        for (final Map.Entry<FractureMode, Double> entry : energies.entrySet()) {
            if (entry.getValue() > bestEnergy) {
                bestEnergy = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    private static SoundEvent vehicleSound(final FractureMode mode) {
        return switch (mode) {
            case FIBROUS -> SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR;
            case DUCTILE -> SoundEvents.ANVIL_LAND;
            case GLASS -> SoundEvents.GLASS_BREAK;
            case SOFT, PLANT -> SoundEvents.WOOL_BREAK;
            case GRANULAR -> SoundEvents.GRAVEL_BREAK;
            case BRITTLE -> SoundEvents.STONE_BREAK;
            case LIQUID -> SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
        };
    }

    private static SoundEvent hitSound(final FractureMode mode) {
        return switch (mode) {
            case GRANULAR -> SoundEvents.ROOTED_DIRT_BREAK;
            case BRITTLE -> SoundEvents.DEEPSLATE_BREAK;
            case FIBROUS -> SoundEvents.WOOD_BREAK;
            case DUCTILE -> SoundEvents.ANVIL_LAND;
            case GLASS -> SoundEvents.GLASS_BREAK;
            case SOFT, PLANT -> SoundEvents.GRASS_BREAK;
            case LIQUID -> SoundEvents.GENERIC_SPLASH;
        };
    }
}
