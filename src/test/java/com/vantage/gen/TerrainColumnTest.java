package com.vantage.gen;

import com.vantage.core.Voxel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainColumnTest {
    private static TerrainColumn column(int surface) {
        TerrainColumn c = new TerrainColumn();
        c.minY = -64;
        c.worldTop = 320;
        c.seaLevel = 63;
        c.surface = surface;
        c.topVid = 2;
        c.underVid = 3;
        c.seabedVid = 4;
        c.stoneVid = 5;
        c.deepVid = 6;
        c.waterVid = 10;
        return c;
    }

    private static int at(int[] out, int y) {
        return out[y + 64];
    }

    @Test
    void landColumnAtBlockLevel() {
        TerrainColumn c = column(80); // top block at y=79
        int[] out = new int[384];
        c.fill(1, 384, out, 0, 1);
        assertEquals(Voxel.pack(0, 0, 15), at(out, 80));
        assertEquals(2, at(out, 79));
        assertEquals(3, at(out, 77));
        assertEquals(5, at(out, 70));
        assertEquals(Voxel.FILLER_VID, at(out, 20));
        assertEquals(Voxel.FILLER_VID, at(out, -60));
    }

    @Test
    void seaColumnHasWaterWithFadingLightAndIce() {
        TerrainColumn c = column(40); // sea floor top block at y=39
        c.iceVid = 11;
        int[] out = new int[384];
        c.fill(1, 384, out, 0, 1);
        assertEquals(Voxel.pack(11, 0, 15), at(out, 62));
        assertEquals(Voxel.pack(10, 0, 14), at(out, 61));
        assertEquals(Voxel.pack(10, 0, 0), at(out, 41));
        assertEquals(4, at(out, 39));
        assertEquals(Voxel.pack(0, 0, 15), at(out, 63));
    }

    @Test
    void coarseVoxelsShowWhatIsSeenFromAbove() {
        TerrainColumn c = column(70); // land at y=69
        c.leavesVid = 7;
        c.canopyBottom = 73;
        c.canopyTop = 77;
        int[] out = new int[6];
        c.fill(64, 6, out, 0, 1); // voxels: [-64,0) [0,64) [64,128) [128,192) ...
        assertEquals(Voxel.FILLER_VID, out[0]);
        assertEquals(5, out[1], "64 blocks below the canopy voxel: stone");
        assertEquals(7, out[2], "canopy wins over the ground in the same voxel");
        assertEquals(Voxel.pack(0, 0, 15), out[3]);
    }

    @Test
    void waterBeatsSeaFloorInsideOneVoxel() {
        TerrainColumn c = column(50); // floor at 49, water 50..62
        int[] out = new int[3];
        c.fill(128, 3, out, 0, 1); // [-64,64) holds floor and water
        // Lit like its deepest water (12 blocks down), so what shows beneath stays dark.
        assertEquals(Voxel.pack(10, 0, 3), out[0]);
        assertEquals(Voxel.pack(0, 0, 15), out[1]);
    }
}
