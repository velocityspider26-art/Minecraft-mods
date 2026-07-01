package com.velocityspider.radarballistics.ballistics;

/**
 * Computes a lead-and-elevation firing solution for a projectile with gravity and drag
 * against a moving target.
 *
 * <h2>Why the vanilla auto-tracking misses</h2>
 * A naive tracker aims the barrel straight at where the target <em>is now</em>. But the
 * projectile takes time to arrive, and during that flight the target moves and the shell
 * drops under gravity (and loses speed to drag). By the time the shell gets there, the
 * target has left. The fix is to solve for the point where the projectile and the target
 * will meet, then aim there.
 *
 * <h2>The two coupled problems</h2>
 * <ol>
 *   <li><b>Lead:</b> the intercept point depends on the flight time, but the flight time
 *       depends on how far away the intercept point is — a chicken-and-egg loop. We solve
 *       it with a damped fixed-point iteration on the time-to-impact.</li>
 *   <li><b>Elevation:</b> for a given aim point, at what barrel angle does a shell of this
 *       muzzle speed actually pass through that point given gravity and drag? With drag
 *       there is no closed form, so we simulate the discrete per-tick flight and search the
 *       launch pitch with a bracket-and-bisection root find, preferring the flatter (low)
 *       arc.</li>
 * </ol>
 *
 * <p>Because drag acts anti-parallel to velocity and gravity acts vertically, the flight
 * stays in the single vertical plane that contains the launch direction. That lets the
 * elevation search run as a fast 2D (range, height) simulation while yaw is just the
 * horizontal bearing to the aim point — exact, not an approximation.</p>
 *
 * <p>Instances are immutable and thread-safe; the tuning parameters are fixed at
 * construction. Use {@link #standard()} for sensible defaults.</p>
 */
public final class BallisticSolver {

    private final int maxOuterIterations;
    private final double timeToleranceTicks;
    private final int maxFlightTicks;
    private final double leadDamping;

    /**
     * @param maxOuterIterations  cap on lead fixed-point refinements before giving up
     * @param timeToleranceTicks  convergence threshold on the flight-time estimate, in ticks
     * @param maxFlightTicks       hard cap on simulated flight length (out-of-range guard)
     * @param leadDamping          fixed-point relaxation in (0, 1]; lower is more stable but
     *                             slower to converge. 0.6 works well.
     */
    public BallisticSolver(int maxOuterIterations, double timeToleranceTicks,
                           int maxFlightTicks, double leadDamping) {
        this.maxOuterIterations = maxOuterIterations;
        this.timeToleranceTicks = timeToleranceTicks;
        this.maxFlightTicks = maxFlightTicks;
        this.leadDamping = leadDamping;
    }

    /** A solver with defaults suitable for typical cannon ranges (up to a few hundred blocks). */
    public static BallisticSolver standard() {
        return new BallisticSolver(48, 0.25, 20 * 60, 0.6);
    }

    /**
     * Solves for the barrel angles that will hit a moving target.
     *
     * @param launcher the muzzle position (where the projectile spawns)
     * @param target   the target's current kinematic state
     * @param proj     the projectile's flight model
     * @return a converged {@link FireSolution}, or an unconverged one if no intercept exists
     */
    public FireSolution solve(Vec3d launcher, TargetState target, ProjectileProfile proj) {
        // Initial flight-time guess: straight-line distance at muzzle speed. It ignores
        // drop and drag but only needs to seed the iteration.
        double tau = launcher.distance(target.position()) / proj.muzzleSpeed();
        if (!Double.isFinite(tau) || tau <= 0) {
            tau = 1.0;
        }

        Elevation lastElevation = null;
        Vec3d aim = target.position();
        int iteration = 0;

        for (; iteration < maxOuterIterations; iteration++) {
            aim = target.predict(tau);
            double range = launcher.horizontalDistance(aim);
            double height = aim.y() - launcher.y();

            Elevation elev = solveElevation(range, height, proj);
            if (!elev.reachable) {
                // The predicted intercept is beyond what this muzzle speed can reach.
                return FireSolution.unsolved(iteration + 1);
            }
            lastElevation = elev;

            double newTau = elev.timeTicks;
            if (Math.abs(newTau - tau) <= timeToleranceTicks) {
                tau = newTau;
                return buildSolution(launcher, target, proj, tau, elev, iteration + 1);
            }
            // Damped update keeps the loop from oscillating between over/under-shoot guesses.
            tau = tau + leadDamping * (newTau - tau);
        }

        // Ran out of iterations. Return the best effort but flagged unconverged.
        if (lastElevation != null) {
            FireSolution best = buildSolution(launcher, target, proj, tau, lastElevation, iteration);
            return new FireSolution(false, best.yawDegrees(), best.pitchDegrees(),
                    best.elevationDegrees(), best.timeToImpactTicks(), best.aimPoint(),
                    best.launchVelocity(), best.impactAngleDegrees(), iteration);
        }
        return FireSolution.unsolved(iteration);
    }

