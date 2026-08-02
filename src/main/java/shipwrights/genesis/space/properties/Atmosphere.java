package shipwrights.genesis.space.properties;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * @param color          the sky's colour at ground level
 * @param transitionColor the colour the sky passes through on the way to black.
 *                        A real sky does not fade straight from blue to space —
 *                        it goes violet in the middle, because the air that is
 *                        left scatters differently once most of it is below you.
 *                        Optional: bodies that leave it out get a violet band
 *                        derived from their own sky, and airless bodies never
 *                        show one at all because the band is scaled by density.
 */
public record Atmosphere(
        double density,
        double thickness,
        boolean precipitation,
        boolean isBreathable,
        PlanetColorPalette color,
        Optional<PlanetColorPalette> transitionColor
) {
    /** Keeps the pre-transition-colour call sites compiling unchanged. */
    public Atmosphere(double density, double thickness, boolean precipitation, boolean isBreathable,
                      PlanetColorPalette color) {
        this(density, thickness, precipitation, isBreathable, color, Optional.empty());
    }

    public static final Codec<Atmosphere> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("density").forGetter(Atmosphere::density),
            Codec.DOUBLE.fieldOf("thickness").forGetter(Atmosphere::thickness),
            Codec.BOOL.fieldOf("precipitation").forGetter(Atmosphere::precipitation),
            Codec.BOOL.fieldOf("isBreathable").forGetter(Atmosphere::isBreathable),
            PlanetColorPalette.CODEC.fieldOf("color").forGetter(Atmosphere::color),
            // Optional so every celestial written before this field still loads.
            PlanetColorPalette.CODEC.optionalFieldOf("transitionColor").forGetter(Atmosphere::transitionColor)
    ).apply(instance, Atmosphere::new));
}
