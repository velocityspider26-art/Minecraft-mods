package dev.velocityspider.caw.engine;

public final class EngineMath {
    private EngineMath() {}

    public static final double NORMAL_CORE_MAX_THRUST_N = 220_000.0;

    public static final double SPOOL_UP_PER_TICK = 1.0 / (20.0 * 3.5);
    public static final double SPOOL_DOWN_PER_TICK = 1.0 / (20.0 * 2.0);

    public static double redstoneTarget(int signal) {
        if (signal <= 0) {
            return 0.0;
        }
        return Math.min(1.0, signal / 15.0);
    }

    public static double stepSpool(double current, double target) {
        double delta = target - current;
        double maxStep = delta >= 0.0 ? SPOOL_UP_PER_TICK : SPOOL_DOWN_PER_TICK;

        if (Math.abs(delta) <= maxStep) {
            return target;
        }

        return current + Math.copySign(maxStep, delta);
    }

    public static double thrustNewtons(double spool, double flowMultiplier, EngineTier tier) {
        if (spool <= 0.0) {
            return 0.0;
        }

        double normalized = Math.pow(Math.min(1.0, spool), 1.55);
        return NORMAL_CORE_MAX_THRUST_N
                * normalized
                * Math.max(0.0, flowMultiplier)
                * tier.thrustMultiplier();
    }
}
