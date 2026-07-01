package com.example.examplemod.content.thruster;

import com.example.examplemod.ExampleMod;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.jetbrains.annotations.Nullable;

/**
 * A directional thruster block. Unlike the original Create: Propulsion thruster (which spawns
 * exhaust particles), this variant is rendered with a procedural 3D plume mesh by
 * {@link com.example.examplemod.client.ThrusterPlumeRenderer}. The plume fires out of the
 * {@link DirectionalBlock#FACING} side and its length/intensity scale with the redstone signal
 * feeding the block.
 */
public class ThrusterBlock extends DirectionalBlock implements EntityBlock {
    public static final MapCodec<ThrusterBlock> CODEC = simpleCodec(ThrusterBlock::new);

    public ThrusterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Nozzle points away from the player, like a piston head.
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
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
        // The plume is purely visual, so we only need a client-side smoothing tick.
        return level.isClientSide ? (lvl, pos, st, be) -> ((ThrusterBlockEntity) be).clientTick() : null;
    }
}
