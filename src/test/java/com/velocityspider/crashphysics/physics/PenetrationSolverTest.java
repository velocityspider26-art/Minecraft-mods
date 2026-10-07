package com.velocityspider.crashphysics.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PenetrationSolverTest {

    private static final MaterialProfile SHIP_PLANKS = new MaterialProfile(500.0, 1.5e6, 0.5e6, false);
    private static final MaterialProfile DIRT = new MaterialProfile(1400.0, 0.3e6, 0.03e6, false);
    private static final MaterialProfile STONE = new MaterialProfile(2600.0, 50e6, 5e6, false);

    @Test
    void strongVehicleDrillsIntoSoftGroundWithoutBeingCrushed() {
        final PenetrationSolver.Interface iface = PenetrationSolver.solve(30.0, SHIP_PLANKS, DIRT);

        assertEquals(30.0, iface.targetErosionSpeed(), 1e-9);
        assertEquals(0.0, iface.projectileErosionSpeed(), 1e-9);
        assertEquals(0.3e6 + 0.5 * 1400.0 * 900.0, iface.pressure(), 1e-3);
    }

    @Test
    void weakVehicleIsCrushedAgainstRock() {
        final PenetrationSolver.Interface iface = PenetrationSolver.solve(30.0, SHIP_PLANKS, STONE);

        assertEquals(0.0, iface.targetErosionSpeed(), 1e-9);
        assertEquals(30.0, iface.projectileErosionSpeed(), 1e-9);
        assertEquals(1.5e6 + 0.5 * 500.0 * 900.0, iface.pressure(), 1e-3);
    }

    @Test
    void atHighSpeedSoftGroundResistsLikeAWallAndBothSidesErode() {
        final double v = 60.0;
        final PenetrationSolver.Interface iface = PenetrationSolver.solve(v, SHIP_PLANKS, DIRT);
        final double u = iface.targetErosionSpeed();

        assertTrue(u > 0.0 && u < v, "both sides should erode, u = " + u);
        assertEquals(v - u, iface.projectileErosionSpeed(), 1e-9);

        // Pressure balance at the interface
        final double projectileSide = 0.5 * SHIP_PLANKS.density() * (v - u) * (v - u) + SHIP_PLANKS.strength();
        final double targetSide = 0.5 * DIRT.density() * u * u + DIRT.strength();
        assertEquals(projectileSide, targetSide, 1e-6 * targetSide);
        assertEquals(targetSide, iface.pressure(), 1e-6 * targetSide);
    }

    @Test
    void equalDensitiesUseTheLinearSolution() {
        final MaterialProfile a = new MaterialProfile(1000.0, 2e6, 1e5, false);
        final MaterialProfile b = new MaterialProfile(1000.0, 1e6, 1e5, false);
        final double v = 100.0;
        final PenetrationSolver.Interface iface = PenetrationSolver.solve(v, a, b);

        // ½ρ(v² − 2vu) + Y − R = 0  =>  u = v/2 + (Y − R)/(ρv)
        assertEquals(v / 2.0 + (2e6 - 1e6) / (1000.0 * v), iface.targetErosionSpeed(), 1e-9);
    }

    @Test
    void interfaceSpeedIsContinuousAcrossTheRigidProjectileBoundary() {
        // Find the speed where the target's resistance at full speed equals the projectile strength
        final double boundary = Math.sqrt(2.0 * (SHIP_PLANKS.strength() - DIRT.strength()) / DIRT.density());

        final PenetrationSolver.Interface below = PenetrationSolver.solve(boundary * 0.999, SHIP_PLANKS, DIRT);
        final PenetrationSolver.Interface above = PenetrationSolver.solve(boundary * 1.001, SHIP_PLANKS, DIRT);

        assertEquals(below.targetErosionSpeed(), boundary * 0.999, 1e-9);
        assertEquals(boundary * 1.001, above.targetErosionSpeed(), 0.01 * boundary);
        assertTrue(above.projectileErosionSpeed() < 0.01 * boundary);
    }

    @Test
    void unbreakableAgainstUnbreakableNeverYields() {
        final PenetrationSolver.Interface iface = PenetrationSolver.solve(80.0, MaterialProfile.UNBREAKABLE, MaterialProfile.UNBREAKABLE);

        assertFalse(iface.yielding());
        assertTrue(Double.isInfinite(iface.pressure()));
    }

    @Test
    void unbreakableTargetCrushesTheProjectile() {
        final PenetrationSolver.Interface iface = PenetrationSolver.solve(20.0, SHIP_PLANKS, MaterialProfile.UNBREAKABLE);

        assertEquals(0.0, iface.targetErosionSpeed());
        assertEquals(20.0, iface.projectileErosionSpeed());
    }

    @Test
    void noMotionMeansNoInteraction() {
        assertFalse(PenetrationSolver.solve(0.0, SHIP_PLANKS, DIRT).yielding());
        assertFalse(PenetrationSolver.solve(-5.0, SHIP_PLANKS, DIRT).yielding());
    }
}
