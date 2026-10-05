package com.vantage.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ChunkTrackingView;

import java.util.Arrays;

/**
 * Which chunk columns vanilla is drawing right now: loaded and inside the vanilla view distance,
 * using the same distance test vanilla's section culling uses. LODs are never drawn there.
 * Built on the main thread; snapshots are immutable and shared with the planner and renderer.
 */
public final class CoverageMap {
    public record Snapshot(int originX, int originZ, int size, byte[] covered, long version) {
        public static final Snapshot EMPTY = new Snapshot(0, 0, 0, new byte[0], 0);

        public boolean covered(int cx, int cz) {
            int x = cx - this.originX;
            int z = cz - this.originZ;
            return x >= 0 && z >= 0 && x < this.size && z < this.size && this.covered[z * this.size + x] != 0;
        }

        /** True if every chunk in the inclusive rectangle is covered. */
        public boolean allCovered(int minX, int minZ, int maxX, int maxZ) {
            if (minX < this.originX || minZ < this.originZ || maxX >= this.originX + this.size || maxZ >= this.originZ + this.size) {
                return false;
            }
            for (int z = minZ; z <= maxZ; z++) {
                int row = (z - this.originZ) * this.size - this.originX;
                for (int x = minX; x <= maxX; x++) {
                    if (this.covered[row + x] == 0) {
                        return false;
                    }
                }
            }
            return true;
        }

        /** True if any chunk in the inclusive rectangle is covered. */
        public boolean anyCovered(int minX, int minZ, int maxX, int maxZ) {
            int x0 = Math.max(minX, this.originX), z0 = Math.max(minZ, this.originZ);
            int x1 = Math.min(maxX, this.originX + this.size - 1), z1 = Math.min(maxZ, this.originZ + this.size - 1);
            for (int z = z0; z <= z1; z++) {
                int row = (z - this.originZ) * this.size - this.originX;
                for (int x = x0; x <= x1; x++) {
                    if (this.covered[row + x] != 0) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private volatile Snapshot current = Snapshot.EMPTY;
    private long version;

    public Snapshot current() {
        return this.current;
    }

    /** Recomputes coverage around the camera chunk. Main thread. */
    public void update(ClientLevel level, int centerX, int centerZ, int viewDistance) {
        int radius = viewDistance + 1;
        int size = radius * 2 + 1;
        int originX = centerX - radius;
        int originZ = centerZ - radius;
        byte[] covered = new byte[size * size];
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int cx = originX + x;
                int cz = originZ + z;
                if (ChunkTrackingView.isInViewDistance(centerX, centerZ, viewDistance, cx, cz)
                        && level.getChunkSource().getChunk(cx, cz, false) != null) {
                    covered[z * size + x] = 1;
                }
            }
        }
        Snapshot old = this.current;
        if (old.originX == originX && old.originZ == originZ && old.size == size && Arrays.equals(old.covered, covered)) {
            return;
        }
        this.current = new Snapshot(originX, originZ, size, covered, ++this.version);
    }

    public void clear() {
        this.current = Snapshot.EMPTY;
    }
}
