package net.mcreator.crustychunks.block;

import net.mcreator.crustychunks.procedures.MediumBombDetonationProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OrdinanceIncendiaryWarheadBlock extends Block {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<AttachFace> FACE = FaceAttachedHorizontalDirectionalBlock.FACE;

   public OrdinanceIncendiaryWarheadBlock() {
      super(
         Properties.of()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .sound(SoundType.ANVIL)
            .strength(1.0F, 5.0F)
            .noOcclusion()
            .isRedstoneConductor((bs, br, bp) -> false)
      );
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(FACE, AttachFace.WALL));
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
         case NORTH -> {
            switch ((AttachFace)state.getValue(FACE)) {
               case FLOOR:
                  yield Shapes.or(box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0), box(5.0, 14.0, 5.0, 11.0, 16.0, 11.0));
               case WALL:
                  yield Shapes.or(box(2.0, 2.0, 2.0, 14.0, 14.0, 16.0), box(5.0, 5.0, 0.0, 11.0, 11.0, 2.0));
               case CEILING:
                  yield Shapes.or(box(2.0, 2.0, 2.0, 14.0, 16.0, 14.0), box(5.0, 0.0, 5.0, 11.0, 2.0, 11.0));
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case EAST -> {
            switch ((AttachFace)state.getValue(FACE)) {
               case FLOOR:
                  yield Shapes.or(box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0), box(5.0, 14.0, 5.0, 11.0, 16.0, 11.0));
               case WALL:
                  yield Shapes.or(box(0.0, 2.0, 2.0, 14.0, 14.0, 14.0), box(14.0, 5.0, 5.0, 16.0, 11.0, 11.0));
               case CEILING:
                  yield Shapes.or(box(2.0, 2.0, 2.0, 14.0, 16.0, 14.0), box(5.0, 0.0, 5.0, 11.0, 2.0, 11.0));
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case WEST -> {
            switch ((AttachFace)state.getValue(FACE)) {
               case FLOOR:
                  yield Shapes.or(box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0), box(5.0, 14.0, 5.0, 11.0, 16.0, 11.0));
               case WALL:
                  yield Shapes.or(box(2.0, 2.0, 2.0, 16.0, 14.0, 14.0), box(0.0, 5.0, 5.0, 2.0, 11.0, 11.0));
               case CEILING:
                  yield Shapes.or(box(2.0, 2.0, 2.0, 14.0, 16.0, 14.0), box(5.0, 0.0, 5.0, 11.0, 2.0, 11.0));
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         default -> {
            switch ((AttachFace)state.getValue(FACE)) {
               case FLOOR:
                  yield Shapes.or(box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0), box(5.0, 14.0, 5.0, 11.0, 16.0, 11.0));
               case WALL:
                  yield Shapes.or(box(2.0, 2.0, 0.0, 14.0, 14.0, 14.0), box(5.0, 5.0, 14.0, 11.0, 11.0, 16.0));
               case CEILING:
                  yield Shapes.or(box(2.0, 2.0, 2.0, 14.0, 16.0, 14.0), box(5.0, 0.0, 5.0, 11.0, 2.0, 11.0));
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
      };
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FACING, FACE});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)((BlockState)super.getStateForPlacement(context).setValue(FACE, this.faceForDirection(context.getNearestLookingDirection())))
         .setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   private AttachFace faceForDirection(Direction direction) {
      if (direction.getAxis() == Axis.Y) {
         return direction == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR;
      } else {
         return AttachFace.WALL;
      }
   }

   public void wasExploded(Level world, BlockPos pos, Explosion e) {
      super.wasExploded(world, pos, e);
      MediumBombDetonationProcedure.execute(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
   }
}
