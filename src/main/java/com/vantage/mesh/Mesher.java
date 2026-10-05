package com.vantage.mesh;

import com.vantage.core.Lod;
import com.vantage.core.VisualClass;
import com.vantage.core.Voxel;

import java.util.Arrays;

/**
 * Greedy mesher for one 32³ LOD section.
 *
 * <p>Faces are merged into rectangles of identical (visual id, light) and split into
 * {@link #GROUPS} groups: the six face directions for opaque geometry, then the six for
 * translucent geometry. Grouping by direction lets the renderer skip every face that points away
 * from the camera without looking at individual quads.
 *
 * <p>Face directions follow Minecraft's {@code Direction} order: 0 down, 1 up, 2 north (-Z),
 * 3 south (+Z), 4 west (-X), 5 east (+X).
 *
 * <p>Each quad is two ints:
 * <pre>
 * w0: x | y&lt;&lt;5 | z&lt;&lt;10 | (w-1)&lt;&lt;15 | (h-1)&lt;&lt;20 | face&lt;&lt;25 | sky&lt;&lt;28
 * w1: vid | block&lt;&lt;20
 * </pre>
 * {@code (x,y,z)} is the voxel at the quad's minimum corner; {@code w} runs along the face's U
 * axis and {@code h} along its V axis (see {@link #U_AXIS}/{@link #V_AXIS}), chosen so that
 * U × V points out of the face.
 *
 * <p>Instances keep scratch buffers and are not thread-safe; use one per worker.
 */
public final class Mesher {
    public static final int GROUPS = 12;
    public static final int TRANSLUCENT_GROUP_OFFSET = 6;

    /** Axis ids: 0 = x, 1 = y, 2 = z. */
    public static final int[] NORMAL_AXIS = {1, 1, 2, 2, 0, 0};
    public static final int[] U_AXIS = {0, 2, 1, 0, 2, 1};
    public static final int[] V_AXIS = {2, 0, 0, 1, 1, 2};
    private static final int[] AXIS_STRIDE = {1, Lod.AREA, Lod.SIZE};
    private static final int[] DIR = {-1, 1, -1, 1, -1, 1};

    /** A neighbour slice that is entirely opaque, e.g. below the bottom of the world. */
    public static final int[] SOLID_SLICE = new int[Lod.AREA];
    private static final int[] UNKNOWN_SLICE = new int[Lod.AREA];

    static {
        Arrays.fill(UNKNOWN_SLICE, Voxel.UNKNOWN_AIR);
    }

    private final VisualClass.Table table;
    private final byte[] cls = new byte[Lod.VOLUME];
    private final int[] filled = new int[Lod.VOLUME];
    private final int[] grid = new int[Lod.AREA];
    private final int[][] nonAirPerLayer = new int[3][Lod.SIZE];
    private final int[][] opaquePerLayer = new int[3][Lod.SIZE];
    private final IntBuffer[] out = new IntBuffer[GROUPS];
    private int minX, minY, minZ, maxX, maxY, maxZ;

    public Mesher(VisualClass.Table table) {
        this.table = table;
        for (int i = 0; i < GROUPS; i++) {
            this.out[i] = new IntBuffer(256);
        }
    }

    /** Neighbour slice index for a voxel position on the given face's axis plane. */
    public static int sliceIndex(int face, int x, int y, int z) {
        return switch (NORMAL_AXIS[face]) {
            case 1 -> z * Lod.SIZE + x;
            case 2 -> y * Lod.SIZE + x;
            default -> y * Lod.SIZE + z;
        };
    }

    /**
     * Extracts the layer of {@code neighbour} that touches face {@code face} of the section being
     * meshed, in {@link #sliceIndex} order.
     */
    public static int[] extractSlice(int[] neighbour, int face, int[] dst) {
        int layer = DIR[face] > 0 ? 0 : Lod.SIZE - 1;
        for (int a = 0; a < Lod.SIZE; a++) {
            for (int b = 0; b < Lod.SIZE; b++) {
                int x, y, z;
                switch (NORMAL_AXIS[face]) {
                    case 1 -> { x = b; z = a; y = layer; }
                    case 2 -> { x = b; y = a; z = layer; }
                    default -> { z = b; y = a; x = layer; }
                }
                dst[a * Lod.SIZE + b] = neighbour[Lod.index(x, y, z)];
            }
        }
        return dst;
    }

