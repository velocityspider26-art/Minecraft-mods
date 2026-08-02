package shipwrights.genesis.space.surface;

import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.Objects;

/**
 * Exact, structure-preserving 3D LOD data for one real Minecraft chunk.
 *
 * <p>Unlike {@link SparsePlanetLodTile}, this tile does not collapse every
 * column to one height and colour. Each of the 256 block columns owns a small
 * list of vertical material segments. Air gaps, roofs, walls, tree canopies,
 * towers and other separated geometry therefore survive the trip to the
 * high-altitude/orbital renderer.</p>
 */
public record PlanetLodVolumeTile(
        CubeNetSurfaceTransform.Face face,
        int chunkX,
        int chunkZ,
        long revision,
        int[] columnOffsets,
        short[] bottomY,
        short[] topY,
        int[] blockStateIds,
        int[] colours,
        byte[] packedLight,
        byte[] flags) {

    public static final int RESOLUTION = 16;
    public static final int COLUMN_COUNT = RESOLUTION * RESOLUTION;
    public static final int OFFSET_COUNT = COLUMN_COUNT + 1;
    public static final int MAX_SEGMENTS = 8192;
    public static final int MAX_SEGMENTS_PER_COLUMN = 24;

    public static final byte FLAG_LIQUID = 1;
    public static final byte FLAG_TRANSLUCENT = 1 << 1;
    public static final byte FLAG_FOLIAGE = 1 << 2;
    public static final byte FLAG_EMISSIVE = 1 << 3;
    public static final byte FLAG_STRUCTURE = 1 << 4;
    public static final byte FLAG_NATURAL = 1 << 5;

    public PlanetLodVolumeTile {
        Objects.requireNonNull(face, "face");
        if (columnOffsets == null || columnOffsets.length != OFFSET_COUNT) {
            throw new IllegalArgumentException("Volume tile requires 257 column offsets");
        }
        int segmentCount = columnOffsets[COLUMN_COUNT];
        if (segmentCount < 0 || segmentCount > MAX_SEGMENTS) {
            throw new IllegalArgumentException("Invalid volume segment count " + segmentCount);
        }
        if (bottomY == null || bottomY.length != segmentCount
                || topY == null || topY.length != segmentCount
                || blockStateIds == null || blockStateIds.length != segmentCount
                || colours == null || colours.length != segmentCount
                || packedLight == null || packedLight.length != segmentCount
                || flags == null || flags.length != segmentCount) {
            throw new IllegalArgumentException("Volume segment arrays must have identical lengths");
        }
        int previous = 0;
        for (int column = 0; column < OFFSET_COUNT; column++) {
            int offset = columnOffsets[column];
            if (offset < previous || offset > segmentCount) {
                throw new IllegalArgumentException("Invalid column offset at " + column + ": " + offset);
            }
            if (column > 0 && offset - previous > MAX_SEGMENTS_PER_COLUMN) {
                throw new IllegalArgumentException("Column " + (column - 1) + " exceeds segment limit");
            }
            previous = offset;
        }
        for (int segment = 0; segment < segmentCount; segment++) {
            if (topY[segment] <= bottomY[segment]) {
                throw new IllegalArgumentException("Empty/reversed segment " + segment);
            }
            if (blockStateIds[segment] < 0) {
                throw new IllegalArgumentException("Negative block-state id in segment " + segment);
            }
        }
    }

    public int segmentCount() {
        return bottomY.length;
    }

    public int columnIndex(int x, int z) {
        if (x < 0 || x >= RESOLUTION || z < 0 || z >= RESOLUTION) {
            throw new IndexOutOfBoundsException("volume column " + x + "," + z);
        }
        return z * RESOLUTION + x;
    }

    public int firstSegment(int x, int z) {
        return columnOffsets[columnIndex(x, z)];
    }

    public int endSegment(int x, int z) {
        return columnOffsets[columnIndex(x, z) + 1];
    }

    public int worldMinX() {
        return chunkX << 4;
    }

    public int worldMinZ() {
        return chunkZ << 4;
    }

    public record Key(CubeNetSurfaceTransform.Face face, int chunkX, int chunkZ) {
        public Key {
            Objects.requireNonNull(face, "face");
        }
    }

    public Key key() {
        return new Key(face, chunkX, chunkZ);
    }
}
