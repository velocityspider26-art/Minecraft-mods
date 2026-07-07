package shipwrights.genesis.space;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static shipwrights.genesis.space.CubePlanetMapping.Face;

class CubePlanetMappingTest {

    private static final int SPACING = 20000;
    private static final double SPREAD = 4000;

    @Test
    void dominantAxisPicksTheRightFace() {
        assertEquals(Face.UP, CubePlanetMapping.faceFromApproach(new Vector3d(0.1, 1, -0.2)));
        assertEquals(Face.DOWN, CubePlanetMapping.faceFromApproach(new Vector3d(0.1, -1, 0.2)));
        assertEquals(Face.EAST, CubePlanetMapping.faceFromApproach(new Vector3d(1, 0.1, 0.2)));
        assertEquals(Face.WEST, CubePlanetMapping.faceFromApproach(new Vector3d(-1, 0.2, 0.1)));
        assertEquals(Face.SOUTH, CubePlanetMapping.faceFromApproach(new Vector3d(0.1, 0.2, 1)));
        assertEquals(Face.NORTH, CubePlanetMapping.faceFromApproach(new Vector3d(0.2, 0.1, -1)));
    }

    @Test
    void everyFaceLandsInADistinctRegion() {
        Set<String> regions = new HashSet<>();
        for (Face f : Face.values()) {
            double[] c = CubePlanetMapping.regionCenter(f, SPACING);
            assertTrue(regions.add(c[0] + "," + c[1]), "duplicate region for " + f);
        }
        assertEquals(6, regions.size(), "all six faces must map to distinct regions");
    }

    @Test
    void topAndLeftDoNotCollapseToTheSamePlace() {
        double[] top = CubePlanetMapping.landingXZ(new Vector3d(0, 1, 0), SPACING, SPREAD);
        double[] left = CubePlanetMapping.landingXZ(new Vector3d(-1, 0, 0), SPACING, SPREAD);
        double dist = Math.hypot(top[0] - left[0], top[1] - left[1]);
        assertTrue(dist >= SPACING * 0.9, "top and left landings must be far apart, were " + dist);
    }

    @Test
    void positionOnAFaceShiftsTheLandingWithinItsRegion() {
        double[] center = CubePlanetMapping.landingXZ(new Vector3d(0, 1, 0), SPACING, SPREAD);
        double[] corner = CubePlanetMapping.landingXZ(new Vector3d(0.6, 1, 0.6), SPACING, SPREAD);
        // same face (UP) ...
        assertEquals(Face.UP, CubePlanetMapping.faceFromApproach(new Vector3d(0.6, 1, 0.6)));
        // ... but a different spot within it.
        assertTrue(Math.hypot(center[0] - corner[0], center[1] - corner[1]) > 100);
        // and still inside the region (within the spread radius of the centre).
        double[] rc = CubePlanetMapping.regionCenter(Face.UP, SPACING);
        assertTrue(Math.hypot(corner[0] - rc[0], corner[1] - rc[1]) <= SPREAD * 1.5);
    }

    @Test
    void launchFromARegionExitsTowardThatFace() {
        for (Face f : Face.values()) {
            double[] rc = CubePlanetMapping.regionCenter(f, SPACING);
            CubePlanetMapping.LaunchExit exit = CubePlanetMapping.exitFor(rc[0], rc[1], SPACING, SPREAD);
            assertEquals(f, exit.face(), "region centre of " + f + " should exit through " + f);
            // exit direction points along that face's normal.
            Vector3d n = f.normal();
            double dot = exit.localExitDir().dot(n);
            assertTrue(dot > 0.9, f + " exit dir should align with its normal, dot=" + dot);
        }
    }

    @Test
    void landThenLaunchRoundTripsToTheSameFace() {
        Vector3d[] approaches = {
                new Vector3d(0, 1, 0), new Vector3d(0, -1, 0),
                new Vector3d(1, 0, 0), new Vector3d(-1, 0, 0),
                new Vector3d(0, 0, 1), new Vector3d(0, 0, -1),
                new Vector3d(0.3, 1, -0.4), new Vector3d(-1, 0.2, 0.3),
        };
        for (Vector3d approach : approaches) {
            Face landedFace = CubePlanetMapping.faceFromApproach(approach);
            double[] xz = CubePlanetMapping.landingXZ(approach, SPACING, SPREAD);
            CubePlanetMapping.LaunchExit exit = CubePlanetMapping.exitFor(xz[0], xz[1], SPACING, SPREAD);
            assertEquals(landedFace, exit.face(),
                    "approach " + approach + " landed on " + landedFace + " but launched back through " + exit.face());
        }
    }
}
