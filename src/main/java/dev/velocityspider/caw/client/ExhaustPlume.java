package dev.velocityspider.caw.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Runtime exhaust for the first test build.
 * It deliberately avoids a giant orange flame: the visible plume is mostly pale exhaust
 * and a small amount of smoke, stretched along the nozzle axis.
 */
public final class ExhaustPlume {
    private ExhaustPlume() {}

    public static void emit(ServerLevel level, BlockPos nozzlePos, Direction exhaustDirection, double spool) {
        if (spool < 0.12) {
            return;
        }

        Vec3 axis = Vec3.atLowerCornerOf(exhaustDirection.getNormal());
        Vec3 origin = Vec3.atCenterOf(nozzlePos).add(axis.scale(0.58));

        int segments = 1 + (int) Math.floor(spool * 3.0);
        double length = 0.35 + spool * 1.65;

        for (int i = 0; i < segments; i++) {
            double t = segments == 1 ? 0.0 : (double) i / (segments - 1);
            Vec3 point = origin.add(axis.scale(length * t));
            double spread = 0.018 + t * 0.045;

            level.sendParticles(
                    ParticleTypes.CLOUD,
                    point.x, point.y, point.z,
                    1,
                    spread, spread, spread,
                    0.01 + spool * 0.025
            );
        }

        if (spool > 0.45) {
            Vec3 smokePoint = origin.add(axis.scale(length * 0.8));
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    smokePoint.x, smokePoint.y, smokePoint.z,
                    1,
                    0.03, 0.03, 0.03,
                    0.01 + spool * 0.015
            );
        }
    }
}
