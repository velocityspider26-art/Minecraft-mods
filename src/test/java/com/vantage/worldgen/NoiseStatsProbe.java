package com.vantage.worldgen;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/** Prints noise statistics used to calibrate the planet model. Runs only with -Dvantage.probe=true. */
class NoiseStatsProbe {
    @Test
    void stats() {
        Assumptions.assumeTrue(Boolean.getBoolean("vantage.probe"));
        double[][] amps = {{1.0, 1.0, 0.8, 0.6, 0.45, 0.3, 0.2}, {1.0, 0.5, 0.25}, {1.0, 0.45, 0.2}, {1.0, 0.6, 0.3},
                {1.0, 0.8, 0.5, 0.3}, {1.0, 0.5}, {1.0, 0.6, 0.3}};
        for (double[] a : amps) {
            NormalNoise n = NormalNoise.create(RandomSource.create(7), new NormalNoise.NoiseParameters(0, a[0], Arrays.copyOfRange(a, 1, a.length)));
            DoubleArrayList v = new DoubleArrayList();
            for (int i = 0; i < 40000; i++) {
                v.add(n.getValue((i % 200) * 0.137 + 3.1, (i / 200) * 0.129 - 7.7, i * 0.0013));
            }
            double[] s = v.toDoubleArray();
            Arrays.sort(s);
            double mean = Arrays.stream(s).average().orElse(0);
            double sd = Math.sqrt(Arrays.stream(s).map(x -> (x - mean) * (x - mean)).average().orElse(0));
            System.out.printf("[probe] amps=%s mean=%.3f sd=%.3f p1=%.3f p10=%.3f p50=%.3f p90=%.3f p99=%.3f max=%.3f%n",
                    Arrays.toString(a), mean, sd, s[s.length / 100], s[s.length / 10], s[s.length / 2], s[s.length * 9 / 10],
                    s[s.length * 99 / 100], s[s.length - 1]);
        }
    }
}
