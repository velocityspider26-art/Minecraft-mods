package shipwrights.genesis.client.lod;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.space.voxel.PlanetVoxelBrick;
import shipwrights.genesis.space.voxel.PlanetVoxelBrickBuilder;
import shipwrights.genesis.space.voxel.PlanetVoxelBrickKey;
import shipwrights.genesis.space.voxel.PlanetVoxelLodSelector;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetVoxelLodSelectorTest {
    private static final long SOLID = 1L;

    @Test
    void brickTracksOnlyOccupiedOctants() {
        PlanetVoxelBrick brick = new PlanetVoxelBrickBuilder(key(0, 0, 0, 0), 1)
                .set(1, 2, 3, SOLID)
                .set(12, 2, 3, SOLID)
                .set(1, 12, 12, SOLID)
                .build();

        assertEquals((1 << 0) | (1 << 1) | (1 << 6), brick.occupiedOctantMask());
    }

    @Test
    void missingOccupiedChildKeepsCoarseBrickVisible() {
        PlanetVoxelBrick parent = parentWithTwoOccupiedOctants();
        PlanetVoxelBrick child0 = filled(key(0, 0, 0, 0), 2);

        List<PlanetVoxelLodSelector.Selection> selected = PlanetVoxelLodSelector.selectOrbit(
                List.of(parent, child0), view(32), Set.of());

        assertEquals(1, selected.size());
        assertEquals(parent.key(), selected.getFirst().brick().key());
    }

    @Test
    void completeOccupiedChildrenReplaceTheirParent() {
        PlanetVoxelBrick parent = parentWithTwoOccupiedOctants();
        PlanetVoxelBrick child0 = filled(key(0, 0, 0, 0), 2);
        PlanetVoxelBrick child1 = filled(key(0, 1, 0, 0), 3);

        List<PlanetVoxelLodSelector.Selection> selected = PlanetVoxelLodSelector.selectOrbit(
                List.of(parent, child0, child1), view(32), Set.of());

        assertEquals(2, selected.size());
        assertTrue(selected.stream().anyMatch(selection -> selection.brick().key().equals(child0.key())));
        assertTrue(selected.stream().anyMatch(selection -> selection.brick().key().equals(child1.key())));
    }

    @Test
    void coverageRootsAreNeverDiscardedByDetailBudget() {
        PlanetVoxelBrick left = filled(key(1, -1, 0, 0), 1);
        PlanetVoxelBrick right = filled(key(1, 1, 0, 0), 1);

        List<PlanetVoxelLodSelector.Selection> selected = PlanetVoxelLodSelector.selectOrbit(
                List.of(left, right), view(1), Set.of());

        assertEquals(2, selected.size());
    }

    private static PlanetVoxelBrick parentWithTwoOccupiedOctants() {
        return new PlanetVoxelBrickBuilder(key(1, 0, 0, 0), 1)
                .set(1, 1, 1, SOLID)
                .set(12, 1, 1, SOLID)
                .build();
    }

    private static PlanetVoxelBrick filled(PlanetVoxelBrickKey key, long revision) {
        return new PlanetVoxelBrickBuilder(key, revision)
                .set(1, 1, 1, SOLID)
                .build();
    }

    private static PlanetVoxelBrickKey key(int lod, int u, int y, int v) {
        return new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.SOUTH, lod, u, y, v);
    }

    private static PlanetVoxelLodSelector.OrbitView view(int maximumBricks) {
        return new PlanetVoxelLodSelector.OrbitView(
                new Vector3d(0.0, 0.0, 1200.0),
                new Vector3d(0.0, 0.0, -1.0),
                512.0, 1.0, 0,
                1080, 16.0 / 9.0, Math.toRadians(70.0),
                0.05, 1, maximumBricks, false);
    }
}
