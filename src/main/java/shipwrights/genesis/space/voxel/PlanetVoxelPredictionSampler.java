package shipwrights.genesis.space.voxel;

import java.util.Objects;

/** Pure deterministic conversion from terrain columns into one predicted 3D brick. */
public final class PlanetVoxelPredictionSampler {
    private static final int MAX_VERTICAL_SAMPLES = 16;

    private PlanetVoxelPredictionSampler() {
    }

    @FunctionalInterface
    public interface ColumnSource {
        Column column(int worldX, int worldZ);
    }

    @FunctionalInterface
    public interface Column {
        long materialAt(int worldY);
    }

    public static PlanetVoxelBrick sample(PlanetVoxelBrickKey key, long revision,
                                          int faceCenterX, int faceCenterZ,
                                          int minimumBuildHeight,
                                          int maximumBuildHeight,
                                          ColumnSource source) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(source, "source");
        if (maximumBuildHeight <= minimumBuildHeight) {
            throw new IllegalArgumentException("invalid build-height range");
        }

        PlanetVoxelBrickBuilder output = new PlanetVoxelBrickBuilder(key, revision);
        int cellSize = key.cellSize();
        for (int z = 0; z < PlanetVoxelBrick.EDGE; z++) {
            int worldZ = Math.toIntExact((long) faceCenterZ + key.minV()
                    + (long) z * cellSize + cellSize / 2L);
            for (int x = 0; x < PlanetVoxelBrick.EDGE; x++) {
                int worldX = Math.toIntExact((long) faceCenterX + key.minU()
                        + (long) x * cellSize + cellSize / 2L);
                Column column = Objects.requireNonNull(source.column(worldX, worldZ),
                        "column source returned null");
                for (int y = 0; y < PlanetVoxelBrick.EDGE; y++) {
                    long cellMinimum = key.minY() + (long) y * cellSize;
                    long cellMaximum = cellMinimum + cellSize;
                    int sampleMinimum = (int) Math.max(cellMinimum, minimumBuildHeight);
                    int sampleMaximum = (int) Math.min(cellMaximum, maximumBuildHeight);
                    if (sampleMinimum >= sampleMaximum) {
                        output.setAir(x, y, z, PlanetVoxelAuthority.TERRAIN_PREDICTED);
                        continue;
                    }
                    Sample sample = sampleCell(column, sampleMinimum, sampleMaximum);
                    output.set(x, y, z, sample.material(), sample.coverage(),
                            PlanetVoxelAuthority.TERRAIN_PREDICTED);
                }
            }
        }
        return output.build();
    }

    private static Sample sampleCell(Column column, int minimumY, int maximumY) {
        int height = maximumY - minimumY;
        int sampleCount = Math.min(MAX_VERTICAL_SAMPLES, height);
        long[] materials = new long[sampleCount];
        int[] counts = new int[sampleCount];
        int unique = 0;
        int occupied = 0;
        long representative = PlanetVoxelMaterial.AIR;
        long representativeScore = Long.MIN_VALUE;

        for (int sample = 0; sample < sampleCount; sample++) {
            int worldY = minimumY + (int) (((sample * 2L + 1L) * height)
                    / (sampleCount * 2L));
            long material = column.materialAt(worldY);
            if (PlanetVoxelMaterial.isAir(material)) continue;
            occupied++;
            int slot = -1;
            for (int index = 0; index < unique; index++) {
                if (materials[index] == material) {
                    slot = index;
                    break;
                }
            }
            if (slot < 0) {
                slot = unique++;
                materials[slot] = material;
            }
            int count = ++counts[slot];
            long score = count * 1_000_000L
                    + (long) PlanetVoxelMaterial.reductionPriority(material) * 10L
                    + sample;
            if (score >= representativeScore) {
                representativeScore = score;
                representative = material;
            }
        }

        int coverage = occupied == 0 ? 0
                : Math.max(1, Math.min(255, (occupied * 255 + sampleCount / 2) / sampleCount));
        return new Sample(representative, coverage);
    }

    private record Sample(long material, int coverage) {
    }
}
