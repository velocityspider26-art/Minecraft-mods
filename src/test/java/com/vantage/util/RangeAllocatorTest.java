package com.vantage.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeAllocatorTest {
    @Test
    void coalescesFreedNeighbours() {
        RangeAllocator a = new RangeAllocator(100);
        long x = a.allocate(30);
        long y = a.allocate(30);
        long z = a.allocate(40);
        assertEquals(-1, a.allocate(1));
        a.free(x, 30);
        a.free(z, 40);
        assertEquals(40, a.largestFree());
        a.free(y, 30);
        assertEquals(100, a.largestFree());
        assertEquals(0, a.used());
    }

    @Test
    void growAddsFreeSpace() {
        RangeAllocator a = new RangeAllocator(10);
        long x = a.allocate(10);
        assertEquals(0, x);
        a.grow(25);
        assertEquals(10, a.allocate(15));
        assertEquals(25, a.used());
    }

    @Test
    void randomStressNeverOverlaps() {
        Random r = new Random(7);
        RangeAllocator a = new RangeAllocator(10_000);
        BitSet used = new BitSet();
        List<long[]> live = new ArrayList<>();
        for (int i = 0; i < 50_000; i++) {
            if (live.isEmpty() || r.nextInt(3) != 0) {
                int size = 1 + r.nextInt(200);
                long off = a.allocate(size);
                if (off >= 0) {
                    for (long p = off; p < off + size; p++) {
                        assertFalse(used.get((int) p), "double allocation");
                        used.set((int) p);
                    }
                    live.add(new long[]{off, size});
                }
            } else {
                long[] b = live.remove(r.nextInt(live.size()));
                a.free(b[0], b[1]);
                used.clear((int) b[0], (int) (b[0] + b[1]));
            }
            assertEquals(used.cardinality(), a.used());
        }
        for (long[] b : live) {
            a.free(b[0], b[1]);
        }
        assertEquals(10_000, a.largestFree());
        assertTrue(a.used() == 0);
    }
}
