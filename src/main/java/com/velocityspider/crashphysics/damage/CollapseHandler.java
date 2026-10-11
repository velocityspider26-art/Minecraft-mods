package com.velocityspider.crashphysics.damage;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashConfig;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes parts of buildings and trees that a crash cut loose fall down, the way Teardown does it.
 * <p>
 * After a crash breaks terrain, the blocks around each hole are searched. A connected section that turns out to be
 * small and no longer attached to anything (the top of a tree whose trunk was sliced, a bridge deck with its pillar
 * knocked out) is turned into a Sable physics object and falls, tumbling and crashing on its own. Tiny loose bits just
 * fall as falling blocks.
 */
public final class CollapseHandler {

    private static final int CHECKS_PER_TICK = 8;
    private static final long MAX_NANOS_PER_TICK = 3_000_000L;
    private static final int MAX_SEEDS = 1024;
    private static final Direction[] DIRECTIONS = Direction.values();

    /**
     * A section reaching this far below where it was cut is part of the ground [blocks]
     */
    private static final int SUPPORT_DEPTH = 12;

    private static final Direction[] SEARCH_ORDER = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN};

    private final ServerLevel level;
    private final LongLinkedOpenHashSet seeds = new LongLinkedOpenHashSet();
    private boolean loggedFailure;
    @Nullable
    private LevelChunk lastChunk;

    public CollapseHandler(final ServerLevel level) {
        this.level = level;
    }

    /**
     * Remembers the neighbours of a terrain block that was just destroyed, to check whether they still hold on.
     */
    public void seed(final BlockPos destroyed) {
        if (this.seeds.size() >= MAX_SEEDS) {
            return;
        }
        final long origin = destroyed.asLong();
        for (final Direction direction : DIRECTIONS) {
            this.seeds.add(BlockPos.offset(origin, direction));
        }
    }

    public void process(final Destruction destruction) {
        if (this.seeds.isEmpty()) {
            return;
        }
        if (!CrashConfig.COLLAPSE.getAsBoolean() || !CrashConfig.WORLD_DAMAGE.getAsBoolean()) {
            this.seeds.clear();
            return;
        }

        final int maxCollapse = CrashConfig.MAX_COLLAPSE_BLOCKS.getAsInt();
        this.lastChunk = null;
        final LongOpenHashSet checked = new LongOpenHashSet();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int checks = 0;

        final long deadline = System.nanoTime() + MAX_NANOS_PER_TICK;
        while (!this.seeds.isEmpty() && checks < CHECKS_PER_TICK && System.nanoTime() < deadline) {
            final long seed = this.seeds.removeFirstLong();
            if (checked.contains(seed)) {
                continue;
            }

            pos.set(seed);
            if (!this.level.isLoaded(pos) || !this.isSolid(this.level.getBlockState(pos), pos)) {
                continue;
            }

            checks++;
            final LongArrayList floating = this.findFloating(seed, maxCollapse, checked);
            if (floating != null) {
                this.collapse(floating);
            }
        }
    }

    /**
     * Searches the section connected to a block, heading downwards first: ground that reaches well below where the
     * crash cut is part of the landscape, and is found to be held up within a few steps.
     *
     * @return the section if it is loose and small enough to fall, or null if it is held up
     */
    @Nullable
    private LongArrayList findFloating(final long start, final int maxCollapse, final LongOpenHashSet checked) {
        final LongArrayList stack = new LongArrayList();
        final LongOpenHashSet visited = new LongOpenHashSet();
        final LongArrayList blocks = new LongArrayList();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final int floor = this.level.getMinBuildHeight() + 1;
        final int startY = BlockPos.getY(start);

        stack.add(start);
        visited.add(start);
        boolean supported = false;

        search:
        while (!stack.isEmpty()) {
            final long current = stack.popLong();
            blocks.add(current);

            // Too big to come down, reaching deep below the cut, or reaching bedrock level: held up
            final int y = BlockPos.getY(current);
            if (blocks.size() > maxCollapse || startY - y >= SUPPORT_DEPTH || y <= floor) {
                supported = true;
                break;
            }

            // Pushed last, so the way down is tried first
            for (final Direction direction : SEARCH_ORDER) {
                final long next = BlockPos.offset(current, direction);
                if (!visited.add(next)) {
                    continue;
                }

                pos.set(next);
                final LevelChunk chunk = this.chunkAt(pos.getX(), pos.getZ());
                if (chunk == null) {
                    // Unloaded: assume it holds
                    supported = true;
                    break search;
                }

                final BlockState state = chunk.getBlockState(pos);
                if (!this.isSolid(state, pos)) {
                    continue;
                }
                if (state.getDestroySpeed(this.level, pos) < 0.0f) {
                    // Anchored to something indestructible
                    supported = true;
                    break search;
                }
                stack.add(next);
            }
        }

        checked.addAll(visited);
        return supported ? null : blocks;
    }

    @Nullable
    private LevelChunk chunkAt(final int x, final int z) {
        final LevelChunk last = this.lastChunk;
        if (last != null && last.getPos().x == x >> 4 && last.getPos().z == z >> 4) {
            return last;
        }
        final LevelChunk chunk = this.level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        this.lastChunk = chunk;
        return chunk;
    }

    private void collapse(final LongArrayList blocks) {
        if (blocks.size() <= 3) {
            for (int i = 0; i < blocks.size(); i++) {
                final BlockPos pos = BlockPos.of(blocks.getLong(i));
                final BlockState state = this.level.getBlockState(pos);
                if (!state.hasBlockEntity()) {
                    FallingBlockEntity.fall(this.level, pos, state);
                }
            }
            return;
        }

        final List<BlockPos> positions = new ArrayList<>(blocks.size());
        for (int i = 0; i < blocks.size(); i++) {
            positions.add(BlockPos.of(blocks.getLong(i)));
        }

        try {
            final BoundingBox3i bounds = BoundingBox3i.from(positions).expand(1, 1, 1);
            SubLevelAssemblyHelper.assembleBlocks(this.level, positions.getFirst(), positions, bounds);
        } catch (final RuntimeException e) {
            if (!this.loggedFailure) {
                this.loggedFailure = true;
                CrashPhysics.LOGGER.error("Failed to turn a loose section of {} blocks into a physics object", positions.size(), e);
            }
        }
    }

    private boolean isSolid(final BlockState state, final BlockPos pos) {
        return !state.isAir() && !state.getCollisionShape(this.level, pos).isEmpty();
    }
}
