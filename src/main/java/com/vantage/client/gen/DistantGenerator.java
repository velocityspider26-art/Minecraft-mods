package com.vantage.client.gen;

import com.vantage.Vantage;
import com.vantage.client.render.Planner;
import com.vantage.client.visual.VisualRegistry;
import com.vantage.core.Lod;
import com.vantage.core.SectionKey;
import com.vantage.core.Voxel;
import com.vantage.gen.BiomeLook;
import com.vantage.gen.TerrainColumn;
import com.vantage.util.WorkerPool;
import com.vantage.world.LodWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Fills the LOD world where nothing has been explored yet, from the world generator: terrain
 * height and biome per voxel column, turned into ground, water, snow and trees. It never generates
 * chunks or touches the save, and real data always replaces what it made.
 *
 * <p>Samples come from a {@link Source}: the local world generator in singleplayer (and for the
 * host of a game opened to friends), or the server for everyone else. Work comes from the
 * planner, which asks for the sections it wants to draw; coarse levels come first because the
 * planner only refines what it already draws.
 */
public final class DistantGenerator implements Planner.Generator {
    private static final int MAX_QUEUED = 48;

    /** Where terrain samples come from. */
    public interface Source {
        /** True if {@link #sample} answers before returning, on the calling thread. */
        boolean local();

        /** Most requests to have outstanding at once. */
        int capacity();

        int seaLevel();

        /** Base rock of the world (stone in the overworld). */
        BlockState stone();

        /**
         * Samples the voxel columns {@code columns} selects in section column {@code (sx, sz)} at
         * {@code level}; calls {@code done} exactly once, with null to give up for now.
         */
        void sample(int level, int sx, int sz, boolean[] columns, Consumer<Samples> done);
    }

    /**
     * Sampled columns ({@code z * 32 + x}): surface height ({@link Integer#MIN_VALUE} when not
     * sampled), the client's biome, and how that biome looks.
     */
    public record Samples(int[] heights, Holder<Biome>[] biomes, BiomeLook[] looks) {
    }

    /** Per-biome visual ids. */
    private record Look(BiomeLook look, int top, int under, int seabed, int leaves, int water, int ice, int snow, int stone, int deep) {
    }

    private final LodWorld world;
    private final WorkerPool pool;
    private final VisualRegistry visuals;
    private final Source source;
    private final ThreadLocal<TerrainColumn> columns = ThreadLocal.withInitial(TerrainColumn::new);
    private final ConcurrentHashMap<ResourceKey<Biome>, Look> looks = new ConcurrentHashMap<>();
    private final Set<Long> queued = ConcurrentHashMap.newKeySet();
    private final Set<Long> failed = ConcurrentHashMap.newKeySet();
    /** Columns found fully known; data never becomes unknown again, so they are never re-checked. */
    private final Set<Long> complete = ConcurrentHashMap.newKeySet();
    private final AtomicInteger outstanding = new AtomicInteger();
    private final AtomicLong generatedSections = new AtomicLong();
    private final AtomicLong nanos = new AtomicLong();
    private volatile boolean closed;

    public DistantGenerator(LodWorld world, WorkerPool pool, VisualRegistry visuals, Source source) {
        this.world = world;
        this.pool = pool;
        this.visuals = visuals;
        this.source = source;
    }

    public boolean local() {
        return this.source.local();
    }

    /** Asks for the section column containing {@code key} to be generated. Any thread. */
    @Override
    public void request(long key, double priority) {
        if (this.closed) {
            return;
        }
        long column = SectionKey.of(SectionKey.level(key), SectionKey.x(key), 0, SectionKey.z(key));
        if (this.queued.size() >= MAX_QUEUED || this.failed.contains(column) || this.complete.contains(column)
                || (!this.source.local() && this.outstanding.get() >= this.source.capacity())
                || !this.queued.add(column)) {
            return;
        }
        this.pool.submit(priority, () -> this.start(column, priority));
    }

    public int queued() {
        return this.queued.size();
    }

    public long generatedSections() {
        return this.generatedSections.get();
    }

    /** Average milliseconds of local work per generated section column. */
    public double averageMillis() {
        long n = this.generatedSections.get();
        return n == 0 ? 0 : this.nanos.get() / 1e6 / n;
    }

    public void close() {
        this.closed = true;
    }

