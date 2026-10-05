package com.vantage.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Shape of a Vantage planet.
 *
 * @param radius            planet radius in blocks: sets how far apart the poles are and, with
 *                          {@code wrap}, after how many blocks east the world repeats; distant
 *                          terrain is drawn curved with the same radius
 * @param oceanFraction     share of the surface under the sea
 * @param continentScale    typical size of a continent, in blocks
 * @param mountainScale     typical spacing between mountain ranges, in blocks
 * @param spawnLatitude     latitude in degrees at z = 0 (north is towards -z)
 * @param temperatureOffset shifts every climate warmer (positive) or colder
 * @param humidityOffset    shifts every climate wetter (positive) or drier
 * @param wrap              terrain repeats seamlessly every {@code 2·π·radius} blocks along x,
 *                          like walking around a planet
 */
public record PlanetSettings(int radius, float oceanFraction, float continentScale, float mountainScale,
                             float spawnLatitude, float temperatureOffset, float humidityOffset, boolean wrap) {
    public static final PlanetSettings DEFAULT = new PlanetSettings(30000, 0.55f, 6000f, 3000f, 40f, 0f, 0f, true);

    public static final Codec<PlanetSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1000, 10_000_000).optionalFieldOf("radius", DEFAULT.radius).forGetter(PlanetSettings::radius),
            Codec.floatRange(0.05f, 0.95f).optionalFieldOf("ocean_fraction", DEFAULT.oceanFraction).forGetter(PlanetSettings::oceanFraction),
            Codec.floatRange(200f, 1_000_000f).optionalFieldOf("continent_scale", DEFAULT.continentScale).forGetter(PlanetSettings::continentScale),
            Codec.floatRange(100f, 1_000_000f).optionalFieldOf("mountain_scale", DEFAULT.mountainScale).forGetter(PlanetSettings::mountainScale),
            Codec.floatRange(-90f, 90f).optionalFieldOf("spawn_latitude", DEFAULT.spawnLatitude).forGetter(PlanetSettings::spawnLatitude),
            Codec.floatRange(-2f, 2f).optionalFieldOf("temperature_offset", DEFAULT.temperatureOffset).forGetter(PlanetSettings::temperatureOffset),
            Codec.floatRange(-2f, 2f).optionalFieldOf("humidity_offset", DEFAULT.humidityOffset).forGetter(PlanetSettings::humidityOffset),
            Codec.BOOL.optionalFieldOf("wrap", DEFAULT.wrap).forGetter(PlanetSettings::wrap)
    ).apply(i, PlanetSettings::new));

    /** Blocks from one pole to the other. */
    public double poleToPole() {
        return Math.PI * this.radius;
    }

    /** Blocks after which the terrain repeats along x when it wraps. */
    public double circumference() {
        return 2.0 * Math.PI * this.radius;
    }
}
