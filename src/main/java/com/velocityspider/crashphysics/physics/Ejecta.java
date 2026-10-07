package com.velocityspider.crashphysics.physics;

import java.util.random.RandomGenerator;

/**
 * Where the material dug out of a crater or trench gets thrown.
 * <p>
 * Material displaced by an impact leaves the crater at a fraction of the penetration speed, mostly upwards and
 * outwards, and biased in the direction the vehicle was travelling (a sliding crash throws a bow wave of dirt ahead of
 * it, a vertical one throws a ring around the crater). It lands around the hole and builds up a rim.
 */
public final class Ejecta {

    private Ejecta() {
    }

    /**
     * Computes a launch velocity for one piece of ejected material.
     *
     * @param motionX          direction the vehicle was moving, x (does not need to be normalised)
     * @param motionY          direction the vehicle was moving, y
     * @param motionZ          direction the vehicle was moving, z
     * @param penetrationSpeed how fast the material was penetrated [m/s]
     * @param speedFactor      fraction of the penetration speed the material leaves with
     * @param maxSpeed         speed cap [m/s]
     * @param random           random source
     * @param out              receives the velocity [m/s], length 3
     */
    public static void launchVelocity(final double motionX, final double motionY, final double motionZ,
                                      final double penetrationSpeed, final double speedFactor, final double maxSpeed,
                                      final RandomGenerator random, final double[] out) {
        double hx = motionX;
        double hz = motionZ;
        final double horizontal = Math.sqrt(hx * hx + hz * hz);
        final double length = Math.sqrt(horizontal * horizontal + motionY * motionY);

        // How much of the motion was along the ground (0 for a vertical dive, 1 for a slide)
        final double slide = length > 1.0e-9 ? horizontal / length : 0.0;
        if (horizontal > 1.0e-9) {
            hx /= horizontal;
            hz /= horizontal;
        }

        // Random spread around a cone; a vertical dive throws material evenly in all directions
        final double angle = random.nextDouble() * Math.PI * 2.0;
        final double spread = 0.35 + 0.65 * (1.0 - slide);
        double dx = hx * slide * 0.9 + Math.cos(angle) * spread * 0.8;
        double dz = hz * slide * 0.9 + Math.sin(angle) * spread * 0.8;
        double dy = 0.7 + random.nextDouble() * 0.6;

        final double norm = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= norm;
        dy /= norm;
        dz /= norm;

        final double speed = Math.min(maxSpeed, penetrationSpeed * speedFactor * (0.45 + random.nextDouble() * 0.75));
        out[0] = dx * speed;
        out[1] = dy * speed;
        out[2] = dz * speed;
    }
}
