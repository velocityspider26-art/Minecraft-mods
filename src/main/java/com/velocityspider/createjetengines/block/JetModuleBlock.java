package com.velocityspider.createjetengines.block;

import com.velocityspider.createjetengines.blockentity.CombustionCoreBlockEntity;
import com.velocityspider.createjetengines.blockentity.JetModuleBlockEntity;
import com.velocityspider.createjetengines.engine.ModuleType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nullable;

/**
 * Shared behaviour for every jet engine module.
 *
 * <p>{@link #FACING} points <em>downstream</em>, toward the exhaust. Every module in one chain must
 * agree on it; {@code EngineScanner} treats a mismatched facing as a break in the chain.
 *
 * <p>Collision is a plain full cube on purpose. Detailed rotated voxel shapes are a known source of
 * placement and registration trouble, so the geometry stays in the model while the engine logic
 * settles.
 */
public abstract class JetModuleBlock extends Block {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    protected JetModuleBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    public abstract ModuleType getModuleType();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Exhaust points away from the placing player, along their line of sight.
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        invalidateEngineAt(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        invalidateEngineAt(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean replacingSelf = newState.is(this);
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!replacingSelf) {
            // Tell the rest of the chain before this block's entity is gone.
            invalidateEngineAt(level, pos);
        }
    }

    /**
     * Marks every module that could reasonably belong to a chain through {@code pos} as dirty.
     * The bound is deliberately tight: a maximum chain is fan + 4 compressors + core + afterburner
     * + nozzle, so nothing further than that along the axis can be affected.
     */
    private static void invalidateEngineAt(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockPos.MutableBlockPos cursor = pos.mutable();
            for (int i = 0; i < JetModuleBlockEntity.MAX_CHAIN_REACH; i++) {
                cursor.move(dir);
                BlockEntity be = level.getBlockEntity(cursor);
                if (be instanceof JetModuleBlockEntity module) {
                    module.invalidateStructure();
                } else if (!(level.getBlockState(cursor).getBlock() instanceof JetModuleBlock)) {
                    break;
                }
            }
        }
        if (level.getBlockEntity(pos) instanceof CombustionCoreBlockEntity core) {
            core.invalidateStructure();
        }
    }
}
