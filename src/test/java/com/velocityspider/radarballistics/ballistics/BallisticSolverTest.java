package com.velocityspider.radarballistics.ballistics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifies the fire-control engine by, for each scenario, computing a solution and then
 * simulating the projectile AND the moving target forward in full 3D. Because projectiles
 * hit-scan along their swept path between ticks, the pass criterion is the closest approach
 * of the continuous projectile path to the moving target &mdash; which should be a small
 * fraction of a block for a good solution.
 */
class BallisticSolverTest {

    private static final ProjectileProfile SHELL = new ProjectileProfile(8.0, 0.05, 0.01);
    private final BallisticSolver solver = BallisticSolver.standard();

    /** Simulates both bodies forward and returns the minimum swept-path miss distance. */
    private double closestApproach(Vec3d launcher, TargetState target,
                                   ProjectileProfile proj, FireSolution sol) {
        Vec3d pPos = launcher;
        Vec3d pVel = sol.launchVelocity();
        double min = Double.MAX_VALUE;
        int flightTicks = (int) Math.ceil(sol.timeToImpactTicks()) + 5;
        final int sub = 50;
        for (int t = 0; t < flightTicks; t++) {
            ProjectileProfile.State ns = proj.step(pPos, pVel);
            Vec3d pNext = ns.position();
            for (int k = 0; k <= sub; k++) {
                double f = k / (double) sub;
                Vec3d pInterp = pPos.add(pNext.subtract(pPos).scale(f));
                double d = pInterp.distance(target.predict(t + f));
                if (d < min) {
                    min = d;
                }
            }
            pPos = pNext;
            pVel = ns.velocity();
        }
        return min;
    }

    private void assertHits(String name, Vec3d launcher, TargetState target, double tolerance) {
        FireSolution sol = solver.solve(launcher, target, SHELL);
        assertTrue(sol.converged(), name + ": expected a converged solution");
        double miss = closestApproach(launcher, target, SHELL, sol);
        assertTrue(miss < tolerance,
                String.format("%s: miss %.3f exceeded tolerance %.3f", name, miss, tolerance));
    }

    @Test
    void hitsStationaryTargetNearby() {
        assertHits("stationary near",
                new Vec3d(0, 70, 0), TargetState.stationary(new Vec3d(120, 70, 0)), 0.25);
    }

    @Test
    void hitsElevatedStationaryTarget() {
        assertHits("stationary high",
                new Vec3d(0, 64, 0), TargetState.stationary(new Vec3d(80, 110, 40)), 0.25);
    }

    @Test
    void hitsTargetBelow() {
        assertHits("stationary below",
                new Vec3d(0, 120, 0), TargetState.stationary(new Vec3d(150, 64, -30)), 0.25);
    }

    @Test
    void leadsFastCrossingTarget() {
        // The exact case naive auto-tracking whiffs on: a target moving across the line of fire.
        assertHits("crossing target",
                new Vec3d(0, 70, 0),
                new TargetState(new Vec3d(150, 75, 0), new Vec3d(0, 0, 0.8)), 0.25);
    }

    @Test
    void leadsIncomingTarget() {
        assertHits("incoming target",
                new Vec3d(0, 70, 0),
                new TargetState(new Vec3d(200, 90, 0), new Vec3d(-0.6, 0, 0)), 0.25);
    }

    @Test
    void leadsClimbingAndRecedingTarget() {
        assertHits("climbing away",
                new Vec3d(0, 70, 0),
                new TargetState(new Vec3d(60, 80, 60), new Vec3d(0.5, 0.25, 0.4)), 0.25);
    }

    @Test
    void leadsAcceleratingFallingTarget() {
        // Continuous vs. discrete acceleration introduces a small unavoidable error; allow more.
        assertHits("falling target",
                new Vec3d(0, 70, 0),
                new TargetState(new Vec3d(140, 130, 20), new Vec3d(0.3, -0.2, 0),
                        new Vec3d(0, -0.04, 0)), 0.5);
    }

    @Test
    void leadsFastCrosserWithSlowShell() {
        ProjectileProfile slow = new ProjectileProfile(4.0, 0.05, 0.02);
        Vec3d launcher = new Vec3d(0, 70, 0);
        TargetState target = new TargetState(new Vec3d(90, 72, 0), new Vec3d(0, 0, 0.9));
        FireSolution sol = solver.solve(launcher, target, slow);
        assertTrue(sol.converged(), "slow shell: expected convergence");
        double miss = closestApproach(launcher, target, slow, sol);
        assertTrue(miss < 0.25, "slow shell miss too large: " + miss);
    }

    @Test
    void leadsCrossingTargetWithQuadraticDrag() {
        ProjectileProfile quad = new ProjectileProfile(9.0, 0.05, 0.01, true);
        Vec3d launcher = new Vec3d(0, 70, 0);
        TargetState target = new TargetState(new Vec3d(130, 78, 0), new Vec3d(0, 0, 0.7));
        FireSolution sol = solver.solve(launcher, target, quad);
        assertTrue(sol.converged(), "quadratic-drag shell: expected convergence");
        double miss = closestApproach(launcher, target, quad, sol);
        assertTrue(miss < 0.25, "quadratic-drag miss too large: " + miss);
    }

    @Test
    void reportsOutOfRangeTargetAsUnsolved() {
        FireSolution sol = solver.solve(new Vec3d(0, 70, 0),
                TargetState.stationary(new Vec3d(5000, 70, 0)), SHELL);
        assertFalse(sol.converged(), "a target 5000 blocks away must not converge");
    }

    @Test
    void yawPointsAtTheTarget() {
        // A target due east (+X) should give a Minecraft yaw of -90 (west is +90, east is -90).
        FireSolution sol = solver.solve(new Vec3d(0, 70, 0),
                TargetState.stationary(new Vec3d(100, 70, 0)), SHELL);
        assertTrue(sol.converged());
        assertTrue(Math.abs(sol.yawDegrees() - (-90.0)) < 0.5,
                "expected yaw ~-90 for a due-east target, got " + sol.yawDegrees());
    }
}
