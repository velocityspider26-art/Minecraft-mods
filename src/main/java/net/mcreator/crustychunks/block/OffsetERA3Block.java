package net.mcreator.crustychunks.block;

import net.mcreator.crustychunks.procedures.ERAProcedureProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OffsetERA3Block extends Block {
   public static final DirectionProperty FACING = DirectionalBlock.FACING;

   public OffsetERA3Block() {
      super(Properties.of().sound(SoundType.ANVIL).strength(7.0F, 10.0F).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
      return true;
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
         box(1.0, 0.0, 7.0, 7.0, 7.0, 9.0),
         new VoxelShape[]{box(9.0, 0.0, 7.0, 15.0, 7.0, 9.0), box(9.0, 8.0, 7.0, 15.0, 15.0, 9.0), box(0.0, 0.0, 9.0, 16.0, 16.0, 16.0)}
      );
         case EAST -> Shapes.or(
         box(7.0, 0.0, 1.0, 9.0, 7.0, 7.0),
         new VoxelShape[]{box(7.0, 0.0, 9.0, 9.0, 7.0, 15.0), box(7.0, 8.0, 9.0, 9.0, 15.0, 15.0), box(0.0, 0.0, 0.0, 7.0, 16.0, 16.0)}
      );
         case WEST -> Shapes.or(
         box(7.0, 0.0, 9.0, 9.0, 7.0, 15.0),
         new VoxelShape[]{box(7.0, 0.0, 1.0, 9.0, 7.0, 7.0), box(7.0, 8.0, 1.0, 9.0, 15.0, 7.0), box(9.0, 0.0, 0.0, 16.0, 16.0, 16.0)}
      );
         case UP -> Shapes.or(
         box(1.0, 7.0, 0.0, 7.0, 9.0, 7.0),
         new VoxelShape[]{box(9.0, 7.0, 0.0, 15.0, 9.0, 7.0), box(9.0, 7.0, 8.0, 15.0, 9.0, 15.0), box(0.0, 0.0, 0.0, 16.0, 7.0, 16.0)}
      );
         case DOWN -> Shapes.or(
         box(1.0, 7.0, 9.0, 7.0, 9.0, 16.0),
         new VoxelShape[]{box(9.0, 7.0, 9.0, 15.0, 9.0, 16.0), box(9.0, 7.0, 1.0, 15.0, 9.0, 8.0), box(0.0, 9.0, 0.0, 16.0, 16.0, 16.0)}
      );
         default -> Shapes.or(
         box(9.0, 0.0, 7.0, 15.0, 7.0, 9.0),
         new VoxelShape[]{box(1.0, 0.0, 7.0, 7.0, 7.0, 9.0), box(1.0, 8.0, 7.0, 7.0, 15.0, 9.0), box(0.0, 0.0, 0.0, 16.0, 16.0, 7.0)}
      );
      };
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)super.getStateForPlacement(context).setValue(FACING, context.getNearestLookingDirection().getOpposite());
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   public void wasExploded(Level world, BlockPos pos, Explosion e) {
      super.wasExploded(world, pos, e);
      ERAProcedureProcedure.execute(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
   }
}
