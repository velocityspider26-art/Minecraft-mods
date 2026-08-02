package shipwrights.genesis.space.voxel;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable palette-compressed 16^3 voxel brick.
 *
 * <p>Coverage is stored separately from material. LOD0 occupied voxels have
 * coverage 255; coarser levels retain the fraction of their represented volume
 * that was occupied. The renderer can therefore preserve thin roofs and walls
 * without pretending every reduced voxel is a solid cube.</p>
 */
public final class PlanetVoxelBrick {
    public static final int EDGE = 16;
    public static final int CELL_COUNT = EDGE * EDGE * EDGE;
    public static final int MAX_PALETTE = 0xFFFF;

    private final PlanetVoxelBrickKey key;
    private final long revision;
    private final long[] palette;
    private final short[] paletteIndices;
    private final byte[] coverage;
    private final byte[] authority;
    private final int nonAirCount;
    private final int authoritativeCellCount;
    private final int occupiedOctantMask;

    public PlanetVoxelBrick(PlanetVoxelBrickKey key, long revision,
                            long[] palette, short[] paletteIndices, byte[] coverage) {
        this(key, revision, palette, paletteIndices, coverage,
                legacyAuthority(paletteIndices));
    }

    public PlanetVoxelBrick(PlanetVoxelBrickKey key, long revision,
                            long[] palette, short[] paletteIndices, byte[] coverage,
                            byte[] authority) {
        this.key = Objects.requireNonNull(key, "key");
        if (revision < 0) throw new IllegalArgumentException("negative revision");
        if (palette == null || palette.length == 0 || palette.length > MAX_PALETTE) {
            throw new IllegalArgumentException("invalid palette length");
        }
        if (palette[0] != PlanetVoxelMaterial.AIR) {
            throw new IllegalArgumentException("palette[0] must be air");
        }
        if (paletteIndices == null || paletteIndices.length != CELL_COUNT
                || coverage == null || coverage.length != CELL_COUNT
                || authority == null || authority.length != CELL_COUNT) {
            throw new IllegalArgumentException("brick requires exactly " + CELL_COUNT + " cells");
        }
        int occupied = 0;
        int authoritative = 0;
        int octants = 0;
        for (int i = 0; i < CELL_COUNT; i++) {
            int index = Short.toUnsignedInt(paletteIndices[i]);
            if (index >= palette.length) {
                throw new IllegalArgumentException("palette index " + index + " outside " + palette.length);
            }
            if (index == 0 && coverage[i] != 0) {
                throw new IllegalArgumentException("air cell has non-zero coverage at " + i);
            }
            PlanetVoxelAuthority source = PlanetVoxelAuthority.fromCode(authority[i]);
            if (source != PlanetVoxelAuthority.EMPTY) authoritative++;
            if (index != 0) {
                occupied++;
                int x = i & 15;
                int z = (i >>> 4) & 15;
                int y = (i >>> 8) & 15;
                int octant = (x >>> 3) | ((y >>> 3) << 1) | ((z >>> 3) << 2);
                octants |= 1 << octant;
            }
        }
        this.revision = revision;
        this.palette = palette.clone();
        this.paletteIndices = paletteIndices.clone();
        this.coverage = coverage.clone();
        this.authority = authority.clone();
        this.nonAirCount = occupied;
        this.authoritativeCellCount = authoritative;
        this.occupiedOctantMask = octants;
    }

    public PlanetVoxelBrickKey key() {
        return key;
    }

    public long revision() {
        return revision;
    }

    public int paletteSize() {
        return palette.length;
    }

    public int nonAirCount() {
        return nonAirCount;
    }

    public boolean isEmpty() {
        return nonAirCount == 0;
    }

    /** True for an exact/predicted all-air brick as well as visible geometry. */
    public boolean hasData() {
        return authoritativeCellCount > 0;
    }

    /**
     * Bit mask of the eight 8x8x8 child volumes that contain geometry.
     *
     * <p>The LOD selector uses this to replace a coarse brick only when every
     * occupied child volume is resident. Empty child volumes do not need an
     * explicit network tombstone, while missing occupied children keep the
     * coarse 3D brick visible instead of opening a hole.</p>
     */
    public int occupiedOctantMask() {
        return occupiedOctantMask;
    }

    public long material(int x, int y, int z) {
        return palette[Short.toUnsignedInt(paletteIndices[index(x, y, z)])];
    }

    public int coverage(int x, int y, int z) {
        return Byte.toUnsignedInt(coverage[index(x, y, z)]);
    }

    public PlanetVoxelAuthority authority(int x, int y, int z) {
        return PlanetVoxelAuthority.fromCode(authority[index(x, y, z)]);
    }

    public long[] paletteCopy() {
        return palette.clone();
    }

    public short[] indicesCopy() {
        return paletteIndices.clone();
    }

    public byte[] coverageCopy() {
        return coverage.clone();
    }

    public byte[] authorityCopy() {
        return authority.clone();
    }

    public long estimatedBytes() {
        return 96L + palette.length * Long.BYTES
                + paletteIndices.length * Short.BYTES + coverage.length + authority.length;
    }

    public static int index(int x, int y, int z) {
        if ((x | y | z) < 0 || x >= EDGE || y >= EDGE || z >= EDGE) {
            throw new IndexOutOfBoundsException("voxel " + x + "," + y + "," + z);
        }
        return (y << 8) | (z << 4) | x;
    }

    @Override
    public String toString() {
        return "PlanetVoxelBrick{" + key + ", rev=" + revision
                + ", palette=" + palette.length + ", occupied=" + nonAirCount + '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PlanetVoxelBrick brick)) return false;
        return revision == brick.revision && key.equals(brick.key)
                && Arrays.equals(palette, brick.palette)
                && Arrays.equals(paletteIndices, brick.paletteIndices)
                && Arrays.equals(coverage, brick.coverage)
                && Arrays.equals(authority, brick.authority);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(key, revision);
        result = 31 * result + Arrays.hashCode(palette);
        result = 31 * result + Arrays.hashCode(paletteIndices);
        result = 31 * result + Arrays.hashCode(coverage);
        result = 31 * result + Arrays.hashCode(authority);
        return result;
    }

    private static byte[] legacyAuthority(short[] indices) {
        if (indices == null || indices.length != CELL_COUNT) return new byte[0];
        byte[] result = new byte[CELL_COUNT];
        for (int i = 0; i < result.length; i++) {
            if (indices[i] != 0) result[i] = PlanetVoxelAuthority.GENERATED_EXACT.code();
        }
        return result;
    }
}
