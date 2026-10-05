package com.vantage.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetModelTest {
    /** Same octaves as data/vantage/worldgen/noise/planet_*.json, seeded directly. */
    static PlanetNoises noises(long seed) {
        RandomSource r = RandomSource.create(seed);
        return new PlanetNoises(n(r, 1.0, 1.0, 0.8, 0.6, 0.45, 0.3, 0.2), n(r, 1.0, 0.5, 0.25), n(r, 1.0, 0.45, 0.2),
                n(r, 1.0, 0.6, 0.3), n(r, 1.0, 0.8, 0.5, 0.3), n(r, 1.0, 0.6, 0.4, 0.25, 0.15), n(r, 1.0, 0.6, 0.4, 0.25, 0.15));
    }

    private static NoiseHolder n(RandomSource r, double... amps) {
        NormalNoise.NoiseParameters p = new NormalNoise.NoiseParameters(0, amps[0], Arrays.copyOfRange(amps, 1, amps.length));
        return new NoiseHolder(Holder.direct(p), NormalNoise.create(r.fork(), p));
    }

    @Test
    void surfaceSharesAreEarthLike() {
        PlanetSettings p = PlanetSettings.DEFAULT;
        PlanetNoises n = noises(42);
        double[] v = new double[5];
        int total = 0, ocean = 0, deep = 0, mountains = 0, land = 0, islands = 0;
        for (int i = 0; i < 200; i++) {
            for (int j = 0; j < 200; j++) {
                PlanetModel.compute(p, n, i * 701.0 - 70_000, j * 467.0 - 46_700, v);
                total++;
                if (v[0] < -1.05) {
                    islands++;
                }
                if (v[0] < -0.19) {
                    ocean++;
                    if (v[0] < -0.455) {
                        deep++;
                    }
                } else {
                    land++;
                    if (v[1] < -0.375) {
                        mountains++;
                    }
                }
            }
        }
        double oceanShare = ocean / (double) total;
        double mountainShare = mountains / (double) land;
        double islandShare = islands / (double) total;
        System.out.printf("[planet] ocean %.2f (deep %.2f), mountains %.2f of land, mushroom islands %.4f%n", oceanShare,
                deep / (double) total, mountainShare, islandShare);
        assertTrue(islandShare > 0.0005 && islandShare < 0.01, "mushroom islands should be rare: " + islandShare);
        assertTrue(oceanShare > 0.35 && oceanShare < 0.6, "ocean share " + oceanShare);
        assertTrue(mountainShare > 0.08 && mountainShare < 0.35, "mountain share " + mountainShare);
    }

    @Test
    void wrapsAroundThePlanet() {
        PlanetSettings p = PlanetSettings.DEFAULT;
        PlanetNoises n = noises(7);
        double[] a = new double[5];
        double[] b = new double[5];
        for (int i = 0; i < 50; i++) {
            double x = i * 913.0 - 20_000;
            double z = i * 337.0 - 9_000;
            PlanetModel.compute(p, n, x, z, a);
            PlanetModel.compute(p, n, x + p.circumference(), z, b);
            for (int k = 0; k < 5; k++) {
                assertEquals(a[k], b[k], 1e-6, "parameter " + k + " at x=" + x);
            }
        }
    }

    @Test
    void climateFollowsEarthsBelts() {
        PlanetSettings p = PlanetSettings.DEFAULT;
        PlanetNoises n = noises(3);
        double[] equator = meanClimate(p, n, 0);
        double[] subtropics = meanClimate(p, n, 22);
        double[] temperate = meanClimate(p, n, 45);
        double[] pole = meanClimate(p, n, 88);
        System.out.printf("[planet] temperature/humidity: equator %.2f/%.2f, subtropics %.2f/%.2f, mid-latitudes %.2f/%.2f, pole %.2f/%.2f%n",
                equator[0], equator[1], subtropics[0], subtropics[1], temperate[0], temperate[1], pole[0], pole[1]);
        assertTrue(equator[0] > 0.25 && equator[0] < 0.55, "the tropics should be warm, not desert-hot: " + equator[0]);
        assertTrue(equator[1] > 0.2, "the tropics should be wet: " + equator[1]);
        assertTrue(subtropics[0] > 0.55 && subtropics[1] < 0.0, "the subtropics should be hot and dry");
        assertTrue(temperate[0] > -0.15 && temperate[0] < 0.2, "mid-latitudes should be temperate: " + temperate[0]);
        assertTrue(pole[0] < -0.6, "the poles should be frozen: " + pole[0]);
        // Both hemispheres alike.
        assertEquals(PlanetModel.latitudeTemperature(-30), PlanetModel.latitudeTemperature(30), 1e-9);
    }

    /** Mean temperature and humidity along a line of latitude (degrees north). */
    private static double[] meanClimate(PlanetSettings p, PlanetNoises n, double latitude) {
        double z = Math.toRadians(p.spawnLatitude() - latitude) * p.radius();
        double[] v = new double[5];
        double t = 0;
        double h = 0;
        for (int i = 0; i < 200; i++) {
            PlanetModel.compute(p, n, i * 941.0, z, v);
            t += v[3];
            h += v[4];
        }
        return new double[]{t / 200, h / 200};
    }
}
