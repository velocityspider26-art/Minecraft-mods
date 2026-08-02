package shipwrights.genesis.space.voxel;

import org.junit.jupiter.api.Test;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class PlanetVoxelAuthorityTest {
    private static final long TERRAIN = PlanetVoxelMaterial.pack(1, 0x66884A, 15, 0,
            PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_NATURAL);
    private static final long BUILD = PlanetVoxelMaterial.pack(2, 0xA86B45, 15, 2,
            PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_STRUCTURE);

    @Test
    void exactAirAndSolidOutrankNewerPredictions() throws Exception {
        PlanetVoxelStore memory = new PlanetVoxelStore(16L * 1024L * 1024L);
        PlanetVoxelPyramid pyramid = new PlanetVoxelPyramid(memory, null,
                new AtomicLong(100L), 2);
        PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(
                CubeNetSurfaceTransform.Face.UP, 0, 0, 0, 0);

        PlanetVoxelBrickBuilder predicted = new PlanetVoxelBrickBuilder(key, 1L);
        predicted.set(0, 0, 0, TERRAIN, 255, PlanetVoxelAuthority.TERRAIN_PREDICTED);
        predicted.set(1, 0, 0, TERRAIN, 255, PlanetVoxelAuthority.TERRAIN_PREDICTED);
        pyramid.acceptBrick(predicted.build());

        PlanetVoxelBrickBuilder exact = new PlanetVoxelBrickBuilder(key, 2L);
        exact.setAir(0, 0, 0, PlanetVoxelAuthority.GENERATED_EXACT);
        exact.set(1, 0, 0, BUILD, 255, PlanetVoxelAuthority.GENERATED_EXACT);
        pyramid.acceptBrick(exact.build());

        PlanetVoxelBrickBuilder newerPrediction = new PlanetVoxelBrickBuilder(key, 10_000L);
        newerPrediction.set(0, 0, 0, TERRAIN, 255,
                PlanetVoxelAuthority.TERRAIN_PREDICTED);
        newerPrediction.set(1, 0, 0, TERRAIN, 255,
                PlanetVoxelAuthority.TERRAIN_PREDICTED);
        pyramid.acceptBrick(newerPrediction.build());

        PlanetVoxelBrick stored = memory.get(key);
        assertNotNull(stored);
        assertEquals(PlanetVoxelMaterial.AIR, stored.material(0, 0, 0));
        assertEquals(PlanetVoxelAuthority.GENERATED_EXACT, stored.authority(0, 0, 0));
        assertEquals(BUILD, stored.material(1, 0, 0));
        assertEquals(PlanetVoxelAuthority.GENERATED_EXACT, stored.authority(1, 0, 0));
    }

    @Test
    void partialExactRefinementRetainsCoarsePredictedFallback() throws Exception {
        PlanetVoxelStore memory = new PlanetVoxelStore(16L * 1024L * 1024L);
        PlanetVoxelPyramid pyramid = new PlanetVoxelPyramid(memory, null,
                new AtomicLong(100L), 2);
        PlanetVoxelBrickKey parentKey = new PlanetVoxelBrickKey(
                CubeNetSurfaceTransform.Face.SOUTH, 1, 0, 0, 0);
        PlanetVoxelBrickBuilder coarse = new PlanetVoxelBrickBuilder(parentKey, 1L);
        for (int y = 0; y < PlanetVoxelBrick.EDGE; y++) {
            for (int z = 0; z < PlanetVoxelBrick.EDGE; z++) {
                for (int x = 0; x < PlanetVoxelBrick.EDGE; x++) {
                    coarse.set(x, y, z, TERRAIN, 255,
                            PlanetVoxelAuthority.TERRAIN_PREDICTED);
                }
            }
        }
        pyramid.acceptBrick(coarse.build());

        PlanetVoxelBrickKey childKey = new PlanetVoxelBrickKey(
                CubeNetSurfaceTransform.Face.SOUTH, 0, 0, 0, 0);
        PlanetVoxelBrick exactAir = new PlanetVoxelBrickBuilder(childKey, 2L)
                .fillAuthority(PlanetVoxelAuthority.GENERATED_EXACT)
                .build();
        pyramid.acceptBrick(exactAir);

        PlanetVoxelBrick refinedParent = memory.get(parentKey);
        assertNotNull(refinedParent);
        assertEquals(PlanetVoxelMaterial.AIR, refinedParent.material(0, 0, 0));
        assertEquals(PlanetVoxelAuthority.GENERATED_EXACT,
                refinedParent.authority(0, 0, 0));
        assertEquals(TERRAIN, refinedParent.material(15, 15, 15));
        assertEquals(PlanetVoxelAuthority.TERRAIN_PREDICTED,
                refinedParent.authority(15, 15, 15));
    }
}
