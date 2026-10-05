package com.vantage.mesh;

import com.vantage.core.Lod;
import com.vantage.core.VisualClass;
import com.vantage.core.Voxel;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rough throughput check on terrain-like sections; prints numbers for the record. */
class MesherBenchmarkTest {
    static final VisualClass.Table TABLE = vid -> vid == 0 ? VisualClass.AIR : vid >= 10 ? VisualClass.TRANSLUCENT : VisualClass.OPAQUE;

    static int[] terrain(Random r) {
        int[] v = new int[Lod.VOLUME];
        double fx = r.nextDouble() * 0.3, fz = r.nextDouble() * 0.3;
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++) {
                int h = 12 + (int) (6 * Math.sin(x * fx) + 6 * Math.cos(z * fz));
                for (int y = 0; y < 32; y++) {
                    int vox;
                    if (y < h - 3) {
                        vox = Voxel.FILLER_VID;
                    } else if (y < h) {
                        vox = 2 + r.nextInt(3);
                    } else if (y < 14) {
                        vox = Voxel.pack(10, 0, 15);
                    } else {
                        vox = Voxel.pack(0, 0, 15);
                    }
                    v[Lod.index(x, y, z)] = vox;
                }
            }
        }
        // a few trees
        for (int t = 0; t < 6; t++) {
            int tx = 2 + r.nextInt(28), tz = 2 + r.nextInt(28);
            for (int dy = 0; dy < 8; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        int y = 20 + dy;
                        if (dy >= 4 && y < 32) {
                            v[Lod.index(tx + dx, y, tz + dz)] = 6;
                        }
                    }
                }
            }
        }
        return v;
    }

    @Test
    void meshesTerrainQuickly() {
        Random r = new Random(11);
        int n = 300;
        int[][] sections = new int[n][];
        for (int i = 0; i < n; i++) {
            sections[i] = terrain(r);
        }
        Mesher m = new Mesher(TABLE);
        for (int i = 0; i < 50; i++) {
            m.build(sections[i], 0, null, true);
        }
        long quads = 0;
        long t = System.nanoTime();
        for (int i = 0; i < n; i++) {
            quads += m.build(sections[i], 0, null, true).quadCount();
        }
        double ms = (System.nanoTime() - t) / 1e6 / n;
        System.out.printf("[bench] mesher: %.3f ms/section, %.0f quads/section%n", ms, quads / (double) n);
        assertTrue(ms < 50, "mesher too slow: " + ms + " ms/section");
    }
}
