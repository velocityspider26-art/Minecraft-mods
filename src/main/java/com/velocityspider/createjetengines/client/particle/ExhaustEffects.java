package com.velocityspider.createjetengines.client.particle;

import com.velocityspider.createjetengines.blockentity.CombustionCoreBlockEntity;
import com.velocityspider.createjetengines.blockentity.JetModuleBlockEntity;
import com.velocityspider.createjetengines.client.SubLevelClientUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Exhaust and intake particles.
 *
 * <p>Every spawn point is computed as a sublevel-local offset first and only then projected into
 * world space, and directions go through the normal transform rather than the position transform.
 * That is what keeps the plume attached to a rolling, pitching aircraft instead of leaving it
 * behind at the block's pre-assembly coordinates.
 */
public final class ExhaustEffects {

    private ExhaustEffects() {
    }

    /**
     * @param nozzle the nozzle module; effects originate from its exhaust face
     */
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
        Vec3 localMouth = pos.getCenter().add(localDir.scale(0.55D));
        Vec3 carrier = SubLevelClientUtil.velocityAt(level, localMouth);

        boolean lit = core.isAfterburnerActive();
        float throttle = core.getThrottle();

        // ---- dry exhaust: always present while running -----------------------------------
        int shimmer = 1 + (int) (spool * 3);
        for (int i = 0; i < shimmer; i++) {
            spawn(level, ParticleTypes.WHITE_ASH, localMouth, worldDir, carrier, rng,
                    0.10D, 0.35D + spool * 0.9D, 0.22D);
        }
        if (rng.nextFloat() < 0.35F + spool * 0.4F) {
            // faint translucent plume body
            spawn(level, ParticleTypes.CLOUD, localMouth.add(localDir.scale(0.25D)),
                    worldDir, carrier, rng, 0.09D, 0.5D + spool * 1.4D, 0.16D);
        }

        // light grey smoke while spooling up, darker on abrupt throttle changes
        if (spool < 0.5F && rng.nextFloat() < 0.30F) {
            spawn(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, localMouth, worldDir, carrier, rng,
                    0.08D, 0.22D, 0.10D);
        }
        if (throttle - spool > 0.25F && rng.nextFloat() < 0.5F) {
            spawn(level, ParticleTypes.LARGE_SMOKE, localMouth, worldDir, carrier, rng,
                    0.10D, 0.30D, 0.12D);
        }

        // condensation in cold or humid air at low power
        if (spool < 0.6F && level.isRaining() && rng.nextFloat() < 0.2F) {
            spawn(level, ParticleTypes.CLOUD, localMouth, worldDir, carrier, rng,
                    0.12D, 0.18D, 0.20D);
        }

        // ---- afterburner --------------------------------------------------------------------
        if (lit) {
            int flames = 3 + (int) (throttle * 6);
            for (int i = 0; i < flames; i++) {
                double t = rng.nextDouble();
                Vec3 at = localMouth.add(localDir.scale(t * 2.6D * (0.4D + 0.6D * throttle)));
                // blue core near the nozzle, orange further out
                ParticleOptions type = t < 0.4D ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
                spawn(level, type, at, worldDir, carrier, rng, 0.05D, 1.6D + throttle * 1.6D, 0.10D);
            }
            // shock diamonds: brighter puffs at regular intervals down the plume
            for (int k = 1; k <= 3; k++) {
                if (rng.nextFloat() > 0.55F) {
                    continue;
                }
                Vec3 at = localMouth.add(localDir.scale(0.55D * k * (0.6D + throttle)));
                spawn(level, ParticleTypes.SOUL_FIRE_FLAME, at, worldDir, carrier, rng,
                        0.03D, 1.2D, 0.04D);
            }
        }

        // ---- intake suction -----------------------------------------------------------------
        var chain = core.getChain();
        if (chain.valid() && chain.fanPos() != null && spool > 0.25F && rng.nextFloat() < spool * 0.5F) {
            Vec3 localIntake = chain.fanPos().getCenter().subtract(localDir.scale(0.75D));
            // pulled inward, i.e. along the exhaust direction
            spawn(level, ParticleTypes.CLOUD, localIntake, worldDir, carrier, rng,
                    0.22D, 0.25D * spool, 0.05D);
        }
    }

    /**
     * Spawns one particle at a sublevel-local point, converting both the point and the velocity
     * into world space, and adding the sublevel's own motion so the plume trails correctly.
     */
    private static void spawn(Level level, ParticleOptions type, Vec3 localPos, Vec3 worldDir,
                              Vec3 carrier, RandomSource rng,
                              double spread, double speed, double jitter) {
        Vec3 world = SubLevelClientUtil.toWorld(level, localPos);
        Vec3 vel = worldDir.scale(speed)
                .add(carrier)
                .add((rng.nextDouble() - 0.5D) * jitter,
                        (rng.nextDouble() - 0.5D) * jitter,
                        (rng.nextDouble() - 0.5D) * jitter);
        level.addParticle(type,
                world.x + (rng.nextDouble() - 0.5D) * spread,
                world.y + (rng.nextDouble() - 0.5D) * spread,
                world.z + (rng.nextDouble() - 0.5D) * spread,
                vel.x, vel.y, vel.z);
    }
}