    /**
     * Builds the mesh.
     *
     * @param voxels     section voxels, or {@code null} if the section is uniformly {@code uniform}
     * @param neighbours six neighbour slices by face; {@code null} entries mean unknown (treated as
     *                   sky-lit air so the frontier of the explored world gets closed off)
     * @param skyCull    skip faces that look into voxels with no light at all (caves)
     */
    public MeshData build(int[] voxels, int uniform, int[][] neighbours, boolean skyCull) {
        if (voxels == null) {
            if (Voxel.isAir(uniform)) {
                return MeshData.EMPTY;
            }
            Arrays.fill(this.filled, uniform);
            voxels = this.filled;
        }
        for (IntBuffer b : this.out) {
            b.clear();
        }
        this.minX = this.minY = this.minZ = Lod.SIZE;
        this.maxX = this.maxY = this.maxZ = 0;

        this.classify(voxels);
        for (int face = 0; face < 6; face++) {
            int[] slice = neighbours == null || neighbours[face] == null ? UNKNOWN_SLICE : neighbours[face];
            this.meshFace(voxels, face, slice, neighbours, skyCull);
        }
        return this.collect();
    }

    private void classify(int[] voxels) {
        for (int[] a : this.nonAirPerLayer) {
            Arrays.fill(a, 0);
        }
        for (int[] a : this.opaquePerLayer) {
            Arrays.fill(a, 0);
        }
        int[] nx = this.nonAirPerLayer[0], ny = this.nonAirPerLayer[1], nz = this.nonAirPerLayer[2];
        int[] ox = this.opaquePerLayer[0], oy = this.opaquePerLayer[1], oz = this.opaquePerLayer[2];
        int lastVid = -1;
        byte lastCls = 0;
        for (int i = 0; i < Lod.VOLUME; i++) {
            int vid = Voxel.vid(voxels[i]);
            byte c;
            if (vid == lastVid) {
                c = lastCls;
            } else {
                c = vid == 0 ? VisualClass.AIR : this.table.classOf(vid);
                lastVid = vid;
                lastCls = c;
            }
            this.cls[i] = c;
            if (c != VisualClass.AIR) {
                int x = i & 31, z = (i >> 5) & 31, y = i >> 10;
                nx[x]++;
                ny[y]++;
                nz[z]++;
                if (c == VisualClass.OPAQUE) {
                    ox[x]++;
                    oy[y]++;
                    oz[z]++;
                }
            }
        }
    }

    private void meshFace(int[] vox, int face, int[] slice, int[][] neighbours, boolean skyCull) {
        final int axis = NORMAL_AXIS[face];
        final int uAxis = U_AXIS[face];
        final int vAxis = V_AXIS[face];
        final int sStride = AXIS_STRIDE[axis];
        final int uStride = AXIS_STRIDE[uAxis];
        final int vStride = AXIS_STRIDE[vAxis];
        final int dir = DIR[face];
        final boolean horizontal = axis != 1;
        final int[] grid = this.grid;

        for (int s = 0; s < Lod.SIZE; s++) {
            if (this.nonAirPerLayer[axis][s] == 0) {
                continue;
            }
            int ns = s + dir;
            boolean border = ns < 0 || ns >= Lod.SIZE;
            if (!border && this.opaquePerLayer[axis][s] == Lod.AREA && this.opaquePerLayer[axis][ns] == Lod.AREA) {
                continue;
            }
            boolean any = false;
            for (int v = 0; v < Lod.SIZE; v++) {
                for (int u = 0; u < Lod.SIZE; u++) {
                    int idx = s * sStride + u * uStride + v * vStride;
                    byte ca = this.cls[idx];
                    int key = 0;
                    if (ca != VisualClass.AIR) {
                        int b;
                        byte cb;
                        if (border) {
                            int x = idx & 31, z = (idx >> 5) & 31, y = idx >> 10;
                            b = slice[sliceIndex(face, x, y, z)];
                            cb = this.classOfVoxel(b);
                        } else {
                            int nIdx = idx + dir * sStride;
                            b = vox[nIdx];
                            cb = this.cls[nIdx];
                        }
                        key = faceKey(vox[idx], ca, b, cb, skyCull);
                        if (key == 0 && border && horizontal && ca == VisualClass.OPAQUE && cb == VisualClass.OPAQUE) {
                            key = this.skirtKey(vox[idx], face, idx, slice);
                        }
                    }
                    grid[v * Lod.SIZE + u] = key;
                    any |= key != 0;
                }
            }
            if (any) {
                this.merge(face, s, uAxis, vAxis);
            }
        }
    }

    private byte classOfVoxel(int voxel) {
        int vid = Voxel.vid(voxel);
        return vid == 0 ? VisualClass.AIR : this.table.classOf(vid);
    }

    /** Returns {@code 1 + (vid | light << 20)} if a face must be drawn, else 0. */
    private static int faceKey(int a, byte ca, int b, byte cb, boolean skyCull) {
        boolean lit = !skyCull || Voxel.light(b) != 0;
        boolean draw;
        if (ca == VisualClass.OPAQUE) {
            draw = cb != VisualClass.OPAQUE && lit;
        } else {
            draw = (cb == VisualClass.AIR && lit) || (cb == VisualClass.TRANSLUCENT && Voxel.vid(b) != Voxel.vid(a));
        }
        return draw ? 1 + (Voxel.vid(a) | (Voxel.light(b) << 20)) : 0;
    }

