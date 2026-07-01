package com.velocityspider.deathstar.structure;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.velocityspider.deathstar.config.DeathStarConfig;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Builds the ruined Death Star out of real blocks and then assembles those blocks into a Sable
 * physics sub-level, so the result is a genuine moving physics body rather than a static build.
 *
 * <p>The whole operation runs synchronously on the server thread inside a single command/use, in
 * this order:
 * <ol>
 *     <li>generate the voxel layout with {@link DeathStarBlueprint},</li>
 *     <li>{@code setBlock} every voxel into the world around the target centre,</li>
 *     <li>hand the exact set of positions to {@link SubLevelAssemblyHelper#assembleBlocks} which
 *         lifts them out of the world into a sub-level and registers a rigid body for them.</li>
 * </ol>
 */
public final class DeathStarAssembler {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Place blocks as quietly as possible. We deliberately skip client sync (UPDATE_CLIENTS) and
    // neighbour updates: the blocks are pulled straight back out of the world by the assembly in the
    // same server tick, so any client packets would just be overwritten, and neighbour cascades
    // would only waste time and fight the trench/hole geometry. UPDATE_KNOWN_SHAPE stops the block
    // from re-deriving its own shape from neighbours as it is placed.
    private static final int PLACE_FLAGS = Block.UPDATE_KNOWN_SHAPE;

    /** Outcome of an assembly attempt, so callers can report a friendly message. */
    public sealed interface Result {
        record Success(int blockCount) implements Result {}
        record TooManyBlocks(int blockCount, int limit) implements Result {}
        record SableUnavailable() implements Result {}
        record Failed(String message) implements Result {}
    }

    private DeathStarAssembler() {}

    /**
     * Summons a Death Star centred on {@code center}.
     *
     * @param level  the server level to build in
     * @param center the world position of the sphere's centre
     * @param radius outer radius in blocks
     * @param seed   seed controlling the exact ruin pattern
     */
    public static Result summon(ServerLevel level, BlockPos center, int radius, long seed) {
        // Sable must be present and have a sub-level container for this dimension. If it is not,
        // there is nowhere to put a physics body, so bail out before touching the world.
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return new Result.SableUnavailable();
        }

        int thickness = DeathStarConfig.SHELL_THICKNESS.get();
        List<DeathStarBlueprint.Voxel> voxels = DeathStarBlueprint.build(seed, radius, thickness);

        int coreCount = DeathStarConfig.REACTOR_CORE.get() ? countCore(radius) : 0;
        int total = voxels.size() + coreCount;
        int limit = DeathStarConfig.MAX_BLOCKS.get();
        if (total > limit) {
            return new Result.TooManyBlocks(total, limit);
        }

        List<BlockPos> placed = new ArrayList<>(total);
        try {
            // 1. Lay down the hull.
            for (DeathStarBlueprint.Voxel v : voxels) {
                BlockPos pos = center.offset(v.x(), v.y(), v.z());
                level.setBlock(pos, v.state(), PLACE_FLAGS);
                placed.add(pos);
            }

            // 2. Optional solid reactor core at the very centre. It doubles as the assembly anchor
            //    so there is always a real block sitting at the centre of mass.
            if (DeathStarConfig.REACTOR_CORE.get()) {
                placeCore(level, center, radius, placed);
            }

            // 3. Hand everything to Sable. This removes the blocks from the world and re-homes them
            //    inside a fresh sub-level whose rigid body the physics engine now simulates.
            BoundingBox3i bounds = BoundingBox3i.from(placed);
            ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, center, placed, bounds);

            LOGGER.info("Assembled Death Star: {} blocks, radius {}, sub-level {}",
                    placed.size(), radius, subLevel != null ? subLevel.getUniqueId() : "<null>");
            return new Result.Success(placed.size());
        } catch (Exception e) {
            LOGGER.error("Death Star assembly failed", e);
            // Best-effort cleanup: if assembly threw partway, scrub any blocks still sitting in the
            // world so we do not leave a broken static husk behind.
            for (BlockPos pos : placed) {
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), PLACE_FLAGS);
                }
            }
            return new Result.Failed(String.valueOf(e.getMessage()));
        }
    }

    /** Builds a player-facing chat message describing an assembly outcome. */
    public static Component describe(Result result) {
        return switch (result) {
            case Result.Success s -> Component.translatable("message.deathstar.summoned", s.blockCount())
                    .withStyle(ChatFormatting.GRAY);
            case Result.TooManyBlocks t -> Component.translatable("message.deathstar.too_many", t.blockCount(), t.limit())
                    .withStyle(ChatFormatting.RED);
            case Result.SableUnavailable u -> Component.translatable("message.deathstar.no_sable")
                    .withStyle(ChatFormatting.RED);
            case Result.Failed f -> Component.translatable("message.deathstar.failed", f.message())
                    .withStyle(ChatFormatting.RED);
        };
    }

    /** Half-extent of the cubic reactor core, scaled gently with the sphere. */
    private static int coreHalf(int radius) {
        return Math.max(1, radius / 12);
    }

    private static int countCore(int radius) {
        int h = coreHalf(radius);
        int side = 2 * h + 1;
        return side * side * side;
    }

    private static void placeCore(ServerLevel level, BlockPos center, int radius, List<BlockPos> placed) {
        int h = coreHalf(radius);
        for (int x = -h; x <= h; x++) {
            for (int y = -h; y <= h; y++) {
                for (int z = -h; z <= h; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    // Glowing core, iron casing.
                    boolean centre = Math.abs(x) + Math.abs(y) + Math.abs(z) <= 1;
                    level.setBlock(pos, (centre ? Blocks.SEA_LANTERN : Blocks.NETHERITE_BLOCK).defaultBlockState(), PLACE_FLAGS);
                    placed.add(pos);
                }
            }
        }
    }
}
