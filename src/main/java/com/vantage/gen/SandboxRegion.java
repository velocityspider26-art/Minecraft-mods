package com.vantage.gen;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongFunction;

/**
 * World-generation region over chunks {@link WorldGenSandbox} makes for distant terrain. Features
 * place into those chunks only: nothing reaches the real world (no block entities, entities,
 * ticks, POI or light updates), and chunks outside the batch read as empty and take no writes.
 */
final class SandboxRegion extends WorldGenRegion {
    private final Long2ObjectMap<ProtoChunk> chunks;
    private final LongFunction<ProtoChunk> outside;
    private final int minY;
    private final int maxY;
    private long lastKey = Long.MIN_VALUE;
    @Nullable
    private ProtoChunk last;

    SandboxRegion(ServerLevel level, ProtoChunk center, Long2ObjectMap<ProtoChunk> chunks, LongFunction<ProtoChunk> outside) {
        super(level, StaticCache2D.create(center.getPos().x, center.getPos().z, 0, (x, z) -> new Holder(new ChunkPos(x, z))),
                ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES), center);
        this.chunks = chunks;
        this.outside = outside;
        this.minY = level.getMinBuildHeight();
        this.maxY = level.getMaxBuildHeight();
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean require) {
        long key = ChunkPos.asLong(x, z);
        if (key == this.lastKey) {
            return this.last;
        }
        ProtoChunk chunk = this.chunks.get(key);
        if (chunk != null) {
            // Biome and block lookups come in runs over one chunk.
            this.lastKey = key;
            this.last = chunk;
            return chunk;
        }
        // Not ours: biome lookups then fall back to the biome source, block reads see air.
        return require ? this.outside.apply(key) : null;
    }

    @Override
    public boolean hasChunk(int x, int z) {
        return this.chunks.containsKey(ChunkPos.asLong(x, z));
    }

    @Override
    public boolean isOldChunkAround(ChunkPos pos, int radius) {
        return false;
    }

    /** As in Minecraft: features may write into the chunk being decorated and the eight around it. */
    @Override
    public boolean ensureCanWrite(BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        ChunkPos center = this.getCenter();
        return Math.abs(cx - center.x) <= 1 && Math.abs(cz - center.z) <= 1
                && pos.getY() >= this.minY && pos.getY() < this.maxY && this.chunks.containsKey(ChunkPos.asLong(cx, cz));
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
        if (!this.ensureCanWrite(pos)) {
            return false;
        }
        this.chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4)).setBlockState(pos, state, false);
        return true;
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropBlock, @Nullable Entity entity, int recursionLeft) {
        BlockState state = this.getBlockState(pos);
        return !state.isAir() && this.setBlock(pos, this.getFluidState(pos).createLegacyBlock(), 3, recursionLeft);
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        return true;
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    /** Placeholder required by the region's constructor; chunks come from {@link #chunks}. */
    private static final class Holder extends GenerationChunkHolder {
        Holder(ChunkPos pos) {
            super(pos);
        }

        @Override
        public int getTicketLevel() {
            return 0;
        }

        @Override
        public int getQueueLevel() {
            return 0;
        }
    }
}
