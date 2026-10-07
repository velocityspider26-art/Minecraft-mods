package com.velocityspider.crashphysics.damage;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashConfig;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes parts of buildings and trees that a crash cut loose fall down, the way Teardown does it.
 * <p>
 * After a crash breaks terrain, the blocks around each hole are flood-filled. A connected section that turns out to be
 * small and no longer attached to anything (the top of a tree whose trunk was sliced, a bridge deck with its pillar
 * knocked out) is turned into a Sable physics object and falls, tumbling and crashing on its own. Tiny loose bits just
 * fall as falling blocks.
 */
public final class CollapseHandler {

    private static final int CHECKS_PER_TICK = 6;
    private static final int MAX_SEEDS = 1024;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final ServerLevel level;
    private final LongLinkedOpenHashSet seeds = new LongLinkedOpenHashSet();
    private boolean loggedFailure;

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
        final LongOpenHashSet checked = new LongOpenHashSet();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int checks = 0;

        while (!this.seeds.isEmpty() && checks < CHECKS_PER_TICK) {
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
     * Flood-fills the section connected to a block.
     *
     * @return the section if it is loose and small enough to fall, or null if it is held up
     */
    @Nullable
    private LongArrayList findFloating(final long start, final int maxCollapse, final LongOpenHashSet checked) {
        final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        final LongOpenHashSet visited = new LongOpenHashSet();
        final LongArrayList blocks = new LongArrayList();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final int floor = this.level.getMinBuildHeight() + 1;

        queue.enqueue(start);
        visited.add(start);
        boolean supported = false;

        search:
        while (!queue.isEmpty()) {
            final long current = queue.dequeueLong();
            blocks.add(current);

            // Too big to come down, or reaching bedrock level: treat as held up
            if (blocks.size() > maxCollapse || BlockPos.getY(current) <= floor) {
                supported = true;
                break;
            }

            for (final Direction direction : DIRECTIONS) {
                final long next = BlockPos.offset(current, direction);
                if (!visited.add(next)) {
                    continue;
                }

                pos.set(next);
                if (!this.level.isLoaded(pos)) {
                    supported = true;
                    break search;
                }

                final BlockState state = this.level.getBlockState(pos);
                if (!this.isSolid(state, pos)) {
                    continue;
                }
                if (state.getDestroySpeed(this.level, pos) < 0.0f) {
                    // Anchored to something indestructible
                    supported = true;
                    break search;
                }
                queue.enqueue(next);
            }
        }

        checked.addAll(visited);
        return supported ? null : blocks;
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
