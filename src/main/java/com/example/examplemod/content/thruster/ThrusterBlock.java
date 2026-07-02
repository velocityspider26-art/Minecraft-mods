package com.example.examplemod.content.thruster;

import com.example.examplemod.ExampleMod;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A directional thruster block modelled after the Create: Propulsion Simulated thruster, but with
 * the particle exhaust replaced by a procedural 3D plume mesh
 * ({@link com.example.examplemod.client.ThrusterPlumeRenderer}).
 *
 * <p>The plume fires out of the {@link DirectionalBlock#FACING} (nozzle) side. Unlike the creative
 * variant, this thruster needs a fuel source: right-click it with a lava bucket or with the mod's
 * kerosene to fill its fuel buffer. While it has fuel it burns and the {@link #LIT} state is true,
 * which drives both the plume and the block's light emission.</p>
 */
public class ThrusterBlock extends DirectionalBlock implements EntityBlock {
    public static final MapCodec<ThrusterBlock> CODEC = simpleCodec(ThrusterBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** Fuel (in ticks) granted per fuel item. */
    public static final int LAVA_FUEL = 20000;
    public static final int KEROSENE_FUEL = 2400;

    public ThrusterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Nozzle (and therefore the plume) points in the direction the player is looking.
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    /** Fuel value (in ticks) for a given item stack, or 0 if it is not a valid fuel. */
    public static int getFuelValue(ItemStack stack) {
        if (stack.is(Items.LAVA_BUCKET)) {
            return LAVA_FUEL;
        }
        if (stack.is(ExampleMod.KEROSENE.get())) {
            return KEROSENE_FUEL;
        }
        return 0;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        int fuel = getFuelValue(stack);
        if (fuel > 0 && level.getBlockEntity(pos) instanceof ThrusterBlockEntity be && !be.isCreative() && be.hasFuelRoom()) {
            if (!level.isClientSide) {
                be.addFuel(fuel);
                if (stack.is(Items.LAVA_BUCKET)) {
                    if (!player.getAbilities().instabuild) {
                        player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    }
                } else {
                    stack.consume(1, player);
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.6f, 1.4f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThrusterBlockEntity(ExampleMod.THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (lvl.isClientSide) {
                ((ThrusterBlockEntity) be).clientTick();
            } else {
                ((ThrusterBlockEntity) be).serverTick();
            }
        };
    }
}
