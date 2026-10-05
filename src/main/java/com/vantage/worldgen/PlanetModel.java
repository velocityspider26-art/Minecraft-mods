package com.vantage.worldgen;

/**
 * The planet's climate maths: continents, mountain ranges, peaks and valleys, temperature and
 * humidity at a column, in the ranges Minecraft's terrain splines and biome table expect.
 *
 * <ul>
 *   <li>With {@code wrap}, noise is sampled on a cylinder of the planet's radius, so the world
 *       repeats seamlessly every {@code 2·π·radius} blocks east-west.</li>
 *   <li>Latitude grows towards -z (north). Climate follows Earth's belts: warm, wet tropics
 *       (jungle), hot, dry subtropics (deserts, badlands, savanna), temperate mid-latitudes
 *       (forests, plains), boreal forest, then frozen poles. Noise breaks the belts up.</li>
 *   <li>Continents are a warped fractal noise, biased so the requested share is ocean.</li>
 *   <li>Mountain ranges run along the zero lines of a second warped noise: long, winding
 *       chains instead of scattered peaks.</li>
 * </ul>
 */
public final class PlanetModel {
    static final double WARP = 0.35;
    /** Domain warp of temperature and humidity, so climate borders wander instead of forming round blobs. */
    static final double CLIMATE_WARP = 0.8;
    static final double CONT_GAIN = 2.2;
    /** Spread (standard deviation) of continentalness after the gain; the noise's is about 0.33. */
    static final double CONT_SPREAD = 0.72;
    static final double RANGE_SHARPNESS = 7.0;
    static final double EROSION_GAIN = 2.4;
    static final double RIDGE_GAIN = 2.5;
    static final double TEMP_GAIN = 1.8;
    static final double VEG_GAIN = 1.6;
    /** Continentalness where deep ocean starts, and how much deeper it may get (to -1.0). */
    static final double DEEP_OCEAN = -0.455;
    static final double OCEAN_DEPTH = 0.545;
    static final double WET_BELOW = 0.0;

    /**
     * Temperature by latitude, every 10 degrees from the equator to the pole. In Minecraft's biome
     * table "hot" means desert, so the hottest belt is the subtropics, not the equator.
     */
    private static final double[] LATITUDE_TEMPERATURE = {0.40, 0.50, 0.72, 0.62, 0.08, -0.02, -0.26, -0.50, -0.72, -0.90};

    private static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(Cache::new);

    private PlanetModel() {
    }

    private static final class Cache {
        Object noises;
        PlanetSettings planet;
        int x = Integer.MIN_VALUE;
        int z = Integer.MIN_VALUE;
        final double[] values = new double[5];
    }

    /** Climate parameter {@code kind} ({@link PlanetClimate.Kind} ordinal) at a column. */
    static double get(PlanetSettings planet, PlanetNoises noises, int kind, int x, int z) {
        Cache c = CACHE.get();
        Object token = noises.continents().noise();
        if (c.x != x || c.z != z || c.noises != token || c.planet != planet) {
            compute(planet, noises, x, z, c.values);
            c.x = x;
            c.z = z;
            c.noises = token;
            c.planet = planet;
        }
        return c.values[kind];
    }

