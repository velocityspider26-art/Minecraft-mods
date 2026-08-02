package shipwrights.genesis.client.lod;

import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.voxel.PlanetVoxelBrick;
import shipwrights.genesis.space.voxel.PlanetVoxelBrickKey;
import shipwrights.genesis.space.voxel.PlanetVoxelGreedyMesher;
import shipwrights.genesis.space.voxel.PlanetVoxelMaterial;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchronous CPU mesh cache for streamed planet voxel bricks.
 *
 * <p>Meshing never touches OpenGL. The render thread only consumes completed
 * immutable meshes. Neighbour bricks are sampled while meshing so internal
 * faces at brick borders disappear instead of creating visible seams.</p>
 */
public final class PlanetVoxelMeshCache {
    private static final int MAX_ENTRIES = 16_384;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2,
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger();

                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable,
                            "Genesis-PlanetVoxelMesher-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    thread.setPriority(Thread.MIN_PRIORITY);
                    return thread;
                }
            });
    private static final Map<CacheKey, Entry> CACHE = new ConcurrentHashMap<>();

    private PlanetVoxelMeshCache() {
    }

    public static PlanetVoxelGreedyMesher.Mesh getOrSchedule(ResourceLocation planet,
                                                              PlanetVoxelBrick brick) {
        CacheKey cacheKey = new CacheKey(planet, brick.key());
        Entry existing = CACHE.get(cacheKey);
        if (existing == null || existing.revision != brick.revision()) {
            CompletableFuture<PlanetVoxelGreedyMesher.Mesh> future =
                    CompletableFuture.supplyAsync(() -> mesh(planet, brick), EXECUTOR)
                            .exceptionally(error -> {
                                GenesisMod.LOGGER.warn("[PLANET-VOXEL] failed to mesh {}",
                                        brick.key(), error);
                                return null;
                            });
            Entry replacement = new Entry(brick.revision(), future);
            CACHE.put(cacheKey, replacement);
            existing = replacement;
            trimIfNeeded();
        }
        return existing.future.getNow(null);
    }

    public static void invalidate(ResourceLocation planet, PlanetVoxelBrickKey key) {
        CACHE.remove(new CacheKey(planet, key));
        PlanetVoxelRenderer.invalidateGpu(planet, key);
        // Neighbour border visibility can also change. Remove the six adjacent
        // meshes so their edge faces are rebuilt against the new brick.
        for (int[] offset : OFFSETS) {
            PlanetVoxelBrickKey adjacent = new PlanetVoxelBrickKey(key.face(), key.lod(),
                    key.brickU() + offset[0], key.brickY() + offset[1],
                    key.brickV() + offset[2]);
            CACHE.remove(new CacheKey(planet, adjacent));
            PlanetVoxelRenderer.invalidateGpu(planet, adjacent);
        }
    }

    public static void clear() {
        CACHE.clear();
    }

    private static PlanetVoxelGreedyMesher.Mesh mesh(ResourceLocation planet,
                                                       PlanetVoxelBrick brick) {
        PlanetVoxelBrickKey key = brick.key();
        return PlanetVoxelGreedyMesher.mesh(brick, new PlanetVoxelGreedyMesher.NeighbourLookup() {
            @Override
            public long material(int x, int y, int z) {
                Sample sample = sample(key, x, y, z);
                PlanetVoxelBrick neighbour = PlanetVoxelClientCache.get(planet, sample.key);
                return neighbour == null ? PlanetVoxelMaterial.AIR
                        : neighbour.material(sample.x, sample.y, sample.z);
            }

            @Override
            public int coverage(int x, int y, int z) {
                Sample sample = sample(key, x, y, z);
                PlanetVoxelBrick neighbour = PlanetVoxelClientCache.get(planet, sample.key);
                return neighbour == null ? 0
                        : neighbour.coverage(sample.x, sample.y, sample.z);
            }
        });
    }

    private static Sample sample(PlanetVoxelBrickKey key, int x, int y, int z) {
        int dx = Math.floorDiv(x, PlanetVoxelBrick.EDGE);
        int dy = Math.floorDiv(y, PlanetVoxelBrick.EDGE);
        int dz = Math.floorDiv(z, PlanetVoxelBrick.EDGE);
        PlanetVoxelBrickKey target = new PlanetVoxelBrickKey(key.face(), key.lod(),
                key.brickU() + dx, key.brickY() + dy, key.brickV() + dz);
        return new Sample(target,
                Math.floorMod(x, PlanetVoxelBrick.EDGE),
                Math.floorMod(y, PlanetVoxelBrick.EDGE),
                Math.floorMod(z, PlanetVoxelBrick.EDGE));
    }

    private static void trimIfNeeded() {
        if (CACHE.size() <= MAX_ENTRIES) return;
        int remove = CACHE.size() - MAX_ENTRIES;
        for (CacheKey key : CACHE.keySet()) {
            if (remove-- <= 0) break;
            CACHE.remove(key);
        }
    }

    private static final int[][] OFFSETS = {
            {-1, 0, 0}, {1, 0, 0}, {0, -1, 0},
            {0, 1, 0}, {0, 0, -1}, {0, 0, 1}
    };

    private record CacheKey(ResourceLocation planet, PlanetVoxelBrickKey key) {
    }

    private record Entry(long revision,
                         CompletableFuture<PlanetVoxelGreedyMesher.Mesh> future) {
    }

    private record Sample(PlanetVoxelBrickKey key, int x, int y, int z) {
    }
}
