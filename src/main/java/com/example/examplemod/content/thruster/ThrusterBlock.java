package com.example.examplemod.content.thruster;

import com.example.examplemod.ExampleMod;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A directional thruster block modelled after the Create: Propulsion Simulated thruster, with the
 * particle exhaust replaced by a procedural 3D plume mesh
 * ({@link com.example.examplemod.client.ThrusterPlumeRenderer}).
 *
 * <p>The plume fires out of the {@link DirectionalBlock#FACING} (nozzle) side. This thruster needs
 * fuel: pipe fuel items into it with a funnel/hopper (it exposes an item-handler capability, just
 * like the Create thruster) or right-click it with a fuel item. While it has fuel it burns and the
 * {@link #LIT} state is true, which drives both the plume and the block's light emission.</p>
 *
 * <p>Right-clicking with a wrench cycles the {@link #PLUME} look through the five real-world-inspired
 * variants (sneak to cycle backwards).</p>
 */
public class ThrusterBlock extends DirectionalBlock implements EntityBlock {
    public static final MapCodec<ThrusterBlock> CODEC = simpleCodec(ThrusterBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<PlumeType> PLUME = EnumProperty.create("plume", PlumeType.class);

    public ThrusterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, Boolean.FALSE)
                .setValue(PLUME, PlumeType.KEROLOX));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, PLUME);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Nozzle (and therefore the plume) points in the direction the player is looking.
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    private static boolean isWrench(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getPath().equals("wrench");
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Wrench: cycle the plume variant (sneak = backwards). Runs before the wrench's own useOn.
        if (isWrench(stack)) {
            if (!level.isClientSide) {
                PlumeType current = state.getValue(PLUME);
                PlumeType next = player.isShiftKeyDown() ? current.previous() : current.next();
                level.setBlock(pos, state.setValue(PLUME, next), 3);
                level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.9f, 1.2f);
                player.displayClientMessage(Component.literal("Plume: " + next.getDisplayName()), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Fuel: let the player fill the tank from a held bucket (lava or kerosene).
        if (level.getBlockEntity(pos) instanceof ThrusterBlockEntity be && !be.isCreative()
                && FluidUtil.interactWithFluidHandler(player, hand, be.getFuelTank())) {
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
