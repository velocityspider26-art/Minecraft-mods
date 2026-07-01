package com.velocityspider.radarballistics.ballistics;

/**
 * The result of a firing-solution computation.
 *
 * <p>When {@link #converged()} is {@code true}, aiming the launcher at
 * {@link #yawDegrees()}/{@link #pitchDegrees()} (Minecraft convention) and firing will put
 * the projectile at {@link #aimPoint()} at the same tick the target arrives there. When it
 * is {@code false} the target is out of range or the solver could not converge, and the
 * aim fields hold the best effort achieved so far.</p>
 *
 * @param converged          whether a valid intercept was found
 * @param yawDegrees         launch yaw in Minecraft convention (0 = +Z/south, clockwise)
 * @param pitchDegrees       launch pitch in Minecraft convention (negative = aiming up)
 * @param elevationDegrees   barrel elevation above the horizon (positive = up); handy for
 *                           cannon mounts that think in terms of elevation rather than pitch
 * @param timeToImpactTicks  ticks between firing and impact
 * @param aimPoint           the predicted intercept point in world space
 * @param launchVelocity     the full initial velocity vector to give the projectile
 * @param impactAngleDegrees the projectile's descent angle below horizontal at impact
 * @param iterations         outer refinement iterations the solver used (diagnostic)
 */
public record FireSolution(
        boolean converged,
        double yawDegrees,
        double pitchDegrees,
        double elevationDegrees,
        double timeToImpactTicks,
        Vec3d aimPoint,
        Vec3d launchVelocity,
        double impactAngleDegrees,
        int iterations) {

    /** A sentinel returned when no solution exists (e.g. target beyond maximum range). */
    public static FireSolution unsolved(int iterations) {
        return new FireSolution(false, 0, 0, 0, 0, Vec3d.ZERO, Vec3d.ZERO, 0, iterations);
    }
}
