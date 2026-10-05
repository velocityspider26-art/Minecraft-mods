package com.vantage.client.ingest;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionImporterTest {
    /** Packs like Minecraft's SimpleBitStorage: entries never straddle two longs. */
    private static long[] pack(int[] values, int bits) {
        int perLong = 64 / bits;
        long[] out = new long[(values.length + perLong - 1) / perLong];
        for (int i = 0; i < values.length; i++) {
            out[i / perLong] |= (long) values[i] << ((i % perLong) * bits);
        }
        return out;
    }

    @Test
    void unpacksEveryBitWidth() {
        Random r = new Random(4);
        for (int bits = 1; bits <= 15; bits++) {
            int palette = Math.min(1 << bits, 4000);
            int[] values = new int[4096];
            short[] expected = new short[4096];
            for (int i = 0; i < values.length; i++) {
                values[i] = r.nextInt(palette);
                expected[i] = (short) values[i];
            }
            assertArrayEquals(expected, RegionImporter.unpack(pack(values, bits), bits, 4096, palette), "bits " + bits);
        }
    }

    @Test
    void outOfRangeIndicesFallBackToZero() {
        long[] data = pack(new int[]{3, 1, 7}, 4);
        assertArrayEquals(new short[]{3, 1, 0}, RegionImporter.unpack(data, 4, 3, 4));
    }

    @Test
    void ceilLog2() {
        assertEquals(0, RegionImporter.ceilLog2(1));
        assertEquals(1, RegionImporter.ceilLog2(2));
        assertEquals(2, RegionImporter.ceilLog2(3));
        assertEquals(4, RegionImporter.ceilLog2(16));
        assertEquals(5, RegionImporter.ceilLog2(17));
    }
}
