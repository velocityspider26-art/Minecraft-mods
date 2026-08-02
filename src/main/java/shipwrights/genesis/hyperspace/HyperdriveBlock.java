package shipwrights.genesis.hyperspace;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

public class HyperdriveBlock extends Block implements EntityBlock {
    public static final String SCREEN_HYPERDRIVE = "genesis.screen.hyperdrive";

    /** Hyperdrive class baked into the block: 6 slowest .. 0.5 fastest, 0.1 Falken-only. */
    private final double driveClass;

    public HyperdriveBlock(BlockBehaviour.Properties properties) {
        this(properties, shipwrights.genesis.hyperspace.HyperspaceData.DEFAULT_DRIVE_CLASS);
    }

    public HyperdriveBlock(BlockBehaviour.Properties properties, double driveClass) {
        super(properties);
        this.driveClass = driveClass;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED, false));
    }

    public double driveClass() {
        return driveClass;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HyperdriveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == GenesisBlockEntities.HYPERDRIVE.get()
                ? (tickerLevel, tickerPos, tickerState, blockEntity) -> ((HyperdriveBlockEntity) blockEntity).tickServer()
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HyperdriveBlockEntity) {
                MenuProvider provider = new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable(SCREEN_HYPERDRIVE);
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player menuPlayer) {
                        return new HyperdriveMenu(windowId, menuPlayer, pos);
                    }
                };
                serverPlayer.openMenu(provider, data -> data.writeBlockPos(pos));
            }
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
