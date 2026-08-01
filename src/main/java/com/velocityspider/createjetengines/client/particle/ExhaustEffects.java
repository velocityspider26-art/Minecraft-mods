package com.velocityspider.createjetengines.client.particle;

import com.velocityspider.createjetengines.blockentity.CombustionCoreBlockEntity;
import com.velocityspider.createjetengines.blockentity.JetModuleBlockEntity;
import com.velocityspider.createjetengines.client.SubLevelClientUtil;
import com.velocityspider.createjetengines.registry.JetParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Exhaust and intake effects.
 *
 * <p>Everything is emitted as a cone about the nozzle axis: a point on the nozzle mouth, a velocity
 * along the exhaust direction, and a small perpendicular spread. The sublevel's own velocity is
 * added on top, so the plume trails behind a moving aircraft instead of hanging in the air where it
 * was born.
 *
 * <p>Spawn points are computed in sublevel-local space and only then projected into the world, and
 * directions go through the normal transform rather than the position transform — mixing those two
 * up is what makes a plume appear mirrored or pointing sideways.
 */
public final class ExhaustEffects {

    private ExhaustEffects() {
    }

    public static void emit(JetModuleBlockEntity nozzle, CombustionCoreBlockEntity core, float partialTick) {
        Level level = nozzle.getLevel();
        if (level == null) {
            return;
        }
        float spool = core.getSpool();
        if (spool <= 0.02F) {
            return;
        }

        RandomSource rng = level.getRandom();
        Direction exhaust = nozzle.getFacing();
        BlockPos pos = nozzle.getBlockPos();

        Vec3 localDir = new Vec3(exhaust.getStepX(), exhaust.getStepY(), exhaust.getStepZ());
        Vec3 worldDir = SubLevelClientUtil.dirToWorld(level, pos, localDir).normalize();
        Vec3 mouth = pos.getCenter().add(localDir.scale(0.52D));
        Vec3 carrier = SubLevelClientUtil.velocityAt(level, mouth);

        // perpendicular basis for the cone spread, built in world space
        Vec3 up = Math.abs(worldDir.y) > 0.9D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = worldDir.cross(up).normalize();
        Vec3 other = worldDir.cross(side).normalize();

        boolean lit = core.isAfterburnerActive();
        float throttle = core.getThrottle();
        float nozzleOpen = core.getNozzleOpen();
        // A wide-open nozzle at low power spreads; a tight one at high power collimates.
        double spread = 0.055D + 0.075D * (1.0D - spool) + 0.03D * nozzleOpen;
        // Kept low on purpose: the plume is built by spawning along the axis, so the
        // particles only need enough speed to drift, not to fly downrange.
        double exhaustSpeed = 0.10D + 0.30D * spool;

        // ---- dry exhaust: hot air, always present while running -------------------------
        int haze = 2 + (int) (spool * 5);
        for (int i = 0; i < haze; i++) {
            emitCone(level, JetParticles.EXHAUST_HAZE.get(), mouth, worldDir, side, other, carrier,
                    rng, 0.16D, exhaustSpeed, spread, 0.0D);
        }

        // ---- soot: heavy while spooling up, and on a sharp throttle push -----------------
        boolean spoolingUp = spool < 0.55F;
        boolean richTransient = throttle - spool > 0.22F;
        if (spoolingUp && rng.nextFloat() < 0.45F) {
            emitCone(level, JetParticles.EXHAUST_SOOT.get(), mouth, worldDir, side, other, carrier,
                    rng, 0.14D, exhaustSpeed * 0.55D, spread * 1.5D, 0.0D);
        }
        if (richTransient) {
            for (int i = 0; i < 2; i++) {
                emitCone(level, JetParticles.EXHAUST_SOOT.get(), mouth, worldDir, side, other, carrier,
                        rng, 0.16D, exhaustSpeed * 0.7D, spread * 1.8D, 0.0D);
            }
        }

        // ---- afterburner ------------------------------------------------------------------
        if (lit) {
            double plumeLength = 1.5D + 2.3D * throttle;
            int flames = 7 + (int) (throttle * 11);
            for (int i = 0; i < flames; i++) {
                // Spawn along the plume so the whole column is populated at once, rather than
                // waiting for particles to travel out from the mouth.
                double along = rng.nextDouble() * plumeLength;
                double taper = 1.0D - 0.55D * (along / plumeLength);
                emitCone(level, JetParticles.JET_FLAME.get(), mouth, worldDir, side, other, carrier,
                        rng, 0.13D * taper, exhaustSpeed * 0.8D, spread * 0.55D * taper, along);
            }

            // shock diamonds at regular intervals down the core flow
            double spacing = 0.50D + 0.30D * throttle;
            for (int k = 1; k <= 3; k++) {
                double along = spacing * k;
                if (along > plumeLength * 0.8D || rng.nextFloat() > 0.22F) {
                    continue;
                }
                emitCone(level, JetParticles.SHOCK_DIAMOND.get(), mouth, worldDir, side, other, carrier,
                        rng, 0.015D, exhaustSpeed * 0.35D, 0.006D, along);
            }
        }

        // ---- intake suction ---------------------------------------------------------------
        var chain = core.getChain();
        if (chain.valid() && chain.fanPos() != null && spool > 0.28F && rng.nextFloat() < spool * 0.55F) {
            Vec3 intake = chain.fanPos().getCenter().subtract(localDir.scale(0.85D));
            Vec3 intakeCarrier = SubLevelClientUtil.velocityAt(level, intake);
            // drawn inward, i.e. along the exhaust direction
            emitCone(level, JetParticles.EXHAUST_HAZE.get(), intake, worldDir, side, other,
                    intakeCarrier, rng, 0.38D, 0.18D * spool, 0.02D, 0.0D);
        }
    }

    /**
     * Spawns one particle on the nozzle axis.
     *
     * @param localOrigin spawn point in sublevel-local coordinates
     * @param dir         world-space exhaust direction (unit)
     * @param spread      perpendicular velocity magnitude, i.e. the cone half-angle
     * @param along       distance downstream of the mouth to spawn at
     */
    private static void emitCone(Level level, ParticleOptions type, Vec3 localOrigin,
                                 Vec3 dir, Vec3 side, Vec3 other, Vec3 carrier,
                                 RandomSource rng, double jitter, double speed,
                                 double spread, double along) {
        // The origin is local and must be projected out first; `along` is already a
        // world-space direction, so it is added after the projection.
        Vec3 world = SubLevelClientUtil.toWorld(level, localOrigin).add(dir.scale(along));

        double a = rng.nextDouble() * Math.PI * 2.0D;
        double r = Math.sqrt(rng.nextDouble());
        Vec3 radial = side.scale(Math.cos(a) * r).add(other.scale(Math.sin(a) * r));

        Vec3 spawn = world.add(radial.scale(jitter));
        Vec3 velocity = dir.scale(speed).add(radial.scale(spread)).add(carrier);

        level.addParticle(type, spawn.x, spawn.y, spawn.z, velocity.x, velocity.y, velocity.z);
    }
}
