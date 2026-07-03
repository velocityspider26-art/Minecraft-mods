package net.mcreator.crustychunks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CoveredFlameThrowerBarrelBlock extends Block implements SimpleWaterloggedBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, 4);

   public CoveredFlameThrowerBarrelBlock() {
      super(Properties.of().sound(SoundType.ANVIL).strength(1.0F, 10.0F).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(DAMAGE, 0))
            .setValue(WATERLOGGED, false)
      );
   }

   public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
      return state.getFluidState().isEmpty();
   }

   public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 0;
   }

   public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return switch ((Direction)state.getValue(FACING)) {
         case NORTH -> Shapes.or(
         box(6.0, 6.0, -7.0, 10.0, 10.0, -1.0),
         new VoxelShape[]{
            box(7.0, 7.0, -6.0, 9.0, 9.0, 9.0),
            box(7.0, 4.0, -5.0, 9.0, 6.0, 8.0),
            box(7.0, 7.0, -7.0, 9.0, 9.0, -6.0),
            box(0.0, 0.0, 8.0, 16.0, 16.0, 12.0),
            box(0.0, 0.0, 12.0, 16.0, 16.0, 16.0),
            box(4.0, 4.0, 0.0, 12.0, 12.0, 8.0)
         }
      );
         case EAST -> Shapes.or(
         box(17.0, 6.0, 6.0, 23.0, 10.0, 10.0),
         new VoxelShape[]{
            box(7.0, 7.0, 7.0, 22.0, 9.0, 9.0),
            box(8.0, 4.0, 7.0, 21.0, 6.0, 9.0),
            box(22.0, 7.0, 7.0, 23.0, 9.0, 9.0),
            box(4.0, 0.0, 0.0, 8.0, 16.0, 16.0),
            box(0.0, 0.0, 0.0, 4.0, 16.0, 16.0),
            box(8.0, 4.0, 4.0, 16.0, 12.0, 12.0)
         }
      );
         case WEST -> Shapes.or(
         box(-7.0, 6.0, 6.0, -1.0, 10.0, 10.0),
         new VoxelShape[]{
            box(-6.0, 7.0, 7.0, 9.0, 9.0, 9.0),
            box(-5.0, 4.0, 7.0, 8.0, 6.0, 9.0),
            box(-7.0, 7.0, 7.0, -6.0, 9.0, 9.0),
            box(8.0, 0.0, 0.0, 12.0, 16.0, 16.0),
            box(12.0, 0.0, 0.0, 16.0, 16.0, 16.0),
            box(0.0, 4.0, 4.0, 8.0, 12.0, 12.0)
         }
      );
         default -> Shapes.or(
         box(6.0, 6.0, 17.0, 10.0, 10.0, 23.0),
         new VoxelShape[]{
            box(7.0, 7.0, 7.0, 9.0, 9.0, 22.0),
            box(7.0, 4.0, 8.0, 9.0, 6.0, 21.0),
            box(7.0, 7.0, 22.0, 9.0, 9.0, 23.0),
            box(0.0, 0.0, 4.0, 16.0, 16.0, 8.0),
            box(0.0, 0.0, 0.0, 16.0, 16.0, 4.0),
            box(4.0, 4.0, 8.0, 12.0, 12.0, 16.0)
         }
      );
      };
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FACING, DAMAGE, WATERLOGGED});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      boolean flag = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
      return (BlockState)((BlockState)((BlockState)super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite())).setValue(DAMAGE, 0))
         .setValue(WATERLOGGED, flag);
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
      if ((Boolean)state.getValue(WATERLOGGED)) {
         world.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
      }

      return super.updateShape(state, facing, facingState, world, currentPos, facingPos);
   }
}
