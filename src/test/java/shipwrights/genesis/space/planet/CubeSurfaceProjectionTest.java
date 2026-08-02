package shipwrights.genesis.space.planet;

import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coordinate correctness for the cube projection.
 *
 * <p>These are the tests that matter most in this system: if the mapping is off
 * by even a face's worth of offset, a player descends onto the wrong continent
 * and nothing about the rendering makes it obvious.</p>
 */
class CubeSurfaceProjectionTest {

    private static final double SIZE = 512.0;
    private static final double PADDING = 64.0;
    private static final int SEA_LEVEL = 63;

    private static CubeSurfaceProjection projection() {
        return new CubeSurfaceProjection(new CubeNetSurfaceTransform(SIZE, PADDING), SEA_LEVEL);
    }

    @Test
    void faceLocalRoundTripsThroughCubeSpaceOnEveryFace() {
        CubeSurfaceProjection projection = projection();
        double half = projection.faceHalfSpan();
        double[] samples = {0.0, 1.0, -1.0, half * 0.5, -half * 0.5,
                half - 1.0, -(half - 1.0)};

        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            for (double u : samples) {
                for (double v : samples) {
                    Vector3d cube = projection.faceLocalToCube(face, u, v, 0.0);
                    CubeSurfaceProjection.CubeSurfaceHit back = projection.cubeToFaceLocal(cube);
                    assertSame(face, back.face(), "face changed for " + face + " " + u + "," + v);
                    assertEquals(u, back.surfaceX(), 1.0E-6, "u drifted on " + face);
                    assertEquals(v, back.surfaceZ(), 1.0E-6, "v drifted on " + face);
                    assertEquals(0.0, back.distance(), 1.0E-6, "elevation drifted on " + face);
                }
            }
        }
    }

    @Test
    void faceCornersAndEdgesStayOnACubeFace() {
        CubeSurfaceProjection projection = projection();
        double half = projection.faceHalfSpan();
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            for (double u : new double[]{-half, 0.0, half}) {
                for (double v : new double[]{-half, 0.0, half}) {
                    Vector3d cube = projection.faceLocalToCube(face, u, v, 0.0);
                    CubeSurfaceProjection.CubeSurfaceHit back = projection.cubeToFaceLocal(cube);
                    // On a shared edge or corner two faces are equally valid;
                    // what must never happen is landing somewhere else entirely.
                    assertTrue(Math.abs(back.surfaceX()) <= half + 1.0E-6
                                    && Math.abs(back.surfaceZ()) <= half + 1.0E-6,
                            "edge sample left the face on " + face);
                    assertEquals(0.0, back.distance(), 1.0E-6);
                }
            }
        }
    }

    @Test
    void elevationSurvivesTheRoundTrip() {
        CubeSurfaceProjection projection = projection();
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            for (double elevation : new double[]{-48.0, 0.0, 96.0, 320.0}) {
                Vector3d cube = projection.faceLocalToCube(face, 128.0, -256.0, elevation);
                CubeSurfaceProjection.CubeSurfaceHit back = projection.cubeToFaceLocal(cube);
                assertSame(face, back.face());
                assertEquals(elevation, back.distance(), 1.0E-6);
            }
        }
    }

    @Test
    void worldToFaceAndBackIsExactIncludingNegativeCoordinates() {
        CubeSurfaceProjection projection = projection();
        Random random = new Random(20260802L);
        for (int sample = 0; sample < 4000; sample++) {
            CubeNetSurfaceTransform.Face face = CubeNetSurfaceTransform.Face.values()[
                    random.nextInt(CubeNetSurfaceTransform.Face.values().length)];
            double half = projection.faceHalfSpan();
            double u = (random.nextDouble() * 2.0 - 1.0) * (half - 1.0);
            double v = (random.nextDouble() * 2.0 - 1.0) * (half - 1.0);
            double worldX = projection.faceToWorldX(face, u);
            double worldZ = projection.faceToWorldZ(face, v);

            CubeSurfaceProjection.PlanetSurfacePosition back =
                    projection.worldToFace(worldX, 70.0, worldZ);
            assertSame(face, back.face(), "world column landed on the wrong face");
            assertEquals(u, back.surfaceX(), 1.0E-9);
            assertEquals(v, back.surfaceZ(), 1.0E-9);
            assertEquals(70.0, back.height(), 0.0);
        }
    }

    @Test
    void columnsOutsideTheNetAreReportedRatherThanSilentlyRemapped() {
        CubeSurfaceProjection projection = projection();
        double faceSpan = projection.faceHalfSpan() * 2.0;
        assertTrue(projection.isInsideNet(0.0, 0.0));
        assertTrue(!projection.isInsideNet(faceSpan * 6.0, faceSpan * 6.0),
                "a column far outside the 3x2 net must not claim to be inside it");
    }

    @Test
    void rayFromOutsideHitsTheFaceItIsAimedAt() {
        CubeSurfaceProjection projection = projection();
        double half = projection.faceHalfSpan();
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            Vector3d normal = projection.faceNormal(face);
            Vector3d origin = new Vector3d(normal).mul(half * 4.0);
            Vector3d direction = new Vector3d(normal).negate();
            CubeSurfaceProjection.CubeSurfaceHit hit =
                    projection.cubeRayIntersection(origin, direction, 0.0);
            assertNotNull(hit, "ray straight at " + face + " missed the cube");
            assertSame(face, hit.face());
            assertEquals(0.0, hit.surfaceX(), 1.0E-6);
            assertEquals(0.0, hit.surfaceZ(), 1.0E-6);
            assertEquals(half * 3.0, hit.distance(), 1.0E-6);
        }
    }

    @Test
    void rayThatMissesTheCubeReturnsNothing() {
        CubeSurfaceProjection projection = projection();
        double half = projection.faceHalfSpan();
        Vector3d origin = new Vector3d(0.0, half * 8.0, 0.0);
        assertNull(projection.cubeRayIntersection(origin, new Vector3d(0.0, 1.0, 0.0), 0.0),
                "a ray pointing away from the cube must not report a hit");
    }

    @Test
    void offsetRayResolvesTheSurfaceCoordinateItIsAimedAt() {
        CubeSurfaceProjection projection = projection();
        double half = projection.faceHalfSpan();
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            Vector3d target = projection.faceLocalToCube(face, half * 0.4, -half * 0.25, 0.0);
            Vector3d origin = new Vector3d(projection.faceNormal(face)).mul(half * 5.0);
            Vector3d direction = new Vector3d(target).sub(origin);
            CubeSurfaceProjection.CubeSurfaceHit hit =
                    projection.cubeRayIntersection(origin, direction, 0.0);
            assertNotNull(hit, "aimed ray missed " + face);
            assertSame(face, hit.face());
            assertEquals(half * 0.4, hit.surfaceX(), 1.0E-6);
            assertEquals(-half * 0.25, hit.surfaceZ(), 1.0E-6);
        }
    }

    @Test
    void velocityTransformsAreInverses() {
        CubeSurfaceProjection projection = projection();
        Random random = new Random(4242L);
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            for (int sample = 0; sample < 200; sample++) {
                Vector3d surface = new Vector3d(random.nextDouble() * 8.0 - 4.0,
                        random.nextDouble() * 8.0 - 4.0, random.nextDouble() * 8.0 - 4.0);
                Vector3d space = projection.velocityToSpaceFrame(face, surface);
                Vector3d back = projection.velocityToSurfaceFrame(face, space);
                assertEquals(surface.x, back.x, 1.0E-9);
                assertEquals(surface.y, back.y, 1.0E-9);
                assertEquals(surface.z, back.z, 1.0E-9);
                assertEquals(surface.length(), space.length(), 1.0E-9,
                        "the frame change must not rescale speed");
            }
        }
    }

    @Test
    void gravityUpRotatesFromSpaceUpToTheFaceNormal() {
        CubeSurfaceProjection projection = projection();
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            Vector3d start = projection.approachGravityUp(face, 0.0);
            assertEquals(1.0, start.y, 1.0E-9, "alignment 0 must still be world up");
            Vector3d end = projection.approachGravityUp(face, 1.0);
            Vector3d normal = projection.faceNormal(face);
            assertEquals(normal.x, end.x, 1.0E-9);
            assertEquals(normal.y, end.y, 1.0E-9);
            assertEquals(normal.z, end.z, 1.0E-9);
            assertEquals(1.0, projection.approachGravityUp(face, 0.5).length(), 1.0E-9);
        }
    }
}
