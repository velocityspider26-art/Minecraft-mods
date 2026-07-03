package com.example.examplemod.content.thruster;

import net.minecraft.util.StringRepresentable;

/**
 * The visual/behavioural tier a thruster runs at, driven by the fuel loaded in its tank (see
 * {@link ThrusterFuelType}). Each tier carries a nominal thrust multiplier so the client can size the
 * plume to roughly match the physical thrust without an extra sync (the server uses the precise,
 * data-driven multiplier from the fuel definition). The renderer maps each tier to a distinct plume
 * profile.
 *
 * <p>Tiers are inspired by real propellant grades, from a dirty low-grade burn to an unstable exotic
 * propellant.</p>
 */
public enum PlumeType implements StringRepresentable {
    LOW_GRADE("low_grade", "Low-grade", 0.65f),
    STANDARD("standard", "Standard", 1.00f),
    REFINED("refined", "Refined", 1.35f),
    HIGH_ENERGY("high_energy", "High-energy", 1.75f),
    EXOTIC("exotic", "Exotic", 2.25f);

    private final String serializedName;
    private final String displayName;
    private final float nominalThrust;

    PlumeType(String serializedName, String displayName, float nominalThrust) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.nominalThrust = nominalThrust;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Nominal thrust multiplier for client-side plume sizing. */
    public float getNominalThrust() {
        return nominalThrust;
    }

    public static PlumeType byName(String name, PlumeType fallback) {
        if (name != null) {
            for (PlumeType type : values()) {
                if (type.serializedName.equalsIgnoreCase(name)) {
                    return type;
                }
            }
        }
        return fallback;
    }

    public PlumeType next() {
        PlumeType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public PlumeType previous() {
        PlumeType[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }
}
