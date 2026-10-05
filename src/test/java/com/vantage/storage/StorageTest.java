package com.vantage.storage;

import com.vantage.core.Lod;
import com.vantage.core.SectionKey;
import com.vantage.core.Voxel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageTest {
    private static int[] terrain(Random r) {
        int[] v = new int[Lod.VOLUME];
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++) {
                int h = 10 + r.nextInt(4);
                for (int y = 0; y < 32; y++) {
                    v[Lod.index(x, y, z)] = y < h - 3 ? Voxel.FILLER_VID : y < h ? 5 + r.nextInt(2) : Voxel.pack(0, 0, 15);
                }
            }
        }
        return v;
    }

    @Test
    void codecRoundTrips() {
        Random r = new Random(3);
        int[] v = terrain(r);
        byte[] blob = SectionCodec.encode(v);
        assertTrue(blob.length < 4096, "surface section should compress well, got " + blob.length);
        assertArrayEquals(v, SectionCodec.decode(blob, null));

        int[] noise = new int[Lod.VOLUME];
        for (int i = 0; i < noise.length; i++) {
            noise[i] = r.nextInt(1000);
        }
        assertArrayEquals(noise, SectionCodec.decode(SectionCodec.encode(noise), null));
        assertNull(SectionCodec.uniformValue(noise));
        int[] flat = new int[Lod.VOLUME];
        java.util.Arrays.fill(flat, 9);
        assertEquals(9, SectionCodec.uniformValue(flat));
    }

    @Test
    void regionStorePersistsAndReusesSpace(@TempDir Path dir) throws Exception {
        Random r = new Random(5);
        long a = SectionKey.of(0, -3, 2, 17);
        long b = SectionKey.of(0, -3, 3, 17);
        long c = SectionKey.of(4, 100, 0, -100);
        int[] va = terrain(r);
        int[] vb = terrain(r);
        try (RegionStore s = new RegionStore(dir, 384)) {
            assertFalse(RegionStore.isPresent(s.entry(a)));
            s.writeBlob(a, SectionCodec.encode(va));
            s.writeBlob(b, SectionCodec.encode(vb));
            s.writeUniform(c, 77);
            // Rewrite a few times; sectors must be recycled rather than grow without bound.
            for (int i = 0; i < 20; i++) {
                s.writeBlob(a, SectionCodec.encode(va));
            }
        }
        try (RegionStore s = new RegionStore(dir, 384)) {
            assertArrayEquals(va, SectionCodec.decode(s.readBlob(a), null));
            assertArrayEquals(vb, SectionCodec.decode(s.readBlob(b), null));
            long e = s.entry(c);
            assertTrue(RegionStore.isUniform(e));
            assertEquals(77, RegionStore.uniformValue(e));
            assertFalse(RegionStore.isPresent(s.entry(SectionKey.of(0, -3, 4, 17))));
        }
        long size = java.nio.file.Files.size(dir.resolve("L0").resolve("-1.1.vlod"));
        assertTrue(size < 64 * 1024, "region file grew to " + size);
    }
}
