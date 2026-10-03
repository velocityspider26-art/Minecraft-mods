package dev.velocityspider.caw.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Tight runtime jet plume.
 *
 * The old effect was intentionally simple and looked like smoke puffs. This version instead
 * builds a narrow, fast, almost-transparent exhaust core with sparse expansion haze. At high
 * spool the core develops alternating bright/faint sections to hint at shock diamonds without
 * turning into a giant orange sci-fi flame.
 */
public final class ExhaustPlume {
    private static final DustParticleOptions HOT_CORE =
            new DustParticleOptions(new Vector3f(0.82f, 0.90f, 1.00f), 0.48f);
    private static final DustParticleOptions COOL_CORE =
            new DustParticleOptions(new Vector3f(0.66f, 0.74f, 0.82f), 0.34f);

    private ExhaustPlume() {}

    public static void emit(ServerLevel level, BlockPos nozzlePos, Direction exhaustDirection, double spool) {
        if (spool < 0.10) {
            return;
        }

        Vec3 axis = Vec3.atLowerCornerOf(exhaustDirection.getNormal()).normalize();
        Vec3 origin = Vec3.atCenterOf(nozzlePos).add(axis.scale(0.62));

        // A clean turbine exhaust is mostly invisible. Keep the effect narrow and let power
        // increase its length rather than simply filling the screen with smoke.
        double length = 0.45 + 3.35 * spool;
        int coreSegments = 4 + (int) Math.floor(spool * 10.0);

        for (int i = 0; i < coreSegments; i++) {
            double t = (double) i / Math.max(1, coreSegments - 1);
            Vec3 point = origin.add(axis.scale(length * t));

            // Alternating density at high spool gives a subtle shock-cell pattern.
            boolean diamond = spool > 0.72 && ((i / 2) & 1) == 0;
            DustParticleOptions core = diamond ? HOT_CORE : COOL_CORE;

            double radial = 0.006 + (0.018 * t);
            level.sendParticles(
                    core,
                    point.x, point.y, point.z,
                    1,
                    radial, radial, radial,
                    0.0
            );
        }

        // Very sparse cool haze around the stream. No giant cloud train.
        int hazeSegments = 1 + (int) Math.floor(spool * 3.0);
        for (int i = 0; i < hazeSegments; i++) {
            double t = (i + 1.0) / (hazeSegments + 1.0);
            Vec3 point = origin.add(axis.scale(length * t));
            double spread = 0.015 + 0.055 * t;

            level.sendParticles(
                    ParticleTypes.WHITE_ASH,
                    point.x, point.y, point.z,
                    1,
                    spread, spread, spread,
                    0.002 + 0.004 * spool
            );
        }

        // Dirty exhaust only appears at low/intermediate spool. Once the engine is hot and
        // running hard, the visible smoke almost disappears.
        if (spool > 0.18 && spool < 0.58 && level.random.nextFloat() < 0.18f) {
            Vec3 smokePoint = origin.add(axis.scale(0.35 + level.random.nextDouble() * 0.45));
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    smokePoint.x, smokePoint.y, smokePoint.z,
                    1,
                    0.018, 0.018, 0.018,
                    0.004
            );
        }
    }
}
