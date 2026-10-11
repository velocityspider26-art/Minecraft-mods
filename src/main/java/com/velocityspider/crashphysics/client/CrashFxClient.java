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
 * Plays the crash effects the server sends: water thrown up in crowns and columns, wakes and bow spray, sparks off metal,
 * rings of dust rolling out from a hard landing, smoke rising from wrecks, and sounds that arrive as late as sound
 * would.
 */
@EventBusSubscriber(modid = CrashPhysics.MODID, value = Dist.CLIENT)
public final class CrashFxClient {

    /**
     * Speed of sound in air [m/s]
     */
    private static final double SPEED_OF_SOUND = 343.0;

    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<Smoke> SMOKE = new ArrayList<>();
    private static final Map<Entity, Boolean> WAS_IN_WATER = new WeakHashMap<>();

    private static final class Smoke {
        final double x, y, z;
        final float strength;
        final int duration;
        final float radius;
        int age;

        Smoke(final double x, final double y, final double z, final float strength, final int duration, final float radius) {
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
                    if (SMOKE.size() < 64) {
                        SMOKE.add(new Smoke(fx.x(), fx.y(), fx.z(), fx.magnitude(), (int) fx.vx(), Math.max(1.0f, fx.vy())));
                    }
                }
                case CrashFxPayload.SOUND -> sound(fx);
                case CrashFxPayload.BUBBLES -> bubbles(level, fx);
                case CrashFxPayload.DEBRIS_DUST -> debrisDust(level, fx);
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
     * Spawns a particle with exactly this velocity [blocks per tick]; vanilla's own spawning would scramble it.
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
     * Something hitting water: a crater in the surface that sends out rings, a crown of droplets, and for big impacts
     * a column of spray with mist hanging over it.
     */
    private static void splash(final ClientLevel level, final CrashFxPayload.Fx fx) {
        final float magnitude = Math.max(0.0f, fx.magnitude());
        final double speed = Math.sqrt(fx.vx() * fx.vx() + fx.vy() * fx.vy() + fx.vz() * fx.vz());
        final float surface = WaterWaves.surface(level, fx.x(), fx.y(), fx.z());
        final double top = Float.isNaN(surface) ? fx.y() : surface;

        final float radius = 0.8f + 0.55f * magnitude;
        WaterWaves.displace(fx.x(), top, fx.z(), radius, Math.min(1.4f, 0.12f + 0.2f * magnitude));

        // The crown: a ring of droplets thrown up and out
        final int crown = amount(18 + 26 * magnitude);
        final double kick = 0.18 + 0.05 * magnitude + Math.min(speed, 60.0) * 0.004;
        for (int n = 0; n < crown; n++) {
            final double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            final double out = kick * (0.6 + RANDOM.nextDouble() * 0.6);
            final double r = radius * (0.5 + RANDOM.nextDouble() * 0.5);
            final Particle drop = spawn(ParticleTypes.SPLASH, fx.x() + Math.cos(angle) * r, top + 0.1, fx.z() + Math.sin(angle) * r,
                    Math.cos(angle) * out, out * (1.2 + RANDOM.nextDouble()), Math.sin(angle) * out);
            if (drop != null) {
                drop.setLifetime(20 + RANDOM.nextInt(20));
            }
        }

        // A column of spray shooting up, and mist hanging where it was
        if (magnitude > 1.5f) {
            final int column = amount(20 * (magnitude - 1.0f));
            final double height = Math.min(2.2, 0.35 + 0.25 * (magnitude - 1.5));
            for (int n = 0; n < column; n++) {
                final double spread = radius * 0.4;
                final Particle drop = spawn(ParticleTypes.SPLASH, fx.x() + (RANDOM.nextDouble() - 0.5) * spread, top + 0.2,
                        fx.z() + (RANDOM.nextDouble() - 0.5) * spread,
                        (RANDOM.nextDouble() - 0.5) * 0.12, height * (0.5 + RANDOM.nextDouble() * 0.5), (RANDOM.nextDouble() - 0.5) * 0.12);
                if (drop != null) {
                    drop.setLifetime(30 + RANDOM.nextInt(30));
                }
            }
            final int mist = amount(6 * (magnitude - 1.0f));
            for (int n = 0; n < mist; n++) {
                final Particle cloud = spawn(ParticleTypes.CLOUD, fx.x() + (RANDOM.nextDouble() - 0.5) * radius * 1.5,
                        top + 0.5 + RANDOM.nextDouble() * height * 6.0, fx.z() + (RANDOM.nextDouble() - 0.5) * radius * 1.5,
                        (RANDOM.nextDouble() - 0.5) * 0.04, 0.01 + RANDOM.nextDouble() * 0.02, (RANDOM.nextDouble() - 0.5) * 0.04);
                if (cloud != null) {
                    cloud.setColor(0.92f, 0.95f, 1.0f);
                    cloud.scale(1.5f + RANDOM.nextFloat());
                }
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
     * speed throws spray off the bow and leaves foam.
     */
    private static void wake(final ClientLevel level, final CrashFxPayload.Fx fx) {
        final double speed = Math.sqrt(fx.vx() * fx.vx() + fx.vz() * fx.vz());
        final float width = Math.max(0.5f, fx.magnitude());
        final float surface = WaterWaves.surface(level, fx.x(), fx.y(), fx.z());
        final double top = Float.isNaN(surface) ? fx.y() : surface;

        WaterWaves.push(fx.x(), top, fx.z(), width * 0.9f, (float) Math.min(3.0, 0.3 + speed * 0.5));

        if (speed > 1.5 && RANDOM.nextDouble() < 0.5) {
            spawn(ParticleTypes.FISHING, fx.x() + (RANDOM.nextDouble() - 0.5) * width, top + 0.05, fx.z() + (RANDOM.nextDouble() - 0.5) * width,
                    0.0, 0.0, 0.0);
        }
        if (speed > 4.0) {
            // Spray peeling off both sides of the bow
            final int spray = amount(Math.min(8.0, (speed - 4.0) * 0.6));
            final double sideX = -fx.vz() / speed;
            final double sideZ = fx.vx() / speed;
            for (int n = 0; n < spray; n++) {
                final double side = RANDOM.nextBoolean() ? 1.0 : -1.0;
                final double out = 0.06 + speed * 0.012 * RANDOM.nextDouble();
                final Particle drop = spawn(ParticleTypes.SPLASH, fx.x(), top + 0.15, fx.z(),
                        sideX * side * out + fx.vx() / 20.0 * 0.6, 0.12 + speed * 0.01 * RANDOM.nextDouble(), sideZ * side * out + fx.vz() / 20.0 * 0.6);
                if (drop != null) {
                    drop.setLifetime(15 + RANDOM.nextInt(15));
                }
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
     * Metal striking something hard: a spray of white-hot sparks and glowing fragments arcing away.
     */
    private static void sparks(final ClientLevel level, final CrashFxPayload.Fx fx) {
        final float magnitude = Math.max(0.0f, fx.magnitude());
        final int count = amount(10 + 14 * magnitude);
        final double vx = fx.vx() / 20.0;
        final double vy = fx.vy() / 20.0;
        final double vz = fx.vz() / 20.0;
        for (int n = 0; n < count; n++) {
            final double speed = 0.25 + RANDOM.nextDouble() * (0.35 + magnitude * 0.08);
            // Mostly thrown up and along the way it was going, scattered
            final double dx = RANDOM.nextGaussian() * 0.6 + vx * 0.5;
            final double dy = Math.abs(RANDOM.nextGaussian()) * 0.7 + 0.2;
            final double dz = RANDOM.nextGaussian() * 0.6 + vz * 0.5;
            final double length = Math.max(1.0e-3, Math.sqrt(dx * dx + dy * dy + dz * dz));
            final double roll = RANDOM.nextDouble();
            final ParticleOptions type = roll < 0.6 ? ParticleTypes.ELECTRIC_SPARK : roll < 0.85 ? ParticleTypes.LAVA : ParticleTypes.SMALL_FLAME;
            final Particle spark = spawn(type, fx.x(), fx.y(), fx.z(), dx / length * speed, dy / length * speed + vy * 0.2, dz / length * speed);
            if (spark != null) {
                if (type == ParticleTypes.ELECTRIC_SPARK) {
                    spark.setColor(1.0f, 0.75f + RANDOM.nextFloat() * 0.2f, 0.35f);
                    spark.setLifetime(6 + RANDOM.nextInt(10));
                } else if (type == ParticleTypes.LAVA) {
                    spark.scale(0.4f);
                }
            }
        }
        if (magnitude > 3.0f) {
            spawn(ParticleTypes.FLASH, fx.x(), fx.y(), fx.z(), 0.0, 0.0, 0.0);
        }
    }

    /**
     * A hard landing on loose ground: a ring of dust rolling out low along the ground, clods thrown up, and a cloud of
     * dust rising from the middle.
     */
    private static void dustRing(final ClientLevel level, final CrashFxPayload.Fx fx) {
        final float magnitude = Math.max(0.0f, fx.magnitude());
        final BlockState ground = Block.stateById(fx.data());
        final int mapColor = ground.getMapColor(level, BlockPos.containing(fx.x(), fx.y(), fx.z())).col;
        final float r = Mth.lerp(0.35f, ((mapColor >> 16) & 0xFF) / 255.0f, 0.8f);
        final float g = Mth.lerp(0.35f, ((mapColor >> 8) & 0xFF) / 255.0f, 0.75f);
        final float b = Mth.lerp(0.35f, (mapColor & 0xFF) / 255.0f, 0.7f);

        // Rolling out from under the whole vehicle, not just the point that hit
        final double start = Math.max(0.8, fx.vx());
        final int ring = amount((16 + 12 * magnitude) * Math.max(1.0, start / 3.0));
        final double outward = 0.25 + 0.08 * magnitude;
        for (int n = 0; n < ring; n++) {
            final double angle = (n + RANDOM.nextDouble() * 0.5) / ring * Math.PI * 2.0;
            final double speed = outward * (0.7 + RANDOM.nextDouble() * 0.5);
            final Particle dust = spawn(ParticleTypes.CLOUD, fx.x() + Math.cos(angle) * start, fx.y() + 0.3, fx.z() + Math.sin(angle) * start,
                    Math.cos(angle) * speed, 0.01 + RANDOM.nextDouble() * 0.03, Math.sin(angle) * speed);
            if (dust != null) {
                dust.setColor(r, g, b);
                dust.scale(3.0f + RANDOM.nextFloat() * (2.0f + magnitude * 0.6f));
            }
        }

        if (!ground.isAir()) {
            final BlockParticleOption clods = new BlockParticleOption(ParticleTypes.BLOCK, ground);
            final int count = amount(10 + 10 * magnitude);
            for (int n = 0; n < count; n++) {
                final double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                final double out = 0.1 + RANDOM.nextDouble() * (0.15 + magnitude * 0.05);
                spawn(clods, fx.x(), fx.y() + 0.5, fx.z(), Math.cos(angle) * out, 0.2 + RANDOM.nextDouble() * (0.3 + magnitude * 0.06), Math.sin(angle) * out);
            }
        }

        final int plume = amount(3 + 3 * magnitude);
        for (int n = 0; n < plume; n++) {
            final double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            final Particle cloud = spawn(ParticleTypes.CAMPFIRE_COSY_SMOKE, fx.x() + Math.cos(angle) * start, fx.y() + 0.5,
                    fx.z() + Math.sin(angle) * start, (RANDOM.nextDouble() - 0.5) * 0.05, 0.03 + RANDOM.nextDouble() * 0.04,
                    (RANDOM.nextDouble() - 0.5) * 0.05);
            if (cloud != null) {
                cloud.setColor(r, g, b);
            }
        }
    }

    /**
     * Dust knocked off a breaking block.
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
            SMOKE.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }

        // Wrecks smoulder: smoke drifting up for a while, thinning out
        for (final Iterator<Smoke> iterator = SMOKE.iterator(); iterator.hasNext(); ) {
            final Smoke smoke = iterator.next();
            if (++smoke.age > smoke.duration) {
                iterator.remove();
                continue;
            }
            final float left = 1.0f - (float) smoke.age / smoke.duration;
            final float rate = smoke.strength * left * (float) CrashClientConfig.PARTICLE_AMOUNT.getAsDouble();
            if (RANDOM.nextFloat() < rate * 0.5f) {
                final Particle particle = spawn(ParticleTypes.CAMPFIRE_COSY_SMOKE, smoke.x + (RANDOM.nextDouble() - 0.5) * 2.0 * smoke.radius,
                        smoke.y + RANDOM.nextDouble(), smoke.z + (RANDOM.nextDouble() - 0.5) * 2.0 * smoke.radius, (RANDOM.nextDouble() - 0.5) * 0.02, 0.04 + RANDOM.nextDouble() * 0.03,
                        (RANDOM.nextDouble() - 0.5) * 0.02);
                if (particle != null) {
                    final float grey = 0.25f + RANDOM.nextFloat() * 0.25f;
                    particle.setColor(grey, grey, grey);
                }
            }
            if (smoke.strength > 1.5f && smoke.age % 12 == 0 && left > 0.4f) {
                // A tall column, visible from far away
                spawn(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, smoke.x, smoke.y + 0.5, smoke.z, 0.0, 0.07, 0.0);
            }
            if (RANDOM.nextFloat() < rate * 0.3f) {
                spawn(ParticleTypes.LARGE_SMOKE, smoke.x + (RANDOM.nextDouble() - 0.5) * 2.0 * smoke.radius, smoke.y + 0.2,
                        smoke.z + (RANDOM.nextDouble() - 0.5) * 2.0 * smoke.radius,
                        0.0, 0.02, 0.0);
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
