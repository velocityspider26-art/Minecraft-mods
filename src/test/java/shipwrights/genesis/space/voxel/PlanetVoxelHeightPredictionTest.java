package shipwrights.genesis.space.voxel;

import org.junit.jupiter.api.Test;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cheap coarse-LOD prediction path.
 *
 * <p>A quarter of a million full noise columns is minutes of CPU before the
 * planet looks like anything, so coarse bricks are filled from a terrain height
 * instead. These tests pin the two properties that makes acceptable: it must be
 * used only where a voxel is too big to show the difference, and the terrain it
 * produces must still have real vertical shape.</p>
 */
class PlanetVoxelHeightPredictionTest {

    private static final long STONE = PlanetVoxelMaterial.pack(1, 0x808080, 15, 0,
            PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_NATURAL);
    private static final long GRASS = PlanetVoxelMaterial.pack(2, 0x55A040, 15, 0,
            PlanetVoxelMaterial.FLAG_OPAQUE | PlanetVoxelMaterial.FLAG_NATURAL);
    private static final long WATER = PlanetVoxelMaterial.pack(3, 0x3050B0, 15, 0,
            PlanetVoxelMaterial.FLAG_LIQUID);

    /** A source that records how it was asked for data. */
    private static final class CountingSource
            implements PlanetVoxelPredictionSampler.ColumnSource {
        final AtomicInteger columnCalls = new AtomicInteger();
        final AtomicInteger heightCalls = new AtomicInteger();
        final java.util.function.IntBinaryOperator heightAt;

        CountingSource(java.util.function.IntBinaryOperator heightAt) {
            this.heightAt = heightAt;
        }

        @Override
        public PlanetVoxelPredictionSampler.Column column(int worldX, int worldZ) {
            columnCalls.incrementAndGet();
            int surface = heightAt.applyAsInt(worldX, worldZ);
            return worldY -> worldY < surface ? STONE : PlanetVoxelMaterial.AIR;
        }

        @Override
        public PlanetVoxelPredictionSampler.HeightColumn heightColumn(int worldX, int worldZ) {
            heightCalls.incrementAndGet();
            int surface = heightAt.applyAsInt(worldX, worldZ);
            return new PlanetVoxelPredictionSampler.HeightColumn(surface,
                    surface < 63 ? 63 : Integer.MIN_VALUE, GRASS, STONE,
                    surface < 63 ? WATER : PlanetVoxelMaterial.AIR);
        }
    }

    private static PlanetVoxelBrick sample(PlanetVoxelBrickKey key, CountingSource source) {
        return PlanetVoxelPredictionSampler.sample(key, 1L, 0, 0, -64, 320, source);
    }

    @Test
    void coarseBricksUseTheHeightPathAndFineBricksDoNot() {
        CountingSource coarse = new CountingSource((x, z) -> 80);
        sample(new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.UP, 6, 0, 0, 0), coarse);
        assertEquals(0, coarse.columnCalls.get(),
                "a 64-block voxel must not cost a full noise column");
        assertEquals(256, coarse.heightCalls.get());

        CountingSource fine = new CountingSource((x, z) -> 80);
        sample(new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.UP, 0, 0, 0, 0), fine);
        assertEquals(256, fine.columnCalls.get(),
                "block-scale bricks must still resolve real columns");
    }

    @Test
    void fallsBackToColumnsWhenTheSourceHasNoCheapPath() {
        AtomicInteger columns = new AtomicInteger();
        PlanetVoxelPredictionSampler.ColumnSource legacy = (x, z) -> {
            columns.incrementAndGet();
            return worldY -> worldY < 70 ? STONE : PlanetVoxelMaterial.AIR;
        };
        PlanetVoxelBrick brick = PlanetVoxelPredictionSampler.sample(
                new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.UP, 6, 0, 0, 0),
                1L, 0, 0, -64, 320, legacy);
        assertEquals(256, columns.get());
        assertTrue(brick.nonAirCount() > 0);
    }

    @Test
    void heightFilledTerrainKeepsItsVerticalShape() {
        // A ramp across the brick, entirely above sea level so ocean fill does
        // not level the comparison: the number of occupied voxels per column
        // must follow the ramp, or mountains flatten into a plate.
        CountingSource ramp = new CountingSource((x, z) -> 70 + Math.floorDiv(x, 2));
        PlanetVoxelBrick brick = sample(
                new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.UP, 3, 0, 0, 0), ramp);

        int lowColumn = occupied(brick, 0, 0);
        int highColumn = occupied(brick, 15, 0);
        assertTrue(highColumn > lowColumn,
                "expected the tall side to be taller: " + lowColumn + " vs " + highColumn);
    }

    @Test
    void partiallyFilledCellsReportPartialCoverage() {
        // Surface at y=68 with 8-block voxels: the cell spanning 64..72 is half
        // full, and must say so rather than rounding to solid or empty. Above
        // sea level, so nothing tops the cell up with water.
        CountingSource half = new CountingSource((x, z) -> 68);
        PlanetVoxelBrick brick = sample(
                new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.UP, 3, 0, 0, 0), half);
        int coverage = brick.coverage(0, 8, 0);
        assertTrue(coverage > 100 && coverage < 160,
                "half-filled cell reported coverage " + coverage);
    }

    @Test
    void oceanFillsTheVolumeBetweenSeabedAndSeaLevel() {
        CountingSource seabed = new CountingSource((x, z) -> 20);
        PlanetVoxelBrick brick = sample(
                new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.UP, 3, 0, 0, 0), seabed);
        // Sea level 63, terrain 20: a cell between them must hold water.
        int cellY = (40 - 0) / 8;
        long material = brick.material(0, cellY, 0);
        assertEquals(WATER, material, "expected water between the seabed and sea level");
    }

    @Test
    void everythingAboveTheSurfaceIsAir() {
        CountingSource flat = new CountingSource((x, z) -> 70);
        PlanetVoxelBrick brick = sample(
                new PlanetVoxelBrickKey(CubeNetSurfaceTransform.Face.UP, 3, 0, 0, 0), flat);
        for (int y = 0; y < PlanetVoxelBrick.EDGE; y++) {
            long cellBottom = y * 8L;
            if (cellBottom < 70) continue;
            assertTrue(PlanetVoxelMaterial.isAir(brick.material(0, y, 0)),
                    "cell starting at y=" + cellBottom + " is above terrain but not air");
        }
    }

    private static int occupied(PlanetVoxelBrick brick, int x, int z) {
        int count = 0;
        for (int y = 0; y < PlanetVoxelBrick.EDGE; y++) {
            if (!PlanetVoxelMaterial.isAir(brick.material(x, y, z))) count++;
        }
        return count;
    }
}
