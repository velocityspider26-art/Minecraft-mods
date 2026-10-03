package dev.velocityspider.caw.engine;

public enum EngineTier {
    SMALL(0.55, 0.50),
    NORMAL(1.00, 1.00),
    LARGE(2.70, 2.00),
    HUMONGOUS(7.50, 4.00);

    private final double thrustMultiplier;
    private final double linearScale;

    EngineTier(double thrustMultiplier, double linearScale) {
        this.thrustMultiplier = thrustMultiplier;
        this.linearScale = linearScale;
    }

    public double thrustMultiplier() {
        return thrustMultiplier;
    }

    public double linearScale() {
        return linearScale;
    }
}