    private FireSolution buildSolution(Vec3d launcher, TargetState target, ProjectileProfile proj,
                                       double tau, Elevation elev, int iterations) {
        Vec3d aim = target.predict(tau);
        double dx = aim.x() - launcher.x();
        double dz = aim.z() - launcher.z();

        // Minecraft yaw: 0 faces +Z (south) and increases clockwise (toward -X/west).
        double yawDeg = Math.toDegrees(Math.atan2(-dx, dz));
        double elevationDeg = Math.toDegrees(elev.launchAngleRad);
        // Minecraft pitch is positive downward, so aiming up is a negative pitch.
        double pitchDeg = -elevationDeg;

        // Reconstruct the full 3D launch velocity from bearing + elevation.
        double horizLen = Math.hypot(dx, dz);
        double cos = Math.cos(elev.launchAngleRad);
        double sin = Math.sin(elev.launchAngleRad);
        Vec3d horizDir = horizLen < 1.0e-9 ? Vec3d.ZERO : new Vec3d(dx / horizLen, 0, dz / horizLen);
        Vec3d launchVel = new Vec3d(
                horizDir.x() * proj.muzzleSpeed() * cos,
                proj.muzzleSpeed() * sin,
                horizDir.z() * proj.muzzleSpeed() * cos);

        double impactAngleDeg = Math.toDegrees(Math.atan2(-elev.impactVy, elev.impactVh));

        return new FireSolution(true, yawDeg, pitchDeg, elevationDeg, tau, aim,
                launchVel, impactAngleDeg, iterations);
    }

    /**
     * For a target at horizontal distance {@code range} and vertical offset {@code height},
     * finds the launch pitch (radians above horizontal) at which a shell of the given muzzle
     * speed passes through that point, plus the time to get there.
     *
     * <p>We scan the launch angle from steeply down to steeply up, simulate each candidate,
     * and look for where the shell's height as it crosses {@code range} changes from below
     * the target to above it. That sign change brackets the flat (low) arc, which we then
     * refine by bisection. Preferring the low arc gives flatter, faster shots and matches
     * what a gunner would pick.</p>
     */
    private Elevation solveElevation(double range, double height, ProjectileProfile proj) {
        if (range < 1.0e-6) {
            // Straight up/down shot: aim purely vertically toward the target.
            double angle = height >= 0 ? Math.toRadians(89.9) : Math.toRadians(-89.9);
            return simulateVerticalPlane(angle, Math.max(range, 1.0e-6), proj);
        }

        final double lowAngle = Math.toRadians(-85.0);
        final double highAngle = Math.toRadians(85.0);
        final int scanSteps = 340; // ~0.5 degree resolution across the scan range

        // Scan from steeply down to steeply up looking for the flat (low) arc: the first
        // angle interval where the shell's height at the target range rises through the
        // target height. Angles that can't reach the range at all (too steep either way,
        // drag-limited) are simply skipped, which also splits the scan across any gap.
        boolean havePrev = false;
        double prevAngle = 0.0;
        double prevError = 0.0;

        for (int i = 0; i <= scanSteps; i++) {
            double angle = lowAngle + (highAngle - lowAngle) * i / scanSteps;
            Elevation cur = simulateVerticalPlane(angle, range, proj);
            if (!cur.reachable) {
                havePrev = false; // reset: don't bracket across an unreachable gap
                continue;
            }
            double curError = cur.heightAtRange - height;

            if (havePrev && prevError < 0 && curError >= 0) {
                // Found the low-arc bracket [prevAngle, angle]; refine it.
                return bisectElevation(prevAngle, angle, range, height, proj);
            }
            havePrev = true;
            prevAngle = angle;
            prevError = curError;
        }

        // No bracket where the shell rises through the target height: out of reach.
        return Elevation.unreachable();
    }

