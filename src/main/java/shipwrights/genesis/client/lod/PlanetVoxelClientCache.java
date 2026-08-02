package shipwrights.genesis.client.lod;

import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.space.voxel.PlanetVoxelBrick;
import shipwrights.genesis.space.voxel.PlanetVoxelBrickKey;
import shipwrights.genesis.space.voxel.PlanetVoxelStore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Client-side memory-budgeted cache for streamed planet voxel bricks. */
public final class PlanetVoxelClientCache {
    private static final long DEFAULT_BYTES_PER_PLANET = 1024L * 1024L * 1024L;
    private static final int MAX_REVISION_ENTRIES_PER_PLANET = 262_144;
    private static final Object LOCK = new Object();
    private static final Map<ResourceLocation, PlanetVoxelStore> STORES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Map<PlanetVoxelBrickKey, Long>> REVISIONS =
            new LinkedHashMap<>();
    private static long generation;

    private PlanetVoxelClientCache() {
    }

    public static void accept(ResourceLocation planet, PlanetVoxelBrick brick) {
        boolean accepted;
        synchronized (LOCK) {
            Map<PlanetVoxelBrickKey, Long> revisions =
                    revisionsFor(planet);
            long known = revisions.getOrDefault(brick.key(), -1L);
            accepted = brick.revision() > known;
            if (accepted) {
                revisions.put(brick.key(), brick.revision());
                STORES.computeIfAbsent(planet,
                                ignored -> new PlanetVoxelStore(DEFAULT_BYTES_PER_PLANET))
                        .put(brick);
                generation++;
            }
        }
        if (accepted) PlanetVoxelMeshCache.invalidate(planet, brick.key());
    }

    public static void remove(ResourceLocation planet, PlanetVoxelBrickKey key, long revision) {
        boolean accepted;
        synchronized (LOCK) {
            Map<PlanetVoxelBrickKey, Long> revisions =
                    revisionsFor(planet);
            long known = revisions.getOrDefault(key, -1L);
            accepted = revision > known;
            if (accepted) {
                revisions.put(key, revision);
                PlanetVoxelStore store = STORES.get(planet);
                if (store != null) store.remove(key);
                generation++;
            }
        }
        if (accepted) PlanetVoxelMeshCache.invalidate(planet, key);
    }

    /** Compatibility overload for local invalidation sites without a server revision. */
    public static void remove(ResourceLocation planet, PlanetVoxelBrickKey key) {
        long next;
        synchronized (LOCK) {
            next = revisionsFor(planet)
                    .getOrDefault(key, -1L) + 1L;
        }
        remove(planet, key, next);
    }

    public static PlanetVoxelBrick get(ResourceLocation planet, PlanetVoxelBrickKey key) {
        synchronized (LOCK) {
            PlanetVoxelStore store = STORES.get(planet);
            return store == null ? null : store.get(key);
        }
    }

    public static List<PlanetVoxelBrick> snapshot(ResourceLocation planet,
                                                   Predicate<PlanetVoxelBrickKey> filter,
                                                   int limit) {
        synchronized (LOCK) {
            PlanetVoxelStore store = STORES.get(planet);
            return store == null ? List.of() : store.snapshot(filter, limit);
        }
    }

    public static int size(ResourceLocation planet) {
        synchronized (LOCK) {
            PlanetVoxelStore store = STORES.get(planet);
            return store == null ? 0 : store.size();
        }
    }

    public static boolean hasAny(ResourceLocation planet) {
        return size(planet) > 0;
    }

    public static long usedBytes(ResourceLocation planet) {
        synchronized (LOCK) {
            PlanetVoxelStore store = STORES.get(planet);
            return store == null ? 0L : store.usedBytes();
        }
    }

    /** Monotonic cache mutation counter used to avoid rebuilding render snapshots every frame. */
    public static long generation() {
        synchronized (LOCK) {
            return generation;
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            STORES.clear();
            REVISIONS.clear();
            generation++;
        }
        PlanetVoxelMeshCache.clear();
    }

    private static Map<PlanetVoxelBrickKey, Long> revisionsFor(ResourceLocation planet) {
        return REVISIONS.computeIfAbsent(planet, ignored ->
                new LinkedHashMap<>(4096, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(
                            Map.Entry<PlanetVoxelBrickKey, Long> eldest) {
                        return size() > MAX_REVISION_ENTRIES_PER_PLANET;
                    }
                });
    }
}
