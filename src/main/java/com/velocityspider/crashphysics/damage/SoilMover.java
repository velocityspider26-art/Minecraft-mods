package com.velocityspider.crashphysics.damage;

import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.material.CrashMaterials;
import com.velocityspider.crashphysics.material.FractureMode;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;

/**
 * What happens to the soil a crash digs out.
 * <p>
 * Soil isn't destroyed by an impact, it is shoved out of the way: a dive pushes it out all round into a raised rim, a
 * slide ploughs it ahead and out to both sides into berms, and only a little of it is packed down for good. Afterwards
 * the loose walls of the hole slump until they are no steeper than loose soil can stand, which turns a square shaft
 * into a bowl and a slot into a trench with sloping sides.
 */
public final class SoilMover {

    /**
     * Share of displaced soil that is compacted rather than moved
     */
    private static final double COMPACTION = 0.15;

    /**
     * Furthest a block of soil is shoved sideways [blocks]
     */
    private static final int MAX_PUSH = 4;

    /**
     * How far up or down to look for the ground next to a hole [blocks]
     */
    private static final int SURFACE_SEARCH = 4;

    private static final int MAX_SEEDS = 4096;
    private static final long DUG_MEMORY_TICKS = 200;
    private static final int CHECKS_PER_TICK = 96;
    private static final int MOVES_PER_TICK = 48;
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private final ServerLevel level;
    private final RandomSource random = RandomSource.create();
    private final LongLinkedOpenHashSet seeds = new LongLinkedOpenHashSet();

    /**
     * Columns a crash dug into lately, and when: the hole soil is shoved out of, not into
     */
    private final Long2LongOpenHashMap dug = new Long2LongOpenHashMap();
    private long tick;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public SoilMover(final ServerLevel level) {
        this.level = level;
    }

    /**
     * @return if a share of the soil dug out of a crater is packed down for good instead of being moved
     */
    public boolean compacts() {
        return this.random.nextDouble() < COMPACTION;
    }

    /**
     * Remembers that a crash dug soil out of a column.
     */
    public void markDug(final BlockPos pos) {
        this.dug.put(ChunkPos.asLong(pos.getX(), pos.getZ()), this.tick);
    }

    private boolean recentlyDug(final int x, final int z) {
        final long key = ChunkPos.asLong(x, z);
        return this.dug.containsKey(key) && this.tick - this.dug.get(key) < DUG_MEMORY_TICKS;
    }

