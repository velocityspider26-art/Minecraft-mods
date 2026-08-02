package shipwrights.genesis.space.voxel;

/**
 * Compact material carried by one planet-LOD voxel.
 *
 * <p>Zero is reserved for air. Non-air values store a vanilla block-state ID,
 * RGB tint, packed sky/block light and rendering/reduction flags. The format is
 * deliberately independent of the renderer so the same bricks can live on the
 * server, disk and client.</p>
 */
public final class PlanetVoxelMaterial {
    private static final long STATE_MASK = (1L << 24) - 1L;
    private static final long RGB_MASK = (1L << 24) - 1L;

    public static final int FLAG_OPAQUE = 1;
    public static final int FLAG_TRANSLUCENT = 1 << 1;
    public static final int FLAG_LIQUID = 1 << 2;
    public static final int FLAG_FOLIAGE = 1 << 3;
    public static final int FLAG_EMISSIVE = 1 << 4;
    public static final int FLAG_STRUCTURE = 1 << 5;
    public static final int FLAG_NATURAL = 1 << 6;
    public static final int FLAG_NO_OCCLUSION = 1 << 7;

    public static final long AIR = 0L;

    private PlanetVoxelMaterial() {
    }

    public static long pack(int blockStateId, int rgb, int skyLight, int blockLight, int flags) {
        if (blockStateId < 0 || blockStateId >= STATE_MASK) {
            throw new IllegalArgumentException("blockStateId out of 24-bit range: " + blockStateId);
        }
        int sky = clampNibble(skyLight);
        int block = clampNibble(blockLight);
        long encodedState = (blockStateId + 1L) & STATE_MASK;
        return encodedState
                | ((long) rgb & RGB_MASK) << 24
                | (long) block << 48
                | (long) sky << 52
                | ((long) flags & 0xFFL) << 56;
    }

    public static boolean isAir(long material) {
        return material == AIR;
    }

    public static int blockStateId(long material) {
        if (material == AIR) return 0;
        return (int) ((material & STATE_MASK) - 1L);
    }

    public static int rgb(long material) {
        return (int) ((material >>> 24) & RGB_MASK);
    }

    public static int blockLight(long material) {
        return (int) ((material >>> 48) & 0xFL);
    }

    public static int skyLight(long material) {
        return (int) ((material >>> 52) & 0xFL);
    }

    public static int flags(long material) {
        return (int) ((material >>> 56) & 0xFFL);
    }

    public static boolean hasFlag(long material, int flag) {
        return (flags(material) & flag) != 0;
    }

    /**
     * Priority used when eight child voxels collapse into one parent voxel.
     * Artificial structures survive before natural terrain, which prevents
     * villages and player bases from averaging into the ground.
     */
    public static int reductionPriority(long material) {
        if (material == AIR) return 0;
        int flags = flags(material);
        int score = 1_000;
        if ((flags & FLAG_STRUCTURE) != 0) score += 20_000;
        if ((flags & FLAG_EMISSIVE) != 0) score += 8_000;
        if ((flags & FLAG_OPAQUE) != 0) score += 4_000;
        if ((flags & FLAG_FOLIAGE) != 0) score += 2_000;
        if ((flags & FLAG_LIQUID) != 0) score += 1_000;
        if ((flags & FLAG_NATURAL) != 0) score += 500;
        return score;
    }

    private static int clampNibble(int value) {
        return Math.max(0, Math.min(15, value));
    }
}
