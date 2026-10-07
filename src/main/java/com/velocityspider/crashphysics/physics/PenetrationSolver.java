package com.velocityspider.crashphysics.physics;

/**
 * Solves what happens where two blocks meet at speed, using the Tate–Alekseevskii model of high-speed penetration.
 * <p>
 * The block on the moving vehicle is the "projectile" (strength {@code Y}, density {@code ρp}) and the block it hits is
 * the "target" (resistance {@code R}, density {@code ρt}). At the interface both sides push with the same pressure:
 * <pre>
 *     ½·ρp·(v − u)² + Y  =  ½·ρt·u² + R
 * </pre>
 * where {@code v} is the closing speed, {@code u} is how fast the hole in the target deepens and {@code v − u} is how
 * fast the projectile is crushed (eroded). This gives the behaviour you see in real crashes:
 * <ul>
 *     <li>slow, strong vehicle into soft ground: the vehicle stays intact and ploughs a trench ({@code u = v})</li>
 *     <li>weak vehicle into rock: the vehicle crumples and the rock is barely scratched ({@code u = 0})</li>
 *     <li>very fast impacts: even soft ground resists like a wall (the {@code ½·ρt·u²} term), so both sides erode</li>
 * </ul>
 */
public final class PenetrationSolver {

    /**
     * The state of the contact interface.
     *
     * @param targetErosionSpeed     how fast the target is being penetrated [m/s]
     * @param projectileErosionSpeed how fast the projectile is being crushed [m/s]
     * @param pressure               the pressure at the interface [Pa], infinite if neither side can yield
     */
    public record Interface(double targetErosionSpeed, double projectileErosionSpeed, double pressure) {
        public static final Interface NONE = new Interface(0.0, 0.0, 0.0);

        /**
         * @return if anything at this interface is yielding
         */
        public boolean yielding() {
            return this.targetErosionSpeed > 0.0 || this.projectileErosionSpeed > 0.0;
        }
    }

    private PenetrationSolver() {
    }

    /**
     * Solves the interface between a projectile block and a target block.
     *
     * @param closingSpeed the speed the two blocks approach each other along the contact [m/s]
     * @param projectile   the block on the vehicle
     * @param target       the block that was hit
     */
    public static Interface solve(final double closingSpeed, final MaterialProfile projectile, final MaterialProfile target) {
        return solve(closingSpeed, projectile.density(), projectile.effectiveStrength(), target.density(), target.effectiveStrength());
    }

    /**
     * Solves the interface between a projectile block and a target block.
     *
     * @param v    closing speed [m/s]
     * @param rhoP projectile density [kg/m³]
     * @param yP   projectile strength [Pa], may be infinite
     * @param rhoT target density [kg/m³]
     * @param rT   target resistance [Pa], may be infinite
     */
    public static Interface solve(final double v, final double rhoP, final double yP, final double rhoT, final double rT) {
        if (!(v > 0.0)) {
            return Interface.NONE;
        }

        final boolean projectileRigid = Double.isInfinite(yP);
        final boolean targetRigid = Double.isInfinite(rT);

        if (projectileRigid && targetRigid) {
            return new Interface(0.0, 0.0, Double.POSITIVE_INFINITY);
        }

        // The target can't push back hard enough to deform the projectile: the projectile drills straight in
        final double targetPressureAtFullSpeed = rT + 0.5 * rhoT * v * v;
        if (projectileRigid || yP >= targetPressureAtFullSpeed) {
            return new Interface(v, 0.0, targetPressureAtFullSpeed);
        }

        // The projectile can't push hard enough to deform the target: only the projectile is crushed
        final double projectilePressureAtFullSpeed = yP + 0.5 * rhoP * v * v;
        if (targetRigid || rT >= projectilePressureAtFullSpeed) {
            return new Interface(0.0, v, projectilePressureAtFullSpeed);
        }

        // Both erode: solve ½ρp(v−u)² + Yp − ½ρt·u² − Rt = 0 for u in (0, v).
        // f(0) > 0 and f(v) < 0 by the two checks above, and f is strictly decreasing on [0, v], so there is exactly one root.
        final double u = solveInterfaceSpeed(v, rhoP, yP, rhoT, rT);
        return new Interface(u, v - u, rT + 0.5 * rhoT * u * u);
    }

    private static double solveInterfaceSpeed(final double v, final double rhoP, final double yP, final double rhoT, final double rT) {
        // a·u² + b·u + c = 0
        final double a = 0.5 * (rhoP - rhoT);
        final double b = -rhoP * v;
        final double c = 0.5 * rhoP * v * v + yP - rT;

        double u;
        if (Math.abs(a) < 1.0e-9 * (rhoP + rhoT)) {
            u = -c / b;
        } else {
            final double discriminant = b * b - 4.0 * a * c;
            if (discriminant < 0.0) {
                return bisect(v, rhoP, yP, rhoT, rT);
            }
            // Numerically stable form of the quadratic formula
            final double q = -0.5 * (b + Math.copySign(Math.sqrt(discriminant), b));
            final double root1 = q / a;
            final double root2 = c / q;
            u = (root1 >= 0.0 && root1 <= v) ? root1 : root2;
        }

        if (!(u >= 0.0 && u <= v)) {
            return bisect(v, rhoP, yP, rhoT, rT);
        }
        return u;
    }

    private static double bisect(final double v, final double rhoP, final double yP, final double rhoT, final double rT) {
        double lo = 0.0;
        double hi = v;
        for (int i = 0; i < 64; i++) {
            final double mid = 0.5 * (lo + hi);
            final double f = 0.5 * rhoP * (v - mid) * (v - mid) + yP - 0.5 * rhoT * mid * mid - rT;
            if (f > 0.0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return 0.5 * (lo + hi);
    }
}
