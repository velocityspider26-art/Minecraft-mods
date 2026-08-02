package shipwrights.genesis.space.voxel;

import java.util.Objects;

/** Pure deterministic conversion from terrain columns into one predicted 3D brick. */
public final class PlanetVoxelPredictionSampler {
    private static final int MAX_VERTICAL_SAMPLES = 16;

    private PlanetVoxelPredictionSampler() {
    }

    /**
     * Coarsest LOD that still samples full block columns.
     *
     * <p>At LOD 3 one voxel is an 8-block cube and at LOD 6 it is 64 blocks on a
     * side. Nothing a full column resolves that a height field does not — caves,
     * overhangs, individual blocks — survives being averaged into a voxel that
     * large, so above this level the cheap path costs nothing visually and the
     * expensive one costs a noise column per sample.</p>
     */
    private static final int MAX_EXACT_COLUMN_LOD = 2;

    public interface ColumnSource {
        Column column(int worldX, int worldZ);

        /**
         * Terrain summary for a column, if the generator can produce one without
         * evaluating the whole column. Returning null falls back to
         * {@link #column}, so a source that has no cheap path still works.
         */
        default HeightColumn heightColumn(int worldX, int worldZ) {
            return null;
        }
    }

    @FunctionalInterface
    public interface Column {
        long materialAt(int worldY);
    }

    /**
     * What a column looks like without evaluating it block by block: where the
     * ground stops, what it is made of, and where any water sits.
     *
     * @param surfaceY      world Y of the first non-solid block above the terrain
     * @param seaLevel      world Y water fills to, or {@link Integer#MIN_VALUE} for none
     * @param surface       material of the topmost solid block
     * @param subsurface    material below the surface layer
     * @param fluid         material filling {@code surfaceY..seaLevel}, or air
     */
    public record HeightColumn(int surfaceY, int seaLevel,
                               long surface, long subsurface, long fluid) {
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
        boolean coarse = key.lod() > MAX_EXACT_COLUMN_LOD;
        for (int z = 0; z < PlanetVoxelBrick.EDGE; z++) {
            int worldZ = Math.toIntExact((long) faceCenterZ + key.minV()
                    + (long) z * cellSize + cellSize / 2L);
            for (int x = 0; x < PlanetVoxelBrick.EDGE; x++) {
                int worldX = Math.toIntExact((long) faceCenterX + key.minU()
                        + (long) x * cellSize + cellSize / 2L);

                HeightColumn heights = coarse ? source.heightColumn(worldX, worldZ) : null;
                if (heights != null) {
                    fillFromHeights(output, key, x, z, cellSize,
                            minimumBuildHeight, maximumBuildHeight, heights);
                    continue;
                }

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

    /**
     * Fills one voxel column from a terrain height instead of a block column.
     *
     * <p>Coverage is the fraction of each cell the ground actually fills, so a
     * mountainside crossing a cell diagonally still reads as partially solid
     * rather than snapping to full or empty — which is what keeps a coarse
     * planet looking like terrain instead of a staircase.</p>
     */
    private static void fillFromHeights(PlanetVoxelBrickBuilder output, PlanetVoxelBrickKey key,
                                        int x, int z, int cellSize,
                                        int minimumBuildHeight, int maximumBuildHeight,
                                        HeightColumn heights) {
        int groundTop = Math.min(heights.surfaceY(), maximumBuildHeight);
        boolean hasWater = heights.seaLevel() != Integer.MIN_VALUE
                && heights.seaLevel() > groundTop
                && !PlanetVoxelMaterial.isAir(heights.fluid());
        int waterTop = hasWater ? Math.min(heights.seaLevel(), maximumBuildHeight) : groundTop;

        for (int y = 0; y < PlanetVoxelBrick.EDGE; y++) {
            long cellMinimum = key.minY() + (long) y * cellSize;
            long cellMaximum = cellMinimum + cellSize;
            int low = (int) Math.max(cellMinimum, minimumBuildHeight);
            int high = (int) Math.min(cellMaximum, maximumBuildHeight);
            if (low >= high) {
                output.setAir(x, y, z, PlanetVoxelAuthority.TERRAIN_PREDICTED);
                continue;
            }

            int solid = Math.max(0, Math.min(high, groundTop) - low);
            int water = hasWater ? Math.max(0, Math.min(high, waterTop) - Math.max(low, groundTop)) : 0;
            int filled = solid + water;
            if (filled <= 0) {
                output.setAir(x, y, z, PlanetVoxelAuthority.TERRAIN_PREDICTED);
                continue;
            }

            // Solid wins the cell's identity even when water fills more of it:
            // a shoreline reads as land with water against it, not as ocean.
            long material;
            if (solid <= 0) {
                material = heights.fluid();
            } else if (Math.min(high, groundTop) >= groundTop && low < groundTop) {
                // This cell contains the surface, so it wears the surface block.
                material = heights.surface();
            } else {
                material = heights.subsurface();
            }
            int coverage = Math.max(1, Math.min(255, (filled * 255 + (high - low) / 2) / (high - low)));
            output.set(x, y, z, material, coverage, PlanetVoxelAuthority.TERRAIN_PREDICTED);
        }
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
