package com.velocityspider.crashphysics.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Fast scans over boxes of blocks, a whole chunk section at a time, skipping sections that hold nothing but air.
 */
public final class BlockScan {

    public interface Visitor {
        void visit(int x, int y, int z, BlockState state);
    }

    private BlockScan() {
    }

    /**
     * Visits every block with a collision shape in a box (inclusive bounds). For boxes inside a vehicle's plot, which
     * is always loaded.
     */
    public static void solidBlocks(final ServerLevel level, final int minX, final int minY, final int minZ,
                                   final int maxX, final int maxY, final int maxZ, final Visitor visitor) {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final int bottom = Math.max(minY, level.getMinBuildHeight());
        final int top = Math.min(maxY, level.getMaxBuildHeight() - 1);
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                final LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                final int x0 = Math.max(minX, chunkX << 4);
                final int x1 = Math.min(maxX, (chunkX << 4) + 15);
                final int z0 = Math.max(minZ, chunkZ << 4);
                final int z1 = Math.min(maxZ, (chunkZ << 4) + 15);

                for (int sectionY = bottom >> 4; sectionY <= top >> 4; sectionY++) {
                    final int index = level.getSectionIndexFromSectionY(sectionY);
                    if (index < 0 || index >= chunk.getSectionsCount()) {
                        continue;
                    }
                    final LevelChunkSection section = chunk.getSection(index);
                    if (section.hasOnlyAir()) {
                        continue;
                    }
                    final int y0 = Math.max(bottom, sectionY << 4);
                    final int y1 = Math.min(top, (sectionY << 4) + 15);
                    for (int x = x0; x <= x1; x++) {
                        for (int y = y0; y <= y1; y++) {
                            for (int z = z0; z <= z1; z++) {
                                final BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (state.isAir() || state.getCollisionShape(level, pos.set(x, y, z)).isEmpty()) {
                                    continue;
                                }
                                visitor.visit(x, y, z, state);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @return if a box of the world contains anything but air (terrain, water), without loading any chunks
     */
    public static boolean anythingIn(final ServerLevel level, final double minX, final double minY, final double minZ,
                                     final double maxX, final double maxY, final double maxZ) {
        final int bottom = Math.max((int) Math.floor(minY), level.getMinBuildHeight()) >> 4;
        final int top = Math.min((int) Math.floor(maxY), level.getMaxBuildHeight() - 1) >> 4;
        for (int chunkX = (int) Math.floor(minX) >> 4; chunkX <= (int) Math.floor(maxX) >> 4; chunkX++) {
            for (int chunkZ = (int) Math.floor(minZ) >> 4; chunkZ <= (int) Math.floor(maxZ) >> 4; chunkZ++) {
                final LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (int sectionY = bottom; sectionY <= top; sectionY++) {
                    final int index = level.getSectionIndexFromSectionY(sectionY);
                    if (index >= 0 && index < chunk.getSectionsCount() && !chunk.getSection(index).hasOnlyAir()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
