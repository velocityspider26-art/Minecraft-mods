package com.vantage.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MipperTest {
    // vid 0 air, 1 filler, 2..9 opaque, 10..19 translucent
    static final VisualClass.Table TABLE = vid -> vid == 0 ? VisualClass.AIR : vid >= 10 ? VisualClass.TRANSLUCENT : VisualClass.OPAQUE;
    static final int AIR15 = Voxel.pack(0, 0, 15);

    private static int[] kids(int... v) {
        return v;
    }

    @Test
    void allAirKeepsBrightestLight() {
        Mipper m = new Mipper(TABLE);
        int r = m.reduce(kids(Voxel.pack(0, 3, 2), Voxel.pack(0, 0, 9), 0, 0, 0, 0, Voxel.pack(0, 7, 1), 0));
        assertEquals(Voxel.pack(0, 7, 9), r);
    }

    @Test
    void topLayerWins() {
        Mipper m = new Mipper(TABLE);
        // bottom layer (0..3) dirt=3, top layer (4..7) one grass=2, rest air
        int r = m.reduce(kids(3, 3, 3, 3, 2, AIR15, AIR15, AIR15));
        assertEquals(2, r);
    }

    @Test
    void opaqueBeatsTranslucentWithinLayer() {
        Mipper m = new Mipper(TABLE);
        int water = Voxel.pack(10, 0, 14);
        int r = m.reduce(kids(3, 3, 3, 3, water, water, water, 4));
        assertEquals(4, r);
    }

    @Test
    void translucentKeepsLightOfNonOpaqueChildren() {
        Mipper m = new Mipper(TABLE);
        int water = Voxel.pack(10, 0, 12);
        int r = m.reduce(kids(3, 3, 3, 3, water, water, AIR15, AIR15));
        assertEquals(Voxel.pack(10, 0, 15), r);
    }

    @Test
    void mostCommonWinsAmongEquals() {
        Mipper m = new Mipper(TABLE);
        int r = m.reduce(kids(0, 0, 0, 0, 5, 6, 6, 6));
        assertEquals(6, r);
    }

    @Test
    void fillerOnlyWinsWhenNothingElse() {
        Mipper m = new Mipper(TABLE);
        int f = Voxel.FILLER_VID;
        assertEquals(2, m.reduce(kids(f, f, f, 2, f, f, f, f)));
        assertEquals(7, m.reduce(kids(7, f, f, f, f, f, f, f)));
        assertEquals(f, m.reduce(kids(f, f, f, f, AIR15, AIR15, AIR15, AIR15)));
        assertEquals(f, m.reduce(kids(AIR15, AIR15, AIR15, AIR15, f, AIR15, AIR15, AIR15)));
    }

    @Test
    void downsampleUsesYMajorLayout() {
        Mipper m = new Mipper(TABLE);
        int[] src = new int[8];
        // (x=1, y=1, z=0) -> index (1*2+0)*2+1 = 5
        src[5] = 4;
        int[] dst = new int[1];
        m.downsample(src, 2, dst, 1, 0, 0, 0);
        assertEquals(4, dst[0]);
    }
}
