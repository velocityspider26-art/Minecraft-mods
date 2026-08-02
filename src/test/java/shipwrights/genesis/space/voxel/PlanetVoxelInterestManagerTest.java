package shipwrights.genesis.space.voxel;

import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetVoxelInterestManagerTest {
    private static final ResourceLocation EARTH =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final long SOLID = 1L;

    @Test
    void streamPlanOrdersCoarseAncestorBeforeVisibleChildren() {
        PlanetVoxelBrick parent = new PlanetVoxelBrickBuilder(key(1, 0), 1)
                .set(1, 1, 1, SOLID)
                .set(12, 1, 1, SOLID)
                .build();
        PlanetVoxelBrick child0 = brick(key(0, 0), 2);
        PlanetVoxelBrick child1 = brick(key(0, 1), 3);

        PlanetVoxelInterestManager.StreamPlan plan = PlanetVoxelInterestManager.buildPlan(
                List.of(child1, parent, child0), request(), 512.0, 0, Set.of());

        List<PlanetVoxelBrickKey> keys = plan.orderedBricks().stream()
                .map(PlanetVoxelBrick::key).toList();
        assertEquals(parent.key(), keys.getFirst());
        assertTrue(keys.indexOf(child0.key()) > keys.indexOf(parent.key()));
        assertTrue(keys.indexOf(child1.key()) > keys.indexOf(parent.key()));
        assertEquals(Set.of(child0.key(), child1.key()), plan.viewportSelection());
    }

    @Test
    void streamPlanKeepsNewestRevisionForDuplicateKey() {
        PlanetVoxelBrick old = brick(key(1, 0), 2);
        PlanetVoxelBrick current = brick(key(1, 0), 9);

        PlanetVoxelInterestManager.StreamPlan plan = PlanetVoxelInterestManager.buildPlan(
                List.of(old, current), request(), 512.0, 0, Set.of());

        assertEquals(1, plan.orderedBricks().size());
        assertEquals(9, plan.orderedBricks().getFirst().revision());
    }

    @Test
    void streamPlanRequestsMissingOccupiedChildrenAcrossTheViewport() {
        PlanetVoxelBrick parent = new PlanetVoxelBrickBuilder(key(1, 0), 1)
                .set(1, 1, 1, SOLID)
                .set(12, 1, 1, SOLID)
                .build();

        PlanetVoxelInterestManager.StreamPlan plan = PlanetVoxelInterestManager.buildPlan(
                List.of(parent), request(), 512.0, 0, Set.of());

        assertEquals(Set.of(key(0, 0), key(0, 1)), plan.predictionRequests());
    }

    private static PlanetVoxelInterestManager.InterestRequest request() {
        return new PlanetVoxelInterestManager.InterestRequest(EARTH,
                new Vector3d(0.0, 0.0, 1200.0), new Vector3d(0.0, 0.0, -1.0),
                1080, 16.0 / 9.0, Math.toRadians(70.0), 0.05, 128);
    }

    private static PlanetVoxelBrick brick(PlanetVoxelBrickKey key, long revision) {
        return new PlanetVoxelBrickBuilder(key, revision).set(1, 1, 1, SOLID).build();
    }

    private static PlanetVoxelBrickKey key(int lod, int brickU) {
        return new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.SOUTH,
                lod, brickU, 0, 0);
    }
}
