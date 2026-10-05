package com.vantage.core;

/** How a visual id behaves for meshing and mipping. */
public final class VisualClass {
    public static final byte AIR = 0;
    /** Fully hides what is behind it. */
    public static final byte OPAQUE = 1;
    /** Drawn with blending (water, glass, ice). */
    public static final byte TRANSLUCENT = 2;

    private VisualClass() {
    }

    /** Lookup of the class of a visual id. Must be safe to call from any thread. */
    @FunctionalInterface
    public interface Table {
        byte classOf(int vid);
    }
}
