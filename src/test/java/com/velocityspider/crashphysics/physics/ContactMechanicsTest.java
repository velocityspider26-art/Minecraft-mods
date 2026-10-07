package com.velocityspider.crashphysics.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContactMechanicsTest {

    private static final MaterialProfile RIGID_NOSE = new MaterialProfile(7800.0, 1e9, 1e8, false);
    private static final MaterialProfile DIRT = new MaterialProfile(1400.0, 0.3e6, 0.03e6, false);

    @Test
    void gentleTouchdownStaysElastic() {
        // A 5 t share of a ship touching dirt at 1 m/s
        assertFalse(ContactMechanics.exceedsElasticLimit(5000.0, 1.0, DIRT.strength(), 1.0, 0.02));
    }

    @Test
    void hardImpactYields() {
        assertTrue(ContactMechanics.exceedsElasticLimit(5000.0, 10.0, DIRT.strength(), 1.0, 0.02));
    }

    @Test
    void broadContactIsGentlerPerBlockThanAPointImpact() {
        final double shipMass = 50_000.0;
        final double speed = 1.5;
        // Nose-first: one block takes the whole ship
        assertTrue(ContactMechanics.exceedsElasticLimit(shipMass, speed, DIRT.strength(), 1.0, 0.02));
        // Belly landing: 20 blocks share it
        assertFalse(ContactMechanics.exceedsElasticLimit(shipMass / 20.0, speed, DIRT.strength(), 1.0, 0.02));
    }

    @Test
    void contactThatStopsWithinTheStepIsCappedAtTheStoppingImpulse() {
        final double mass = 100.0;
        final double v = 5.0;
        final ContactMechanics.Step step = ContactMechanics.integrate(RIGID_NOSE, DIRT, 1.0, 0.05, mass, v);

        assertTrue(step.stopped());
        assertEquals(mass * v, step.impulse(), 1e-9);
        // It dissipates (almost) all of its kinetic energy
        assertEquals(ContactMechanics.kineticEnergy(mass, v), step.dissipatedEnergy(), 1e-6);
        assertTrue(step.targetErosion() < v * 0.05);
    }

    /**
     * A rigid penetrator slowing down in soil obeys m·dv/dt = −A·(R + ½·ρ·v²), whose exact penetration depth is
     * (m / (A·ρ))·ln((R + ½·ρ·v0²) / R). Stepping the contact model at Sable's sub-step size should reproduce it.
     */
    @Test
    void steppedPenetrationMatchesTheAnalyticalDepth() {
        final double mass = 5000.0;
        final double area = 1.0;
        final double v0 = 40.0;
        final double dt = 1.0 / 80.0;

        double v = v0;
        double depth = 0.0;
        for (int i = 0; i < 10_000 && v > 1e-6; i++) {
            final ContactMechanics.Step step = ContactMechanics.integrate(RIGID_NOSE, DIRT, area, dt, mass, v);
            depth += step.targetErosion();
            v -= step.impulse() / mass;
            if (step.stopped()) {
                break;
            }
        }

        final double expected = mass / (area * DIRT.density()) * Math.log((DIRT.strength() + 0.5 * DIRT.density() * v0 * v0) / DIRT.strength());
        assertEquals(expected, depth, 0.01 * expected, "penetration depth");
    }

    @Test
    void energyIsConservedAcrossTheStep() {
        final double mass = 8000.0;
        final double v = 25.0;
        final ContactMechanics.Step step = ContactMechanics.integrate(RIGID_NOSE, DIRT, 1.0, 1.0 / 80.0, mass, v);
        final double endSpeed = v - step.impulse() / mass;

        assertEquals(ContactMechanics.kineticEnergy(mass, v) - ContactMechanics.kineticEnergy(mass, endSpeed), step.dissipatedEnergy(), 1e-6);
        assertFalse(step.stopped());
    }

    @Test
    void wallThatHoldsBringsTheContactToRestInsideTheStep() {
        // An 8 t share of an iron ram hitting a brick wall at 10 m/s: the bricks crush a few centimetres and it stops
        final MaterialProfile bricks = new MaterialProfile(2300.0, 1.2e7, 1.5e6, false);
        final ContactMechanics.Step step = ContactMechanics.integrate(RIGID_NOSE, bricks, 1.0, 0.025, 8000.0, 10.0, 0.35, Double.POSITIVE_INFINITY);

        assertTrue(step.stopped());
        assertFalse(step.targetFailed());
        assertFalse(step.goesThrough());
        assertEquals(8000.0 * 10.0, step.impulse(), 1e-6);
        // Work done equals the kinetic energy: crush depth ≈ ½·m·v² / (P·A)
        assertEquals(0.5 * 8000.0 * 100.0 / (1.2e7 + 0.5 * 2300.0 * 100.0), step.targetErosion(), 0.005);
    }

    @Test
    void blockThatIsCrushedThroughStopsResistingAndTheRestCarriesOn() {
        final double mass = 20_000.0;
        final double v = 20.0;
        final ContactMechanics.Step unlimited = ContactMechanics.integrate(RIGID_NOSE, DIRT, 1.0, 0.025, mass, v);
        final ContactMechanics.Step limited = ContactMechanics.integrate(RIGID_NOSE, DIRT, 1.0, 0.025, mass, v, 0.1, Double.POSITIVE_INFINITY);

        assertTrue(unlimited.targetErosion() > 0.3);
        assertTrue(limited.targetFailed());
        assertTrue(limited.goesThrough());
        assertEquals(0.1, limited.targetErosion(), 1e-9);
        // Only the part before the block gave way slowed it down
        assertTrue(limited.impulse() > 0.0 && limited.impulse() < 0.5 * unlimited.impulse());
        final double endSpeed = v - limited.impulse() / mass;
        assertEquals(ContactMechanics.kineticEnergy(mass, v) - ContactMechanics.kineticEnergy(mass, endSpeed), limited.dissipatedEnergy(), 1e-6);
    }

    @Test
    void hullBlockCanFailBeforeTheGround() {
        final MaterialProfile planks = new MaterialProfile(600.0, 1.5e6, 5e5, false);
        final MaterialProfile rock = new MaterialProfile(2700.0, 5e7, 5e6, true);
        final ContactMechanics.Step step = ContactMechanics.integrate(planks, rock, 1.0, 0.025, 3000.0, 35.0, Double.POSITIVE_INFINITY, 0.2);

        assertTrue(step.projectileFailed());
        assertFalse(step.targetFailed());
        assertEquals(0.0, step.targetErosion());
        assertEquals(0.2, step.projectileErosion(), 1e-9);
    }

    @Test
    void blockWithNothingLeftGivesWayAtOnce() {
        final ContactMechanics.Step step = ContactMechanics.integrate(RIGID_NOSE, DIRT, 1.0, 0.025, 5000.0, 10.0, 0.0, Double.POSITIVE_INFINITY);

        assertTrue(step.targetFailed());
        assertEquals(0.0, step.impulse(), 1e-9);
        assertEquals(0.0, step.targetErosion(), 1e-12);
    }

    @Test
    void reducedMassHandlesStaticTerrain() {
        assertEquals(400.0, ContactMechanics.reducedMass(400.0, Double.POSITIVE_INFINITY));
        assertEquals(400.0, ContactMechanics.reducedMass(Double.POSITIVE_INFINITY, 400.0));
        assertEquals(200.0, ContactMechanics.reducedMass(400.0, 400.0), 1e-12);
    }
}
