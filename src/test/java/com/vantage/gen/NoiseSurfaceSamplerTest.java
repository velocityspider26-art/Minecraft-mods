package com.vantage.gen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoiseSurfaceSamplerTest {
    private static final long SEED = 8678942899319966093L;

    @Test
    void matchesVanillaHeightmapClosely() {
        HolderLookup.Provider lookup = VanillaRegistries.createLookup();
        Holder<NoiseGeneratorSettings> settings = lookup.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        RandomState random = RandomState.create(settings.value(), lookup.lookupOrThrow(Registries.NOISE), SEED);
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
                MultiNoiseBiomeSource.createFromPreset(lookup.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                        .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD)), settings);
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        NoiseSurfaceSampler sampler = new NoiseSurfaceSampler(random, settings.value().noiseSettings());

        Random rnd = new Random(1);
        int n = 400;
        int close = 0;
        int maxDiff = 0;
        for (int i = 0; i < n; i++) {
            int x = rnd.nextInt(20000) - 10000;
            int z = rnd.nextInt(20000) - 10000;
            int vanilla = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, random);
            int ours = sampler.surfaceY(x, z, 8, 1);
            int diff = Math.abs(vanilla - ours);
            maxDiff = Math.max(maxDiff, diff);
            if (diff <= 3) {
                close++;
            }
        }
        System.out.printf("[bench] surface vs vanilla: %d/%d within 3 blocks, max diff %d%n", close, n, maxDiff);
        assertTrue(close >= n * 9 / 10, "surface heights should mostly match vanilla, got " + close + "/" + n);

        // Hinted search (start just above the neighbour's surface) must agree with the full one.
        int agree = 0;
        int hint = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            int x = 5000 + i * 16;
            int full = sampler.surfaceY(x, 777, 8, 1);
            int hinted = sampler.surfaceY(x, 777, 8, 1, hint, 48);
            if (full == hinted) {
                agree++;
            }
            hint = hinted;
        }
        System.out.printf("[bench] hinted search agrees on %d/%d columns%n", agree, n);
        assertTrue(agree >= n * 97 / 100, "hinted search should almost always match, got " + agree + "/" + n);

        // Speed at the settings used for distant terrain (spacing = voxel size of the level).
        for (int[] cfg : new int[][]{{8, 1, 1}, {16, 8, 8}, {32, 64, 64}, {32, 512, 512}}) {
            int cols = 4096;
            int spacing = cfg[2];
            int before = sampler.evaluations();
            long t0 = System.nanoTime();
            for (int row = 0; row < 64; row++) {
                int h = Integer.MIN_VALUE;
                for (int col = 0; col < 64; col++) {
                    h = sampler.surfaceY(col * spacing + 100_000, row * spacing, cfg[0], cfg[1], h, 32 + spacing / 2);
                }
            }
            double us = (System.nanoTime() - t0) / 1e3 / cols;
            System.out.printf("[bench] surface step %d cell %d spacing %d: %.1f us/column, %.1f evaluations/column%n",
                    cfg[0], cfg[1], spacing, us, (sampler.evaluations() - before) / (double) cols);
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < 4096; i++) {
            generator.getBaseHeight((i & 63) * 64 + 200_000, (i >> 6) * 64, Heightmap.Types.OCEAN_FLOOR_WG, height, random);
        }
        System.out.printf("[bench] vanilla getBaseHeight: %.1f us/column%n", (System.nanoTime() - t0) / 1e3 / 4096);
    }
}
