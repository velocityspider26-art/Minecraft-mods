package com.vantage.gen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * Debug aid: how far real terrain lies below and above Minecraft's quick surface estimate
 * (what the sandbox's noise band is sized from). Runs only with -Dvantage.probe=true.
 */
class PreliminarySurfaceProbe {
    @Test
    void probe() {
        Assumptions.assumeTrue(Boolean.getBoolean("vantage.probe"));
        HolderLookup.Provider lookup = VanillaRegistries.createLookup();
        Holder<NoiseGeneratorSettings> settings = lookup.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        RandomState random = RandomState.create(settings.value(), lookup.lookupOrThrow(Registries.NOISE), 8678942899319966093L);
        NoiseSurfaceSampler sampler = new NoiseSurfaceSampler(random, settings.value().noiseSettings());
        DensityFunction initial = random.router().initialDensityWithoutJaggedness();
        Random rnd = new Random(3);
        int n = 12000;
        int[] below = new int[8];
        int maxBelow = 0;
        int maxAbove = 0;
        int[] maxAboveByHi = new int[8];
        for (int i = 0; i < n; i++) {
            int x = rnd.nextInt(200_000) - 100_000;
            int z = rnd.nextInt(200_000) - 100_000;
            // Lowest estimate over the chunk, as the sandbox uses it.
            int lo = Integer.MAX_VALUE;
            int hi = Integer.MIN_VALUE;
            int cx = x & ~15;
            int cz = z & ~15;
            for (int dz = 0; dz <= 16; dz += 4) {
                for (int dx = 0; dx <= 16; dx += 4) {
                    int p = prelim(initial, cx + dx, cz + dz);
                    lo = Math.min(lo, p);
                    hi = Math.max(hi, p);
                }
            }
            int actual = sampler.surfaceY(x, z, 4, 1) - 1;
            int b = lo - actual;
            maxBelow = Math.max(maxBelow, b);
            maxAbove = Math.max(maxAbove, actual - hi);
            int bucket = Math.min(7, Math.max(0, (hi - 40) / 20));
            maxAboveByHi[bucket] = Math.max(maxAboveByHi[bucket], actual - hi);
            for (int k = 0; k < below.length; k++) {
                if (b > 8 * (k + 1)) {
                    below[k]++;
                }
            }
        }
        StringBuilder s = new StringBuilder("[probe] surface below the chunk's lowest estimate by more than:");
        for (int k = 0; k < below.length; k++) {
            s.append(String.format(" %d=%.4f", 8 * (k + 1), below[k] / (double) n));
        }
        System.out.println(s + " max below " + maxBelow + ", max above highest estimate " + maxAbove);
        StringBuilder t = new StringBuilder("[probe] max above the highest estimate, by highest estimate:");
        for (int k = 0; k < 8; k++) {
            t.append(String.format(" <%d:%d", 60 + 20 * k, maxAboveByHi[k]));
        }
        System.out.println(t);
    }

    private static int prelim(DensityFunction f, int x, int z) {
        int qx = x & ~3;
        int qz = z & ~3;
        for (int y = 320; y >= -64; y -= 8) {
            if (f.compute(new DensityFunction.SinglePointContext(qx, y, qz)) > 0.390625) {
                return y;
            }
        }
        return -64;
    }
}
