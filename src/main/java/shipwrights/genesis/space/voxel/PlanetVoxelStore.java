package shipwrights.genesis.space.voxel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Thread-safe memory-budgeted LRU brick store. */
public final class PlanetVoxelStore {
    private final Object lock = new Object();
    private final long maximumBytes;
    private final LinkedHashMap<PlanetVoxelBrickKey, PlanetVoxelBrick> bricks =
            new LinkedHashMap<>(1024, 0.75f, true);
    private long usedBytes;

    public PlanetVoxelStore(long maximumBytes) {
        if (maximumBytes < 16L * 1024L * 1024L) {
            throw new IllegalArgumentException("voxel store budget must be at least 16 MiB");
        }
        this.maximumBytes = maximumBytes;
    }

    public PlanetVoxelBrick get(PlanetVoxelBrickKey key) {
        synchronized (lock) {
            return bricks.get(key);
        }
    }

    public boolean contains(PlanetVoxelBrickKey key) {
        synchronized (lock) {
            return bricks.containsKey(key);
        }
    }

    public void put(PlanetVoxelBrick brick) {
        synchronized (lock) {
            PlanetVoxelBrick previous = bricks.get(brick.key());
            if (previous != null && previous.revision() > brick.revision()) return;
            if (previous != null) usedBytes -= previous.estimatedBytes();
            bricks.put(brick.key(), brick);
            usedBytes += brick.estimatedBytes();
            evictToBudget();
        }
    }

    public PlanetVoxelBrick remove(PlanetVoxelBrickKey key) {
        synchronized (lock) {
            PlanetVoxelBrick removed = bricks.remove(key);
            if (removed != null) usedBytes -= removed.estimatedBytes();
            return removed;
        }
    }

    public int removeIf(Predicate<PlanetVoxelBrickKey> predicate) {
        synchronized (lock) {
            int removed = 0;
            var iterator = bricks.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<PlanetVoxelBrickKey, PlanetVoxelBrick> entry = iterator.next();
                if (predicate.test(entry.getKey())) {
                    usedBytes -= entry.getValue().estimatedBytes();
                    iterator.remove();
                    removed++;
                }
            }
            return removed;
        }
    }

    public List<PlanetVoxelBrick> snapshot(Predicate<PlanetVoxelBrickKey> predicate, int limit) {
        synchronized (lock) {
            List<PlanetVoxelBrick> result = new ArrayList<>(Math.min(limit, bricks.size()));
            for (PlanetVoxelBrick brick : bricks.values()) {
                if (predicate.test(brick.key())) {
                    result.add(brick);
                    if (result.size() >= limit) break;
                }
            }
            return result;
        }
    }

    public long usedBytes() {
        synchronized (lock) {
            return usedBytes;
        }
    }

    public int size() {
        synchronized (lock) {
            return bricks.size();
        }
    }

    public void clear() {
        synchronized (lock) {
            bricks.clear();
            usedBytes = 0L;
        }
    }

    private void evictToBudget() {
        var iterator = bricks.entrySet().iterator();
        while (usedBytes > maximumBytes && iterator.hasNext()) {
            PlanetVoxelBrick removed = iterator.next().getValue();
            usedBytes -= removed.estimatedBytes();
            iterator.remove();
        }
    }
}
