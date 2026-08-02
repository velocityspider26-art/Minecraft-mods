package shipwrights.genesis.teleportation;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CubeFaceFrameTest {
    private static final double EPSILON = 1.0E-9;

    @Test
    void everyFaceRotationMapsMinecraftBasisToDeclaredAxes() {
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            CubeFaceFrame.Axes axes = CubeFaceFrame.axes(face);
            Quaterniond rotation = CubeFaceFrame.surfaceToCelestial(face);
            assertVector(axes.right(), rotation.transform(new Vector3d(1, 0, 0)));
            assertVector(axes.up(), rotation.transform(new Vector3d(0, 1, 0)));
            assertVector(axes.south(), rotation.transform(new Vector3d(0, 0, 1)));
        }
    }

    @Test
    void currentFaceAlwaysUnfoldsToFlatMinecraftCoordinates() {
        double half = 4096.0;
        double u = 1234.5;
        double v = -987.25;
        double elevation = 71.0;
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            Vector3d celestial = CubeFaceFrame.cubePoint(face, u, v, half, elevation);
            Vector3d surface = CubeFaceFrame.celestialToSurface(face, celestial);
            assertEquals(u, surface.x, EPSILON);
            assertEquals(half + elevation, surface.y, EPSILON);
            assertEquals(v, surface.z, EPSILON);
        }
    }

    private static void assertVector(org.joml.Vector3dc expected, Vector3d actual) {
        assertEquals(expected.x(), actual.x, EPSILON);
        assertEquals(expected.y(), actual.y, EPSILON);
        assertEquals(expected.z(), actual.z, EPSILON);
    }
}
