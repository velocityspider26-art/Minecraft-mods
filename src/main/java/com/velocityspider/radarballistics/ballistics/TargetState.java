package com.velocityspider.radarballistics.ballistics;

/**
 * The kinematic state of a tracked target at the moment of the firing solution.
 *
 * <p>Position is where the target is now; velocity is how far it moves each tick; and
 * acceleration lets the predictor model non-constant motion (a falling contraption, a
 * mob under gravity, a vehicle speeding up). The predicted position after {@code t} ticks
 * is the standard kinematic extrapolation:</p>
 *
 * <pre>
 *   predicted(t) = position + velocity * t + 0.5 * acceleration * t^2
 * </pre>
 *
 * <p>Velocities are in blocks/tick and accelerations in blocks/tick². For a target moving
 * at a constant velocity, pass {@link Vec3d#ZERO} for acceleration.</p>
 */
public record TargetState(Vec3d position, Vec3d velocity, Vec3d acceleration) {

    /** Convenience constructor for a target moving at constant velocity (no acceleration). */
    public TargetState(Vec3d position, Vec3d velocity) {
        this(position, velocity, Vec3d.ZERO);
    }

    /** A stationary target. */
    public static TargetState stationary(Vec3d position) {
        return new TargetState(position, Vec3d.ZERO, Vec3d.ZERO);
    }

    /**
     * Predicts where the target will be after {@code ticks} ticks under constant
     * acceleration.
     *
     * @param ticks elapsed time in ticks (may be fractional)
     * @return the extrapolated position
     */
    public Vec3d predict(double ticks) {
        return position
                .add(velocity.scale(ticks))
                .add(acceleration.scale(0.5 * ticks * ticks));
    }
}