    private Elevation bisectElevation(double loAngle, double hiAngle, double range,
                                      double height, ProjectileProfile proj) {
        Elevation best = simulateVerticalPlane(hiAngle, range, proj);
        for (int i = 0; i < 40; i++) {
            double mid = 0.5 * (loAngle + hiAngle);
            Elevation cur = simulateVerticalPlane(mid, range, proj);
            if (!cur.reachable) {
                // Shouldn't normally happen inside a valid bracket; nudge toward the lower angle.
                hiAngle = mid;
                continue;
            }
            double error = cur.heightAtRange - height;
            best = cur;
            if (Math.abs(error) < 1.0e-4) {
                break;
            }
            if (error < 0) {
                loAngle = mid;
            } else {
                hiAngle = mid;
            }
        }
        return best;
    }

    /**
     * Simulates the discrete per-tick flight in the 2D vertical plane (horizontal range vs.
     * height) for one launch angle, and reports the height and time at which the shell first
     * crosses the target's horizontal range.
     */
    private Elevation simulateVerticalPlane(double angleRad, double range, ProjectileProfile proj) {
        double s = proj.muzzleSpeed();
        double vh = s * Math.cos(angleRad); // horizontal speed component
        double vy = s * Math.sin(angleRad); // vertical speed component
        double h = 0.0; // horizontal distance travelled
        double y = 0.0; // height relative to launch
        double t = 0.0;

        for (int tick = 0; tick < maxFlightTicks; tick++) {
            double prevH = h;
            double prevY = y;
            double prevVy = vy;
            double prevVh = vh;

            // One tick of the projectile model: gravity, then drag, then move.
            vy -= proj.gravity();
            vh *= proj.drag();
            vy *= proj.drag();
            h += vh;
            y += vy;
            t += 1.0;

            if (h >= range) {
                double span = h - prevH;
                double frac = span < 1.0e-12 ? 0.0 : (range - prevH) / span;
                double heightAtRange = prevY + frac * (y - prevY);
                double timeAtRange = (t - 1.0) + frac;
                double impactVy = prevVy + frac * (vy - prevVy);
                double impactVh = prevVh + frac * (vh - prevVh);
                return Elevation.reached(angleRad, heightAtRange, timeAtRange, impactVy, impactVh);
            }
            // Horizontal velocity decays geometrically under drag; once it is negligible the
            // shell can never cover the remaining distance.
            if (vh < 1.0e-6) {
                break;
            }
        }
        return Elevation.unreachable();
    }

    /** Internal result of a single-angle elevation simulation. */
    private static final class Elevation {
        final boolean reachable;
        final double launchAngleRad;
        final double heightAtRange;
        final double timeTicks;
        final double impactVy;
        final double impactVh;

        private Elevation(boolean reachable, double launchAngleRad, double heightAtRange,
                          double timeTicks, double impactVy, double impactVh) {
            this.reachable = reachable;
            this.launchAngleRad = launchAngleRad;
            this.heightAtRange = heightAtRange;
            this.timeTicks = timeTicks;
            this.impactVy = impactVy;
            this.impactVh = impactVh;
        }

        static Elevation reached(double angle, double heightAtRange, double time,
                                 double impactVy, double impactVh) {
            return new Elevation(true, angle, heightAtRange, time, impactVy, impactVh);
        }

        static Elevation unreachable() {
            return new Elevation(false, 0, 0, 0, 0, 0);
        }
    }
}
