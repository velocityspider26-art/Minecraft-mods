package com.velocityspider.radarballistics.ballistics;

/**
 * A minimal immutable 3D vector using {@code double} precision.
 *
 * <p>This type is deliberately free of any Minecraft/NeoForge imports so the whole
 * ballistics engine can be compiled and unit-tested as plain Java (see the standalone
 * verification harness and the JUnit tests). Coordinates follow Minecraft world-space
 * conventions: +X east, +Y up, +Z south. Units are blocks, and velocities are blocks
 * per tick (there are 20 ticks per second).</p>
 */
public record Vec3d(double x, double y, double z) {

    public static final Vec3d ZERO = new Vec3d(0, 0, 0);

    public Vec3d add(Vec3d o) {
        return new Vec3d(x + o.x, y + o.y, z + o.z);
    }

    public Vec3d subtract(Vec3d o) {
        return new Vec3d(x - o.x, y - o.y, z - o.z);
    }

    public Vec3d scale(double s) {
        return new Vec3d(x * s, y * s, z * s);
    }

    /** Squared length. Cheaper than {@link #length()} when only comparing magnitudes. */
    public double lengthSq() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSq());
    }

    /** Horizontal (X/Z plane) distance to another point, ignoring the Y axis. */
    public double horizontalDistance(Vec3d o) {
        double dx = x - o.x;
        double dz = z - o.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public double distance(Vec3d o) {
        return subtract(o).length();
    }

    /** Unit vector in the same direction, or {@link #ZERO} for a zero-length input. */
    public Vec3d normalize() {
        double len = length();
        return len < 1.0e-9 ? ZERO : scale(1.0 / len);
    }
}
