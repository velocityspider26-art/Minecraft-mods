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
 * Builds the Death Star wreckage out of real blocks and assembles it into Sable physics bodies.
 *
 * <p>The wreck is a <em>field of separate pieces</em> ({@link DeathStarBlueprint#buildWreckField}).
 * Each piece is placed at its own scattered offset and assembled into its <em>own</em> sub-level,
 * so the ruins really are in pieces: every shard is an independent rigid body that falls, collides
 * and settles on its own. Pieces are placed and assembled one at a time, so their blocks never
 * coexist in the world and cannot collide during placement.
 */
public final class DeathStarAssembler {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Place blocks as quietly as possible: no client sync, no neighbour/shape updates. The blocks
    // are pulled straight back out of the world by the assembly in the same server tick.
    private static final int PLACE_FLAGS = Block.UPDATE_KNOWN_SHAPE;

    public sealed interface Result {
        record Success(int pieceCount, int blockCount) implements Result {}
        record TooManyBlocks(int blockCount, int limit) implements Result {}
        record SableUnavailable() implements Result {}
        record Failed(String message) implements Result {}
    }

    private DeathStarAssembler() {}

    /**
     * Summons the Death Star wreck centred on {@code center}.
     *
     * @param level  the server level to build in
     * @param center the world position the debris field is scattered around
     * @param radius radius of the intact station the shards were cut from
     * @param seed   seed controlling the exact wreck pattern
     */
    public static Result summon(ServerLevel level, BlockPos center, int radius, long seed) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return new Result.SableUnavailable();
        }

        int thickness = DeathStarConfig.SHELL_THICKNESS.get();
        List<DeathStarBlueprint.Fragment> fragments = DeathStarBlueprint.buildWreckField(seed, radius, thickness);

        int total = 0;
        for (DeathStarBlueprint.Fragment f : fragments) {
            total += f.voxels().size() + f.signs().size();
        }
        int limit = DeathStarConfig.MAX_BLOCKS.get();
        if (total > limit) {
            return new Result.TooManyBlocks(total, limit);
        }

        int drop = DeathStarConfig.DROP_HEIGHT.get();
        int placedTotal = 0;
        int pieces = 0;
        String lastError = null;

        for (DeathStarBlueprint.Fragment fragment : fragments) {
            if (fragment.voxels().isEmpty()) continue;
            BlockPos anchor = center.offset(fragment.offsetX(), fragment.offsetY() + drop, fragment.offsetZ());
            List<BlockPos> placed = new ArrayList<>(fragment.voxels().size() + fragment.signs().size());
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
                placedTotal += placed.size();
                pieces++;
            } catch (Exception e) {
                lastError = String.valueOf(e.getMessage());
                LOGGER.error("Failed to assemble Death Star fragment '{}'", fragment.name(), e);
                // Best-effort cleanup of this piece so it does not linger as a static husk.
                for (BlockPos pos : placed) {
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), PLACE_FLAGS);
                    }
                }
            }
        }

        if (pieces == 0) {
            return new Result.Failed(lastError != null ? lastError : "no pieces assembled");
        }
        LOGGER.info("Assembled Death Star wreck: {} pieces, {} blocks, radius {}", pieces, placedTotal, radius);
        return new Result.Success(pieces, placedTotal);
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
            case Result.Success s -> Component.translatable("message.deathstar.summoned", s.pieceCount(), s.blockCount())
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
