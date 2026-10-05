package com.vantage.client.gen;

import com.vantage.Vantage;
import com.vantage.client.ingest.RegionImporter;
import com.vantage.client.render.Planner;
import com.vantage.client.visual.VisualRegistry;
import com.vantage.core.Lod;
import com.vantage.core.SectionKey;
import com.vantage.core.Voxel;
import com.vantage.gen.NoiseSurfaceSampler;
import com.vantage.gen.TerrainColumn;
import com.vantage.util.WorkerPool;
import com.vantage.world.LodWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
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

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fills the LOD world where nothing has been explored yet, straight from the singleplayer world
 * generator: terrain height from the noise, ground and trees from the biome. It never generates
 * chunks or touches the save, and real data always replaces what it made.
 *
 * <p>Work comes from the planner, which asks for the sections it wants to draw; coarse levels
 * come first because the planner only refines what it already draws.
 */
public final class DistantGenerator implements Planner.Generator {
    private static final int MAX_QUEUED = 48;

    private final LodWorld world;
    private final WorkerPool pool;
    private final VisualRegistry visuals;
    private final Registry<Biome> clientBiomes;
    private final ChunkGenerator generator;
    private final RandomState random;
    private final BiomeSource biomeSource;
    private final Climate.Sampler climate;
    private final ServerLevel level;
    private final @Nullable NoiseGeneratorSettings noise;
    private final BlockState defaultBlock;
    private final int seaLevel;
    private final ThreadLocal<NoiseSurfaceSampler> samplers;
    private final ThreadLocal<TerrainColumn> columns = ThreadLocal.withInitial(TerrainColumn::new);
    private final ConcurrentHashMap<ResourceKey<Biome>, Look> looks = new ConcurrentHashMap<>();
    private final Set<Long> queued = ConcurrentHashMap.newKeySet();
    private final Set<Long> failed = ConcurrentHashMap.newKeySet();
    /** Columns found fully known; data never becomes unknown again, so they are never re-checked. */
    private final Set<Long> complete = ConcurrentHashMap.newKeySet();
    private volatile @Nullable RegionImporter importer;
    private final AtomicLong generatedSections = new AtomicLong();
    private final AtomicLong nanos = new AtomicLong();
    private volatile boolean closed;

    /** Per-biome visual ids. */
    private record Look(BiomeLook look, Holder<Biome> biome, int top, int under, int seabed, int leaves, int water,
                        int ice, int snow, int stone, int deep) {
    }

    private DistantGenerator(LodWorld world, WorkerPool pool, VisualRegistry visuals, Registry<Biome> clientBiomes, ServerLevel level) {
        this.world = world;
        this.pool = pool;
        this.visuals = visuals;
        this.clientBiomes = clientBiomes;
        this.level = level;
        this.generator = level.getChunkSource().getGenerator();
        this.random = level.getChunkSource().randomState();
        this.biomeSource = this.generator.getBiomeSource();
        this.climate = this.random.sampler();
        if (this.generator instanceof NoiseBasedChunkGenerator nb) {
            this.noise = nb.generatorSettings().value();
            this.defaultBlock = this.noise.defaultBlock();
            this.seaLevel = this.noise.seaLevel();
            this.samplers = ThreadLocal.withInitial(() -> new NoiseSurfaceSampler(this.random,
                    this.noise.noiseSettings().clampToHeightAccessor(level)));
        } else {
            this.noise = null;
            this.defaultBlock = Blocks.STONE.defaultBlockState();
            this.seaLevel = this.generator.getSeaLevel();
            this.samplers = null;
        }
    }

    /** A generator for the singleplayer world behind {@code level}, or null if it cannot be used. */
    public static @Nullable DistantGenerator create(LodWorld world, WorkerPool pool, VisualRegistry visuals,
                                                    Registry<Biome> clientBiomes, @Nullable ServerLevel level) {
        if (level == null) {
            return null;
        }
        try {
            DistantGenerator g = new DistantGenerator(world, pool, visuals, clientBiomes, level);
            Vantage.LOGGER.info("Distant terrain generation on for {} ({})", level.dimension().location(),
                    g.noise != null ? "noise" : g.generator.getClass().getSimpleName());
            return g;
        } catch (RuntimeException e) {
            Vantage.LOGGER.warn("Distant terrain generation unavailable for {}: {}", level.dimension().location(), e.toString());
            return null;
        }
    }

