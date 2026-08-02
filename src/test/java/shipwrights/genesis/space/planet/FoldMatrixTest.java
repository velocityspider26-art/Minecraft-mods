package shipwrights.genesis.space.planet;

import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.teleportation.CubeFaceFrame;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The renderer replaces a per-vertex flat-to-cube morph with one matrix per
 * face. That is only legitimate if the matrix reproduces the morph exactly, so
 * these tests pin the equivalence rather than trusting the derivation.
 */
class FoldMatrixTest {

    private static final double SIZE = 512.0;
    private static final double PADDING = 64.0;
    private static final int SEA_LEVEL = 63;

    private static CubeNetSurfaceTransform transform() {
        return new CubeNetSurfaceTransform(SIZE, PADDING);
    }

    private static CubeSurfaceProjection projection() {
        return new CubeSurfaceProjection(transform(), SEA_LEVEL);
    }

    /** The per-vertex morph the matrix path replaces, written out longhand. */
    private static Vector3d referenceMorph(CubeNetSurfaceTransform transform,
                                           CubeNetSurfaceTransform.Face face,
                                           CubeNetSurfaceTransform.Face currentFace,
                                           double u, double worldY, double v,
                                           double nudge, double fold) {
        double half = transform.faceHalfSpan();
        double elevation = worldY - SEA_LEVEL + nudge;
        double flatX = transform.faceCenterX(face) + u;
        double flatY = SEA_LEVEL + elevation;
        double flatZ = transform.faceCenterZ(face) + v;

        Vector3d celestial = CubeFaceFrame.cubePoint(face, u, v, half, elevation);
        Vector3d folded = CubeFaceFrame.celestialToSurface(currentFace, celestial)
                .add(transform.faceCenterX(currentFace), SEA_LEVEL - half,
                        transform.faceCenterZ(currentFace));
        return new Vector3d(
                flatX + (folded.x - flatX) * fold,
                flatY + (folded.y - flatY) * fold,
                flatZ + (folded.z - flatZ) * fold);
    }

