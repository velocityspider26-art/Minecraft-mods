package com.velocityspider.radarballistics.ballistics;

/**
 * The physical flight model of a projectile: how fast it leaves the barrel and how
 * gravity and air drag act on it every tick.
 *
 * <p>Minecraft (and Create: Big Cannons) integrate projectile motion with a fixed
 * timestep of one tick. This engine reproduces that discrete update exactly rather than
 * using a continuous closed-form parabola, because with drag present the two disagree and
 * the discrete version is what the game actually simulates. The per-tick update applied is:</p>
 *
 * <pre>
 *   velocity.y -= gravity        // gravity pulls down first
 *   velocity   *= drag           // then air resistance scales the whole vector
 *   position   += velocity       // finally the projectile moves
 * </pre>
 *
 * <p>{@code gravity} is in blocks/tick² and {@code drag} is a dimensionless per-tick
 * multiplier in (0, 1] where 1.0 means no drag. Defaults approximate a Create: Big Cannons
 * shell; tune them via config to match a specific projectile.</p>
 *
 * @param muzzleSpeed the projectile's initial speed in blocks per tick
 * @param gravity     downward acceleration in blocks per tick squared
 * @param drag        per-tick velocity multiplier in (0, 1]
 */
public record ProjectileProfile(double muzzleSpeed, double gravity, double drag) {

    public ProjectileProfile {
        if (muzzleSpeed <= 0) {
            throw new IllegalArgumentException("muzzleSpeed must be positive: " + muzzleSpeed);
        }
        if (gravity < 0) {
            throw new IllegalArgumentException("gravity must be non-negative: " + gravity);
        }
        if (drag <= 0 || drag > 1) {
            throw new IllegalArgumentException("drag must be in (0, 1]: " + drag);
        }
    }

    /**
     * Advances a projectile state by a single tick using the model documented above.
     *
     * @param position current position
     * @param velocity current velocity (blocks/tick)
     * @return the projectile state after one tick
     */
    public State step(Vec3d position, Vec3d velocity) {
        double vx = velocity.x();
        double vy = velocity.y() - gravity;
        double vz = velocity.z();
        vx *= drag;
        vy *= drag;
        vz *= drag;
        Vec3d newVel = new Vec3d(vx, vy, vz);
        return new State(position.add(newVel), newVel);
    }

    /** Immutable snapshot of a projectile mid-flight. */
    public record State(Vec3d position, Vec3d velocity) {
    }
}