    /** Asks for the section column containing {@code key} to be generated. Any thread. */
    @Override
    public void request(long key, double priority) {
        if (this.closed) {
            return;
        }
        long column = SectionKey.of(SectionKey.level(key), SectionKey.x(key), 0, SectionKey.z(key));
        if (this.queued.size() >= MAX_QUEUED || this.failed.contains(column) || this.complete.contains(column)
                || !this.queued.add(column)) {
            return;
        }
        this.pool.submit(priority, () -> {
            try {
                if (!this.closed) {
                    this.generate(column);
                }
            } catch (Throwable t) {
                if (this.failed.add(column) && this.failed.size() < 4) {
                    Vantage.LOGGER.warn("Distant terrain generation failed at {}", SectionKey.toString(column), t);
                }
            } finally {
                this.queued.remove(column);
            }
        });
    }

    public int queued() {
        return this.queued.size();
    }

    /** Leave places the save already has to this importer while its first pass runs. */
    public void setImporter(@Nullable RegionImporter importer) {
        this.importer = importer;
    }

    public long generatedSections() {
        return this.generatedSections.get();
    }

    /** Average milliseconds per generated section column. */
    public double averageMillis() {
        long n = this.generatedSections.get();
        return n == 0 ? 0 : this.nanos.get() / 1e6 / n;
    }

    public void close() {
        this.closed = true;
    }

    private void generate(long column) {
        int level = SectionKey.level(column);
        int sx = SectionKey.x(column);
        int sz = SectionKey.z(column);
        boolean[] unknown = this.world.unknownColumns(level, sx, sz);
        if (unknown == null) {
            this.complete.add(column);
            return;
        }
        long t0 = System.nanoTime();
        int voxel = Lod.voxelBlocks(level);
        int span = Lod.sectionBlocks(level);
        int sections = this.world.verticalSections(level);
        int rows = sections * Lod.SIZE;
        int[][] data = new int[sections][Lod.VOLUME];
        for (int[] d : data) {
            Arrays.fill(d, Voxel.UNKNOWN_AIR);
        }
        int[] col = new int[rows];
        TerrainColumn tc = this.columns.get();
        tc.minY = this.world.minY;
        tc.worldTop = this.world.minY + this.world.height;
        tc.seaLevel = this.seaLevel;
        NoiseSurfaceSampler sampler = this.samplers == null ? null : this.samplers.get();
        RegionImporter importer = this.importer;
        boolean deferred = false;
        int step = Math.max(8, Math.min(32, voxel));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int vz = 0; vz < Lod.SIZE; vz++) {
            int hint = Integer.MIN_VALUE;
            for (int vx = 0; vx < Lod.SIZE; vx++) {
                if (!unknown[vz * Lod.SIZE + vx]) {
                    hint = Integer.MIN_VALUE;
                    continue;
                }
                int bx = sx * span + vx * voxel + (voxel >> 1);
                int bz = sz * span + vz * voxel + (voxel >> 1);
                if (importer != null && importer.pendingAt(bx, bz)) {
                    deferred = true;
                    hint = Integer.MIN_VALUE;
                    continue;
                }
                int h = sampler != null
                        ? sampler.surfaceY(bx, bz, step, voxel, hint, 32 + (voxel >> 1))
                        : this.generator.getBaseHeight(bx, bz, Heightmap.Types.OCEAN_FLOOR_WG, this.level, this.random);
                hint = h;
                this.describe(tc, bx, bz, h, voxel, pos);
                tc.fill(voxel, rows, col, 0, 1);
                for (int r = 0; r < rows; r++) {
                    data[r >> Lod.SIZE_BITS][Lod.index(vx, r & (Lod.SIZE - 1), vz)] = col[r];
                }
            }
        }

