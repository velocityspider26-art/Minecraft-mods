package shipwrights.genesis.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.content.blockentity.GenesisBlockEntities;
import shipwrights.genesis.content.blockentity.OrbitalStabilizerBlockEntity;

/**
 * Mount on a Sable craft to hold it in a stable circular orbit automatically —
 * it works out the orbital speed for wherever the craft is and trims to it.
 * Right-click to toggle. Lit when it has an orbital lock.
 */
public class OrbitalStabilizerBlock extends Block implements EntityBlock {

    public OrbitalStabilizerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OrbitalStabilizerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == GenesisBlockEntities.ORBITAL_STABILIZER.get()
                ? (tickerLevel, pos, tickerState, blockEntity) ->
                        ((OrbitalStabilizerBlockEntity) blockEntity).tickServer()
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof OrbitalStabilizerBlockEntity stabilizer) {
            stabilizer.setEnabled(!stabilizer.isEnabled());
            serverPlayer.displayClientMessage(Component.literal(
                    stabilizer.isEnabled() ? "Orbital stabilizer engaged" : "Orbital stabilizer disengaged"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.POWERED, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.POWERED);
    }
}
