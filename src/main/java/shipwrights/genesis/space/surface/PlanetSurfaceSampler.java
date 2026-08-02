package shipwrights.genesis.space.surface;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.MapColor;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

/**
 * Builds low-detail chunks for a planet directly from its flat Minecraft world.
 *
 * <p>This is not a screenshot and it is not a biome-colour painting. Every LOD
 * cell asks the planet's real chunk generator for the corresponding vertical
 * block column, finds its visible top block, stores that block material's map
 * colour, and stores its actual Y coordinate. The client uses these six flat
 * cube-net regions as the high-altitude terrain field and as the displaced
 * faces of the orbital cube.</p>
 *
 * <p>The generator is queried without loading chunks. That makes the global
 * planet LOD finite and safe, but also means player-built structures are not part of
 * this baseline pass yet. Loaded-world overlays can be layered on later without
 * changing the coordinate system or renderer.</p>
 */
public final class PlanetSurfaceSampler {
    /** Samples along one edge of each face. One sample is one coarse LOD column. */
    public static final int RESOLUTION = 64;

    private static final long DEADLINE_NANOS = 90_000_000_000L; // 90 s
    private static final int DEADLINE_CHECK_INTERVAL = 128;
    private static final double CPU_SHARE = 0.15;
    private static final long WORK_SLICE_NANOS = 2_000_000L; // 2 ms

    private PlanetSurfaceSampler() {
    }

    /** A whole six-face planet sample, run off the server thread. */
    public static final class Job {
        private final ResourceKey<Level> dimension;
        private final LevelHeightAccessor heightAccessor;
        private final int blockSpan;
        private final double texelStep;
        private final int seaLevel;
        private final int minBuildHeight;
        private final int maxBuildHeight;
        private final RandomState randomState;
        private final ChunkGenerator generator;
        private final BiomeSource biomeSource;
        private final Climate.Sampler climateSampler;

        /**
         * @param blockSpan width in planet blocks of one cube-net face. This must
         *                  match the atmosphere transform's celestial size x16.
         */
        public Job(ServerLevel level, int blockSpan) {
            if (blockSpan <= 0) {
                throw new IllegalArgumentException("blockSpan must be positive");
            }
            this.dimension = level.dimension();
            this.heightAccessor = level;
            this.blockSpan = blockSpan;
            this.texelStep = blockSpan / (double) RESOLUTION;
            this.minBuildHeight = level.getMinBuildHeight();
            this.maxBuildHeight = level.getMaxBuildHeight();
            ServerChunkCache cache = level.getChunkSource();
            this.randomState = cache.randomState();
            this.generator = cache.getGenerator();
            this.biomeSource = this.generator.getBiomeSource();
            this.climateSampler = this.randomState.sampler();
            this.seaLevel = this.generator.getSeaLevel();
        }

        public ResourceKey<Level> dimension() {
            return dimension;
        }