    /**
     * Shoves a block of soil dug out at a position out to the edge of the hole and piles it on the ground there.
     *
     * @param motionX direction the vehicle was moving (does not need to be normalised)
     * @return if a place was found for it
     */
    public boolean displace(final BlockPos from, final BlockState soil, final double motionX, final double motionY, final double motionZ) {
        final double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
        final double speed = Math.sqrt(horizontal * horizontal + motionY * motionY);
        final boolean sliding = speed > 1.0e-6 && horizontal / speed > 0.4;
        final double heading = Math.atan2(motionZ, motionX);

        for (int attempt = 0; attempt < 3; attempt++) {
            // A slide shoves soil ahead and out to both sides (a bow wave and two berms); a dive spreads it all round
            final double angle = sliding
                    ? heading + (this.random.nextDouble() - 0.5) * Math.PI * 1.3
                    : this.random.nextDouble() * Math.PI * 2.0;
            final double dx = Math.cos(angle);
            final double dz = Math.sin(angle);

            for (int step = 1; step <= MAX_PUSH; step++) {
                final int x = Mth.floor(from.getX() + 0.5 + dx * step);
                final int z = Mth.floor(from.getZ() + 0.5 + dz * step);
                if ((x == from.getX() && z == from.getZ()) || this.recentlyDug(x, z)) {
                    // Still inside the hole
                    continue;
                }
                final int top = this.surface(x, from.getY(), z);
                if (top == Integer.MIN_VALUE) {
                    // A wall or a cliff that way
                    break;
                }
                if (top < from.getY() - 1) {
                    // Still inside the hole: keep going out to its edge
                    continue;
                }
                if (this.place(x, top + 1, z, soil)) {
                    this.seed(this.cursor.set(x, top + 1, z));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remembers a spot where the ground changed, to check whether the slopes around it still stand.
     */
    public void seed(final BlockPos pos) {
        if (this.seeds.size() < MAX_SEEDS) {
            this.seeds.add(pos.asLong());
        }
    }

    /**
     * Lets loose slopes that are too steep slump a little. Called once per tick, so craters visibly settle.
     */
    public void process() {
        this.tick++;
        if (this.tick % 100 == 0 && !this.dug.isEmpty()) {
            this.dug.long2LongEntrySet().removeIf(entry -> this.tick - entry.getLongValue() >= DUG_MEMORY_TICKS);
        }
        if (this.seeds.isEmpty()) {
            return;
        }
        if (!CrashConfig.SLUMPING.getAsBoolean() || !CrashConfig.WORLD_DAMAGE.getAsBoolean()) {
            this.seeds.clear();
            return;
        }

        int checks = 0;
        int moves = 0;
        while (!this.seeds.isEmpty() && checks < CHECKS_PER_TICK && moves < MOVES_PER_TICK) {
            final long seed = this.seeds.removeFirstLong();
            checks++;

            final int x = BlockPos.getX(seed);
            final int y = BlockPos.getY(seed);
            final int z = BlockPos.getZ(seed);
            if (!this.level.isLoaded(this.cursor.set(x, y, z))) {
                continue;
            }
            final int top = this.surface(x, y, z);
            if (top == Integer.MIN_VALUE) {
                continue;
            }

            for (final Direction direction : HORIZONTAL) {
                final int nx = x + direction.getStepX();
                final int nz = z + direction.getStepZ();
                final int neighbourTop = this.surface(nx, top, nz);
                if (neighbourTop == Integer.MIN_VALUE) {
                    continue;
                }

                // A drop of two blocks or more is steeper than loose soil stands: the top of the higher side slides down
                boolean moved = false;
                if (neighbourTop - top >= 2) {
                    moved = this.slide(nx, neighbourTop, nz, x, top + 1, z);
                } else if (top - neighbourTop >= 2) {
                    moved = this.slide(x, top, z, nx, neighbourTop + 1, nz);
                }
                if (moved) {
                    moves++;
                    // Both columns changed, and so did the slopes around them
                    this.seed(this.cursor.set(nx, neighbourTop, nz));
                    this.seed(this.cursor.set(x, top, z));
                    break;
                }
            }
        }
    }

    /**
     * Moves the top block of a loose slope down to the foot of it.
     */
    private boolean slide(final int fromX, final int fromY, final int fromZ, final int toX, final int toY, final int toZ) {
        final BlockState state = this.level.getBlockState(this.cursor.set(fromX, fromY, fromZ));
        if (state.isAir() || state.hasBlockEntity() || CrashMaterials.get(state).fracture() != FractureMode.GRANULAR
                || state.getDestroySpeed(this.level, this.cursor) < 0.0f) {
            return false;
        }
        if (!this.canFill(toX, toY, toZ)) {
            return false;
        }

        // Whatever grew on top goes with it
        final BlockPos above = new BlockPos(fromX, fromY + 1, fromZ);
        final BlockState cover = this.level.getBlockState(above);
        if (!cover.isAir() && cover.getCollisionShape(this.level, above).isEmpty() && cover.getFluidState().isEmpty()) {
            this.level.destroyBlock(above, false);
        }

        this.level.setBlock(this.cursor.set(fromX, fromY, fromZ), state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        return this.place(toX, toY, toZ, state);
    }

    private boolean place(final int x, final int y, final int z, final BlockState soil) {
        if (!this.canFill(x, y, z)) {
            return false;
        }
        final BlockPos pos = new BlockPos(x, y, z);
        final BlockState replaced = this.level.getBlockState(pos);
        if (!replaced.isAir() && replaced.getFluidState().isEmpty()) {
            // Grass and flowers are buried
            this.level.destroyBlock(pos, false);
        }
        return this.level.setBlock(pos, soil, Block.UPDATE_ALL);
    }

    /**
     * @return if soil can be put at a position: the space is free, and no vehicle is in it
     */
    private boolean canFill(final int x, final int y, final int z) {
        final BlockState state = this.level.getBlockState(this.cursor.set(x, y, z));
        if (!state.canBeReplaced() || !this.level.isInWorldBounds(this.cursor)) {
            return false;
        }
        final SubLevelContainer container = SubLevelContainer.getContainer(this.level);
        if (container == null) {
            return true;
        }
        for (final SubLevel subLevel : container.queryIntersecting(new BoundingBox3d(new AABB(x - 0.25, y - 0.25, z - 0.25, x + 1.25, y + 1.25, z + 1.25)))) {
            if (this.occupies(subLevel, x, y, z)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return if any block of a vehicle reaches into a cell of the world
     */
    private boolean occupies(final SubLevel subLevel, final int x, final int y, final int z) {
        final Pose3dc pose = subLevel.logicalPose();
        final Vector3d point = new Vector3d();
        final BlockPos.MutableBlockPos plot = new BlockPos.MutableBlockPos();
        // The centre and the corners of the cell, pulled in a little so neighbouring cells don't count
        for (int sample = 0; sample < 9; sample++) {
            final double offset = 0.4;
            final double px = x + 0.5 + (sample == 8 ? 0.0 : (sample & 1) == 0 ? -offset : offset);
            final double py = y + 0.5 + (sample == 8 ? 0.0 : (sample & 2) == 0 ? -offset : offset);
            final double pz = z + 0.5 + (sample == 8 ? 0.0 : (sample & 4) == 0 ? -offset : offset);
            pose.transformPositionInverse(point.set(px, py, pz));
            plot.set(Mth.floor(point.x), Mth.floor(point.y), Mth.floor(point.z));
            if (!subLevel.getPlot().contains(plot.getX() + 0.5, plot.getZ() + 0.5)) {
                continue;
            }
            final BlockState state = this.level.getBlockState(plot);
            if (!state.isAir() && !state.getCollisionShape(this.level, plot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the ground in a column near a height: the highest block with a collision shape that has free space above
     * it, within a few blocks.
     *
     * @return its height, or {@link Integer#MIN_VALUE} if there is none (a cliff, a wall, a deep hole)
     */
    private int surface(final int x, final int nearY, final int z) {
        for (int y = nearY + SURFACE_SEARCH; y >= nearY - SURFACE_SEARCH; y--) {
            final BlockState state = this.level.getBlockState(this.cursor.set(x, y, z));
            if (state.isAir() || state.getCollisionShape(this.level, this.cursor).isEmpty()) {
                continue;
            }
            final BlockState above = this.level.getBlockState(this.cursor.set(x, y + 1, z));
            return above.canBeReplaced() ? y : Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }
}