    /**
     * Side faces on the section border that are hidden by the neighbour at the same level are still
     * emitted when the neighbour column is open within two voxels above. If the neighbour is drawn
     * at a coarser level its surface can sit lower, and this short skirt covers the crack.
     */
    private int skirtKey(int a, int face, int idx, int[] slice) {
        int x = idx & 31, z = (idx >> 5) & 31, y = idx >> 10;
        for (int dy = 1; dy <= 2 && y + dy < Lod.SIZE; dy++) {
            int above = slice[sliceIndex(face, x, y + dy, z)];
            if (this.classOfVoxel(above) != VisualClass.OPAQUE) {
                return 1 + (Voxel.vid(a) | (Voxel.light(above) << 20));
            }
        }
        return 0;
    }

    private void merge(int face, int s, int uAxis, int vAxis) {
        final int[] grid = this.grid;
        for (int v = 0; v < Lod.SIZE; v++) {
            int row = v * Lod.SIZE;
            for (int u = 0; u < Lod.SIZE; ) {
                int k = grid[row + u];
                if (k == 0) {
                    u++;
                    continue;
                }
                int w = 1;
                while (u + w < Lod.SIZE && grid[row + u + w] == k) {
                    w++;
                }
                int h = 1;
                outer:
                while (v + h < Lod.SIZE) {
                    int r = (v + h) * Lod.SIZE + u;
                    for (int i = 0; i < w; i++) {
                        if (grid[r + i] != k) {
                            break outer;
                        }
                    }
                    h++;
                }
                for (int dv = 0; dv < h; dv++) {
                    Arrays.fill(grid, (v + dv) * Lod.SIZE + u, (v + dv) * Lod.SIZE + u + w, 0);
                }
                this.emit(face, s, u, v, w, h, k - 1, uAxis, vAxis);
                u += w;
            }
        }
    }

    private void emit(int face, int s, int u, int v, int w, int h, int key, int uAxis, int vAxis) {
        int nAxis = NORMAL_AXIS[face];
        int x = coord(0, nAxis, uAxis, s, u, v);
        int y = coord(1, nAxis, uAxis, s, u, v);
        int z = coord(2, nAxis, uAxis, s, u, v);
        int vid = key & Voxel.VID_MASK;
        int light = key >>> 20;
        int w0 = x | (y << 5) | (z << 10) | ((w - 1) << 15) | ((h - 1) << 20) | (face << 25) | ((light >>> 4) << 28);
        int w1 = vid | ((light & 0xF) << 20);
        int group = face + (this.table.classOf(vid) == VisualClass.TRANSLUCENT ? TRANSLUCENT_GROUP_OFFSET : 0);
        this.out[group].add2(w0, w1);

        this.minX = Math.min(this.minX, x);
        this.minY = Math.min(this.minY, y);
        this.minZ = Math.min(this.minZ, z);
        this.maxX = Math.max(this.maxX, coord(0, nAxis, uAxis, s + 1, u + w, v + h));
        this.maxY = Math.max(this.maxY, coord(1, nAxis, uAxis, s + 1, u + w, v + h));
        this.maxZ = Math.max(this.maxZ, coord(2, nAxis, uAxis, s + 1, u + w, v + h));
    }

    private static int coord(int axis, int nAxis, int uAxis, int s, int u, int v) {
        return axis == nAxis ? s : axis == uAxis ? u : v;
    }

    private MeshData collect() {
        int total = 0;
        int[] counts = new int[GROUPS];
        for (int g = 0; g < GROUPS; g++) {
            counts[g] = this.out[g].size() / 2;
            total += counts[g];
        }
        if (total == 0) {
            return MeshData.EMPTY;
        }
        int[] quads = new int[total * 2];
        int at = 0;
        for (int g = 0; g < GROUPS; g++) {
            IntBuffer b = this.out[g];
            System.arraycopy(b.array(), 0, quads, at, b.size());
            at += b.size();
        }
        return new MeshData(quads, counts, this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    /** Minimal growable int list. */
    static final class IntBuffer {
        private int[] data;
        private int size;

        IntBuffer(int capacity) {
            this.data = new int[capacity];
        }

        void add2(int a, int b) {
            if (this.size + 2 > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            this.data[this.size++] = a;
            this.data[this.size++] = b;
        }

        void clear() {
            this.size = 0;
        }

        int size() {
            return this.size;
        }

        int[] array() {
            return this.data;
        }
    }
}
