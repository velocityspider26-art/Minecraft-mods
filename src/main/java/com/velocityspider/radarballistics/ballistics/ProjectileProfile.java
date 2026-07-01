package com.velocityspider.radarballistics.ballistics;

/**
 * The physical flight model of a projectile: how fast it leaves the barrel and how gravity
 * and air drag act on it every tick. The model and its units are chosen to match Create:
 * Big Cannons exactly, so the solver's answers agree with where CBC actually flies the shell.
 *
 * <p>Minecraft and CBC integrate projectile motion with a fixed one-tick timestep, in this
 * order (see CBC's projectile tick / the Create Radar {@code CBCBallistics} model):</p>
 *
 * <pre>
 *   position += velocity          // move first, at the current velocity
 *   velocity.y -= gravity         // then apply gravity for the next tick
 *   velocity = applyDrag(velocity)// then air resistance
 * </pre>
 *
 * <p>Drag matches CBC's {@code BallisticPropertiesComponent}: a coefficient used either
 * linearly ({@code v *= 1 - drag}) or quadratically ({@code v *= 1 / (1 + drag*|v|)}),
 * selected by {@link #quadraticDrag()}. {@code gravity} is stored as a positive downward
 * magnitude in blocks/tick²; CBC exposes it as a negative number, so build from CBC values
 * with {@link #ofCbc}. {@code muzzleSpeed} is blocks/tick.</p>
 *
 * @param muzzleSpeed  initial speed in blocks per tick (must be positive)
 * @param gravity      downward acceleration magnitude in blocks/tick² (non-negative)
 * @param drag         CBC drag coefficient (0 = no drag)
 * @param quadraticDrag whether drag is applied quadratically rather than linearly
 */
public record ProjectileProfile(double muzzleSpeed, double gravity, double drag, boolean quadraticDrag) {

    public ProjectileProfile {
        if (muzzleSpeed <= 0) {
            throw new IllegalArgumentException("muzzleSpeed must be positive: " + muzzleSpeed);
        }
        if (gravity < 0) {
            throw new IllegalArgumentException("gravity must be non-negative: " + gravity);
        }
        if (drag < 0) {
            throw new IllegalArgumentException("drag must be non-negative: " + drag);
        }
    }

    /** Convenience constructor for a linear-drag projectile. */
    public ProjectileProfile(double muzzleSpeed, double gravity, double drag) {
        this(muzzleSpeed, gravity, drag, false);
    }

    /**
     * Builds a profile from raw Create: Big Cannons values, normalising the gravity sign
     * (CBC gravity is negative) to this model's positive-downward convention.
     *
     * @param muzzleSpeed  CBC muzzle speed (blocks/tick)
     * @param cbcGravity   CBC {@code BallisticPropertiesComponent.gravity()} (typically negative)
     * @param drag         CBC {@code BallisticPropertiesComponent.drag()}
     * @param quadraticDrag CBC {@code BallisticPropertiesComponent.isQuadraticDrag()}
     */
    public static ProjectileProfile ofCbc(double muzzleSpeed, double cbcGravity, double drag, boolean quadraticDrag) {
        return new ProjectileProfile(muzzleSpeed, Math.abs(cbcGravity), drag, quadraticDrag);
    }

    /** Applies CBC-style drag to a velocity component pair sharing the given total speed. */
    double dragFactor(double speed) {
        if (drag <= 0) {
            return 1.0;
        }
        if (quadraticDrag) {
            return 1.0 / (1.0 + drag * speed);
        }
        double f = 1.0 - drag;
        return f < 0 ? 0 : f;
    }

    /**
     * Advances a projectile state by a single tick using the model documented above.
     *
     * @param position current position
     * @param velocity current velocity (blocks/tick)
     * @return the projectile state after one tick
     */
    public State step(Vec3d position, Vec3d velocity) {
        // Move first, at the current velocity.
        Vec3d newPos = position.add(velocity);
        // Gravity for the next tick.
        double vx = velocity.x();
        double vy = velocity.y() - gravity;
        double vz = velocity.z();
        // Then drag, based on the post-gravity speed.
        double f = dragFactor(Math.sqrt(vx * vx + vy * vy + vz * vz));
        return new State(newPos, new Vec3d(vx * f, vy * f, vz * f));
    }

    /** Immutable snapshot of a projectile mid-flight. */
    public record State(Vec3d position, Vec3d velocity) {
    }
}
