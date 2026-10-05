package com.vantage.core;

/**
 * A voxel is one {@code int}:
 *
 * <pre>
 * bits  0..19  visual id (0 = air)
 * bits 20..23  block light
 * bits 24..27  sky light
 * bits 28..31  reserved, always 0
 * </pre>
 *
 * Opaque voxels always carry light 0; only air and translucent voxels keep light, because a
 * face is lit by the voxel it looks into.
 */
public final class Voxel {
    public static final int VID_BITS = 20;
    public static final int VID_MASK = (1 << VID_BITS) - 1;
    public static final int MAX_VID = VID_MASK;

    /** Air that has never been ingested. Assumed sky-lit so faces bordering it stay visible. */
    public static final int UNKNOWN_AIR = pack(0, 0, 15);

    /**
     * Reserved visual id for space that can never be seen from a distance: buried blocks and unlit
     * caves. It is opaque, drawn dark (it only shows at cave mouths), and loses every mip tie.
     */
    public static final int FILLER_VID = 1;

    private Voxel() {
    }

    public static int pack(int vid, int blockLight, int skyLight) {
        return vid | (blockLight << 20) | (skyLight << 24);
    }

    public static int vid(int voxel) {
        return voxel & VID_MASK;
    }

    public static int blockLight(int voxel) {
        return (voxel >>> 20) & 0xF;
    }

    public static int skyLight(int voxel) {
        return (voxel >>> 24) & 0xF;
    }

    /** Combined light byte: block light in the low nibble, sky light in the high nibble. */
    public static int light(int voxel) {
        return (voxel >>> 20) & 0xFF;
    }

    public static boolean isAir(int voxel) {
        return (voxel & VID_MASK) == 0;
    }

    public static int withLight(int voxel, int light) {
        return (voxel & VID_MASK) | ((light & 0xFF) << 20);
    }

    public static int withoutLight(int voxel) {
        return voxel & VID_MASK;
    }
}