        /** Samples all six flat regions into block-derived LOD columns. */
        public PlanetSurfaceData sample() {
            try {
                long deadline = System.nanoTime() + DEADLINE_NANOS;
                int faceTexels = PlanetSurfaceData.faceLength();
                int texels = PlanetSurfaceData.expectedLength();
                int[] colours = new int[texels];
                short[] heights = new short[texels];
                int minHeight = Integer.MAX_VALUE;
                int maxHeight = Integer.MIN_VALUE;

                long sliceEnd = System.nanoTime() + WORK_SLICE_NANOS;
                CubeNetSurfaceTransform.Face[] faces = CubeNetSurfaceTransform.Face.values();
                for (int index = 0; index < texels; index++) {
                    if (index % DEADLINE_CHECK_INTERVAL == 0 && System.nanoTime() > deadline) {
                        CubeNetSurfaceTransform.Face face = faces[Math.min(faces.length - 1, index / faceTexels)];
                        GenesisMod.LOGGER.warn(
                                "[SURFACE] {} is generating too slowly to build world LOD ({}%, on {} after {}s); keeping its painted fallback",
                                dimension.location(), index * 100 / texels, face,
                                DEADLINE_NANOS / 1_000_000_000L);
                        return null;
                    }

                    while (shipwrights.genesis.teleportation.ArrivalGate.crossingInProgress()) {
                        Thread.sleep(50L);
                        if (System.nanoTime() > deadline) {
                            GenesisMod.LOGGER.info("[SURFACE] {} world LOD abandoned; crossings kept it waiting",
                                    dimension.location());
                            return null;
                        }
                    }

                    long now = System.nanoTime();
                    if (now >= sliceEnd) {
                        long worked = WORK_SLICE_NANOS + (now - sliceEnd);
                        long rest = (long) (worked * (1.0 / CPU_SHARE - 1.0));
                        Thread.sleep(rest / 1_000_000L, (int) (rest % 1_000_000L));
                        sliceEnd = System.nanoTime() + WORK_SLICE_NANOS;
                    }

                    int faceIndex = index / faceTexels;
                    int localIndex = index % faceTexels;
                    int texelX = localIndex % RESOLUTION;
                    int texelY = localIndex / RESOLUTION;
                    CubeNetSurfaceTransform.Face face = faces[faceIndex];

                    double faceCenterX = face.netColumnOffset() * (double) blockSpan;
                    double faceCenterZ = face.netRowOffset() * (double) blockSpan;
                    int worldX = Mth.floor(faceCenterX - blockSpan * 0.5
                            + (texelX + 0.5) * texelStep);
                    int worldZ = Mth.floor(faceCenterZ - blockSpan * 0.5
                            + (texelY + 0.5) * texelStep);

                    // getBaseColumn gives the actual generated block-state column
                    // without loading or retaining a Minecraft chunk.
                    NoiseColumn column = generator.getBaseColumn(worldX, worldZ, heightAccessor, randomState);
                    int surfaceY = findSurfaceY(column);
                    BlockState surfaceState = column.getBlock(surfaceY);
                    Holder<Biome> biome = biomeSource.getNoiseBiome(
                            QuartPos.fromBlock(worldX), QuartPos.fromBlock(surfaceY),
                            QuartPos.fromBlock(worldZ), climateSampler);

                    colours[index] = colorFor(surfaceState, biome, surfaceY, seaLevel);
                    heights[index] = (short) Mth.clamp(surfaceY, Short.MIN_VALUE, Short.MAX_VALUE);
                    minHeight = Math.min(minHeight, surfaceY);
                    maxHeight = Math.max(maxHeight, surfaceY);
                }

                shadeLodCells(colours, heights);
                return new PlanetSurfaceData(colours, heights, minHeight, maxHeight, seaLevel);
            } catch (Throwable error) {
                GenesisMod.LOGGER.warn("[SURFACE] could not build world LOD for {}", dimension.location(), error);
                return null;
            }
        }

        private int findSurfaceY(NoiseColumn column) {
            for (int y = maxBuildHeight - 1; y >= minBuildHeight; y--) {
                if (!column.getBlock(y).isAir()) {
                    return y;
                }
            }
            return minBuildHeight;
        }
    }

    private static int colorFor(BlockState state, Holder<Biome> biome, int height, int seaLevel) {
        MapColor mapColor = state.getBlock().defaultMapColor();
        int rgb;
        if (!state.getFluidState().isEmpty() || mapColor == MapColor.WATER) {
            rgb = biome.value().getWaterColor();
        } else if (mapColor == MapColor.GRASS) {
            rgb = biome.value().getGrassColor(0.0, 0.0);
        } else if (mapColor == MapColor.PLANT) {
            rgb = biome.value().getFoliageColor();
        } else if (mapColor != MapColor.NONE && mapColor.col != 0) {
            rgb = mapColor.col;
        } else {
            rgb = biome.value().getGrassColor(0.0, 0.0);
        }

        // A small altitude response keeps the coarse LOD readable without
        // replacing the underlying block colour with an invented biome palette.
        float altitude = Mth.clamp((height - seaLevel) / 160.0f, -0.25f, 1.0f);
        float factor = altitude >= 0.0f ? 1.0f + altitude * 0.16f : 1.0f + altitude * 0.10f;
        return 0xFF000000 | scaleRgb(rgb, factor);
    }

    /** Adds directional relief shading so LOD cells read as terrain, not a flat map. */
    private static void shadeLodCells(int[] colours, short[] heights) {
        int resolution = RESOLUTION;
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            for (int y = 0; y < resolution; y++) {
                int y0 = Math.max(0, y - 1);
                int y1 = Math.min(resolution - 1, y + 1);
                for (int x = 0; x < resolution; x++) {
                    int x0 = Math.max(0, x - 1);
                    int x1 = Math.min(resolution - 1, x + 1);
                    int index = PlanetSurfaceData.index(face, x, y);
                    double dx = heights[PlanetSurfaceData.index(face, x1, y)]
                            - heights[PlanetSurfaceData.index(face, x0, y)];
                    double dy = heights[PlanetSurfaceData.index(face, x, y1)]
                            - heights[PlanetSurfaceData.index(face, x, y0)];
                    float light = (float) Mth.clamp(1.0 + (-dx - dy) * 0.012, 0.72, 1.22);
                    colours[index] = 0xFF000000 | scaleRgb(colours[index], light);
                }
            }
        }
    }

    private static int scaleRgb(int argbOrRgb, float factor) {
        int r = Mth.clamp((int) (((argbOrRgb >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp((int) (((argbOrRgb >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp((int) ((argbOrRgb & 0xFF) * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }
}
