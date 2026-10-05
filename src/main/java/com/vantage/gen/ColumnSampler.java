package com.vantage.gen;

import com.vantage.core.Lod;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads terrain surface heights and biomes for whole grids of LOD voxel columns straight from a
 * server world's generator, without generating chunks. Used by singleplayer games directly and by
 * servers answering distant-terrain requests. Thread-safe.
 */
public final class ColumnSampler {
    /** Columns to leave out (for example, ones the save already holds). */
    public interface Skip {
        boolean test(int blockX, int blockZ);
    }

    private final ServerLevel level;
    private final ChunkGenerator generator;
    private final RandomState random;
    private final BiomeSource biomeSource;
    private final Climate.Sampler climate;
    private final @Nullable NoiseGeneratorSettings noise;
    private final BlockState defaultBlock;
    private final int seaLevel;
    private final int minY;
    private final int height;
    private final @Nullable ThreadLocal<NoiseSurfaceSampler> samplers;
    private final ConcurrentHashMap<ResourceKey<Biome>, BiomeLook> looks = new ConcurrentHashMap<>();

    public ColumnSampler(ServerLevel level) {
        this.level = level;
        this.generator = level.getChunkSource().getGenerator();
        this.random = level.getChunkSource().randomState();
        this.biomeSource = this.generator.getBiomeSource();
        this.climate = this.random.sampler();
        this.minY = level.getMinBuildHeight();
        this.height = level.getHeight();
        if (this.generator instanceof NoiseBasedChunkGenerator nb) {
            NoiseGeneratorSettings settings = nb.generatorSettings().value();
            this.noise = settings;
            this.defaultBlock = settings.defaultBlock();
            this.seaLevel = settings.seaLevel();
            this.samplers = ThreadLocal.withInitial(() -> new NoiseSurfaceSampler(this.random, settings.noiseSettings().clampToHeightAccessor(level)));
        } else {
            this.noise = null;
            this.defaultBlock = Blocks.STONE.defaultBlockState();
            this.seaLevel = this.generator.getSeaLevel();
            this.samplers = null;
        }
    }

    public ServerLevel level() {
        return this.level;
    }

    public int seaLevel() {
        return this.seaLevel;
    }

    public BlockState defaultBlock() {
        return this.defaultBlock;
    }

    /** True when the generator is noise based (deepslate under stone then makes sense). */
    public boolean noiseBased() {
        return this.noise != null;
    }

    public BiomeLook look(Holder<Biome> biome) {
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        if (key == null) {
            return BiomeLook.resolve(biome, this.defaultBlock);
        }
        BiomeLook l = this.looks.get(key);
        return l != null ? l : this.looks.computeIfAbsent(key, k -> BiomeLook.resolve(biome, this.defaultBlock));
    }

    /**
     * Samples the voxel columns {@code columns} selects in the 32×32 grid of section column
     * {@code (sx, sz)} at {@code lodLevel}, at the centre of each voxel: {@code heights[i]} is one
     * above the top solid block, {@code biomes[i]} the biome there. Columns {@code skip} rejects
     * get {@link Integer#MIN_VALUE}.
     *
     * @return number of columns sampled
     */
    public int sample(int lodLevel, int sx, int sz, boolean[] columns, int[] heights, Holder<Biome>[] biomes, @Nullable Skip skip) {
        int voxel = Lod.voxelBlocks(lodLevel);
        long span = Lod.sectionBlocks(lodLevel);
        NoiseSurfaceSampler sampler = this.samplers == null ? null : this.samplers.get();
        int step = Math.max(8, Math.min(32, voxel));
        int top = this.minY + this.height - 1;
        int count = 0;
        for (int vz = 0; vz < Lod.SIZE; vz++) {
            int hint = Integer.MIN_VALUE;
            for (int vx = 0; vx < Lod.SIZE; vx++) {
                int i = vz * Lod.SIZE + vx;
                heights[i] = Integer.MIN_VALUE;
                if (!columns[i]) {
                    hint = Integer.MIN_VALUE;
                    continue;
                }
                int bx = (int) (sx * span + (long) vx * voxel + (voxel >> 1));
                int bz = (int) (sz * span + (long) vz * voxel + (voxel >> 1));
                if (skip != null && skip.test(bx, bz)) {
                    hint = Integer.MIN_VALUE;
                    continue;
                }
                int h = sampler != null
                        ? sampler.surfaceY(bx, bz, step, voxel, hint, 32 + (voxel >> 1))
                        : this.generator.getBaseHeight(bx, bz, Heightmap.Types.OCEAN_FLOOR_WG, this.level, this.random);
                hint = h;
                heights[i] = h;
                int biomeY = Math.max(this.minY, Math.min(top, Math.max(h - 1, this.seaLevel - 1)));
                biomes[i] = this.biomeSource.getNoiseBiome(QuartPos.fromBlock(bx), QuartPos.fromBlock(biomeY), QuartPos.fromBlock(bz), this.climate);
                count++;
            }
        }
        return count;
    }
}
