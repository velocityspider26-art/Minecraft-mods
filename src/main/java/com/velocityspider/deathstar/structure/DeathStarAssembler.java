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
 * Builds the ruined Death Star out of real blocks and assembles it into a Sable physics body.
 *
 * <p>The whole station is one connected {@link DeathStarBlueprint.Fragment}: it is placed around
 * the target centre and handed to {@link SubLevelAssemblyHelper#assembleBlocks} as a single
 * assembly, so the result is <em>one whole</em> rigid physics body rather than scattered pieces.
 */
public final class DeathStarAssembler {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Place blocks as quietly as possible: no client sync, no neighbour/shape updates. The blocks
    // are pulled straight back out of the world by the assembly in the same server tick.
    private static final int PLACE_FLAGS = Block.UPDATE_KNOWN_SHAPE;

    public sealed interface Result {
        record Success(int blockCount) implements Result {}
        record TooManyBlocks(int blockCount, int limit) implements Result {}
        record SableUnavailable() implements Result {}
        record Failed(String message) implements Result {}
    }

    private DeathStarAssembler() {}

    /**
     * Summons the ruined Death Star as one physics body centred on {@code center}.
     *
     * @param level  the server level to build in
     * @param center the world position of the station's centre (and the assembly anchor)
     * @param radius outer radius in blocks
     * @param seed   seed controlling the exact ruin pattern
     */
    public static Result summon(ServerLevel level, BlockPos center, int radius, long seed) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return new Result.SableUnavailable();
        }

        int thickness = DeathStarConfig.SHELL_THICKNESS.get();
        DeathStarBlueprint.Fragment fragment = DeathStarBlueprint.build(seed, radius, thickness);

        int total = fragment.voxels().size() + fragment.signs().size();
        int limit = DeathStarConfig.MAX_BLOCKS.get();
        if (total > limit) {
            return new Result.TooManyBlocks(total, limit);
        }

        BlockPos anchor = center.above(DeathStarConfig.DROP_HEIGHT.get());
        List<BlockPos> placed = new ArrayList<>(total);
        try {
            for (DeathStarBlueprint.Voxel v : fragment.voxels()) {
                BlockPos pos = anchor.offset(v.x(), v.y(), v.z());
                level.setBlock(pos, v.state(), PLACE_FLAGS);
                placed.add(pos);
            }
            for (DeathStarBlueprint.SignFeature s : fragment.signs()) {
                placeSign(level, anchor.offset(s.x(), s.y(), s.z()), s, placed);
            }

            BoundingBox3i bounds = BoundingBox3i.from(placed);
            SubLevelAssemblyHelper.assembleBlocks(level, anchor, placed, bounds);
            LOGGER.info("Assembled the Death Star: {} blocks, radius {}", placed.size(), radius);
            return new Result.Success(placed.size());
        } catch (Exception e) {
            LOGGER.error("Death Star assembly failed", e);
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
                    text = text.setMessage(i, Component.literal(i < lines.size() ? lines.get(i) : ""));
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
