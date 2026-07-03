package net.mcreator.crustychunks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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

public class MachineGunBarrelBlock extends Block {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

   public MachineGunBarrelBlock() {
      super(Properties.of().sound(SoundType.ANVIL).strength(1.0F, 10.0F).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
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
         box(6.5, 6.5, 0.0, 9.5, 9.5, 16.0), new VoxelShape[]{box(6.6, 6.6, -14.9, 9.4, 9.4, 0.9), box(5.5, 6.5, -16.0, 10.5, 9.5, -9.0)}
      );
         case EAST -> Shapes.or(
         box(0.0, 6.5, 6.5, 16.0, 9.5, 9.5), new VoxelShape[]{box(15.1, 6.6, 6.6, 30.9, 9.4, 9.4), box(25.0, 6.5, 5.5, 32.0, 9.5, 10.5)}
      );
         case WEST -> Shapes.or(
         box(0.0, 6.5, 6.5, 16.0, 9.5, 9.5), new VoxelShape[]{box(-14.9, 6.6, 6.6, 0.9, 9.4, 9.4), box(-16.0, 6.5, 5.5, -9.0, 9.5, 10.5)}
      );
         default -> Shapes.or(
         box(6.5, 6.5, 0.0, 9.5, 9.5, 16.0), new VoxelShape[]{box(6.6, 6.6, 15.1, 9.4, 9.4, 30.9), box(5.5, 6.5, 25.0, 10.5, 9.5, 32.0)}
      );
      };
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }
}
