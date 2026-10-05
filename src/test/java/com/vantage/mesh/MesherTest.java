package com.vantage.mesh;

import com.vantage.core.Lod;
import com.vantage.core.VisualClass;
import com.vantage.core.Voxel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MesherTest {
    static final VisualClass.Table TABLE = vid -> vid == 0 ? VisualClass.AIR : vid >= 10 ? VisualClass.TRANSLUCENT : VisualClass.OPAQUE;
    static final int AIR15 = Voxel.pack(0, 0, 15);
    static final int[][] DIRS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
    static final int[][] U = {{1, 0, 0}, {0, 0, 1}, {0, 1, 0}, {1, 0, 0}, {0, 0, 1}, {0, 1, 0}};
    static final int[][] V = {{0, 0, 1}, {1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, 1, 0}, {0, 0, 1}};

    @Test
    void uAndVSpanTheFaceAndPointOutward() {
        for (int f = 0; f < 6; f++) {
            int[] c = cross(U[f], V[f]);
            assertTrue(Arrays.equals(c, DIRS[f]), "face " + f);
            assertEquals(axisOf(U[f]), Mesher.U_AXIS[f]);
            assertEquals(axisOf(V[f]), Mesher.V_AXIS[f]);
        }
    }

    @Test
    void singleVoxelHasSixFaces() {
        int[] v = filled(AIR15);
        v[Lod.index(5, 6, 7)] = 3;
        MeshData m = new Mesher(TABLE).build(v, 0, null, true);
        assertEquals(6, m.quadCount());
        for (int g = 0; g < 6; g++) {
            assertEquals(1, m.counts[g]);
        }
        assertEquals(5, m.minX);
        assertEquals(6, m.minY);
        assertEquals(7, m.minZ);
        assertEquals(6, m.maxX);
        assertEquals(7, m.maxY);
        assertEquals(8, m.maxZ);
        validate(v, null, true, m);
    }

    @Test
    void uniformSolidSectionMeshesToSixFullQuads() {
        MeshData m = new Mesher(TABLE).build(null, 4, null, true);
        assertEquals(6, m.quadCount());
        for (int i = 0; i < m.quadCount(); i++) {
            int w0 = m.quads[i * 2];
            assertEquals(31, (w0 >>> 15) & 31);
            assertEquals(31, (w0 >>> 20) & 31);
        }
    }

    @Test
    void flatGroundMergesIntoOneTopQuad() {
        int[] v = filled(AIR15);
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 32; z++) {
                for (int x = 0; x < 32; x++) {
                    v[Lod.index(x, y, z)] = 2;
                }
            }
        }
        int[][] n = new int[6][];
        n[0] = Mesher.SOLID_SLICE;
        for (int f = 2; f < 6; f++) {
            n[f] = new int[Lod.AREA];
            Arrays.fill(n[f], 2);
            for (int y = 16; y < 32; y++) {
                for (int a = 0; a < 32; a++) {
                    n[f][y * 32 + a] = AIR15;
                }
            }
        }
        MeshData m = new Mesher(TABLE).build(v, 0, n, true);
        assertEquals(1, m.counts[1], "one merged top quad");
        assertEquals(0, m.counts[0], "nothing faces down into solid ground");
        validate(v, n, true, m);
    }

    @Test
    void darkAirHidesFacesWhenSkyCulling() {
        int[] v = filled(Voxel.pack(0, 0, 0));
        v[Lod.index(10, 10, 10)] = 2;
        int[][] n = new int[6][];
        for (int f = 0; f < 6; f++) {
            n[f] = new int[Lod.AREA];
            Arrays.fill(n[f], Voxel.pack(0, 0, 0));
        }
        assertEquals(0, new Mesher(TABLE).build(v, 0, n, true).quadCount());
        assertEquals(6, new Mesher(TABLE).build(v, 0, n, false).quadCount());
    }

    @Test
    void skirtClosesStepsAtSectionBorders() {
        // Ground at y<=9 here; neighbour on -X is solid up to y=9 but open at y=10.
        int[] v = filled(AIR15);
        for (int y = 0; y <= 9; y++) {
            for (int z = 0; z < 32; z++) {
                for (int x = 0; x < 32; x++) {
                    v[Lod.index(x, y, z)] = 2;
                }
            }
        }
        int[][] n = new int[6][];
        n[4] = new int[Lod.AREA];
        Arrays.fill(n[4], AIR15);
        for (int y = 0; y <= 9; y++) {
            for (int z = 0; z < 32; z++) {
                n[4][y * 32 + z] = 2;
            }
        }
        MeshData m = new Mesher(TABLE).build(v, 0, n, true);
        // Voxels at y=8 and y=9 on the x=0 border get skirt faces towards -X.
        int skirtFaces = 0;
        for (int i = 0; i < m.counts[0] + m.counts[1] + m.counts[2] + m.counts[3] + m.counts[4]; i++) {
            int w0 = m.quads[i * 2];
            if (((w0 >>> 25) & 7) == 4) {
                skirtFaces += (((w0 >>> 15) & 31) + 1) * (((w0 >>> 20) & 31) + 1);
            }
        }
        assertEquals(2 * 32, skirtFaces);
    }

    @Test
    void randomFieldsAreCoveredExactly() {
        Random r = new Random(42);
        Mesher mesher = new Mesher(TABLE);
        for (int iter = 0; iter < 40; iter++) {
            int[] v = new int[Lod.VOLUME];
            double fill = r.nextDouble();
            for (int i = 0; i < Lod.VOLUME; i++) {
                double p = r.nextDouble();
                if (p < fill * 0.7) {
                    v[i] = 2 + r.nextInt(iter % 3 + 1);
                } else if (p < fill * 0.8) {
                    v[i] = Voxel.pack(10 + r.nextInt(2), r.nextInt(3), 15);
                } else {
                    v[i] = Voxel.pack(0, r.nextInt(2), r.nextInt(2) == 0 ? 15 : 0);
                }
            }
            int[][] n = new int[6][];
            for (int f = 0; f < 6; f++) {
                if (r.nextBoolean()) {
                    n[f] = new int[Lod.AREA];
                    for (int i = 0; i < Lod.AREA; i++) {
                        n[f][i] = r.nextBoolean() ? 2 : AIR15;
                    }
                }
            }
            boolean sky = r.nextBoolean();
            validate(v, n, sky, mesher.build(v, 0, n, sky));
        }
    }

    // ---- reference implementation ----

    private static void validate(int[] v, int[][] n, boolean skyCull, MeshData m) {
        int[] owner = new int[Lod.VOLUME * 6];
        Arrays.fill(owner, -1);
        int at = 0;
        for (int g = 0; g < Mesher.GROUPS; g++) {
            for (int q = 0; q < m.counts[g]; q++, at++) {
                int w0 = m.quads[at * 2];
                int w1 = m.quads[at * 2 + 1];
                int x = w0 & 31, y = (w0 >>> 5) & 31, z = (w0 >>> 10) & 31;
                int w = ((w0 >>> 15) & 31) + 1, h = ((w0 >>> 20) & 31) + 1;
                int face = (w0 >>> 25) & 7;
                assertEquals(g % 6, face, "group/face mismatch");
                int key = (w1 & Voxel.VID_MASK) | ((w0 >>> 28) << 24) | (((w1 >>> 20) & 15) << 20);
                for (int du = 0; du < w; du++) {
                    for (int dv = 0; dv < h; dv++) {
                        int px = x + U[face][0] * du + V[face][0] * dv;
                        int py = y + U[face][1] * du + V[face][1] * dv;
                        int pz = z + U[face][2] * du + V[face][2] * dv;
                        if (px > 31 || py > 31 || pz > 31) {
                            fail("quad leaves the section");
                        }
                        int slot = Lod.index(px, py, pz) * 6 + face;
                        if (owner[slot] >= 0) {
                            fail("overlapping quads");
                        }
                        owner[slot] = key;
                    }
                }
            }
        }
        for (int i = 0; i < Lod.VOLUME; i++) {
            int x = i & 31, z = (i >> 5) & 31, y = i >> 10;
            for (int f = 0; f < 6; f++) {
                int expected = expectedKey(v, n, skyCull, x, y, z, f);
                assertEquals(expected, owner[i * 6 + f], "voxel " + x + "," + y + "," + z + " face " + f);
            }
        }
    }

    private static int expectedKey(int[] v, int[][] n, boolean skyCull, int x, int y, int z, int f) {
        int a = v[Lod.index(x, y, z)];
        byte ca = TABLE.classOf(Voxel.vid(a));
        if (ca == VisualClass.AIR) {
            return -1;
        }
        int nx = x + DIRS[f][0], ny = y + DIRS[f][1], nz = z + DIRS[f][2];
        boolean border = nx < 0 || ny < 0 || nz < 0 || nx > 31 || ny > 31 || nz > 31;
        int b = border ? slice(n, f, x, y, z, 0) : v[Lod.index(nx, ny, nz)];
        byte cb = TABLE.classOf(Voxel.vid(b));
        boolean lit = !skyCull || Voxel.light(b) != 0;
        boolean draw = ca == VisualClass.OPAQUE ? cb != VisualClass.OPAQUE && lit : cb == VisualClass.AIR && lit;
        int lightFrom = b;
        if (!draw && border && f >= 2 && ca == VisualClass.OPAQUE && cb == VisualClass.OPAQUE) {
            for (int dy = 1; dy <= 2 && y + dy < 32; dy++) {
                int above = slice(n, f, x, y, z, dy);
                byte c = TABLE.classOf(Voxel.vid(above));
                if (c == VisualClass.TRANSLUCENT) {
                    break;
                }
                if (c == VisualClass.AIR) {
                    draw = true;
                    lightFrom = above;
                    break;
                }
            }
        }
        if (!draw) {
            return -1;
        }
        return Voxel.vid(a) | (Voxel.light(lightFrom) << 20);
    }

    private static int slice(int[][] n, int f, int x, int y, int z, int dy) {
        if (n == null || n[f] == null) {
            return AIR15;
        }
        return n[f][Mesher.sliceIndex(f, x, y + dy, z)];
    }

    private static int[] filled(int value) {
        int[] v = new int[Lod.VOLUME];
        Arrays.fill(v, value);
        return v;
    }

    private static int[] cross(int[] a, int[] b) {
        return new int[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static int axisOf(int[] v) {
        return v[0] != 0 ? 0 : v[1] != 0 ? 1 : 2;
    }
}
