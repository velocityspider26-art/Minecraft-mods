package com.velocityspider.crashphysics.material;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * How a material fails, which decides what is left over when it breaks.
 */
public enum FractureMode implements StringRepresentable {
    /**
     * Soil, sand, gravel, snow: displaced and thrown out of the crater.
     */
    GRANULAR,
    /**
     * Rock, concrete, bricks, ice: shatters into fragments.
     */
    BRITTLE,
    /**
     * Wood: splinters and snaps.
     */
    FIBROUS,
    /**
     * Metal: dents, bends and tears rather than shattering.
     */
    DUCTILE,
    /**
     * Wool, leaves, fabric envelopes: shreds.
     */
    SOFT,
    /**
     * Glass: shatters completely.
     */
    GLASS,
    /**
     * Crops, flowers and other small plants.
     */
    PLANT;

    public static final Codec<FractureMode> CODEC = StringRepresentable.fromEnum(FractureMode::values);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
