package com.example.examplemod.content.thruster;

import net.minecraft.util.StringRepresentable;

/**
 * The five plume looks a thruster can be set to, each inspired by a real-world rocket propellant
 * family. Cycled in-world with a wrench.
 *
 * <ul>
 *   <li>{@link #KEROLOX} - RP-1 / LOX kerosene engines (Merlin, F-1): warm sooty orange.</li>
 *   <li>{@link #METHALOX} - liquid methane / LOX (Raptor): blue-white with mach diamonds.</li>
 *   <li>{@link #HYDROLOX} - liquid hydrogen / LOX (RS-25): faint, nearly transparent pale blue.</li>
 *   <li>{@link #HYPERGOLIC} - N2O4 / UDMH (Draco, Proton): translucent reddish orange.</li>
 *   <li>{@link #SOLID} - APCP solid boosters (Shuttle SRB): brilliant, wide white-orange.</li>
 * </ul>
 */
public enum PlumeType implements StringRepresentable {
    KEROLOX("kerolox", "Kerolox (RP-1/LOX)"),
    METHALOX("methalox", "Methalox (Raptor)"),
    HYDROLOX("hydrolox", "Hydrolox (RS-25)"),
    HYPERGOLIC("hypergolic", "Hypergolic (N2O4/UDMH)"),
    SOLID("solid", "Solid (APCP booster)");

    private final String serializedName;
    private final String displayName;

    PlumeType(String serializedName, String displayName) {
        this.serializedName = serializedName;
        this.displayName = displayName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public String getDisplayName() {
        return displayName;
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
