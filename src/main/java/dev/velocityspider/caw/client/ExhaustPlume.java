package dev.velocityspider.caw.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight runtime exhaust for the first test build.
 * The final renderer can replace this with a proper plume/heat-haze shader later.
 */
public final class ExhaustPlume {
    private ExhaustPlume() {}

    public static void emit(ServerLevel level, BlockPos nozzlePos, Direction direction, double spool) {
        if (spool < 0.12) {
            return;
        }

        Vec3 axis = Vec3.atLowerCornerOf(direction.getNormal());
        Vec3 origin = Vec3.atCenterOf(nozzlePos).add(axis.scale(0.58));

        double speed = 0.04 + spool * 0.10;
        int cloudCount = spool > 0.65 ? 2 : 1;

        level.sendParticles(
                ParticleTypes.CLOUD,
                origin.x, origin.y, origin.z,
                cloudCount,
                0.035, 0.035, 0.035,
                speed
        );

        if (spool > 0.35) {
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    origin.x, origin.y, origin.z,
                    1,
                    0.025, 0.025, 0.025,
                    speed * 0.65
            );
        }
    }
}
