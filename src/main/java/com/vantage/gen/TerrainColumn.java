package com.vantage.gen;

import com.vantage.core.Voxel;

/**
 * Builds the voxels of one generated column at one LOD level.
 *
 * <p>Each voxel shows what is first seen looking down into its block range, matching how real
 * data is mipped (top layer wins, anything beats air): tree canopy, then water or ice, then
 * ground. Ground shows the surface block in the voxel holding the top block, the sub-surface
 * block just below, stone deeper down and {@link Voxel#FILLER_VID} where nothing can show.
 *
 * <p>Reusable; set the fields, then call {@link #fill}. Not thread-safe.
 */
public final class TerrainColumn {
    /** Blocks below the top block that still show the sub-surface block. */
    static final int UNDER_DEPTH = 4;
    /** Blocks below the top block from which on only filler is stored. */
    static final int FILLER_DEPTH = 48;

    // world
    public int minY;
    public int worldTop;
    public int seaLevel;
    // column
    /** One above the highest solid block; {@code <= minY} for an empty column. */
    public int surface;
    public int topVid;
    public int underVid;
    public int seabedVid;
    public int stoneVid;
    public int deepVid;
    public int waterVid;
    /** Visual id of the ice on frozen water, or -1. */
    public int iceVid = -1;
    /** Visual id of tree leaves, or -1 for no canopy. */
    public int leavesVid = -1;
    public int canopyBottom;
    public int canopyTop;

    /**
     * Writes {@code rows} voxels of size {@code voxelBlocks}, bottom up, starting at block
     * {@link #minY}; voxel {@code r} goes to {@code out[offset + r * stride]}.
     */
    public void fill(int voxelBlocks, int rows, int[] out, int offset, int stride) {
        int h = this.surface;
        boolean wet = h < this.seaLevel;
        for (int r = 0; r < rows; r++) {
            int y0 = this.minY + r * voxelBlocks;
            int y1 = y0 + voxelBlocks;
            int v;
            if (y0 >= this.worldTop) {
                v = Voxel.pack(0, 0, 15);
            } else if (this.leavesVid >= 0 && y0 < this.canopyTop && y1 > this.canopyBottom) {
                v = this.leavesVid;
            } else if (wet && y0 < this.seaLevel && y1 > h) {
                int topWater = Math.min(y1, this.seaLevel) - 1;
                // Faces looking into water are underwater ground: light them like the deepest water here.
                int depth = this.seaLevel - 1 - Math.max(y0, h);
                int vid = this.iceVid >= 0 && topWater == this.seaLevel - 1 ? this.iceVid : this.waterVid;
                v = Voxel.pack(vid, 0, Math.max(0, 15 - depth));
            } else if (y0 < h) {
                int topBlock = h - 1;
                if (y1 > topBlock) {
                    v = wet ? this.seabedVid : this.topVid;
                } else if (y1 > topBlock - UNDER_DEPTH) {
                    v = wet ? this.seabedVid : this.underVid;
                } else if (y1 <= topBlock - FILLER_DEPTH) {
                    v = Voxel.FILLER_VID;
                } else {
                    v = y1 <= 0 ? this.deepVid : this.stoneVid;
                }
            } else {
                v = Voxel.pack(0, 0, 15);
            }
            out[offset + r * stride] = v;
        }
    }
}