    private void start(long column, double priority) {
        try {
            if (this.closed) {
                this.queued.remove(column);
                return;
            }
            int level = SectionKey.level(column);
            boolean[] unknown = this.world.unknownColumns(level, SectionKey.x(column), SectionKey.z(column));
            if (unknown == null) {
                this.complete.add(column);
                this.queued.remove(column);
                return;
            }
            boolean local = this.source.local();
            if (!local) {
                this.outstanding.incrementAndGet();
            }
            this.source.sample(level, SectionKey.x(column), SectionKey.z(column), unknown, samples -> {
                if (!local) {
                    this.outstanding.decrementAndGet();
                }
                if (samples == null || this.closed) {
                    this.queued.remove(column);
                } else if (local) {
                    this.finish(column, samples);
                } else {
                    this.pool.submit(priority, () -> this.finish(column, samples));
                }
            });
        } catch (Throwable t) {
            this.fail(column, t);
        }
    }

    private void fail(long column, Throwable t) {
        this.queued.remove(column);
        if (this.failed.add(column) && this.failed.size() < 4) {
            Vantage.LOGGER.warn("Distant terrain generation failed at {}", SectionKey.toString(column), t);
        }
    }

    private void finish(long column, Samples s) {
        try {
            if (this.closed) {
                return;
            }
            long t0 = System.nanoTime();
            int level = SectionKey.level(column);
            int sx = SectionKey.x(column);
            int sz = SectionKey.z(column);
            int voxel = Lod.voxelBlocks(level);
            long span = Lod.sectionBlocks(level);
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
            tc.seaLevel = this.source.seaLevel();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int i = 0; i < Lod.AREA; i++) {
                int h = s.heights()[i];
                if (h == Integer.MIN_VALUE) {
                    continue;
                }
                int vx = i & (Lod.SIZE - 1);
                int vz = i >> Lod.SIZE_BITS;
                int bx = (int) (sx * span + (long) vx * voxel + (voxel >> 1));
                int bz = (int) (sz * span + (long) vz * voxel + (voxel >> 1));
                this.describe(tc, bx, bz, h, voxel, s.biomes()[i], s.looks()[i], pos);
                tc.fill(voxel, rows, col, 0, 1);
                for (int r = 0; r < rows; r++) {
                    data[r >> Lod.SIZE_BITS][Lod.index(vx, r & (Lod.SIZE - 1), vz)] = col[r];
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
            this.generatedSections.incrementAndGet();
            this.nanos.addAndGet(System.nanoTime() - t0);
        } catch (Throwable t) {
            this.fail(column, t);
        } finally {
            this.queued.remove(column);
        }
    }

    /** Sets up {@code tc} for the column at {@code (x, z)} whose surface is {@code h}. */
    private void describe(TerrainColumn tc, int x, int z, int h, int voxel, Holder<Biome> biome, BiomeLook biomeLook,
                          BlockPos.MutableBlockPos pos) {
        Look look = this.look(biome, biomeLook);
        int seaLevel = tc.seaLevel;
        tc.surface = h;
        tc.topVid = look.top;
        tc.underVid = look.under;
        tc.seabedVid = look.seabed;
        tc.stoneVid = look.stone;
        tc.deepVid = look.deep;
        tc.waterVid = look.water;
        Biome b = biome.value();
        boolean land = h >= seaLevel;
        if (land && b.hasPrecipitation() && b.coldEnoughToSnow(pos.set(x, h, z))) {
            tc.topVid = look.snow;
        }
        tc.iceVid = !land && b.coldEnoughToSnow(pos.set(x, seaLevel, z)) ? look.ice : -1;
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

    private Look look(Holder<Biome> biome, BiomeLook look) {
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        if (key == null) {
            return this.makeLook(biome, look);
        }
        Look l = this.looks.get(key);
        return l != null ? l : this.looks.computeIfAbsent(key, k -> this.makeLook(biome, look));
    }

    private Look makeLook(Holder<Biome> biome, BiomeLook look) {
        BlockState stone = this.source.stone();
        BlockState deep = stone.is(Blocks.STONE) ? Blocks.DEEPSLATE.defaultBlockState() : stone;
        return new Look(look,
                this.vid(look.top(), biome),
                this.vid(look.under(), biome),
                this.vid(look.seabed(), biome),
                look.leaves() == null ? -1 : this.vid(look.leaves(), biome),
                this.vid(Blocks.WATER.defaultBlockState(), biome),
                this.vid(Blocks.ICE.defaultBlockState(), biome),
                this.vid(Blocks.SNOW_BLOCK.defaultBlockState(), biome),
                this.vid(stone, biome),
                this.vid(deep, biome));
    }

    private int vid(BlockState state, @Nullable Holder<Biome> biome) {
        boolean tinted = this.visuals.analyzer().shape(state).biomeTinted();
        return this.visuals.idFor(state, tinted ? biome : null);
    }
}
