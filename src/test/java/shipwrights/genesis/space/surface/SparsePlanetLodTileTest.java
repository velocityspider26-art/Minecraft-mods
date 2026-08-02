package shipwrights.genesis.space.surface;

import org.junit.jupiter.api.Test;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SparsePlanetLodTileTest {
    @Test
    void alignedTilesStayInsideEveryCubeFace() {
        CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(512.0, 1000.0);
        Random random = new Random(0x51A7EL);

        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            for (int cellSize : new int[]{1, 4, 16, 64, 128}) {
                int span = SparsePlanetLodTile.RESOLUTION * cellSize;
                int minX = (int) Math.round(transform.faceCenterX(face) - transform.faceHalfSpan());
                int minZ = (int) Math.round(transform.faceCenterZ(face) - transform.faceHalfSpan());
                int tilesPerFace = (int) (transform.faceSpan() / span);

                for (int sample = 0; sample < 1_000; sample++) {
                    int originX = minX + random.nextInt(tilesPerFace) * span;
                    int originZ = minZ + random.nextInt(tilesPerFace) * span;
                    SparsePlanetLodTile tile = tile(face, originX, originZ, cellSize);

                    assertEquals(span, tile.span());
                    assertEquals(face, transform.faceContaining(
                            originX + span * 0.5,
                            originZ + span * 0.5));
                }
            }
        }
    }

    @Test
    void rejectsMisalignedAndMalformedTiles() {
        assertThrows(IllegalArgumentException.class,
                () -> tile(CubeNetSurfaceTransform.Face.UP, 1, 0, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new SparsePlanetLodTile(CubeNetSurfaceTransform.Face.UP, 0, 0, 3,
                        false, 0, new short[256], new int[256]));
        assertThrows(IllegalArgumentException.class,
                () -> new SparsePlanetLodTile(CubeNetSurfaceTransform.Face.UP, 0, 0, 4,
                        false, 0, new short[255], new int[256]));
    }

    private static SparsePlanetLodTile tile(CubeNetSurfaceTransform.Face face,
                                             int originX, int originZ, int cellSize) {
        return new SparsePlanetLodTile(face, originX, originZ, cellSize,
                false, 0, new short[256], new int[256]);
    }
}
