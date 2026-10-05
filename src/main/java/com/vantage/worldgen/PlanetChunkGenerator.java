package com.vantage.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The Vantage Planet world generator: Minecraft's noise generator with the planet's own terrain
 * and climate model ({@link PlanetRouter}). Only the planet settings are saved; the noise router
 * is rebuilt from them when the world loads.
 */
public final class PlanetChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<PlanetChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            PlanetSettings.CODEC.optionalFieldOf("planet", PlanetSettings.DEFAULT).forGetter(PlanetChunkGenerator::planet),
            RegistryOps.retrieveGetter(Registries.DENSITY_FUNCTION),
            RegistryOps.retrieveGetter(Registries.NOISE)
    ).apply(i, PlanetChunkGenerator::new));

    private final PlanetSettings planet;

    public PlanetChunkGenerator(BiomeSource biomeSource, PlanetSettings planet, HolderGetter<DensityFunction> functions,
                                HolderGetter<NormalNoise.NoiseParameters> noises) {
        super(biomeSource, Holder.direct(PlanetRouter.settings(planet, functions, noises)));
        this.planet = planet;
    }

    public PlanetSettings planet() {
        return this.planet;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
}
