package com.velocityspider.radarballistics;

import com.velocityspider.radarballistics.ballistics.BallisticSolver;
import com.velocityspider.radarballistics.ballistics.FireSolution;
import com.velocityspider.radarballistics.ballistics.ProjectileProfile;
import com.velocityspider.radarballistics.ballistics.TargetState;
import com.velocityspider.radarballistics.ballistics.Vec3d;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The Minecraft-facing entry point to the fire-control engine. Both the in-game
 * {@code /radarballistics} command and the (optional) Create Radar auto-tracking hook call
 * through here, so the aiming behaviour is identical wherever it is used.
 *
 * <p>This is the single seam where world objects become the plain-data inputs the pure-Java
 * {@link BallisticSolver} understands. Keeping the physics free of Minecraft types is what
 * lets the engine be unit-tested and numerically verified in isolation.</p>
 */
public final class FireControl {

    private FireControl() {
    }

    /** Converts a Minecraft vector to the engine's vector type. */
    public static Vec3d toEngine(Vec3 v) {
        return new Vec3d(v.x, v.y, v.z);
    }

    /** Converts an engine vector back to a Minecraft vector. */
    public static Vec3 toMinecraft(Vec3d v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    /**
     * Computes a firing solution against a live entity, reading its position and per-tick
     * velocity from the world and using the projectile model and solver from {@link Config}.
     *
     * @param launcherPos the muzzle position (where the shell spawns)
     * @param target      the entity to hit
     * @return the firing solution (check {@link FireSolution#converged()})
     */
    public static FireSolution solveForEntity(Vec3 launcherPos, Entity target) {
        Vec3 targetVel = target.getDeltaMovement();
        TargetState state = Config.LEAD_MOVING_TARGETS.get()
                ? new TargetState(toEngine(target.position()), toEngine(targetVel))
                : TargetState.stationary(toEngine(target.position()));
        return solve(toEngine(launcherPos), state);
    }

    /**
     * Computes a firing solution against an explicit target state (position, velocity and
     * optional acceleration already extracted). Use this from the auto-tracking hook where the
     * target might be a contraption rather than an {@link Entity}.
     *
     * @param launcherPos muzzle position in world space
     * @param target      the target's kinematic state
     * @return the firing solution
     */
    public static FireSolution solve(Vec3d launcherPos, TargetState target) {
        ProjectileProfile profile = Config.projectileProfile();
        BallisticSolver solver = Config.solver();
        if (!Config.LEAD_MOVING_TARGETS.get()) {
            target = TargetState.stationary(target.position());
        }
        return solver.solve(launcherPos, target, profile);
    }
}
