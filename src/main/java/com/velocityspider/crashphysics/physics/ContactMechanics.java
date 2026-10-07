package com.velocityspider.crashphysics.physics;

/**
 * Turns a solved {@link PenetrationSolver.Interface} into what happens over one physics step: how far each block is
 * eroded, how much momentum the resisting material takes out of the vehicle, and how much energy is dissipated.
 */
public final class ContactMechanics {

    private ContactMechanics() {
    }

    /**
     * Decides whether an impact is hard enough for the weaker of the two blocks to start yielding.
     * <p>
     * Before anything crushes, the contact deflects elastically by a small distance {@code δ}, storing at most
     * {@code σ·A·δ} of energy. If the share of kinetic energy this contact has to absorb is larger than that, the weaker
     * block is pushed past its yield point. This is why a heavy ship touching down at 1 m/s leaves the grass intact while
     * the same ship at 10 m/s digs in, and why a broad belly landing is gentler per block than a nose-first hit.
     *
     * @param shareMassKg    the part of the vehicle's effective mass this contact has to stop [kg]
     * @param closingSpeed   speed along the contact normal [m/s]
     * @param weakerStrength the strength of the weaker of the two blocks [Pa]
     * @param area           contact area [m²]
     * @param elasticTravel  elastic deflection before yielding [m]
     */
    public static boolean exceedsElasticLimit(final double shareMassKg, final double closingSpeed, final double weakerStrength,
                                              final double area, final double elasticTravel) {
        if (Double.isInfinite(weakerStrength) || !(closingSpeed > 0.0) || !(shareMassKg > 0.0)) {
            return false;
        }

        final double kineticEnergy = 0.5 * shareMassKg * closingSpeed * closingSpeed;
        return kineticEnergy > weakerStrength * area * elasticTravel;
    }

    /**
     * What one eroding contact does over one step.
     *
     * @param impulse           momentum removed from the relative motion [N·s]
     * @param targetErosion     depth the target block was penetrated by [m]
     * @param projectileErosion length of the projectile block that was crushed [m]
     * @param dissipatedEnergy  energy turned into crushing, heat and thrown debris [J]
     * @param stopped           if the resistance stopped the relative motion within the step
     * @param targetFailed      if the target block was crushed through before the step ended
     * @param projectileFailed  if the projectile block was crushed through before the step ended
     */
    public record Step(double impulse, double targetErosion, double projectileErosion, double dissipatedEnergy, boolean stopped,
                       boolean targetFailed, boolean projectileFailed) {
        public static final Step NONE = new Step(0.0, 0.0, 0.0, 0.0, false, false, false);

        /**
         * @return if the contact carries on into the material, rather than being brought to rest by it within the step
         */
        public boolean goesThrough() {
            return this.targetFailed || this.projectileFailed || !this.stopped;
        }
    }

