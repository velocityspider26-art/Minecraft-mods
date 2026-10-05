package com.vantage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Arrays;

/**
 * Which chunk columns vanilla is drawing right now: loaded, inside the vanilla view distance (same
 * test vanilla's section culling uses) and with the surface section already built by vanilla.
 * LODs are not drawn there. Columns vanilla has not built yet (the outer ring, whose neighbours are
 * missing, or areas that just came into view) are left to the LODs, so they fill vanilla's holes.
 * Built on the main thread; snapshots are immutable and shared with the planner and renderer.
 */
public final class CoverageMap {
    /** Vanilla will draw the column (loaded, inside its view distance). LOD water is hidden there. */
    public static final int LOADED = 1;
    /** Vanilla has built the column's surface. All LOD geometry is hidden there. */
    public static final int BUILT = 2;

    /** {@code covered} holds {@link #LOADED} | {@link #BUILT} bits per chunk column, row-major by z. */
    public record Snapshot(int originX, int originZ, int size, byte[] covered, long version) {
        public static final Snapshot EMPTY = new Snapshot(0, 0, 0, new byte[0], 0);

        /** True if every chunk in the inclusive rectangle is fully drawn by vanilla. */
        public boolean allCovered(int minX, int minZ, int maxX, int maxZ) {
            if (minX < this.originX || minZ < this.originZ || maxX >= this.originX + this.size || maxZ >= this.originZ + this.size) {
                return false;
            }
            for (int z = minZ; z <= maxZ; z++) {
                int row = (z - this.originZ) * this.size - this.originX;
                for (int x = minX; x <= maxX; x++) {
                    if ((this.covered[row + x] & BUILT) == 0) {
                        return false;
                    }
                }
            }
            return true;
        }

        /** True if vanilla draws, or is about to draw, any chunk in the inclusive rectangle. */
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
    private boolean compiledCheckBroken;

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
        LevelRenderer renderer = Minecraft.getInstance().levelRenderer;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int cx = originX + x;
                int cz = originZ + z;
                if (!ChunkTrackingView.isInViewDistance(centerX, centerZ, viewDistance, cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) {
                    continue;
                }
                covered[z * size + x] = (byte) (LOADED | (this.builtByVanilla(renderer, chunk, probe) ? BUILT : 0));
            }
        }
        Snapshot old = this.current;
        if (old.originX == originX && old.originZ == originZ && old.size == size && Arrays.equals(old.covered, covered)) {
            return;
        }
        this.current = new Snapshot(originX, originZ, size, covered, ++this.version);
    }

    /** Whether vanilla has built the section holding the column's surface (where it is seen from afar). */
    private boolean builtByVanilla(LevelRenderer renderer, LevelChunk chunk, BlockPos.MutableBlockPos probe) {
        if (this.compiledCheckBroken) {
            return true;
        }
        try {
            int x = chunk.getPos().getMiddleBlockX();
            int z = chunk.getPos().getMiddleBlockZ();
            int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
            return renderer.isSectionCompiled(probe.set(x, Math.max(y - 1, chunk.getMinBuildHeight()), z));
        } catch (RuntimeException | LinkageError e) {
            // Another renderer replaced vanilla's section bookkeeping; fall back to "loaded means drawn".
            this.compiledCheckBroken = true;
            return true;
        }
    }

    public void clear() {
        this.current = Snapshot.EMPTY;
    }
}