    /** Fills {@code out} with continents, erosion, ridges, temperature, vegetation. */
    public static void compute(PlanetSettings p, PlanetNoises n, double x, double z, double[] out) {
        double r = p.radius();
        double px;
        double py;
        if (p.wrap()) {
            double theta = x / r;
            px = r * Math.cos(theta);
            py = r * Math.sin(theta);
        } else {
            px = x;
            py = 0.0;
        }
        double pz = z;
        // Past a pole, latitude comes back down the other side.
        double signedLat = Math.asin(Math.sin(Math.toRadians(p.spawnLatitude()) - z / r));

        double s = p.continentScale();
        double ws = s * 0.6;
        double wx = n.warp().getValue(px / ws, py / ws, pz / ws);
        double wy = n.warp().getValue(px / ws + 37.1, py / ws - 11.3, pz / ws + 5.7);
        double wz = n.warp().getValue(px / ws - 23.9, py / ws + 41.5, pz / ws - 17.2);
        double continents = n.continents().getValue(px / s + WARP * wx, py / s + WARP * wy, pz / s + WARP * wz);
        double island = n.ridges().getValue(px / 420.0 + 71.3, py / 420.0 - 19.9, pz / 420.0 + 43.1);
        out[0] = clamp(oceanFloor(CONT_GAIN * continents - continentBias(p.oceanFraction()), island), -1.2, 1.0);

        double m = p.mountainScale();
        double ranges = n.ranges().getValue(px / m + 0.5 * wx, py / m + 0.5 * wy, pz / m + 0.5 * wz);
        double spine = 1.0 - Math.min(1.0, Math.abs(ranges) * RANGE_SHARPNESS);
        double mask = spine * spine * (3.0 - 2.0 * spine);
        double plains = 0.4 + 0.55 * EROSION_GAIN * n.erosion().getValue(px / 900.0, py / 900.0, pz / 900.0);
        out[1] = clamp(plains + (-0.95 - plains) * mask, -1.0, 1.0);

        out[2] = clamp(RIDGE_GAIN * n.ridges().getValue(px / 1100.0, py / 1100.0, pz / 1100.0), -1.0, 1.0);

        double t = latitudeTemperature(Math.toDegrees(signedLat))
                + 0.25 * TEMP_GAIN * n.temperature().getValue(px / 1800.0 + CLIMATE_WARP * wx, py / 1800.0 + CLIMATE_WARP * wy,
                pz / 1800.0 + CLIMATE_WARP * wz) + p.temperatureOffset();
        out[3] = clamp(t, -1.0, 1.0);

        double h = 0.4 * Math.cos(6.0 * signedLat)
                + 0.55 * VEG_GAIN * n.vegetation().getValue(px / 1500.0 + CLIMATE_WARP * wy, py / 1500.0 + CLIMATE_WARP * wz,
                pz / 1500.0 + CLIMATE_WARP * wx) + p.humidityOffset();
        out[4] = clamp(h, -1.0, 1.0);
    }

    /**
     * Keeps deep oceans deep. Minecraft's terrain rises again below continentalness -1.05 (that
     * is where mushroom islands come from), which a planet's wide oceans would reach all over;
     * deep water instead levels off towards -1.0, and rare islands come back where the sparse
     * {@code island} noise peaks far out at sea.
     */
    static double oceanFloor(double continentalness, double island) {
        if (continentalness >= DEEP_OCEAN) {
            return continentalness;
        }
        double f = DEEP_OCEAN - OCEAN_DEPTH * Math.tanh((DEEP_OCEAN - continentalness) / OCEAN_DEPTH);
        double farOut = clamp((-0.8 - f) / 0.15, 0.0, 1.0);
        double peak = clamp((island - 0.55) / 0.2, 0.0, 1.0);
        return f - 0.2 * farOut * peak * peak * (3.0 - 2.0 * peak);
    }

    /** Temperature of latitude {@code degrees} (either hemisphere) before noise. */
    static double latitudeTemperature(double degrees) {
        double d = Math.min(90.0, Math.abs(degrees)) / 10.0;
        int i = Math.min(LATITUDE_TEMPERATURE.length - 2, (int) d);
        return LATITUDE_TEMPERATURE[i] + (LATITUDE_TEMPERATURE[i + 1] - LATITUDE_TEMPERATURE[i]) * (d - i);
    }

    /**
     * Shift making {@code oceanFraction} of the surface end up under water, for continent noise
     * spread {@link #CONT_SPREAD} after gain (logistic estimate of the normal quantile). Counting
     * low coasts, rivers and lakes, terrain is under water up to about continentalness
     * {@link #WET_BELOW} (measured), not just in the ocean proper (below -0.19).
     */
    static double continentBias(double oceanFraction) {
        double quantile = Math.log(oceanFraction / (1.0 - oceanFraction)) / 1.702;
        return CONT_SPREAD * quantile - WET_BELOW;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }
}
