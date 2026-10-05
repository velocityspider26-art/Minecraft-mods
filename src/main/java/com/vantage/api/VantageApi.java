package com.vantage.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hooks for other mods, such as space mods with planets. Client side only: guard calls with
 * {@code ModList.get().isLoaded("vantage")}. Thread-safe; call any time, typically during client
 * setup.
 */
public final class VantageApi {
    private static final Map<ResourceKey<Level>, Planet> PLANETS = new ConcurrentHashMap<>();

    private VantageApi() {
    }

    /**
     * How a dimension looks from high up. Replaces the matching config values in that dimension.
     *
     * @param radius           planet radius in blocks for a curved horizon; 0 keeps the ground flat
     * @param hazeDistance     blocks of air at sea level that hide about two thirds of the view;
     *                         0 for no air at all (crisp to the horizon, like the Moon)
     * @param atmosphereHeight blocks over which the air gets about 2.7 times thinner
     * @param hazeColor        haze colour as {@code 0xRRGGBB}, or -1 for the game's fog colour
     */
    public record Planet(int radius, int hazeDistance, int atmosphereHeight, int hazeColor) {
        public Planet {
            if (radius < 0 || hazeDistance < 0 || atmosphereHeight <= 0) {
                throw new IllegalArgumentException("negative radius or haze, or non-positive atmosphere height");
            }
        }
    }

    /** Sets (or with {@code null} clears) the planet settings of a dimension. */
    public static void setPlanet(ResourceKey<Level> dimension, @Nullable Planet planet) {
        if (planet == null) {
            PLANETS.remove(dimension);
        } else {
            PLANETS.put(dimension, planet);
        }
    }

    public static @Nullable Planet planet(ResourceKey<Level> dimension) {
        return PLANETS.get(dimension);
    }
}
