package com.velocityspider.deathstar.structure;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.velocityspider.deathstar.block.ModBlocks;
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
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

/**
 * Builds the ruined Death Star out of real blocks and then assembles those blocks into a Sable
 * physics sub-level, so the result is a genuine moving physics body rather than a static build.
 *
 * <p>The whole operation runs synchronously on the server thread inside a single command/use:
 * <ol>
 *     <li>generate the voxel layout + sign features with {@link DeathStarBlueprint},</li>
 *     <li>{@code setBlock} every voxel (and every Easter-egg sign, with its text) into the world,</li>
 *     <li>hand the exact set of positions to {@link SubLevelAssemblyHelper#assembleBlocks} which
 *         lifts them out of the world into a sub-level and registers a rigid body for them.</li>
 * </ol>
 */
public final class DeathStarAssembler {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Place blocks as quietly as possible: no client sync, no neighbour/shape updates. The blocks
    // are pulled straight back out of the world by the assembly in the same server tick, so any
    // client packets would be overwritten and neighbour cascades would only waste time.
    private static final int PLACE_FLAGS = Block.UPDATE_KNOWN_SHAPE;

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
     * @param center the world position of the sphere's centre (and the assembly anchor)
     * @param radius outer radius in blocks
     * @param seed   seed controlling the exact ruin pattern
     */
    public static Result summon(ServerLevel level, BlockPos center, int radius, long seed) {
        // Sable must be present and have a sub-level container for this dimension.
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return new Result.SableUnavailable();
        }

        int thickness = DeathStarConfig.SHELL_THICKNESS.get();
        DeathStarBlueprint.Blueprint blueprint = DeathStarBlueprint.build(seed, radius, thickness);

        int total = blueprint.voxels().size() + blueprint.signs().size();
        int limit = DeathStarConfig.MAX_BLOCKS.get();
        if (total > limit) {
            return new Result.TooManyBlocks(total, limit);
        }

        List<BlockPos> placed = new ArrayList<>(total);
        try {
            // 1. Lay down the structure.
            for (DeathStarBlueprint.Voxel v : blueprint.voxels()) {
                BlockPos pos = center.offset(v.x(), v.y(), v.z());
                level.setBlock(pos, v.state(), PLACE_FLAGS);
                placed.add(pos);
            }

            // 2. Place the Easter-egg signs with their text (survives assembly via block-entity NBT).
            for (DeathStarBlueprint.SignFeature s : blueprint.signs()) {
                placeSign(level, center.offset(s.x(), s.y(), s.z()), s, placed);
            }

            // 3. Hand everything to Sable. This removes the blocks from the world and re-homes them
            //    inside a fresh sub-level whose rigid body the physics engine now simulates.
            BoundingBox3i bounds = BoundingBox3i.from(placed);
            ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, center, placed, bounds);

            LOGGER.info("Assembled ruined Death Star: {} blocks, radius {}, sub-level {}",
                    placed.size(), radius, subLevel != null ? subLevel.getUniqueId() : "<null>");
            return new Result.Success(placed.size());
        } catch (Exception e) {
            LOGGER.error("Death Star assembly failed", e);
            // Best-effort cleanup so a partial failure does not leave a static husk behind.
            for (BlockPos pos : placed) {
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), PLACE_FLAGS);
                }
            }
            return new Result.Failed(String.valueOf(e.getMessage()));
        }
    }

    /** Places a wall sign (plus a backing block) and writes its text via the sign block entity. */
    private static void placeSign(ServerLevel level, BlockPos pos, DeathStarBlueprint.SignFeature s,
                                  List<BlockPos> placed) {
        try {
            // A wall sign needs a solid block behind it or it will pop off during assembly's
            // neighbour updates. Guarantee one (interior wall) if the spot is empty.
            BlockPos behind = pos.relative(s.facing().getOpposite());
            if (level.getBlockState(behind).isAir()) {
                level.setBlock(behind, ModBlocks.INTERIOR_WALL.get().defaultBlockState(), PLACE_FLAGS);
                placed.add(behind);
            }
            level.setBlock(pos, Blocks.OAK_WALL_SIGN.defaultBlockState()
                    .setValue(WallSignBlock.FACING, s.facing()), PLACE_FLAGS);
            if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                SignText text = sign.getFrontText();
                List<String> lines = s.lines();
                for (int i = 0; i < 4; i++) {
                    String line = i < lines.size() ? lines.get(i) : "";
                    text = text.setMessage(i, Component.literal(line));
                }
                sign.setText(text, true);
                sign.setChanged();
            }
            placed.add(pos);
        } catch (Exception e) {
            LOGGER.warn("Failed to place Death Star sign at {}", pos, e);
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
}
