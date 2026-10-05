package com.vantage.core;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SectionKeyTest {
    @Test
    void roundTripsIncludingNegativeCoordinates() {
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            int level = r.nextInt(Lod.LEVELS);
            int x = r.nextInt(1 << 26) - (1 << 25);
            int z = r.nextInt(1 << 26) - (1 << 25);
            int y = r.nextInt(256);
            long k = SectionKey.of(level, x, y, z);
            assertEquals(level, SectionKey.level(k));
            assertEquals(x, SectionKey.x(k));
            assertEquals(y, SectionKey.y(k));
            assertEquals(z, SectionKey.z(k));
        }
    }

    @Test
    void noneIsNeverAValidSection() {
        assertEquals(15, SectionKey.level(SectionKey.NONE));
        assertEquals(true, Lod.MAX_LEVEL < 15);
        long top = SectionKey.of(Lod.MAX_LEVEL, -1, 255, -1);
        assertEquals(Lod.MAX_LEVEL, SectionKey.level(top));
        assertEquals(-1, SectionKey.x(top));
    }

    @Test
    void childrenOfParentIncludeSelf() {
        long k = SectionKey.of(2, -7, 3, 12);
        long p = SectionKey.parent(k);
        assertEquals(3, SectionKey.level(p));
        assertEquals(-4, SectionKey.x(p));
        assertEquals(1, SectionKey.y(p));
        assertEquals(6, SectionKey.z(p));
        boolean found = false;
        for (int i = 0; i < 8; i++) {
            found |= SectionKey.child(p, i) == k;
        }
        assertEquals(true, found);
    }
}
