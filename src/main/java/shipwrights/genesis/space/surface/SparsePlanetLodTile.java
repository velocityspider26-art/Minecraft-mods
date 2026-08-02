package shipwrights.genesis.space.surface;

import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.Objects;

/**
 * One sparse, independently replaceable patch of a cube face.
 *
 * <p>The tile always contains a 16x16 grid. {@code cellSize} is the number of
 * planet blocks represented by one grid cell, so a tile spans
 * {@code 16 * cellSize} blocks on each side. Coarse generator-predicted tiles
 * and exact generated-chunk tiles therefore use the same packet, cache and
 * renderer.</p>
 */
public record SparsePlanetLodTile(
        CubeNetSurfaceTransform.Face face,
        int originX,
        int originZ,
        int cellSize,
        boolean exact,
        long revision,
        short[] heights,
        int[] colours) {

    public static final int RESOLUTION = 16;
    public static final int CELL_COUNT = RESOLUTION * RESOLUTION;
    public static final int MIN_CELL_SIZE = 1;
    public static final int MAX_CELL_SIZE = 2048;

    public SparsePlanetLodTile {
        Objects.requireNonNull(face, "face");
        if (cellSize < MIN_CELL_SIZE || cellSize > MAX_CELL_SIZE
                || Integer.bitCount(cellSize) != 1) {
            throw new IllegalArgumentException("cellSize must be a power of two in [1, 2048]");
        }
        if (heights == null || heights.length != CELL_COUNT
                || colours == null || colours.length != CELL_COUNT) {
            throw new IllegalArgumentException("Sparse planet LOD tiles must contain exactly 16x16 columns");
        }
        int span = RESOLUTION * cellSize;
        if (Math.floorMod(originX, span) != 0 || Math.floorMod(originZ, span) != 0) {
            throw new IllegalArgumentException("Tile origin must be aligned to its " + span + "-block span");
        }
    }

    public int span() {
        return RESOLUTION * cellSize;
    }

    public int index(int x, int z) {
        if (x < 0 || x >= RESOLUTION || z < 0 || z >= RESOLUTION) {
            throw new IndexOutOfBoundsException("tile cell " + x + "," + z);
        }
        return z * RESOLUTION + x;
    }

    public double cellMinX(int x) {
        return originX + (double) x * cellSize;
    }

    public double cellMinZ(int z) {
        return originZ + (double) z * cellSize;
    }

    public record Key(CubeNetSurfaceTransform.Face face, int originX, int originZ, int cellSize) {
        public Key {
            Objects.requireNonNull(face, "face");
        }
    }

    public Key key() {
        return new Key(face, originX, originZ, cellSize);
    }
}