    /**
     * Integrates an eroding contact over a time step, with blocks that never fail.
     *
     * @see #integrate(MaterialProfile, MaterialProfile, double, double, double, double, double, double)
     */
    public static Step integrate(final MaterialProfile projectile, final MaterialProfile target, final double area, final double dt,
                                 final double effectiveMassKg, final double closingSpeed) {
        return integrate(projectile, target, area, dt, effectiveMassKg, closingSpeed, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /**
     * Integrates an eroding contact over a time step. The interface pressure {@code P(v)} resists the motion with a force
     * {@code P·A} that drops as the contact slows down, so the step is split into a few Heun (second order) micro-steps.
     * The step ends early when the contact stops, or when either block has been crushed through: a block that fails
     * stops resisting, and the rest of the step belongs to whatever is behind it.
     *
     * @param projectile      the block on the vehicle
     * @param target          the block that was hit
     * @param area            contact area [m²]
     * @param dt              step length [s]
     * @param effectiveMassKg reduced effective mass of the two bodies at the contact, along the motion [kg]
     * @param closingSpeed    closing speed at the start of the step [m/s]
     * @param targetLimit     how much more of the target can be crushed before it fails [m]
     * @param projectileLimit how much more of the projectile can be crushed before it fails [m]
     */
    public static Step integrate(final MaterialProfile projectile, final MaterialProfile target, final double area, final double dt,
                                 final double effectiveMassKg, final double closingSpeed, final double targetLimit, final double projectileLimit) {
        if (!(closingSpeed > 0.0) || !(dt > 0.0) || !(effectiveMassKg > 0.0) || !(area > 0.0)) {
            return Step.NONE;
        }

        final PenetrationSolver.Interface initial = PenetrationSolver.solve(closingSpeed, projectile, target);
        if (!initial.yielding() || Double.isInfinite(initial.pressure())) {
            return Step.NONE;
        }

        final int microSteps = 4;
        final double h = dt / microSteps;

        double v = closingSpeed;
        double targetErosion = 0.0;
        double projectileErosion = 0.0;
        boolean stopped = false;
        boolean targetFailed = false;
        boolean projectileFailed = false;

        for (int step = 0; step < microSteps; step++) {
            final PenetrationSolver.Interface start = step == 0 ? initial : PenetrationSolver.solve(v, projectile, target);
            final double startForce = start.pressure() * area;
            final double predicted = v - startForce * h / effectiveMassKg;

            if (predicted > 0.0) {
                final PenetrationSolver.Interface end = PenetrationSolver.solve(predicted, projectile, target);
                final double next = v - 0.5 * (startForce + end.pressure() * area) * h / effectiveMassKg;

                if (next > 0.0) {
                    final double targetStep = 0.5 * (start.targetErosionSpeed() + end.targetErosionSpeed()) * h;
                    final double projectileStep = 0.5 * (start.projectileErosionSpeed() + end.projectileErosionSpeed()) * h;
                    final double reached = failureFraction(targetErosion, targetStep, targetLimit, projectileErosion, projectileStep, projectileLimit);

                    targetErosion += targetStep * reached;
                    projectileErosion += projectileStep * reached;
                    v -= (v - next) * reached;
                    if (reached < 1.0) {
                        targetFailed = targetErosion >= targetLimit - FAILURE_TOLERANCE;
                        projectileFailed = projectileErosion >= projectileLimit - FAILURE_TOLERANCE;
                        break;
                    }
                    continue;
                }
            }

            // Stops within this micro-step. The force falls from P(v)·A to the static resistance as it stops, and the
            // erosion speeds fall off with the closing speed.
            final PenetrationSolver.Interface creeping = PenetrationSolver.solve(Math.min(v, 1.0e-6), projectile, target);
            final double averageForce = 0.5 * (startForce + creeping.pressure() * area);
            final double stopTime = Math.min(h, effectiveMassKg * v / averageForce);

            final double targetStep = 0.5 * start.targetErosionSpeed() * stopTime;
            final double projectileStep = 0.5 * start.projectileErosionSpeed() * stopTime;
            final double reached = failureFraction(targetErosion, targetStep, targetLimit, projectileErosion, projectileStep, projectileLimit);

            targetErosion += targetStep * reached;
            projectileErosion += projectileStep * reached;
            if (reached < 1.0) {
                // A block gives way before the contact comes to rest. With the speed falling linearly, crushing a
                // fraction f of the way to rest leaves √(1 - f) of the speed.
                v *= Math.sqrt(1.0 - reached);
                targetFailed = targetErosion >= targetLimit - FAILURE_TOLERANCE;
                projectileFailed = projectileErosion >= projectileLimit - FAILURE_TOLERANCE;
            } else {
                v = 0.0;
                stopped = true;
            }
            break;
        }

        final double impulse = effectiveMassKg * (closingSpeed - v);
        final double dissipated = 0.5 * effectiveMassKg * (closingSpeed * closingSpeed - v * v);
        return new Step(impulse, targetErosion, projectileErosion, dissipated, stopped, targetFailed, projectileFailed);
    }

    private static final double FAILURE_TOLERANCE = 1.0e-9;

    /**
     * How much of a step's erosion happens before one of the blocks is crushed through [0, 1].
     */
    private static double failureFraction(final double targetErosion, final double targetStep, final double targetLimit,
                                          final double projectileErosion, final double projectileStep, final double projectileLimit) {
        double fraction = 1.0;
        if (targetErosion + targetStep >= targetLimit) {
            fraction = Math.min(fraction, targetStep > 0.0 ? Math.max(0.0, (targetLimit - targetErosion) / targetStep) : 0.0);
        }
        if (projectileErosion + projectileStep >= projectileLimit) {
            fraction = Math.min(fraction, projectileStep > 0.0 ? Math.max(0.0, (projectileLimit - projectileErosion) / projectileStep) : 0.0);
        }
        return fraction;
    }

    /**
     * The kinetic energy of a reduced mass at a given closing speed [J].
     */
    public static double kineticEnergy(final double massKg, final double speed) {
        return 0.5 * massKg * speed * speed;
    }

    /**
     * Combines two effective masses into the reduced mass of the pair. Infinite masses (static terrain) are allowed.
     */
    public static double reducedMass(final double massA, final double massB) {
        if (Double.isInfinite(massA)) {
            return massB;
        }
        if (Double.isInfinite(massB)) {
            return massA;
        }
        if (!(massA > 0.0) || !(massB > 0.0)) {
            return 0.0;
        }
        return massA * massB / (massA + massB);
    }
}
