package shipwrights.genesis.teleportation.integration;

import net.minecraft.util.Mth;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.properties.Atmosphere;
import shipwrights.genesis.space.properties.PlanetProperties;

/**
 * How high "space" begins above a given world. A thick-atmosphere world like
 * Earth needs the full climb; an airless one like Mercury reaches space far
 * lower — there is barely any atmosphere to climb out of. The boundary scales
 * with the celestial's own atmosphere (density and thickness) between a small
 * floor and the configured maximum.
 */
public final class AtmosphereHeights {
    /** Even an airless world keeps a modest boundary so leaving is still a climb. */
    private static final double MIN_HEIGHT = 3000.0;

    private AtmosphereHeights() {
    }

    /** 0 for an airless world, 1 for a full Earth-like atmosphere. */
    public static double atmosphereFactor(Celestial celestial) {
        if (celestial.properties() instanceof PlanetProperties planet) {
            Atmosphere atmosphere = planet.atmosphere();
            return Mth.clamp(0.5 * atmosphere.density() + 0.5 * atmosphere.thickness(), 0.0, 1.0);
        }
        return 1.0;
    }

    /** Altitude at which climbing lifts you into space. */
    public static double exitHeight(Celestial celestial) {
        double base = GenesisCommonConfig.getAtmosphereExitHeight();
        return MIN_HEIGHT + (base - MIN_HEIGHT) * atmosphereFactor(celestial);
    }

    /** Altitude you re-enter at, high above the surface, to fall back down. */
    public static double arrivalHeight(Celestial celestial) {
        double base = GenesisCommonConfig.getAtmosphereEntryHeight();
        return MIN_HEIGHT + (base - MIN_HEIGHT) * atmosphereFactor(celestial);
    }
}
