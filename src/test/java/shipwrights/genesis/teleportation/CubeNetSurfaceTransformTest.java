package shipwrights.genesis.teleportation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubeNetSurfaceTransformTest {
    private static final double EARTH_SIZE = 512.0;
    private static final double EARTH_PADDING = 1000.0;
    private static final double EPS = 1.0E-8;

    private final CubeNetSurfaceTransform transform =
            new CubeNetSurfaceTransform(EARTH_SIZE, EARTH_PADDING);

    @Test
    @DisplayName("Earth keeps the old 8192-block up-face square centred on the origin")
    void upFacePreservesLegacyCoordinates() {
        double radius = transform.entryRadius();
        double x = 700.25;
        double z = -333.5;
        CubeNetSurfaceTransform.SurfacePoint point = transform.localToWorld(x, radius, z);

        double oldScale = (EARTH_SIZE * 0.5) / (EARTH_SIZE * 0.5 + EARTH_PADDING) * 16.0;
        assertEquals(CubeNetSurfaceTransform.Face.UP, point.face());
        assertEquals(x * oldScale, point.worldX(), EPS);
        assertEquals(z * oldScale, point.worldZ(), EPS);
        assertEquals(8192.0, transform.faceSpan(), EPS);
        assertEquals(0.0, transform.faceCenterX(CubeNetSurfaceTransform.Face.UP), EPS);
        assertEquals(0.0, transform.faceCenterZ(CubeNetSurfaceTransform.Face.UP), EPS);
    }

    @Test
    @DisplayName("Face centres use the rendered atlas' north-west-south/east-down-up layout")
    void faceCentresMatchAtlas() {
        double span = transform.faceSpan();
        assertCentre(CubeNetSurfaceTransform.Face.NORTH, -2.0 * span, -span);
        assertCentre(CubeNetSurfaceTransform.Face.WEST, -span, -span);
        assertCentre(CubeNetSurfaceTransform.Face.SOUTH, 0.0, -span);
        assertCentre(CubeNetSurfaceTransform.Face.EAST, -2.0 * span, 0.0);
        assertCentre(CubeNetSurfaceTransform.Face.DOWN, -span, 0.0);
        assertCentre(CubeNetSurfaceTransform.Face.UP, 0.0, 0.0);
    }

    @Test
    @DisplayName("All six cardinal entry directions land in six distinct regions")
    void cardinalDirectionsUseAllFaces() {
        double r = transform.entryRadius();
        assertFaceAndCentre(CubeNetSurfaceTransform.Face.NORTH, 0, 0, -r);
        assertFaceAndCentre(CubeNetSurfaceTransform.Face.WEST, -r, 0, 0);
        assertFaceAndCentre(CubeNetSurfaceTransform.Face.SOUTH, 0, 0, r);
        assertFaceAndCentre(CubeNetSurfaceTransform.Face.EAST, r, 0, 0);
        assertFaceAndCentre(CubeNetSurfaceTransform.Face.DOWN, 0, -r, 0);
        assertFaceAndCentre(CubeNetSurfaceTransform.Face.UP, 0, r, 0);
    }

    @Test
    @DisplayName("Face UV orientation matches PlanetRenderer's cube atlas")
    void faceOrientationMatchesRenderer() {
        double r = transform.entryRadius();
        double u = r * 0.37;
        double v = -r * 0.22;

        assertLocal(CubeNetSurfaceTransform.Face.NORTH, -u, -v, -r, u, v);
        assertLocal(CubeNetSurfaceTransform.Face.WEST, -r, -v, u, u, v);
        assertLocal(CubeNetSurfaceTransform.Face.SOUTH, u, -v, r, u, v);
        assertLocal(CubeNetSurfaceTransform.Face.EAST, r, -v, -u, u, v);
        assertLocal(CubeNetSurfaceTransform.Face.DOWN, u, -r, -v, u, v);
        assertLocal(CubeNetSurfaceTransform.Face.UP, u, r, v, u, v);
    }

    @Test
    @DisplayName("World and padded-cube coordinates round-trip on every face")
    void randomRoundTripsAreExact() {
        Random random = new Random(0xC0B3_6E7L);
        double r = transform.entryRadius();

        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            for (int i = 0; i < 1000; i++) {
                // Stay off the mathematically shared edge, where two face names
                // are both valid and the net intentionally canonicalises one.
                double u = (random.nextDouble() * 1.98 - 0.99) * r;
                double v = (random.nextDouble() * 1.98 - 0.99) * r;
                CubeNetSurfaceTransform.SurfacePoint world = transform.faceToWorld(face, u, v);
                CubeNetSurfaceTransform.LocalPoint local =
                        transform.worldToLocal(world.worldX(), world.worldZ());
                CubeNetSurfaceTransform.SurfacePoint again =
                        transform.localToWorld(local.x(), local.y(), local.z());

                assertEquals(face, local.face());
                assertFalse(local.outsideNetFallback());
                assertEquals(u, local.u(), EPS);
                assertEquals(v, local.v(), EPS);
                assertEquals(face, again.face());
                assertEquals(world.worldX(), again.worldX(), EPS);
                assertEquals(world.worldZ(), again.worldZ(), EPS);
            }
        }
    }

    @Test
    @DisplayName("Arbitrary local points are projected along their ray before mapping")
    void localPointIsProjectedToEntryCube() {
        double x = 4.0;
        double y = 10.0;
        double z = -2.0;
        CubeNetSurfaceTransform.SurfacePoint world = transform.localToWorld(x, y, z);
        CubeNetSurfaceTransform.LocalPoint shell = transform.worldToLocal(world.worldX(), world.worldZ());

        double factor = transform.entryRadius() / 10.0;
        assertEquals(CubeNetSurfaceTransform.Face.UP, shell.face());
        assertEquals(x * factor, shell.x(), EPS);
        assertEquals(y * factor, shell.y(), EPS);
        assertEquals(z * factor, shell.z(), EPS);
    }

    @Test
    @DisplayName("Positions outside the net use a bounded up-face legacy fallback")
    void outsideNetFallsBackSafely() {
        double far = transform.faceSpan() * 20.0;
        assertNull(transform.faceContaining(far, far));

        CubeNetSurfaceTransform.LocalPoint local = transform.worldToLocal(far, far);
        assertEquals(CubeNetSurfaceTransform.Face.UP, local.face());
        assertTrue(local.outsideNetFallback());
        assertEquals(transform.entryRadius(), local.x(), EPS);
        assertEquals(transform.entryRadius(), local.y(), EPS);
        assertEquals(transform.entryRadius(), local.z(), EPS);
    }

    private void assertCentre(CubeNetSurfaceTransform.Face face, double x, double z) {
        assertEquals(x, transform.faceCenterX(face), EPS);
        assertEquals(z, transform.faceCenterZ(face), EPS);
    }

    private void assertFaceAndCentre(CubeNetSurfaceTransform.Face face, double x, double y, double z) {
        CubeNetSurfaceTransform.SurfacePoint point = transform.localToWorld(x, y, z);
        assertEquals(face, point.face());
        assertEquals(transform.faceCenterX(face), point.worldX(), EPS);
        assertEquals(transform.faceCenterZ(face), point.worldZ(), EPS);
    }

    private void assertLocal(CubeNetSurfaceTransform.Face face,
                             double expectedX, double expectedY, double expectedZ,
                             double u, double v) {
        CubeNetSurfaceTransform.LocalPoint point = transform.faceToLocal(face, u, v);
        assertEquals(expectedX, point.x(), EPS);
        assertEquals(expectedY, point.y(), EPS);
        assertEquals(expectedZ, point.z(), EPS);
        CubeNetSurfaceTransform.SurfacePoint world = transform.faceToWorld(face, u, v);
        assertEquals(face, transform.faceContaining(world.worldX(), world.worldZ()));
    }
}
