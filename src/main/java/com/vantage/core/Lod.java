package com.vantage.core;

/**
 * Geometry of the LOD pyramid.
 *
 * <p>A section is a cube of {@link #SIZE}³ voxels. At level {@code L} one voxel spans {@code 2^L}
 * blocks, so a section spans {@code 32·2^L} blocks. Levels 0..4 are produced directly from
 * 16³ chunk sections (16 = 2⁴); levels 5..{@link #MAX_LEVEL} are mipped from the level below.
 * The top level's voxels are 1024 blocks wide, enough to draw hundreds of kilometres cheaply.
 *
 * <p>Section Y coordinates are relative to the dimension's minimum build height, so they are
 * never negative.
 */
public final class Lod {
    public static final int SIZE_BITS = 5;
    public static final int SIZE = 1 << SIZE_BITS;
    public static final int AREA = SIZE * SIZE;
    public static final int VOLUME = SIZE * SIZE * SIZE;
    public static final int MAX_LEVEL = 10;
    public static final int LEVELS = MAX_LEVEL + 1;
    /** Levels that a single 16³ chunk section can fill on its own. */
    public static final int CHUNK_LEVELS = 5;

    private Lod() {
    }

    /** Voxel index inside a section: y-major so horizontal layers are contiguous. */
    public static int index(int x, int y, int z) {
        return (y << (2 * SIZE_BITS)) | (z << SIZE_BITS) | x;
    }

    public static int voxelBlocks(int level) {
        return 1 << level;
    }

    public static int sectionBlocks(int level) {
        return SIZE << level;
    }

    /** Number of vertical sections needed at {@code level} for a world {@code height} blocks tall. */
    public static int verticalSections(int level, int height) {
        int span = sectionBlocks(level);
        return Math.max(1, (height + span - 1) / span);
    }
}
