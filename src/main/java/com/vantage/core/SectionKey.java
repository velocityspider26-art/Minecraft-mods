package com.vantage.core;

/**
 * Packs a section address {@code (level, x, y, z)} into a {@code long}.
 *
 * <pre>
 * bits 60..62  level (0..7)
 * bits 52..59  y     (0..255, relative to the dimension floor)
 * bits 26..51  x     (signed 26 bits)
 * bits  0..25  z     (signed 26 bits)
 * </pre>
 */
public final class SectionKey {
    private SectionKey() {
    }

    public static long of(int level, int x, int y, int z) {
        return ((long) level << 60)
                | ((long) (y & 0xFF) << 52)
                | ((long) (x & 0x3FFFFFF) << 26)
                | (z & 0x3FFFFFFL);
    }

    public static int level(long key) {
        return (int) (key >>> 60) & 0x7;
    }

    public static int y(long key) {
        return (int) (key >>> 52) & 0xFF;
    }

    public static int x(long key) {
        return (int) (key << 12 >> 38);
    }

    public static int z(long key) {
        return (int) (key << 38 >> 38);
    }

    public static long parent(long key) {
        return of(level(key) + 1, x(key) >> 1, y(key) >> 1, z(key) >> 1);
    }

    /** Child {@code i} where bit 0 = +x, bit 1 = +z, bit 2 = +y. */
    public static long child(long key, int i) {
        return of(level(key) - 1, (x(key) << 1) | (i & 1), (y(key) << 1) | ((i >> 2) & 1), (z(key) << 1) | ((i >> 1) & 1));
    }

    public static long offset(long key, int dx, int dy, int dz) {
        return of(level(key), x(key) + dx, y(key) + dy, z(key) + dz);
    }

    public static String toString(long key) {
        return "L" + level(key) + "[" + x(key) + "," + y(key) + "," + z(key) + "]";
    }
}
