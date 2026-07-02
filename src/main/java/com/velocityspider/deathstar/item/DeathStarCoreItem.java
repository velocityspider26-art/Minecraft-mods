package com.velocityspider.deathstar.item;

import com.velocityspider.deathstar.config.DeathStarConfig;
import com.velocityspider.deathstar.structure.DeathStarAssembler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The "Death Star Core" item. Right-click a block to assemble a ruined Death Star that rests on
 * that spot. Consumed on use (unless the player is in creative) so it behaves like a summon charge.
 */
public class DeathStarCoreItem extends Item {

    public DeathStarCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            // Let the server side do the real work; report success on the client so the arm swings.
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }

        int radius = DeathStarConfig.RADIUS.get();
        // Rest the sphere on the clicked block: its centre one radius above the click point.
        BlockPos clicked = context.getClickedPos();
        BlockPos center = new BlockPos(
                clicked.getX(),
                clicked.getY() + 1 + radius,
                clicked.getZ());

        DeathStarAssembler.Result result = DeathStarAssembler.summon(level, center, radius, level.getRandom().nextLong());

        if (context.getPlayer() instanceof ServerPlayer player) {
            player.displayClientMessage(DeathStarAssembler.describe(result), true);
        }

        if (result instanceof DeathStarAssembler.Result.Success) {
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
}
