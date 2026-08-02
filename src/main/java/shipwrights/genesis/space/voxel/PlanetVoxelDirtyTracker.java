package shipwrights.genesis.space.voxel;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/** Deduplicated priority queue for chunk/brick ingestion and mip rebuilds. */
public final class PlanetVoxelDirtyTracker {
    public enum Reason {
        BLOCK_CHANGE(4),
        CHUNK_LOAD(3),
        ORBIT_REQUEST(2),
        BACKGROUND_IMPORT(1);

        private final int urgency;

        Reason(int urgency) {
            this.urgency = urgency;
        }
    }

    public record Task(PlanetVoxelBrickKey key, Reason reason, double distanceSq,
                       long sequence, long version) implements Comparable<Task> {
        public Task {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(reason, "reason");
        }

        @Override
        public int compareTo(Task other) {
            int urgency = Integer.compare(other.reason.urgency, reason.urgency);
            if (urgency != 0) return urgency;
            int distance = Double.compare(distanceSq, other.distanceSq);
            if (distance != 0) return distance;
            return Long.compare(sequence, other.sequence);
        }
    }

    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentHashMap<PlanetVoxelBrickKey, Long> versions = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();

    public void mark(PlanetVoxelBrickKey key, Reason reason, double distanceSq) {
        long version = versions.merge(key, 1L, Long::sum);
        queue.offer(new Task(key, reason, Math.max(0.0, distanceSq),
                sequence.getAndIncrement(), version));
    }

    public Task poll() {
        while (true) {
            Task task = queue.poll();
            if (task == null) return null;
            Long current = versions.get(task.key());
            if (current != null && current == task.version()
                    && versions.remove(task.key(), current)) {
                return task;
            }
        }
    }

    public int pendingUnique() {
        return versions.size();
    }

    public void clear() {
        versions.clear();
        queue.clear();
    }
}
