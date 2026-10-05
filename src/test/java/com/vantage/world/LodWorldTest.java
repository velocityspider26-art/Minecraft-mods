package com.vantage.world;

import com.vantage.core.Lod;
import com.vantage.core.Mipper;
import com.vantage.core.SectionKey;
import com.vantage.core.VisualClass;
import com.vantage.core.Voxel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LodWorldTest {
    static final VisualClass.Table TABLE = vid -> vid == 0 ? VisualClass.AIR : vid >= 10 ? VisualClass.TRANSLUCENT : VisualClass.OPAQUE;

    /** A column of flat ground: solid (vid 4) below y=70 (LOD-space y, from the floor), air above. */
    private static VoxelColumn column(int cx, int cz, int sections) {
        VoxelColumn col = new VoxelColumn(cx, cz, sections);
        Mipper mipper = new Mipper(TABLE);
        for (int cy = 0; cy < sections; cy++) {
            int[] l0 = new int[4096];
            for (int y = 0; y < 16; y++) {
                for (int i = 0; i < 256; i++) {
                    l0[(y << 8) | i] = cy * 16 + y < 70 ? 4 : Voxel.pack(0, 0, 15);
                }
            }
            int[] l1 = new int[512], l2 = new int[64], l3 = new int[8], l4 = new int[1];
            mipper.downsample(l0, 16, l1, 8, 0, 0, 0);
            mipper.downsample(l1, 8, l2, 4, 0, 0, 0);
            mipper.downsample(l2, 4, l3, 2, 0, 0, 0);
            mipper.downsample(l3, 2, l4, 1, 0, 0, 0);
            col.levels[cy] = new int[][]{l0, l1, l2, l3, l4};
        }
        return col;
    }

    @Test
    void insertFillsEveryLevelAndNotifies(@TempDir Path dir) {
        List<Long> changed = new ArrayList<>();
        try (LodWorld w = new LodWorld(dir, -64, 384, true, TABLE, 64L << 20)) {
            w.setListener((k, m) -> {
                synchronized (changed) {
                    changed.add(k);
                }
            });
            w.insert(column(3, -5, 24));
            // Level 0: chunk (3,-5) is in section x=1, z=-3; LOD y 69 -> section 2, local y 5.
            LodSection.Snapshot s0 = w.snapshot(SectionKey.of(0, 1, 2, -3));
            int localX = (3 & 1) * 16, localZ = ((-5) & 1) * 16;
            assertEquals(4, Voxel.vid(s0.get(Lod.index(localX, 5, localZ))));
            assertEquals(0, Voxel.vid(s0.get(Lod.index(localX, 6, localZ))));
            // Voxels of the section outside this chunk stay unknown (sky-lit air).
            assertEquals(Voxel.UNKNOWN_AIR, s0.get(Lod.index((localX + 16) & 31, 5, localZ)));
            // Level 4: one voxel per chunk section; y 64..79 has ground -> solid.
            LodSection.Snapshot s4 = w.snapshot(SectionKey.of(4, 0, 0, -1));
            assertEquals(4, Voxel.vid(s4.get(Lod.index(3, 4, (-5) & 31))));
            assertEquals(0, Voxel.vid(s4.get(Lod.index(3, 5, (-5) & 31))));
            assertTrue(changed.contains(SectionKey.of(4, 0, 0, -1)));
            assertEquals(LodWorld.PRESENT, w.content(SectionKey.of(0, 1, 2, -3)));
            assertEquals(LodWorld.ABSENT, w.content(SectionKey.of(0, 50, 2, 50)));
        }
    }

    @Test
    void higherLevelsAndPersistence(@TempDir Path dir) {
        try (LodWorld w = new LodWorld(dir, -64, 384, true, TABLE, 64L << 20)) {
            w.insert(column(0, 0, 24));
            w.processPendingMips();
            LodSection.Snapshot s5 = w.snapshot(SectionKey.of(5, 0, 0, 0));
            // Level 5 voxel = 32 blocks; ground below y=70 -> voxels y=0,1 solid, y=2 (64..95) solid too.
            assertEquals(4, Voxel.vid(s5.get(Lod.index(0, 2, 0))));
            assertEquals(0, Voxel.vid(s5.get(Lod.index(0, 3, 0))));
            LodSection.Snapshot s6 = w.snapshot(SectionKey.of(6, 0, 0, 0));
            assertEquals(4, Voxel.vid(s6.get(Lod.index(0, 1, 0))));
        }
        try (LodWorld w = new LodWorld(dir, -64, 384, true, TABLE, 64L << 20)) {
            LodSection.Snapshot s0 = w.snapshot(SectionKey.of(0, 0, 2, 0));
            assertEquals(4, Voxel.vid(s0.get(Lod.index(0, 5, 0))));
            LodSection.Snapshot s6 = w.snapshot(SectionKey.of(6, 0, 0, 0));
            assertEquals(4, Voxel.vid(s6.get(Lod.index(0, 1, 0))));
            assertEquals(LodWorld.EMPTY, w.content(SectionKey.of(0, 0, 11, 0)));
        }
    }

    @Test
    void reinsertingSameDataChangesNothing(@TempDir Path dir) {
        int[] count = {0};
        try (LodWorld w = new LodWorld(dir, -64, 384, true, TABLE, 64L << 20)) {
            w.insert(column(7, 7, 24));
            w.setListener((k, m) -> count[0]++);
            w.insert(column(7, 7, 24));
            assertEquals(0, count[0]);
        }
    }
}
