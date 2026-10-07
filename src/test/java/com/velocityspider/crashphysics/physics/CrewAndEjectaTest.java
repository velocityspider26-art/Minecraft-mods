package com.velocityspider.crashphysics.physics;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrewAndEjectaTest {

    @Test
    void noInjuryBelowTolerance() {
        assertEquals(0.0, CrewLoads.damage(19.9, 20.0, 10.0));
    }

    @Test
    void twiceTheToleranceDealsTheScaledDamage() {
        assertEquals(10.0, CrewLoads.damage(40.0, 20.0, 10.0), 1e-9);
        assertTrue(CrewLoads.damage(60.0, 20.0, 10.0) > 20.0);
    }

    @Test
    void oneGIsOneG() {
        assertEquals(1.0, CrewLoads.toG(0.0, CrewLoads.STANDARD_GRAVITY, 0.0), 1e-12);
    }

    @Test
    void ejectaIsThrownUpwardsWithinTheSpeedCap() {
        final SplittableRandom random = new SplittableRandom(42);
        final double[] velocity = new double[3];
        for (int i = 0; i < 1000; i++) {
            Ejecta.launchVelocity(0.0, -1.0, 0.0, 50.0, 0.4, 18.0, random, velocity);
            assertTrue(velocity[1] > 0.0, "ejecta should go up");
            final double speed = Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2]);
            assertTrue(speed <= 18.0 + 1e-9, "speed " + speed);
        }
    }

    @Test
    void slidingCrashThrowsMaterialForward() {
        final SplittableRandom random = new SplittableRandom(7);
        final double[] velocity = new double[3];
        double forward = 0.0;
        for (int i = 0; i < 1000; i++) {
            Ejecta.launchVelocity(1.0, -0.1, 0.0, 40.0, 0.4, 18.0, random, velocity);
            forward += velocity[0];
        }
        assertTrue(forward > 0.0, "a slide should throw a bow wave ahead of the vehicle");
    }
}
