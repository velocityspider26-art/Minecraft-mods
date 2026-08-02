package shipwrights.genesis.client.lod;

import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.surface.PlanetLodVolumeTile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Asynchronous logical mesh cache for exact volume tiles.
 *
 * <p>The worker performs interval subtraction and side-strip merging once per
 * tile revision. Render frames only transform/upload the finished quads; they
 * no longer rediscover walls and air gaps from thousands of columns every
 * frame.</p>
 */
public final class PlanetVolumeMeshCache {
    public enum Side {
        WEST, EAST, NORTH, SOUTH
    }

    public record TopFace(int x, int z, int y, int stateId, int colour,
                          byte packedLight, byte flags) {
    }

    public record SideFace(Side side, int fixed, int horizontal0, int horizontal1,
                           int y0, int y1, int stateId, int colour,
                           byte packedLight, byte flags) {
    }

    public record Mesh(PlanetLodVolumeTile.Key key, long revision,
                       TopFace[] tops, SideFace[] sides) {
        public int quadCount() {
            return tops.length + sides.length;
        }
    }

    private record CacheKey(ResourceLocation planet, PlanetLodVolumeTile.Key tile) {
    }

    private static final int MAX_MESHES = 8192;
    private static final Object LOCK = new Object();
    private static final LinkedHashMap<CacheKey, Entry> ENTRIES =
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CacheKey, Entry> eldest) {
                    return size() > MAX_MESHES;
                }
            };
    private static final ThreadFactory THREAD_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "Genesis-Planet-Volume-Mesher");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    };
    private static final ExecutorService WORKER = Executors.newFixedThreadPool(2, THREAD_FACTORY);

    private PlanetVolumeMeshCache() {
    }

    /** Returns a ready mesh, or schedules it and returns {@code null}. */
    public static Mesh getOrSchedule(ResourceLocation planet, PlanetLodVolumeTile tile) {
        CacheKey key = new CacheKey(planet, tile.key());
        Entry entry;
        synchronized (LOCK) {
            entry = ENTRIES.get(key);
            if (entry != null && entry.revision == tile.revision()) {
                if (entry.mesh != null) {
                    return entry.mesh;
                }
                if (entry.building.get()) {
                    return null;
                }
            } else {
                entry = new Entry(tile.revision());
                ENTRIES.put(key, entry);
            }
            if (!entry.building.compareAndSet(false, true)) {
                return null;
            }
        }

        Entry target = entry;
        WORKER.execute(() -> {
            Mesh built = null;
            try {
                built = build(tile);
            } catch (Throwable error) {
                GenesisMod.LOGGER.warn("[VOLUME-LOD] mesh build failed for {} {},{}",
                        tile.face(), tile.chunkX(), tile.chunkZ(), error);
            }
            synchronized (LOCK) {
                Entry current = ENTRIES.get(key);
                if (current == target && current.revision == tile.revision()) {
                    current.mesh = built;
                    current.building.set(false);
                }
            }
        });
        return null;
    }

    public static void invalidate(ResourceLocation planet, PlanetLodVolumeTile.Key tile) {
        synchronized (LOCK) {
            ENTRIES.remove(new CacheKey(planet, tile));
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            ENTRIES.clear();
        }
    }

    private static Mesh build(PlanetLodVolumeTile tile) {
        List<TopFace> tops = new ArrayList<>(512);
        List<SideFace> rawSides = new ArrayList<>(2048);

        for (int z = 0; z < PlanetLodVolumeTile.RESOLUTION; z++) {
            for (int x = 0; x < PlanetLodVolumeTile.RESOLUTION; x++) {
                int start = tile.firstSegment(x, z);
                int end = tile.endSegment(x, z);
                for (int segment = start; segment < end; segment++) {
                    int bottom = tile.bottomY()[segment];
                    int top = tile.topY()[segment];
                    if (!occupiedAt(tile, x, z, top)) {
                        tops.add(new TopFace(x, z, top, tile.blockStateIds()[segment],
                                tile.colours()[segment], tile.packedLight()[segment], tile.flags()[segment]));
                    }
                    addExposedSides(tile, rawSides, x, z, segment, bottom, top);
                }
            }
        }

        rawSides.sort(Comparator
                .comparing((SideFace side) -> side.side().ordinal())
                .thenComparingInt(SideFace::fixed)
                .thenComparingInt(SideFace::y0)
                .thenComparingInt(SideFace::y1)
                .thenComparingInt(SideFace::stateId)
                .thenComparingInt(SideFace::colour)
                .thenComparingInt(side -> Byte.toUnsignedInt(side.packedLight()))
                .thenComparingInt(side -> Byte.toUnsignedInt(side.flags()))
                .thenComparingInt(SideFace::horizontal0));

        List<SideFace> merged = new ArrayList<>(rawSides.size());
        for (SideFace side : rawSides) {
            if (!merged.isEmpty()) {
                SideFace previous = merged.get(merged.size() - 1);
                if (canMerge(previous, side)) {
                    merged.set(merged.size() - 1, new SideFace(previous.side(), previous.fixed(),
                            previous.horizontal0(), side.horizontal1(), previous.y0(), previous.y1(),
                            previous.stateId(), previous.colour(), previous.packedLight(), previous.flags()));
                    continue;
                }
            }
            merged.add(side);
        }

        return new Mesh(tile.key(), tile.revision(),
                tops.toArray(TopFace[]::new), merged.toArray(SideFace[]::new));
    }

    private static boolean canMerge(SideFace a, SideFace b) {
        return a.side() == b.side()
                && a.fixed() == b.fixed()
                && a.horizontal1() == b.horizontal0()
                && a.y0() == b.y0() && a.y1() == b.y1()
                && a.stateId() == b.stateId()
                && a.colour() == b.colour()
                && a.packedLight() == b.packedLight()
                && a.flags() == b.flags();
    }

    private static boolean occupiedAt(PlanetLodVolumeTile tile, int x, int z, int y) {
        if (x < 0 || x >= PlanetLodVolumeTile.RESOLUTION
                || z < 0 || z >= PlanetLodVolumeTile.RESOLUTION) {
            return false;
        }
        for (int segment = tile.firstSegment(x, z); segment < tile.endSegment(x, z); segment++) {
            if (y >= tile.bottomY()[segment] && y < tile.topY()[segment]) {
                return true;
            }
        }
        return false;
    }

    private static void addExposedSides(PlanetLodVolumeTile tile, List<SideFace> output,
                                        int x, int z, int segment, int bottom, int top) {
        addDirection(tile, output, x, z, x - 1, z, segment, bottom, top, Side.WEST, x, z, z + 1);
        addDirection(tile, output, x, z, x + 1, z, segment, bottom, top, Side.EAST, x + 1, z, z + 1);
        addDirection(tile, output, x, z, x, z - 1, segment, bottom, top, Side.NORTH, z, x, x + 1);
        addDirection(tile, output, x, z, x, z + 1, segment, bottom, top, Side.SOUTH, z + 1, x, x + 1);
    }

    private static void addDirection(PlanetLodVolumeTile tile, List<SideFace> output,
                                     int sourceX, int sourceZ, int neighbourX, int neighbourZ,
                                     int segment, int bottom, int top, Side side,
                                     int fixed, int horizontal0, int horizontal1) {
        List<int[]> visible = new ArrayList<>(4);
        visible.add(new int[]{bottom, top});
        if (neighbourX >= 0 && neighbourX < PlanetLodVolumeTile.RESOLUTION
                && neighbourZ >= 0 && neighbourZ < PlanetLodVolumeTile.RESOLUTION) {
            for (int neighbour = tile.firstSegment(neighbourX, neighbourZ);
                 neighbour < tile.endSegment(neighbourX, neighbourZ) && !visible.isEmpty(); neighbour++) {
                subtract(visible, tile.bottomY()[neighbour], tile.topY()[neighbour]);
            }
        }
        for (int[] interval : visible) {
            if (interval[1] <= interval[0]) {
                continue;
            }
            output.add(new SideFace(side, fixed, horizontal0, horizontal1,
                    interval[0], interval[1], tile.blockStateIds()[segment],
                    tile.colours()[segment], tile.packedLight()[segment], tile.flags()[segment]));
        }
    }

    private static void subtract(List<int[]> intervals, int cutBottom, int cutTop) {
        for (int index = intervals.size() - 1; index >= 0; index--) {
            int[] value = intervals.get(index);
            int bottom = value[0];
            int top = value[1];
            int overlapBottom = Math.max(bottom, cutBottom);
            int overlapTop = Math.min(top, cutTop);
            if (overlapTop <= overlapBottom) {
                continue;
            }
            intervals.remove(index);
            if (bottom < overlapBottom) {
                intervals.add(new int[]{bottom, overlapBottom});
            }
            if (overlapTop < top) {
                intervals.add(new int[]{overlapTop, top});
            }
        }
    }

    private static final class Entry {
        private final long revision;
        private final AtomicBoolean building = new AtomicBoolean();
        private volatile Mesh mesh;

        private Entry(long revision) {
            this.revision = revision;
        }
    }
}
