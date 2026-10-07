package com.velocityspider.crashphysics.physics;

/**
 * How crash accelerations affect the people (and mobs) riding a vehicle.
 * <p>
 * Restrained occupants survive brief decelerations of roughly 20-40 g, while anyone standing loose is thrown around at
 * a fraction of that. Damage grows quickly once the tolerance is exceeded.
 */
public final class CrewLoads {

    public static final double STANDARD_GRAVITY = 9.80665;

    private CrewLoads() {
    }

    /**
     * Damage dealt by a short acceleration pulse.
     *
     * @param gForce     the peak acceleration felt [g]
     * @param toleranceG the acceleration that can be survived without injury [g]
     * @param scale      damage at twice the tolerance [hit points]
     * @return the damage [hit points]
     */
    public static double damage(final double gForce, final double toleranceG, final double scale) {
        if (!(toleranceG > 0.0) || !(gForce > toleranceG)) {
            return 0.0;
        }

        final double overload = gForce / toleranceG - 1.0;
        return scale * Math.pow(overload, 1.5);
    }

    /**
     * Converts a proper acceleration (what an accelerometer would read) into g.
     *
     * @param ax acceleration x [m/s²]
     * @param ay acceleration y [m/s²]
     * @param az acceleration z [m/s²]
     */
    public static double toG(final double ax, final double ay, final double az) {
        return Math.sqrt(ax * ax + ay * ay + az * az) / STANDARD_GRAVITY;
    }
}
