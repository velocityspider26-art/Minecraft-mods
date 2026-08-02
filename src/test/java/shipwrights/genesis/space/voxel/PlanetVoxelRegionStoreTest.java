package shipwrights.genesis.space.voxel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlanetVoxelRegionStoreTest {
    private static final long FIRST_OFFSET_ENTRY = 16L;
    private static final long FIRST_LENGTH_ENTRY = 16L + 4096L * Long.BYTES;

    @TempDir
    Path directory;

    @Test
    void restoresCoarseBricksBeforeFineBricksWhenBounded() throws Exception {
        PlanetVoxelBrick fine = brick(CubeNetSurfaceTransform.Face.UP, 0, 0, 0, 0, 1L);
        PlanetVoxelBrick coarse = brick(CubeNetSurfaceTransform.Face.SOUTH, 5, 0, 0, 0, 2L);

        try (PlanetVoxelRegionStore store = new PlanetVoxelRegionStore(directory)) {
            store.write(fine);
            store.write(coarse);
            store.flush();
            PlanetVoxelRegionStore.LoadResult result = store.loadAll(1);

            assertEquals(1, result.bricks().size());
            assertEquals(coarse.key(), result.bricks().getFirst().key());
            assertEquals(0, result.corruptRecords());
        }
    }

    @Test
    void isolatesCorruptRecordAndRestoresNeighbor() throws Exception {
        PlanetVoxelBrick corrupt = brick(CubeNetSurfaceTransform.Face.WEST, 2, 0, 0, 0, 11L);
        PlanetVoxelBrick intact = brick(CubeNetSurfaceTransform.Face.WEST, 2, 1, 0, 0, 12L);
        Path region = directory.resolve("west").resolve("lod2").resolve("0.0.0.pvr");

        try (PlanetVoxelRegionStore store = new PlanetVoxelRegionStore(directory)) {
            store.write(corrupt);
            store.write(intact);
            store.flush();
        }

        try (RandomAccessFile file = new RandomAccessFile(region.toFile(), "rw")) {
            file.seek(FIRST_OFFSET_ENTRY);
            long recordOffset = file.readLong();
            file.seek(FIRST_LENGTH_ENTRY);
            int recordLength = file.readInt();
            assertTrue(recordOffset > 0L && recordLength > 8);
            long corruptOffset = recordOffset + recordLength - 1L;
            file.seek(corruptOffset);
            int stored = file.readUnsignedByte();
            file.seek(corruptOffset);
            file.writeByte(stored ^ 0x5A);
        }

        try (PlanetVoxelRegionStore store = new PlanetVoxelRegionStore(directory)) {
            PlanetVoxelRegionStore.LoadResult result = store.loadAll(16);
            Map<PlanetVoxelBrickKey, PlanetVoxelBrick> restored = result.bricks().stream()
                    .collect(Collectors.toMap(PlanetVoxelBrick::key, Function.identity()));

            assertEquals(1, result.corruptRecords());
            assertEquals(intact, restored.get(intact.key()));
            assertTrue(!restored.containsKey(corrupt.key()));
        }
    }

    @Test
    void roundTripsPerVoxelAuthorityIncludingExactAir() throws Exception {
        PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(
                CubeNetSurfaceTransform.Face.NORTH, 3, -2, 1, 4);
        PlanetVoxelBrickBuilder builder = new PlanetVoxelBrickBuilder(key, 77L);
        long material = PlanetVoxelMaterial.pack(9, 0x447755, 13, 1,
                PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_NATURAL);
        builder.set(2, 3, 4, material, 181, PlanetVoxelAuthority.TERRAIN_PREDICTED);
        builder.setAir(5, 6, 7, PlanetVoxelAuthority.PLAYER_MODIFIED);
        PlanetVoxelBrick expected = builder.build();

        try (PlanetVoxelRegionStore store = new PlanetVoxelRegionStore(directory)) {
            store.write(expected);
            store.flush();
            PlanetVoxelBrick actual = store.read(key).orElseThrow();

            assertEquals(expected, actual);
            assertEquals(PlanetVoxelAuthority.TERRAIN_PREDICTED,
                    actual.authority(2, 3, 4));
            assertEquals(PlanetVoxelAuthority.PLAYER_MODIFIED,
                    actual.authority(5, 6, 7));
            assertEquals(PlanetVoxelMaterial.AIR, actual.material(5, 6, 7));
        }
    }

    private static PlanetVoxelBrick brick(CubeNetSurfaceTransform.Face face, int lod,
                                           int u, int y, int v, long revision) {
        PlanetVoxelBrickBuilder builder = new PlanetVoxelBrickBuilder(
                new PlanetVoxelBrickKey(face, lod, u, y, v), revision);
        long material = PlanetVoxelMaterial.pack(7, 0xAA7744, 15, 3,
                PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_STRUCTURE);
        builder.set(0, 0, 0, material, 255);
        builder.set(15, 15, 15, material, 192);
        return builder.build();
    }
}
