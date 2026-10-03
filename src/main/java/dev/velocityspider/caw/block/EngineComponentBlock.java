package dev.velocityspider.caw.block;

import dev.velocityspider.caw.engine.EngineComponentType;
import dev.velocityspider.caw.engine.EngineTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Shared directional behavior for every modular jet-engine stage. */
public abstract class EngineComponentBlock extends DirectionalBlock {
    private final EngineComponentType componentType;
    private final EngineTier tier;

    protected EngineComponentBlock(EngineComponentType componentType, EngineTier tier, Properties properties) {
        super(properties);
        this.componentType = componentType;
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public EngineComponentType componentType() {
        return componentType;
    }

    public EngineTier tier() {
        return tier;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public static boolean matches(BlockGetter level, BlockPos pos, EngineComponentType expected, Direction facing) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof EngineComponentBlock block)) {
            return false;
        }
        return block.componentType() == expected && state.getValue(FACING) == facing;
    }
}