        int[] uniforms = new int[sections];
        int[][] arrays = new int[sections][];
        for (int y = 0; y < sections; y++) {
            int[] d = data[y];
            int first = d[0];
            boolean uniform = true;
            for (int i = 1; i < Lod.VOLUME; i++) {
                if (d[i] != first) {
                    uniform = false;
                    break;
                }
            }
            if (uniform) {
                uniforms[y] = first;
            } else {
                arrays[y] = d;
            }
        }
        this.world.fillGenerated(level, sx, sz, arrays, uniforms);
        if (!deferred) {
            this.generatedSections.incrementAndGet();
        }
        this.nanos.addAndGet(System.nanoTime() - t0);
    }

    /** Sets up {@code tc} for the column at {@code (x, z)} whose surface is {@code h}. */
    private void describe(TerrainColumn tc, int x, int z, int h, int voxel, BlockPos.MutableBlockPos pos) {
        int top = this.world.minY + this.world.height - 1;
        int biomeY = Math.max(this.world.minY, Math.min(top, Math.max(h - 1, this.seaLevel - 1)));
        Holder<Biome> biome = this.biomeSource.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(biomeY),
                QuartPos.fromBlock(z), this.climate);
        Look look = this.look(biome);
        tc.surface = h;
        tc.topVid = look.top;
        tc.underVid = look.under;
        tc.seabedVid = look.seabed;
        tc.stoneVid = look.stone;
        tc.deepVid = look.deep;
        tc.waterVid = look.water;
        Biome b = biome.value();
        boolean land = h >= this.seaLevel;
        if (land && b.hasPrecipitation() && b.coldEnoughToSnow(pos.set(x, h, z))) {
            tc.topVid = look.snow;
        }
        tc.iceVid = !land && b.coldEnoughToSnow(pos.set(x, this.seaLevel, z)) ? look.ice : -1;
        tc.leavesVid = -1;
        if (land && look.leaves >= 0 && hash(x, z) < canopyChance(look.look.canopy(), voxel)) {
            int th = look.look.treeHeight();
            tc.leavesVid = look.leaves;
            tc.canopyBottom = h + Math.max(1, th - 3);
            tc.canopyTop = h + th + 1;
        }
    }

    /**
     * Chance that a voxel column shows tree tops. Fine levels scatter single trees; coarse voxels
     * span many trees and, like mipped real data, show leaves only where trees cover most ground.
     */
    static float canopyChance(float canopy, int voxel) {
        float sharpness = Math.min(16f, voxel);
        return Math.max(0f, Math.min(1f, (canopy - 0.5f) * sharpness + 0.5f));
    }

    private static float hash(int x, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 40) / (float) (1L << 24);
    }

    private Look look(Holder<Biome> biome) {
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        if (key == null) {
            return this.makeLook(biome);
        }
        Look l = this.looks.get(key);
        if (l == null) {
            l = this.looks.computeIfAbsent(key, k -> this.makeLook(biome));
        }
        return l;
    }

    private Look makeLook(Holder<Biome> biome) {
        BiomeLook look = BiomeLook.resolve(biome, this.defaultBlock);
        Holder<Biome> client = biome.unwrapKey().flatMap(this.clientBiomes::getHolder).<Holder<Biome>>map(h -> h).orElse(null);
        return new Look(look, biome,
                this.vid(look.top(), client),
                this.vid(look.under(), client),
                this.vid(look.seabed(), client),
                look.leaves() == null ? -1 : this.vid(look.leaves(), client),
                this.vid(Blocks.WATER.defaultBlockState(), client),
                this.vid(Blocks.ICE.defaultBlockState(), client),
                this.vid(Blocks.SNOW_BLOCK.defaultBlockState(), client),
                this.vid(this.noise != null ? this.defaultBlock : Blocks.STONE.defaultBlockState(), client),
                this.vid(this.noise != null && this.defaultBlock.is(Blocks.STONE) ? Blocks.DEEPSLATE.defaultBlockState() : this.defaultBlock, client));
    }

    private int vid(BlockState state, @Nullable Holder<Biome> biome) {
        boolean tinted = this.visuals.analyzer().shape(state).biomeTinted();
        return this.visuals.idFor(state, tinted ? biome : null);
    }
}
