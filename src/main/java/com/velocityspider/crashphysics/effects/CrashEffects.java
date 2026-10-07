package com.velocityspider.crashphysics.effects;

import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.material.FractureMode;
import com.velocityspider.crashphysics.network.CameraShakePayload;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Sounds, dust and camera shake for crashes.
 * <p>
 * Impacts are added up per vehicle over a tick and played once at the end of it, scaled by how much energy the crash
 * dissipated, so a long scrape doesn't turn into hundreds of overlapping sounds.
 */
public final class CrashEffects {

    /**
     * Below this energy [J] an impact makes no extra noise beyond the blocks breaking
     */
    private static final double MIN_ENERGY = 4_000.0;

    private final ServerLevel level;
    private final RandomSource random = RandomSource.create();
    private final Reference2ObjectOpenHashMap<ServerSubLevel, Impact> impacts = new Reference2ObjectOpenHashMap<>();
    private final Reference2LongOpenHashMap<ServerSubLevel> lastSound = new Reference2LongOpenHashMap<>();
    private long tick;

    private static final class Impact {
        double energy;
        double weightedX;
        double weightedY;
        double weightedZ;
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
     */
    public void recordImpact(final ServerSubLevel vehicle, final double x, final double y, final double z, final double energy,
                             final FractureMode vehicleMaterial, final FractureMode hitMaterial, final BlockState hitState) {
        if (!(energy > 0.0)) {
            return;
        }

        final Impact impact = this.impacts.computeIfAbsent(vehicle, key -> new Impact());
        impact.energy += energy;
        impact.weightedX += x * energy;
        impact.weightedY += y * energy;
        impact.weightedZ += z * energy;
        impact.vehicleMaterials.merge(vehicleMaterial, energy, Double::sum);
        impact.hitMaterials.merge(hitMaterial, energy, Double::sum);
        if (energy > impact.dustEnergy && !hitState.isAir()) {
            impact.dustEnergy = energy;
            impact.dustState = hitState;
        }
    }

    public void flush() {
        this.tick++;
        if (this.impacts.isEmpty()) {
            return;
        }

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
            final double magnitude = Math.log10(impact.energy / 1000.0);

            // Don't stack sounds every tick of a long slide unless the crash gets much worse
            final boolean playedRecently = this.lastSound.containsKey(entry.getKey()) && this.tick - this.lastSound.getLong(entry.getKey()) < 3;
            if (!playedRecently || magnitude > 2.5) {
                this.lastSound.put(entry.getKey(), this.tick);
                this.playSounds(impact, x, y, z, magnitude, volumeScale);
            }

            this.spawnDust(impact, x, y, z, magnitude);

            if (shake) {
                this.shakeCameras(x, y, z, impact.energy);
            }
        }

        this.impacts.clear();
        if (this.lastSound.size() > 512) {
            this.lastSound.reference2LongEntrySet().removeIf(entry -> entry.getKey().isRemoved());
        }
    }

    private void playSounds(final Impact impact, final double x, final double y, final double z, final double magnitude, final double volumeScale) {
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

        // The deep thump of something big hitting hard
        if (magnitude > 2.5) {
            this.play(SoundEvents.GENERIC_EXPLODE.value(), x, y, z, (float) (Mth.clamp(0.4 + 0.5 * (magnitude - 2.5), 0.4, 4.0) * volumeScale),
                    (float) Mth.clamp(0.75 - 0.08 * (magnitude - 2.5), 0.4, 0.75));
        }
    }

    private void play(final SoundEvent sound, final double x, final double y, final double z, final float volume, final float pitch) {
        this.level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, volume, pitch);
    }

    private void spawnDust(final Impact impact, final double x, final double y, final double z, final double magnitude) {
        if (impact.dustState != null) {
            final int count = (int) Mth.clamp(magnitude * 10.0, 4, 120);
            final double spread = Mth.clamp(0.3 + magnitude * 0.4, 0.3, 3.0);
            this.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, impact.dustState), x, y + 0.3, z, count, spread, spread * 0.5, spread, 0.15);
        }

        if (magnitude > 2.0) {
            final int smoke = (int) Mth.clamp((magnitude - 2.0) * 12.0, 3, 60);
            this.level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.5, z, smoke, 1.2, 0.6, 1.2, 0.02);
        }
        if (magnitude > 3.5) {
            this.level.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.5, z, (int) Mth.clamp((magnitude - 3.5) * 4.0, 1, 12), 1.5, 1.0, 1.5, 0.0);
        }
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
        };
    }
}
