package shipwrights.genesis.space.voxel;

import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public final class PlanetVoxelEngineSelfTest {
    public static void main(String[] args) throws Exception {
        materialRoundTrip();
        brickPaletteRoundTrip();
        structurePreservingReduction();
        greedyMeshing();
        randomReductionStress();
        regionPersistence();
        pyramidBatchUpdate();
        dirtyQueueDeduplication();
        lruBudget();
        System.out.println("PlanetVoxelEngineSelfTest: PASS");
    }

    private static void materialRoundTrip() {
        long material = PlanetVoxelMaterial.pack(123456, 0x42A0E0, 15, 7,
                PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_STRUCTURE);
        check(PlanetVoxelMaterial.blockStateId(material) == 123456, "state id");
        check(PlanetVoxelMaterial.rgb(material) == 0x42A0E0, "rgb");
        check(PlanetVoxelMaterial.skyLight(material) == 15, "sky");
        check(PlanetVoxelMaterial.blockLight(material) == 7, "block light");
        check(PlanetVoxelMaterial.hasFlag(material, PlanetVoxelMaterial.FLAG_STRUCTURE), "structure flag");
    }

    private static void brickPaletteRoundTrip() {
        PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.UP, 0, -3, 4, 8);
        PlanetVoxelBrickBuilder builder = new PlanetVoxelBrickBuilder(key, 10);
        long stone = PlanetVoxelMaterial.pack(1, 0x777777, 15, 0,
                PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_NATURAL);
        long roof = PlanetVoxelMaterial.pack(2, 0xAA2222, 15, 0,
                PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_STRUCTURE);
        builder.set(0, 0, 0, stone).set(15, 15, 15, roof, 128);
        PlanetVoxelBrick brick = builder.build();
        check(brick.material(0, 0, 0) == stone, "stone lookup");
        check(brick.material(15, 15, 15) == roof, "roof lookup");
        check(brick.coverage(15, 15, 15) == 128, "coverage lookup");
        check(brick.paletteSize() == 3, "palette compression");
    }


    private static void greedyMeshing() {
        PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.UP, 0, 0, 0, 0);
        PlanetVoxelBrickBuilder builder = new PlanetVoxelBrickBuilder(key, 1);
        long stone = PlanetVoxelMaterial.pack(1, 0x777777, 15, 0,
                PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_NATURAL);
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) builder.set(x, 0, z, stone);
        PlanetVoxelGreedyMesher.Mesh mesh = PlanetVoxelGreedyMesher.mesh(builder.build(), null);
        check(mesh.quadCount() == 6, "one-block-thick 16x16 slab greedily merges to six quads, got " + mesh.quadCount());
        int area = 0;
        for (PlanetVoxelGreedyMesher.Quad quad : mesh.solid()) area += quad.area();
        check(area == 16 * 16 * 2 + 16 * 4, "slab exposed area");
    }

    private static void structurePreservingReduction() {
        PlanetVoxelBrick[] children = new PlanetVoxelBrick[8];
        long terrain = PlanetVoxelMaterial.pack(3, 0x55AA44, 15, 0,
                PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_NATURAL);
        long structure = PlanetVoxelMaterial.pack(4, 0xAA6633, 15, 0,
                PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_STRUCTURE);
        for (int slot = 0; slot < 8; slot++) {
            PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.UP, 0,
                    slot & 1, (slot >>> 1) & 1, (slot >>> 2) & 1);
            PlanetVoxelBrickBuilder builder = new PlanetVoxelBrickBuilder(key, 1);
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) builder.set(x, y, z, terrain);
                }
            }
            children[slot] = builder.build();
        }
        PlanetVoxelBrickBuilder withHouse = new PlanetVoxelBrickBuilder(children[7].key(), 2);
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) withHouse.set(x, y, z, terrain);
            }
        }
        withHouse.set(15, 15, 15, structure);
        children[7] = withHouse.build();
        PlanetVoxelBrick parent = PlanetVoxelReducer.reduce(children, 3);
        check(PlanetVoxelMaterial.hasFlag(parent.material(15, 15, 15),
                PlanetVoxelMaterial.FLAG_STRUCTURE), "structure survives mip reduction");
    }

    private static void randomReductionStress() {
        Random random = new Random(0x5EEDC0DEL);
        long terrain = PlanetVoxelMaterial.pack(10, 0x668855, 15, 0,
                PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_NATURAL);
        long build = PlanetVoxelMaterial.pack(11, 0xCCAA88, 15, 0,
                PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_STRUCTURE);
        for (int pass = 0; pass < 100; pass++) {
            PlanetVoxelBrick[] children = new PlanetVoxelBrick[8];
            for (int slot = 0; slot < 8; slot++) {
                PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.SOUTH, 0,
                        slot & 1, (slot >>> 1) & 1, (slot >>> 2) & 1);
                PlanetVoxelBrickBuilder builder = new PlanetVoxelBrickBuilder(key, pass);
                for (int i = 0; i < 1000; i++) {
                    int x = random.nextInt(16), y = random.nextInt(16), z = random.nextInt(16);
                    builder.set(x, y, z, random.nextInt(12) == 0 ? build : terrain,
                            64 + random.nextInt(192));
                }
                children[slot] = builder.build();
            }
            PlanetVoxelBrick parent = PlanetVoxelReducer.reduce(children, pass + 1L);
            check(parent.key().lod() == 1, "parent lod");
            check(parent.paletteSize() <= 3, "parent palette bounded");
        }
    }

    private static void regionPersistence() throws Exception {
        Path directory = Files.createTempDirectory("genesis-voxel-test");
        try {
            PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.WEST, 2,
                    -18, 7, 33);
            PlanetVoxelBrickBuilder builder = new PlanetVoxelBrickBuilder(key, 99);
            long material = PlanetVoxelMaterial.pack(42, 0x123456, 12, 3,
                    PlanetVoxelMaterial.FLAG_STRUCTURE | PlanetVoxelMaterial.FLAG_OPAQUE);
            builder.set(1, 2, 3, material, 77);
            PlanetVoxelBrick expected = builder.build();
            try (PlanetVoxelRegionStore store = new PlanetVoxelRegionStore(directory)) {
                store.write(expected);
                PlanetVoxelBrick actual = store.read(key).orElseThrow();
                check(expected.equals(actual), "region read/write equality");
            }
        } finally {
            try (var walk = Files.walk(directory)) {
                for (Path path : walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }


    private static void pyramidBatchUpdate() throws Exception {
        Path directory = Files.createTempDirectory("genesis-voxel-pyramid-test");
        try (PlanetVoxelRegionStore disk = new PlanetVoxelRegionStore(directory)) {
            PlanetVoxelStore memory = new PlanetVoxelStore(16L * 1024L * 1024L);
            PlanetVoxelPyramid pyramid = new PlanetVoxelPyramid(memory, disk,
                    new java.util.concurrent.atomic.AtomicLong(100), 3);
            long material = PlanetVoxelMaterial.pack(99, 0xabcdef, 15, 0,
                    PlanetVoxelMaterial.FLAG_STRUCTURE | PlanetVoxelMaterial.FLAG_OPAQUE);
            PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.EAST, 0, 0, 0, 0);
            PlanetVoxelBrickBuilder builder = new PlanetVoxelBrickBuilder(key, 1);
            builder.set(0, 0, 0, material);
            PlanetVoxelPyramid.Update added = pyramid.applyBatch(java.util.List.of(builder.build()), java.util.List.of());
            check(added.upserts().size() == 4, "leaf plus three parents");
            check(memory.contains(key.parent().parent().parent()), "top parent resident");
            PlanetVoxelPyramid.Update removed = pyramid.applyBatch(java.util.List.of(), java.util.List.of(key));
            check(removed.removals().size() == 4, "leaf plus three parent removals");
            check(disk.read(key).isEmpty(), "leaf deleted from disk index");
        } finally {
            try (var walk = Files.walk(directory)) {
                for (Path path : walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void dirtyQueueDeduplication() {
        PlanetVoxelDirtyTracker tracker = new PlanetVoxelDirtyTracker();
        PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.UP, 0, 1, 2, 3);
        tracker.mark(key, PlanetVoxelDirtyTracker.Reason.BACKGROUND_IMPORT, 1000);
        tracker.mark(key, PlanetVoxelDirtyTracker.Reason.BLOCK_CHANGE, 1);
        check(tracker.pendingUnique() == 1, "dirty dedup");
        PlanetVoxelDirtyTracker.Task task = tracker.poll();
        check(task != null && task.reason() == PlanetVoxelDirtyTracker.Reason.BLOCK_CHANGE,
                "newest urgent task wins");
        check(tracker.poll() == null, "stale queue entries skipped");
    }

    private static void lruBudget() {
        PlanetVoxelStore store = new PlanetVoxelStore(16L * 1024L * 1024L);
        long material = PlanetVoxelMaterial.pack(8, 0x777777, 15, 0,
                PlanetVoxelMaterial.FLAG_OPAQUE);
        for (int i = 0; i < 1000; i++) {
            PlanetVoxelBrickBuilder builder = new PlanetVoxelBrickBuilder(
                    new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.NORTH, 0, i, 0, 0), i);
            builder.set(0, 0, 0, material);
            store.put(builder.build());
        }
        check(store.usedBytes() <= 16L * 1024L * 1024L, "LRU memory budget");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
