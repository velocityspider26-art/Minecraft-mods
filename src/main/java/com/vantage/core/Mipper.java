package com.vantage.core;

/**
 * Reduces 2×2×2 voxels to one.
 *
 * <p>Rules, in order:
 * <ol>
 *   <li>Anything beats air, so thin features (tree trunks, towers) survive at coarse levels.</li>
 *   <li>The top layer wins, so the parent shows what is seen from above (grass over dirt,
 *       water over sand).</li>
 *   <li>Within a layer, opaque beats translucent, then the most common visual id wins.</li>
 *   <li>{@link Voxel#FILLER_VID} (hidden space) only wins when there is nothing else.</li>
 * </ol>
 * Air and translucent results keep the brightest light of the non-opaque children, so a face
 * next to them is lit as brightly as its brightest part.
 *
 * <p>Instances keep scratch state and are not thread-safe; use one per worker.
 */
public final class Mipper {
    private final VisualClass.Table table;
    private final byte[] cls = new byte[8];
    private final int[] kids = new int[8];

    public Mipper(VisualClass.Table table) {
        this.table = table;
    }

    /** Children are indexed {@code dx | dz << 1 | dy << 2}, matching {@link SectionKey#child}. */
    public int reduce(int[] c) {
        int maxBlock = 0;
        int maxSky = 0;
        for (int i = 0; i < 8; i++) {
            int v = c[i];
            byte k = this.table.classOf(Voxel.vid(v));
            this.cls[i] = k;
            if (k != VisualClass.OPAQUE) {
                maxBlock = Math.max(maxBlock, Voxel.blockLight(v));
                maxSky = Math.max(maxSky, Voxel.skyLight(v));
            }
        }
        int fallback = -1;
        for (int layer = 4; layer >= 0; layer -= 4) {
            int chosen = -1;
            int chosenScore = -1;
            for (int i = layer; i < layer + 4; i++) {
                if (this.cls[i] == VisualClass.AIR) {
                    continue;
                }
                int vid = Voxel.vid(c[i]);
                int count = 0;
                for (int j = layer; j < layer + 4; j++) {
                    if (this.cls[j] != VisualClass.AIR && Voxel.vid(c[j]) == vid) {
                        count++;
                    }
                }
                int score = vid == Voxel.FILLER_VID ? 0 : (this.cls[i] == VisualClass.OPAQUE ? 8 : 0) + count;
                if (score > chosenScore) {
                    chosenScore = score;
                    chosen = i;
                }
            }
            if (chosen >= 0) {
                int vid = Voxel.vid(c[chosen]);
                if (vid == Voxel.FILLER_VID) {
                    // Only filler in this layer: something visible below should still win.
                    if (fallback < 0) {
                        fallback = chosen;
                    }
                    continue;
                }
                return this.cls[chosen] == VisualClass.OPAQUE ? vid : Voxel.pack(vid, maxBlock, maxSky);
            }
        }
        if (fallback >= 0) {
            return Voxel.FILLER_VID;
        }
        return Voxel.pack(0, maxBlock, maxSky);
    }

    /**
     * Halves a cube of voxels stored y-major ({@code (y*size + z)*size + x}).
     *
     * @param src     source cube, {@code srcSize}³ voxels
     * @param srcSize source edge length (even)
     * @param dst     destination array, written as a cube of edge {@code dstStride} at the offset
     * @param dstStride edge length of the destination array
     */
    public void downsample(int[] src, int srcSize, int[] dst, int dstStride, int offX, int offY, int offZ) {
        int half = srcSize >> 1;
        int[] c = this.kids;
        for (int y = 0; y < half; y++) {
            for (int z = 0; z < half; z++) {
                for (int x = 0; x < half; x++) {
                    int sx = x << 1;
                    int sy = y << 1;
                    int sz = z << 1;
                    for (int i = 0; i < 8; i++) {
                        int cx = sx + (i & 1);
                        int cz = sz + ((i >> 1) & 1);
                        int cy = sy + ((i >> 2) & 1);
                        c[i] = src[(cy * srcSize + cz) * srcSize + cx];
                    }
                    dst[((y + offY) * dstStride + (z + offZ)) * dstStride + (x + offX)] = this.reduce(c);
                }
            }
        }
    }
}
