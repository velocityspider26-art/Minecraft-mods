package com.velocityspider.crashphysics.client;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashClientConfig;
import com.velocityspider.crashphysics.network.CrashFxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Plays the crash effects the server sends: water thrown up in crowns and columns that falls back as spray, wakes and
 * bow spray, white-hot sparks off metal, rings of dust rolling out from a hard landing, wrecks that keep smoking, water
 * jetting into holed hulls, and sounds that arrive as late as sound would.
 */
@EventBusSubscriber(modid = CrashPhysics.MODID, value = Dist.CLIENT)
public final class CrashFxClient {

    /**
     * Speed of sound in air [m/s]
     */
    private static final double SPEED_OF_SOUND = 343.0;

    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<Smoulder> SMOULDERING = new ArrayList<>();
    private static final Map<Entity, Boolean> WAS_IN_WATER = new WeakHashMap<>();

    /**
     * A wreck smoking after a crash.
     */
    private static final class Smoulder {
        final double x, y, z;
        final float strength;
        final int duration;
        final float radius;
        int age;

        Smoulder(final double x, final double y, final double z, final float strength, final int duration, final float radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.strength = strength;
            this.duration = duration;
            this.radius = radius;
        }
    }

    private CrashFxClient() {
    }

    public static void play(final List<CrashFxPayload.Fx> effects) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (final CrashFxPayload.Fx fx : effects) {
            switch (fx.kind()) {
                case CrashFxPayload.SPLASH -> splash(level, fx);
                case CrashFxPayload.WAKE -> wake(level, fx);
                case CrashFxPayload.SPARKS -> sparks(level, fx);
                case CrashFxPayload.DUST_RING -> dustRing(level, fx);
                case CrashFxPayload.SMOKE -> {
                    if (SMOULDERING.size() < 64) {
                        SMOULDERING.add(new Smoulder(fx.x(), fx.y(), fx.z(), fx.magnitude(), (int) fx.vx(), Math.max(1.0f, fx.vy())));
                    }
                }
                case CrashFxPayload.SOUND -> sound(fx);
                case CrashFxPayload.BUBBLES -> bubbles(level, fx);
                case CrashFxPayload.DEBRIS_DUST -> debrisDust(level, fx);
                case CrashFxPayload.BREACH -> breach(level, fx);
                default -> {
                }
            }
        }
    }

    private static int amount(final double count) {
        final double scaled = count * CrashClientConfig.PARTICLE_AMOUNT.getAsDouble();
        final int whole = (int) scaled;
        return whole + (RANDOM.nextDouble() < scaled - whole ? 1 : 0);
    }

    /**
     * Spawns a vanilla particle with exactly this velocity [blocks per tick]; vanilla's own spawning would scramble it.
     */
    @Nullable
    private static Particle spawn(final ParticleOptions type, final double x, final double y, final double z, final double vx, final double vy, final double vz) {
        final Particle particle = Minecraft.getInstance().particleEngine.createParticle(type, x, y, z, vx, vy, vz);
        if (particle != null) {
            particle.setParticleSpeed(vx, vy, vz);
        }
        return particle;
    }

    /**
     * Something hitting water: a crater in the surface that sends out rings, a crown of spray thrown up and out from
     * its rim, and for big impacts a column of water shooting up and falling back, with mist hanging over it. The spray
     * falls back in and rings the water where it lands.
     */
    private static void splash(final ClientLevel level, final CrashFxPayload.Fx fx) {
        final float magnitude = Math.max(0.0f, fx.magnitude());
        final double speed = Math.sqrt(fx.vx() * fx.vx() + fx.vy() * fx.vy() + fx.vz() * fx.vz());
        final float surface = WaterWaves.surface(level, fx.x(), fx.y(), fx.z());
        final double top = Float.isNaN(surface) ? fx.y() : surface;

        final float radius = 0.8f + 0.55f * magnitude;
        WaterWaves.displace(fx.x(), top, fx.z(), radius, Math.min(1.4f, 0.12f + 0.2f * magnitude));

        // The crown: a sheet of water thrown up and out from the rim of the crater, breaking into drops
        final int crown = amount(24 + 30 * magnitude);
        final double kick = 0.16 + 0.05 * magnitude + Math.min(speed, 60.0) * 0.004;
        for (int n = 0; n < crown; n++) {
            final double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            final double out = kick * (0.5 + RANDOM.nextDouble() * 0.6);
            final double r = radius * (0.6 + RANDOM.nextDouble() * 0.5);
            CrashParticles.droplet(level, fx.x() + Math.cos(angle) * r, top + 0.05, fx.z() + Math.sin(angle) * r,
                    Math.cos(angle) * out * 0.6, out * (1.0 + RANDOM.nextDouble() * 0.8), Math.sin(angle) * out * 0.6,
                    0.04f + RANDOM.nextFloat() * 0.05f);
        }

        // A column of water shooting up out of the crater as it closes, then falling back
        if (magnitude > 1.5f) {
            final int column = amount(25 * (magnitude - 1.0f));
            final double height = Math.min(1.6, 0.3 + 0.2 * (magnitude - 1.5));
            for (int n = 0; n < column; n++) {
                final double spread = radius * 0.35;
                CrashParticles.droplet(level, fx.x() + (RANDOM.nextDouble() - 0.5) * spread, top + 0.1, fx.z() + (RANDOM.nextDouble() - 0.5) * spread,
                        RANDOM.nextGaussian() * 0.04, height * (0.45 + RANDOM.nextDouble() * 0.55), RANDOM.nextGaussian() * 0.04,
                        0.05f + RANDOM.nextFloat() * 0.06f);
            }
            // Spray hanging in the air over it, drifting away
            final int mist = amount(3 * (magnitude - 1.0f));
            for (int n = 0; n < mist; n++) {
                CrashParticles.mist(level, fx.x() + (RANDOM.nextDouble() - 0.5) * radius * 1.4, top + 0.5 + RANDOM.nextDouble() * height * 8.0,
                        fx.z() + (RANDOM.nextDouble() - 0.5) * radius * 1.4, RANDOM.nextGaussian() * 0.02, 0.02 + RANDOM.nextDouble() * 0.03,
                        RANDOM.nextGaussian() * 0.02, 1.0f + magnitude * 0.35f, 40 + RANDOM.nextInt(30));
            }
        }

        // Air dragged under comes back up
        final int bubbles = amount(10 + 12 * magnitude);
        for (int n = 0; n < bubbles; n++) {
            spawn(ParticleTypes.BUBBLE, fx.x() + (RANDOM.nextDouble() - 0.5) * radius, top - 0.5 - RANDOM.nextDouble() * 1.5,
                    fx.z() + (RANDOM.nextDouble() - 0.5) * radius, 0.0, 0.05 + RANDOM.nextDouble() * 0.1, 0.0);
        }
    }

    /**
     * A hull moving through the water: it pushes the surface down and aside, which makes the bow wave and wake, and at
     * speed throws sheets of spray off both sides of the bow.
     */
    private static void wake(final ClientLevel level, final CrashFxPayload.Fx fx) {
        final double speed = Math.sqrt(fx.vx() * fx.vx() + fx.vz() * fx.vz());
        final float width = Math.max(0.5f, fx.magnitude());
        final float surface = WaterWaves.surface(level, fx.x(), fx.y(), fx.z());
        final double top = Float.isNaN(surface) ? fx.y() : surface;

        WaterWaves.push(fx.x(), top, fx.z(), width * 0.9f, (float) Math.min(1.5, 0.15 + speed * 0.25));

        if (speed > 3.0) {
            final int spray = amount(Math.min(10.0, (speed - 3.0) * 0.7));
            final double sideX = -fx.vz() / speed;
            final double sideZ = fx.vx() / speed;
            for (int n = 0; n < spray; n++) {
                final double side = RANDOM.nextBoolean() ? 1.0 : -1.0;
                final double out = 0.05 + speed * 0.012 * RANDOM.nextDouble();
                CrashParticles.droplet(level, fx.x() + sideX * side * 0.4, top + 0.1, fx.z() + sideZ * side * 0.4,
                        sideX * side * out + fx.vx() / 20.0 * 0.7, 0.1 + speed * 0.012 * RANDOM.nextDouble(), sideZ * side * out + fx.vz() / 20.0 * 0.7,
                        0.03f + RANDOM.nextFloat() * 0.04f);
            }
            if (speed > 8.0 && RANDOM.nextFloat() < 0.15f) {
                CrashParticles.mist(level, fx.x(), top + 0.4, fx.z(), fx.vx() / 40.0, 0.02, fx.vz() / 40.0, 1.0f, 30);
            }
        }
    }

    private static void bubbles(final ClientLevel level, final CrashFxPayload.Fx fx) {
        final int count = amount(Math.max(1.0f, fx.magnitude()));
        for (int n = 0; n < count; n++) {
            spawn(ParticleTypes.BUBBLE, fx.x() + (RANDOM.nextDouble() - 0.5), fx.y() + (RANDOM.nextDouble() - 0.5), fx.z() + (RANDOM.nextDouble() - 0.5),
                    0.0, 0.08 + RANDOM.nextDouble() * 0.08, 0.0);
        }
    }

    /**
     * Water forcing its way in through a hole in a hull: a jet of water inside, air bubbling out and the surface
     * outside drawn down over the hole.
     */
    private static void breach(final ClientLevel level, final CrashFxPayload.Fx fx) {
        final double rate = fx.magnitude();
        // The jet comes in about as fast as water would fall from the surface down to the hole
        final double speed = Math.min(0.45, 0.12 + rate * 0.03);
        final int count = amount(Math.min(12.0, 3.0 + rate));
        for (int n = 0; n < count; n++) {
            CrashParticles.droplet(level, fx.x() + (RANDOM.nextDouble() - 0.5) * 0.6, fx.y() + (RANDOM.nextDouble() - 0.5) * 0.6,
                    fx.z() + (RANDOM.nextDouble() - 0.5) * 0.6, fx.vx() * speed + RANDOM.nextGaussian() * 0.03,
                    fx.vy() * speed + RANDOM.nextGaussian() * 0.03 + 0.03, fx.vz() * speed + RANDOM.nextGaussian() * 0.03,
                    0.05f + RANDOM.nextFloat() * 0.05f);
        }
        if (RANDOM.nextDouble() < 0.6) {
            spawn(ParticleTypes.BUBBLE, fx.x() - fx.vx() * 0.6, fx.y() - fx.vy() * 0.6, fx.z() - fx.vz() * 0.6, 0.0, 0.1, 0.0);
        }
        final float surface = WaterWaves.surface(level, fx.x(), fx.y() + 1.0, fx.z());
        if (!Float.isNaN(surface) && surface - fx.y() < 2.5) {
            WaterWaves.push(fx.x(), surface, fx.z(), 0.8f, (float) Math.min(0.6, 0.05 + rate * 0.02));
        }
    }

    /**
     * Metal striking something hard: a spray of white-hot sparks glancing off the surface it hit (thrown back off a
     * wall, forward and up off the ground in a slide), bouncing and cooling to red.
     */
    private static void sparks(final ClientLevel level, final CrashFxPayload.Fx fx) {
        final float magnitude = Math.max(0.0f, fx.magnitude());
        // A scrape throws a steady stream, a hit one burst
        final boolean scrape = fx.data() == 1;
        final int count = amount(scrape ? 3 + 3 * magnitude : 14 + 18 * magnitude);
        // The way it glanced off, the velocity reflected in the surface it hit [m/s]
        final double speed = Math.sqrt(fx.vx() * fx.vx() + fx.vy() * fx.vy() + fx.vz() * fx.vz());
        final double rx = speed > 1.0e-3 ? fx.vx() / speed : 0.0;
        final double ry = speed > 1.0e-3 ? fx.vy() / speed : 1.0;
        final double rz = speed > 1.0e-3 ? fx.vz() / speed : 0.0;
        for (int n = 0; n < count; n++) {
            final double throwSpeed = 0.12 + RANDOM.nextDouble() * (0.25 + Math.min(speed, 60.0) * 0.008);
            // A cone around the glancing direction, opened up a lot, and always a little upwards
            final double dx = rx + RANDOM.nextGaussian() * 0.55;
            final double dy = ry + RANDOM.nextGaussian() * 0.45 + 0.3;
            final double dz = rz + RANDOM.nextGaussian() * 0.55;
            final double length = Math.max(1.0e-3, Math.sqrt(dx * dx + dy * dy + dz * dz));
            // Just off the surface, so they don't start inside what was hit
            CrashParticles.spark(level, fx.x() + rx * 0.3, fx.y() + ry * 0.3 + 0.05, fx.z() + rz * 0.3,
                    dx / length * throwSpeed, dy / length * throwSpeed, dz / length * throwSpeed);
        }
        if (scrape) {
            return;
        }
        // A puff of hot metal smoke
        CrashParticles.smoke(level, fx.x() + rx * 0.4, fx.y() + 0.2, fx.z() + rz * 0.4, 0.0, 0.02, 0.0, 0.4f, 1.4f, 40, 0.0008f, 0.35f,
                0.35f, 0.33f, 0.32f, 0.55f, 0.55f, 0.55f);
    }

    /**
     * A hard landing on loose ground: a ring of dust rolling out low from under the whole vehicle, clods thrown up, and
     * a cloud of dust rising and drifting off.
     */
    private static void dustRing(final ClientLevel level, final CrashFxPayload.Fx fx) {
        final float magnitude = Math.max(0.0f, fx.magnitude());
        final BlockState ground = Block.stateById(fx.data());
        final int mapColor = ground.getMapColor(level, BlockPos.containing(fx.x(), fx.y(), fx.z())).col;
        final float r = Mth.lerp(0.3f, ((mapColor >> 16) & 0xFF) / 255.0f, 0.78f);
        final float g = Mth.lerp(0.3f, ((mapColor >> 8) & 0xFF) / 255.0f, 0.72f);
        final float b = Mth.lerp(0.3f, (mapColor & 0xFF) / 255.0f, 0.66f);

        // Rolling out from under the whole vehicle, not just the point that hit
        final double start = Math.max(0.8, fx.vx());
        final int ring = amount((14 + 10 * magnitude) * Math.max(1.0, start / 3.0));
        final double outward = 0.22 + 0.07 * magnitude;
        for (int n = 0; n < ring; n++) {
            final double angle = (n + RANDOM.nextDouble() * 0.5) / ring * Math.PI * 2.0;
            final double speed = outward * (0.7 + RANDOM.nextDouble() * 0.5);
            CrashParticles.dust(level, fx.x() + Math.cos(angle) * start, fx.y() + 0.3, fx.z() + Math.sin(angle) * start,
                    Math.cos(angle) * speed, 0.01 + RANDOM.nextDouble() * 0.02, Math.sin(angle) * speed,
                    1.0f + RANDOM.nextFloat() * (0.8f + magnitude * 0.3f), 60 + RANDOM.nextInt(60), r, g, b);
        }

        if (!ground.isAir()) {
            final BlockParticleOption clods = new BlockParticleOption(ParticleTypes.BLOCK, ground);
            final int count = amount(10 + 10 * magnitude);
            for (int n = 0; n < count; n++) {
                final double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                final double out = 0.1 + RANDOM.nextDouble() * (0.15 + magnitude * 0.05);
                spawn(clods, fx.x() + Math.cos(angle) * start * 0.8, fx.y() + 0.5, fx.z() + Math.sin(angle) * start * 0.8,
                        Math.cos(angle) * out, 0.2 + RANDOM.nextDouble() * (0.3 + magnitude * 0.06), Math.sin(angle) * out);
            }
        }

        // The cloud kicked up over it all
        final int plume = amount(4 + 4 * magnitude);
        for (int n = 0; n < plume; n++) {
            final double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            final double at = start * RANDOM.nextDouble();
            CrashParticles.smoke(level, fx.x() + Math.cos(angle) * at, fx.y() + 0.5 + RANDOM.nextDouble(), fx.z() + Math.sin(angle) * at,
                    RANDOM.nextGaussian() * 0.02, 0.03 + RANDOM.nextDouble() * 0.03, RANDOM.nextGaussian() * 0.02,
                    1.2f, 3.0f + magnitude * 0.4f, 120, 0.0002f, 0.5f, r, g, b, r * 1.1f, g * 1.1f, b * 1.1f);
        }
    }

    /**
     * Dust and chips knocked off a breaking block.
     */
    private static void debrisDust(final ClientLevel level, final CrashFxPayload.Fx fx) {
        final BlockState state = Block.stateById(fx.data());
        if (state.isAir()) {
            return;
        }
        final BlockParticleOption chips = new BlockParticleOption(ParticleTypes.BLOCK, state);
        final int count = amount(6 + 4 * fx.magnitude());
        for (int n = 0; n < count; n++) {
            spawn(chips, fx.x() + (RANDOM.nextDouble() - 0.5), fx.y() + (RANDOM.nextDouble() - 0.5), fx.z() + (RANDOM.nextDouble() - 0.5),
                    (RANDOM.nextDouble() - 0.5) * 0.3 + fx.vx() / 40.0, RANDOM.nextDouble() * 0.3 + fx.vy() / 40.0, (RANDOM.nextDouble() - 0.5) * 0.3 + fx.vz() / 40.0);
        }
        final int mapColor = state.getMapColor(level, BlockPos.containing(fx.x(), fx.y(), fx.z())).col;
        final float r = Mth.lerp(0.4f, ((mapColor >> 16) & 0xFF) / 255.0f, 0.75f);
        final float g = Mth.lerp(0.4f, ((mapColor >> 8) & 0xFF) / 255.0f, 0.72f);
        final float b = Mth.lerp(0.4f, (mapColor & 0xFF) / 255.0f, 0.68f);
        if (RANDOM.nextFloat() < 0.6f) {
            CrashParticles.dust(level, fx.x(), fx.y(), fx.z(), fx.vx() / 60.0, 0.03, fx.vz() / 60.0, 0.8f, 50, r, g, b);
        }
    }

    /**
     * A crash sound, heard as late as sound takes to get here.
     */
    private static void sound(final CrashFxPayload.Fx fx) {
        final SoundEvent event = BuiltInRegistries.SOUND_EVENT.byId(fx.data());
        if (event == null) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final Vec3 listener = minecraft.gameRenderer.getMainCamera().getPosition();
        final double distance = listener.distanceTo(new Vec3(fx.x(), fx.y(), fx.z()));
        final int delay = CrashClientConfig.SOUND_DELAY.getAsBoolean() ? (int) Math.round(distance / SPEED_OF_SOUND * 20.0) : 0;
        final SimpleSoundInstance instance = new SimpleSoundInstance(event, SoundSource.BLOCKS, fx.magnitude(), fx.vx(), RANDOM, fx.x(), fx.y(), fx.z());
        if (delay > 0) {
            minecraft.getSoundManager().playDelayed(instance, delay);
        } else {
            minecraft.getSoundManager().play(instance);
        }
    }

    @SubscribeEvent
    static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            SMOULDERING.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }

        // Wrecks smoulder: thick dark smoke billowing up, paling and spreading as it rises, thinning out over time
        for (final Iterator<Smoulder> iterator = SMOULDERING.iterator(); iterator.hasNext(); ) {
            final Smoulder smoulder = iterator.next();
            if (++smoulder.age > smoulder.duration) {
                iterator.remove();
                continue;
            }
            final float left = 1.0f - (float) smoulder.age / smoulder.duration;
            final float rate = smoulder.strength * left * (float) CrashClientConfig.PARTICLE_AMOUNT.getAsDouble();
            if (RANDOM.nextFloat() < rate * 0.6f) {
                final float dark = 0.12f + RANDOM.nextFloat() * 0.1f;
                final float pale = 0.45f + RANDOM.nextFloat() * 0.15f;
                CrashParticles.smoke(level, smoulder.x + (RANDOM.nextDouble() - 0.5) * 2.0 * smoulder.radius, smoulder.y + RANDOM.nextDouble() * 0.5,
                        smoulder.z + (RANDOM.nextDouble() - 0.5) * 2.0 * smoulder.radius, RANDOM.nextGaussian() * 0.01, 0.05 + RANDOM.nextDouble() * 0.04,
                        RANDOM.nextGaussian() * 0.01, 0.6f + smoulder.strength * 0.2f, 2.0f + smoulder.strength * 0.7f, 140 + RANDOM.nextInt(80),
                        0.0012f, 0.75f, dark, dark, dark, pale, pale, pale);
            }
            if (smoulder.strength > 1.5f && smoulder.age % 5 == 0 && left > 0.3f) {
                // The tall column over a big wreck, visible from far away
                final float grey = 0.2f + RANDOM.nextFloat() * 0.1f;
                CrashParticles.smoke(level, smoulder.x + RANDOM.nextGaussian() * 0.5, smoulder.y + 0.5, smoulder.z + RANDOM.nextGaussian() * 0.5,
                        0.0, 0.12 + RANDOM.nextDouble() * 0.05, 0.0, 1.0f, 3.5f, 260, 0.0018f, 0.65f, grey, grey, grey, 0.62f, 0.62f, 0.64f);
            }
        }
    }

    /**
     * Ripples from anything moving in the water near the player, and a splash when something falls in.
     */
    static void rippleEntities(final ClientLevel level) {
        final Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        final double range = Math.min(48.0, CrashClientConfig.WAVE_RANGE.getAsInt());
        for (final Entity entity : level.entitiesForRendering()) {
            if (entity.distanceToSqr(camera) > range * range) {
                continue;
            }
            final boolean inWater = entity.isInWater();
            final Boolean before = WAS_IN_WATER.put(entity, inWater);
            if (!inWater) {
                continue;
            }
            final double surfaceDepth = entity.getFluidHeight(FluidTags.WATER);
            if (surfaceDepth <= 0.0 || surfaceDepth > entity.getBbHeight() + 0.5) {
                // Deep under water: the surface doesn't feel it
                continue;
            }
            final Vec3 motion = entity.getDeltaMovement();
            final double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z) * 20.0;
            final float width = Math.max(0.4f, entity.getBbWidth());
            if (before != null && !before && motion.y < -0.15) {
                // Fell in
                WaterWaves.displace(entity.getX(), entity.getY() + surfaceDepth, entity.getZ(), width * 0.9f,
                        (float) Math.min(0.5, -motion.y * 0.6 * width));
            } else if (horizontal > 0.4) {
                WaterWaves.push(entity.getX(), entity.getY() + surfaceDepth, entity.getZ(), width * 0.6f, (float) Math.min(1.0, horizontal * 0.06 * width));
            }
        }
    }
}
