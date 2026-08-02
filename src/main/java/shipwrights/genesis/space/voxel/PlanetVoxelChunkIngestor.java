package shipwrights.genesis.space.voxel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.MapColor;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.ArrayList;
import java.util.List;

/** Converts one already-loaded Minecraft chunk into exact LOD0 voxel bricks. */
public final class PlanetVoxelChunkIngestor {
    private PlanetVoxelChunkIngestor() {
    }

    public record Capture(CubeNetSurfaceTransform.Face face,
                          int chunkX, int chunkZ, int brickU, int brickV,
                          List<PlanetVoxelBrick> bricks,
                          int[] emptySectionYs) {
    }

    public static Capture capture(ServerLevel level, LevelChunk chunk,
                                  CubeNetSurfaceTransform transform, long revisionBase) {
        return capture(level, chunk, transform, revisionBase,
                PlanetVoxelAuthority.GENERATED_EXACT);
    }

    /** Must run on the server thread; it never loads or generates another chunk. */
    public static Capture capture(ServerLevel level, LevelChunk chunk,
                                  CubeNetSurfaceTransform transform, long revisionBase,
                                  PlanetVoxelAuthority authority) {
        if (authority != PlanetVoxelAuthority.GENERATED_EXACT
                && authority != PlanetVoxelAuthority.PLAYER_MODIFIED) {
            throw new IllegalArgumentException("chunk capture requires exact authority");
        }
        int originX = chunk.getPos().getMinBlockX();
        int originZ = chunk.getPos().getMinBlockZ();
        CubeNetSurfaceTransform.Face face = transform.faceContaining(originX + 8.0, originZ + 8.0);
        if (face == null) {
            return new Capture(CubeNetSurfaceTransform.Face.UP, chunk.getPos().x, chunk.getPos().z,
                    0, 0, List.of(), new int[0]);
        }

        int centerX = exactInt(transform.faceCenterX(face), "faceCenterX");
        int centerZ = exactInt(transform.faceCenterZ(face), "faceCenterZ");
        int brickU = Math.floorDiv(originX - centerX, PlanetVoxelBrick.EDGE);
        int brickV = Math.floorDiv(originZ - centerZ, PlanetVoxelBrick.EDGE);
        LevelChunkSection[] sections = chunk.getSections();
        List<PlanetVoxelBrick> bricks = new ArrayList<>(sections.length);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            int sectionY = level.getSectionYFromSectionIndex(sectionIndex);
            PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(face, 0, brickU, sectionY, brickV);
            PlanetVoxelBrickBuilder builder = new PlanetVoxelBrickBuilder(
                    key, revisionBase + sectionIndex);
            if (section == null || section.hasOnlyAir()) {
                // Exact air is still authoritative: it prevents lower-priority
                // predicted terrain from reappearing inside a cleared volume.
                bricks.add(builder.fillAuthority(authority).build());
                continue;
            }
            int worldMinY = sectionY << 4;
            for (int y = 0; y < 16; y++) {
                int worldY = worldMinY + y;
                for (int z = 0; z < 16; z++) {
                    int worldZ = originZ + z;
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.isAir()) {
                            builder.setAir(x, y, z, authority);
                            continue;
                        }
                        int worldX = originX + x;
                        pos.set(worldX, worldY, worldZ);
                        builder.set(x, y, z, material(level, pos, state), 255, authority);
                    }
                }
            }
            bricks.add(builder.build());
        }
        return new Capture(face, chunk.getPos().x, chunk.getPos().z, brickU, brickV,
                List.copyOf(bricks), new int[0]);
    }

    private static long material(ServerLevel level, BlockPos pos, BlockState state) {
        MapColor mapColor = state.getMapColor(level, pos);
        Holder<Biome> biome = level.getBiome(pos);
        int rgb;
        if (!state.getFluidState().isEmpty() || mapColor == MapColor.WATER) {
            rgb = biome.value().getWaterColor();
        } else if (mapColor == MapColor.GRASS) {
            rgb = biome.value().getGrassColor(pos.getX(), pos.getZ());
        } else if (mapColor == MapColor.PLANT) {
            rgb = biome.value().getFoliageColor();
        } else {
            rgb = mapColor == MapColor.NONE ? 0x7F7F7F : mapColor.col;
        }
        int sky = Mth.clamp(level.getBrightness(LightLayer.SKY, pos), 0, 15);
        int block = Mth.clamp(level.getBrightness(LightLayer.BLOCK, pos), 0, 15);
        return PlanetVoxelMaterial.pack(Block.getId(state), rgb, sky, block, flags(state));
    }

    static int flags(BlockState state) {
        int flags = 0;
        if (state.canOcclude()) flags |= PlanetVoxelMaterial.FLAG_OPAQUE;
        else flags |= PlanetVoxelMaterial.FLAG_TRANSLUCENT | PlanetVoxelMaterial.FLAG_NO_OCCLUSION;
        if (!state.getFluidState().isEmpty()) flags |= PlanetVoxelMaterial.FLAG_LIQUID;
        if (state.getLightEmission() > 0) flags |= PlanetVoxelMaterial.FLAG_EMISSIVE;

        String path = state.getBlock().getDescriptionId();
        boolean foliage = path.contains("leaves") || path.contains("grass")
                || path.contains("flower") || path.contains("vine")
                || path.contains("crop") || path.contains("sapling");
        if (foliage) flags |= PlanetVoxelMaterial.FLAG_FOLIAGE;
        boolean natural = path.contains("stone") || path.contains("deepslate")
                || path.contains("dirt") || path.contains("grass_block")
                || path.contains("sand") || path.contains("gravel")
                || path.contains("clay") || path.contains("terracotta")
                || path.contains("snow") || path.contains("ice")
                || path.contains("ore") || path.contains("mud")
                || path.contains("water") || path.contains("lava")
                || path.contains("log") || path.contains("leaves");
        flags |= natural ? PlanetVoxelMaterial.FLAG_NATURAL : PlanetVoxelMaterial.FLAG_STRUCTURE;
        return flags;
    }

    private static int exactInt(double value, String name) {
        int rounded = (int) Math.rint(value);
        if (Math.abs(value - rounded) > 1.0E-6) {
            throw new IllegalStateException(name + " is not block-aligned: " + value);
        }
        return rounded;
    }
}
