package com.vantage.world;

/**
 * Voxelized chunk column ready to be inserted into a {@link LodWorld}.
 *
 * <p>{@code levels[cy][L]} holds the {@code (16 >> L)}³ voxels of chunk section {@code cy}
 * (counted from the dimension floor) at LOD level {@code L}, y-major. A {@code null} section was
 * not available and is left untouched.
 */
public final class VoxelColumn {
    public final int chunkX;
    public final int chunkZ;
    public final int[][][] levels;

    public VoxelColumn(int chunkX, int chunkZ, int sectionCount) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.levels = new int[sectionCount][][];
    }
}
