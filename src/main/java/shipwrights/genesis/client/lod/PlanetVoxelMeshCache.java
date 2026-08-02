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
    private static final AtomicInteger IN_FLIGHT = new AtomicInteger();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            shipwrights.genesis.config.GenesisClientConfig.getPlanetMeshWorkers(),
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
            IN_FLIGHT.incrementAndGet();
            shipwrights.genesis.space.planet.PlanetRenderDiagnostics.onMeshJobQueued();
            CompletableFuture<PlanetVoxelGreedyMesher.Mesh> future =
                    CompletableFuture.supplyAsync(() -> mesh(planet, brick), EXECUTOR)
                            .exceptionally(error -> {
                                GenesisMod.LOGGER.warn("[PLANET-VOXEL] failed to mesh {}",
                                        brick.key(), error);
                                return null;
                            })
                            .whenComplete((mesh, error) -> {
                                IN_FLIGHT.decrementAndGet();
                                shipwrights.genesis.space.planet.PlanetRenderDiagnostics
                                        .onMeshJobCompleted();
                            });
            Entry replacement = new Entry(brick.revision(), future);
            CACHE.put(cacheKey, replacement);
            existing = replacement;
            trimIfNeeded();
        }
        return existing.future.getNow(null);
    }

    /** A finished mesh if one exists, without starting work for a missing one. */
    public static PlanetVoxelGreedyMesher.Mesh peek(ResourceLocation planet,
                                                     PlanetVoxelBrick brick) {
        Entry entry = CACHE.get(new CacheKey(planet, brick.key()));
        if (entry == null || entry.revision != brick.revision()) return null;
        return entry.future.getNow(null);
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

    /** Mesh jobs submitted but not yet finished. Reported by the debug overlay. */
    public static int queuedJobs() {
        return Math.max(0, IN_FLIGHT.get());
    }

    private static PlanetVoxelGreedyMesher.Mesh mesh(ResourceLocation planet,
                                                       PlanetVoxelBrick brick) {
        PlanetVoxelBrickKey key = brick.key();
        // Fetch the six neighbours once instead of per boundary voxel. The
        // client cache is guarded by one lock, and the mesher asks about every
        // voxel on every face, so the naive version took that lock on the order
        // of a thousand times per brick -- on three worker threads, against a
        // render thread that needs the same lock to snapshot and to size the
        // cache. That contention is felt as frame stutter, not as slow meshing.
        PlanetVoxelBrick[] neighbours = new PlanetVoxelBrick[OFFSETS.length];
        for (int index = 0; index < OFFSETS.length; index++) {
            int[] offset = OFFSETS[index];
            neighbours[index] = PlanetVoxelClientCache.get(planet, new PlanetVoxelBrickKey(
                    key.face(), key.lod(), key.brickU() + offset[0],
                    key.brickY() + offset[1], key.brickV() + offset[2]));
        }

        return PlanetVoxelGreedyMesher.mesh(brick, new PlanetVoxelGreedyMesher.NeighbourLookup() {
            @Override
            public long material(int x, int y, int z) {
                Resolved resolved = resolve(x, y, z);
                return resolved == null ? PlanetVoxelMaterial.AIR
                        : resolved.brick.material(resolved.x, resolved.y, resolved.z);
            }

            @Override
            public int coverage(int x, int y, int z) {
                Resolved resolved = resolve(x, y, z);
                return resolved == null ? 0
                        : resolved.brick.coverage(resolved.x, resolved.y, resolved.z);
            }

            private Resolved resolve(int x, int y, int z) {
                int dx = Math.floorDiv(x, PlanetVoxelBrick.EDGE);
                int dy = Math.floorDiv(y, PlanetVoxelBrick.EDGE);
                int dz = Math.floorDiv(z, PlanetVoxelBrick.EDGE);
                PlanetVoxelBrick target;
                if (dx == 0 && dy == 0 && dz == 0) {
                    target = brick;
                } else {
                    int index = neighbourIndex(dx, dy, dz);
                    // Diagonals are never sampled by face meshing; if one ever
                    // is, treating it as absent hides a face rather than
                    // reintroducing a per-voxel cache lookup.
                    target = index < 0 ? null : neighbours[index];
                }
                if (target == null) return null;
                return new Resolved(target,
                        Math.floorMod(x, PlanetVoxelBrick.EDGE),
                        Math.floorMod(y, PlanetVoxelBrick.EDGE),
                        Math.floorMod(z, PlanetVoxelBrick.EDGE));
            }
        });
    }

    private static int neighbourIndex(int dx, int dy, int dz) {
        for (int index = 0; index < OFFSETS.length; index++) {
            int[] offset = OFFSETS[index];
            if (offset[0] == dx && offset[1] == dy && offset[2] == dz) return index;
        }
        return -1;
    }

    private record Resolved(PlanetVoxelBrick brick, int x, int y, int z) {
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

}
