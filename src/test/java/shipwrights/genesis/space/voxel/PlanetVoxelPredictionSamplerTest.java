package shipwrights.genesis.space.voxel;

import org.junit.jupiter.api.Test;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlanetVoxelPredictionSamplerTest {
    private static final long STONE = PlanetVoxelMaterial.pack(1, 0x777777, 15, 0,
            PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_NATURAL);

    @Test
    void samplesVerticalVolumeInsteadOfOnlyAHeightSurface() {
        PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(
                CubeNetSurfaceTransform.Face.UP, 2, 0, 0, 0);
        PlanetVoxelBrick brick = PlanetVoxelPredictionSampler.sample(key, 12L,
                0, 0, 0, 32,
                (worldX, worldZ) -> worldY -> worldY < 10
                        ? STONE : PlanetVoxelMaterial.AIR);

        assertEquals(STONE, brick.material(0, 0, 0));
        assertEquals(255, brick.coverage(0, 0, 0));
        assertEquals(STONE, brick.material(0, 2, 0));
        assertTrue(brick.coverage(0, 2, 0) > 0
                && brick.coverage(0, 2, 0) < 255);
        assertEquals(PlanetVoxelMaterial.AIR, brick.material(0, 3, 0));
        assertEquals(PlanetVoxelAuthority.TERRAIN_PREDICTED,
                brick.authority(0, 0, 0));
        assertEquals(PlanetVoxelAuthority.TERRAIN_PREDICTED,
                brick.authority(0, 3, 0));
    }

    @Test
    void mapsBrickCellsBackToExactFaceWorldCoordinates() {
        PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(
                CubeNetSurfaceTransform.Face.WEST, 2, -1, 0, 1);
        int[] minimumSeen = {Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] maximumSeen = {Integer.MIN_VALUE, Integer.MIN_VALUE};

        PlanetVoxelPredictionSampler.sample(key, 4L, -8192, -4096,
                0, 16, (worldX, worldZ) -> {
                    minimumSeen[0] = Math.min(minimumSeen[0], worldX);
                    minimumSeen[1] = Math.min(minimumSeen[1], worldZ);
                    maximumSeen[0] = Math.max(maximumSeen[0], worldX);
                    maximumSeen[1] = Math.max(maximumSeen[1], worldZ);
                    return worldY -> STONE;
                });

        assertEquals(-8254, minimumSeen[0]);
        assertEquals(-8194, maximumSeen[0]);
        assertEquals(-4030, minimumSeen[1]);
        assertEquals(-3970, maximumSeen[1]);
    }
}