    @Test
    void foldMatrixMatchesThePerVertexMorphEverywhere() {
        CubeNetSurfaceTransform transform = transform();
        CubeSurfaceProjection projection = projection();
        Random random = new Random(99L);
        double half = projection.faceHalfSpan();
        double nudge = 0.72;

        for (CubeNetSurfaceTransform.Face currentFace : CubeNetSurfaceTransform.Face.values()) {
            for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
                for (double fold : new double[]{0.0, 0.13, 0.5, 0.87, 1.0}) {
                    Matrix4d matrix = projection.foldMatrix(face, currentFace, fold);
                    for (int sample = 0; sample < 40; sample++) {
                        double u = (random.nextDouble() * 2.0 - 1.0) * half;
                        double v = (random.nextDouble() * 2.0 - 1.0) * half;
                        double worldY = random.nextDouble() * 384.0 - 64.0;

                        Vector3d expected = referenceMorph(transform, face, currentFace,
                                u, worldY, v, nudge, fold);
                        Vector3d actual = matrix.transformPosition(
                                projection.localVector(u, worldY, v, nudge));

                        assertEquals(expected.x, actual.x, 1.0E-7,
                                "x mismatch " + face + "->" + currentFace + " fold " + fold);
                        assertEquals(expected.y, actual.y, 1.0E-7,
                                "y mismatch " + face + "->" + currentFace + " fold " + fold);
                        assertEquals(expected.z, actual.z, 1.0E-7,
                                "z mismatch " + face + "->" + currentFace + " fold " + fold);
                    }
                }
            }
        }
    }

    @Test
    void theCameraSOwnFaceNeverMovesAtAnyFoldValue() {
        CubeSurfaceProjection projection = projection();
        double half = projection.faceHalfSpan();
        // This is what makes the flat zone around the craft exact instead of
        // tuned: the ground the player is standing on is identical flat and
        // folded, so no fold schedule can ever distort it.
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            for (double fold : new double[]{0.0, 0.25, 0.5, 0.75, 1.0}) {
                Matrix4d folded = projection.foldMatrix(face, face, fold);
                for (double u : new double[]{-half, -13.0, 0.0, 91.5, half}) {
                    for (double v : new double[]{-half, -7.0, 0.0, 640.25, half}) {
                        Vector3d local = projection.localVector(u, 72.0, v, 0.72);
                        Vector3d point = folded.transformPosition(new Vector3d(local));
                        Vector3d flat = projection.flatMatrix(face)
                                .transformPosition(new Vector3d(local));
                        assertEquals(flat.x, point.x, 1.0E-9);
                        assertEquals(flat.y, point.y, 1.0E-9);
                        assertEquals(flat.z, point.z, 1.0E-9);
                    }
                }
            }
        }
    }

    @Test
    void flatMatrixPlacesTerrainAtItsRealWorldCoordinates() {
        CubeNetSurfaceTransform transform = transform();
        CubeSurfaceProjection projection = projection();
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            Vector3d point = projection.flatMatrix(face)
                    .transformPosition(projection.localVector(120.0, 88.0, -300.0, 0.0));
            assertEquals(transform.faceCenterX(face) + 120.0, point.x, 1.0E-9);
            assertEquals(88.0, point.y, 1.0E-9);
            assertEquals(transform.faceCenterZ(face) - 300.0, point.z, 1.0E-9);
        }
    }

    @Test
    void adjacentFacesMeetAlongTheirSharedEdgeWhenFullyFolded() {
        CubeSurfaceProjection projection = projection();
        double half = projection.faceHalfSpan();
        CubeNetSurfaceTransform.Face viewer = CubeNetSurfaceTransform.Face.UP;

        // The UP/SOUTH seam: UP's +v edge and SOUTH's -v edge are the same
        // physical line on the cube, so folded geometry from both must land on
        // top of each other or the planet shows a crack along every seam.
        for (double u : new double[]{-half, -half * 0.5, 0.0, half * 0.5, half}) {
            Vector3d fromUp = projection.foldMatrix(CubeNetSurfaceTransform.Face.UP, viewer, 1.0)
                    .transformPosition(projection.localVector(u, SEA_LEVEL, half, 0.0));
            Vector3d fromSouth = projection.foldMatrix(CubeNetSurfaceTransform.Face.SOUTH, viewer, 1.0)
                    .transformPosition(projection.localVector(u, SEA_LEVEL, -half, 0.0));
            assertEquals(fromUp.x, fromSouth.x, 1.0E-6, "seam split in x at u=" + u);
            assertEquals(fromUp.y, fromSouth.y, 1.0E-6, "seam split in y at u=" + u);
            assertEquals(fromUp.z, fromSouth.z, 1.0E-6, "seam split in z at u=" + u);
        }
    }

    @Test
    void foldIsContinuousInTheFoldParameter() {
        CubeSurfaceProjection projection = projection();
        double half = projection.faceHalfSpan();
        Vector3d local = projection.localVector(half * 0.3, 140.0, -half * 0.7, 0.72);
        CubeNetSurfaceTransform.Face face = CubeNetSurfaceTransform.Face.NORTH;
        CubeNetSurfaceTransform.Face viewer = CubeNetSurfaceTransform.Face.UP;

        Vector3d previous = null;
        double largestStep = 0.0;
        for (int step = 0; step <= 1000; step++) {
            double fold = step / 1000.0;
            Vector3d point = projection.foldMatrix(face, viewer, fold)
                    .transformPosition(new Vector3d(local));
            if (previous != null) largestStep = Math.max(largestStep, previous.distance(point));
            previous = point;
        }
        // Total travel is bounded by the cube's own size, so a 0.001 step must
        // move far less than a face span. A discontinuity would show as a jump.
        assertTrue(largestStep < half * 0.02,
                "fold jumped by " + largestStep + " within a 0.001 step");
    }

    @Test
    void orbitMatrixScalesAndPlacesTheCubeWithoutDistortingIt() {
        CubeSurfaceProjection projection = projection();
        double half = projection.faceHalfSpan();
        Vector3d planetPosition = new Vector3d(1200.0, -340.0, 880.0);
        double scale = 0.25;

        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            Matrix4d matrix = projection.orbitMatrix(face, planetPosition,
                    new org.joml.Quaterniond(), scale);
            Vector3d centre = matrix.transformPosition(
                    projection.localVector(0.0, SEA_LEVEL, 0.0, 0.0));
            Vector3d expected = new Vector3d(projection.faceNormal(face))
                    .mul(half * scale).add(planetPosition);
            assertEquals(expected.x, centre.x, 1.0E-6);
            assertEquals(expected.y, centre.y, 1.0E-6);
            assertEquals(expected.z, centre.z, 1.0E-6);

            // A rigid scale: two points a known distance apart in face-local
            // space stay that distance apart, times the scale.
            Vector3d a = matrix.transformPosition(
                    projection.localVector(0.0, SEA_LEVEL, 0.0, 0.0));
            Vector3d b = matrix.transformPosition(
                    projection.localVector(100.0, SEA_LEVEL, 0.0, 0.0));
            assertEquals(100.0 * scale, a.distance(b), 1.0E-6);
        }
    }
}
