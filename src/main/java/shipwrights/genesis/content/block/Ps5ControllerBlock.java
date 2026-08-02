package shipwrights.genesis.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class Ps5ControllerBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Ps5ControllerBlock> CODEC = simpleCodec(Ps5ControllerBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = createShapes();

    public Ps5ControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    private static Map<Direction, VoxelShape> createShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.NORTH, Shapes.or(
                Block.box(2.0, 2.0, 4.0, 14.0, 7.5, 12.0),
                Block.box(2.0, 0.0, 8.0, 6.0, 5.0, 15.0),
                Block.box(10.0, 0.0, 8.0, 14.0, 5.0, 15.0)));
        shapes.put(Direction.SOUTH, Shapes.or(
                Block.box(2.0, 2.0, 4.0, 14.0, 7.5, 12.0),
                Block.box(2.0, 0.0, 1.0, 6.0, 5.0, 8.0),
                Block.box(10.0, 0.0, 1.0, 14.0, 5.0, 8.0)));
        shapes.put(Direction.EAST, Shapes.or(
                Block.box(4.0, 2.0, 2.0, 12.0, 7.5, 14.0),
                Block.box(1.0, 0.0, 2.0, 8.0, 5.0, 6.0),
                Block.box(1.0, 0.0, 10.0, 8.0, 5.0, 14.0)));
        shapes.put(Direction.WEST, Shapes.or(
                Block.box(4.0, 2.0, 2.0, 12.0, 7.5, 14.0),
                Block.box(8.0, 0.0, 2.0, 15.0, 5.0, 6.0),
                Block.box(8.0, 0.0, 10.0, 15.0, 5.0, 14.0)));
        return shapes;
    }
}
