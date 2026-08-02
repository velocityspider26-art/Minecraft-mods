package shipwrights.genesis.client.lod;

import shipwrights.genesis.space.voxel.PlanetVoxelBrick;
import shipwrights.genesis.space.voxel.PlanetVoxelBrickKey;
import shipwrights.genesis.space.voxel.PlanetVoxelMaterial;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.BitSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Occupancy-based readiness check for replacing the temporary coarse Earth shell. */
public final class PlanetVoxelCoverage {
    private static final int TARGET_CELLS_PER_AXIS = 64;
    private static final int MAX_CELLS_PER_AXIS = 512;
    private static final double COMPLETE_RATIO = 0.985;

    private PlanetVoxelCoverage() {
    }

    public record Result(boolean complete, int lod, int cellsPerAxis,
                         Map<CubeNetSurfaceTransform.Face, Double> faceRatios) {
    }

    public static Result analyze(List<PlanetVoxelBrick> bricks, double faceHalfExtent) {
        if (bricks == null || bricks.isEmpty() || !Double.isFinite(faceHalfExtent)
                || faceHalfExtent <= 0.0) {
            return emptyResult();
        }

        int highestResidentLod = bricks.stream()
                .mapToInt(brick -> brick.key().lod())
                .max().orElse(-1);
        if (highestResidentLod < 0) return emptyResult();

        int preferredLod = 0;
        while (preferredLod < PlanetVoxelBrickKey.MAX_LOD
                && Math.ceil((faceHalfExtent * 2.0) / (1 << preferredLod))
                > TARGET_CELLS_PER_AXIS) {
            preferredLod++;
        }
        int lod = Math.min(preferredLod, highestResidentLod);
        int cellSize = 1 << lod;
        int minimumCell = (int) Math.floor(-faceHalfExtent / cellSize);
        int maximumCellExclusive = (int) Math.ceil(faceHalfExtent / cellSize);
        int cellsPerAxis = maximumCellExclusive - minimumCell;
        if (cellsPerAxis <= 0 || cellsPerAxis > MAX_CELLS_PER_AXIS) {
            return emptyResult();
        }

        int cellsPerFace = Math.multiplyExact(cellsPerAxis, cellsPerAxis);
        EnumMap<CubeNetSurfaceTransform.Face, BitSet> occupied =
                new EnumMap<>(CubeNetSurfaceTransform.Face.class);
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            occupied.put(face, new BitSet(cellsPerFace));
        }

        for (PlanetVoxelBrick brick : bricks) {
            PlanetVoxelBrickKey key = brick.key();
            if (key.lod() != lod || brick.isEmpty()) continue;
            BitSet face = occupied.get(key.face());
            int baseU = key.brickU() * PlanetVoxelBrick.EDGE - minimumCell;
            int baseV = key.brickV() * PlanetVoxelBrick.EDGE - minimumCell;
            for (int z = 0; z < PlanetVoxelBrick.EDGE; z++) {
                int gridV = baseV + z;
                if (gridV < 0 || gridV >= cellsPerAxis) continue;
                for (int x = 0; x < PlanetVoxelBrick.EDGE; x++) {
                    int gridU = baseU + x;
                    if (gridU < 0 || gridU >= cellsPerAxis) continue;
                    if (hasOccupiedColumn(brick, x, z)) {
                        face.set(gridV * cellsPerAxis + gridU);
                    }
                }
            }
        }

        EnumMap<CubeNetSurfaceTransform.Face, Double> ratios =
                new EnumMap<>(CubeNetSurfaceTransform.Face.class);
        boolean complete = true;
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            double ratio = occupied.get(face).cardinality() / (double) cellsPerFace;
            ratios.put(face, ratio);
            complete &= ratio >= COMPLETE_RATIO;
        }
        return new Result(complete, lod, cellsPerAxis, Map.copyOf(ratios));
    }

    private static boolean hasOccupiedColumn(PlanetVoxelBrick brick, int x, int z) {
        for (int y = 0; y < PlanetVoxelBrick.EDGE; y++) {
            if (!PlanetVoxelMaterial.isAir(brick.material(x, y, z))) return true;
        }
        return false;
    }

    private static Result emptyResult() {
        EnumMap<CubeNetSurfaceTransform.Face, Double> ratios =
                new EnumMap<>(CubeNetSurfaceTransform.Face.class);
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            ratios.put(face, 0.0);
        }
        return new Result(false, -1, 0, Map.copyOf(ratios));
    }
}
