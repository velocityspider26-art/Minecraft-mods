package com.velocityspider.crashphysics.physics;

/**
 * The mechanical properties of one block treated as a 1 m³ chunk of material.
 *
 * @param density       mass density [kg/m³]
 * @param strength      resistance to being crushed or penetrated [Pa]
 * @param jointStrength how much load a full face-to-face connection to a neighbouring block can carry [Pa]
 * @param unbreakable   if nothing can ever crush this block (bedrock, barriers, ...)
 */
public record MaterialProfile(double density, double strength, double jointStrength, boolean unbreakable) {

    public static final MaterialProfile UNBREAKABLE = new MaterialProfile(3000.0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, true);

    public MaterialProfile {
        if (!(density > 0.0)) {
            throw new IllegalArgumentException("density must be positive, got " + density);
        }
        if (!(strength > 0.0)) {
            throw new IllegalArgumentException("strength must be positive, got " + strength);
        }
        if (!(jointStrength > 0.0)) {
            throw new IllegalArgumentException("jointStrength must be positive, got " + jointStrength);
        }
    }

    /**
     * @return the crush strength, or infinity for unbreakable blocks [Pa]
     */
    public double effectiveStrength() {
        return this.unbreakable ? Double.POSITIVE_INFINITY : this.strength;
    }

    /**
     * @return the joint strength, or infinity for unbreakable blocks [Pa]
     */
    public double effectiveJointStrength() {
        return this.unbreakable ? Double.POSITIVE_INFINITY : this.jointStrength;
    }

    /**
     * Scales strength and joint strength, e.g. for blocks that are part of a lightweight vehicle structure.
     */
    public MaterialProfile scaled(final double strengthScale, final double jointScale) {
        if (this.unbreakable || (strengthScale == 1.0 && jointScale == 1.0)) {
            return this;
        }
        return new MaterialProfile(this.density, this.strength * strengthScale, this.jointStrength * jointScale, false);
    }

    /**
     * Returns a copy with a different density, e.g. the density Sable assigns to a block on a vehicle.
     */
    public MaterialProfile withDensity(final double newDensity) {
        if (newDensity == this.density) {
            return this;
        }
        return new MaterialProfile(newDensity, this.strength, this.jointStrength, this.unbreakable);
    }
}
