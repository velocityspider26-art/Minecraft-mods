package com.vantage.mesh;

/**
 * Output of the {@link Mesher}: quads grouped by {@link Mesher#GROUPS} plus the section-local
 * bounding box of the geometry (in voxels, 0..32).
 */
public final class MeshData {
    public static final MeshData EMPTY = new MeshData(new int[0], new int[Mesher.GROUPS], 0, 0, 0, 0, 0, 0);

    /** Two ints per quad, groups stored back to back in group order. */
    public final int[] quads;
    public final int[] counts;
    public final int minX, minY, minZ, maxX, maxY, maxZ;

    public MeshData(int[] quads, int[] counts, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.quads = quads;
        this.counts = counts;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public int quadCount() {
        return this.quads.length / 2;
    }

    public boolean isEmpty() {
        return this.quads.length == 0;
    }
}
