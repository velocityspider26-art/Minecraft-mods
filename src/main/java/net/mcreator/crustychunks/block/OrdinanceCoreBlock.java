package net.mcreator.crustychunks.block;

import net.mcreator.crustychunks.block.entity.OrdinanceCoreBlockEntity;
import net.mcreator.crustychunks.procedures.OrdinanceCorePowerProcedure;
import net.mcreator.crustychunks.procedures.OrdinanceCoreTriggerProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
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

public class OrdinanceCoreBlock extends Block implements EntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<AttachFace> FACE = FaceAttachedHorizontalDirectionalBlock.FACE;

   public OrdinanceCoreBlock() {
      super(
         Properties.of()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .sound(SoundType.ANVIL)
            .strength(1.0F, 10.0F)
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
                  yield Shapes.or(
                     box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0),
                     new VoxelShape[]{box(7.0, 5.0, 14.0, 9.0, 11.0, 16.0), box(7.0, 5.0, 0.0, 9.0, 11.0, 2.0)}
                  );
               case WALL:
                  yield Shapes.or(
                     box(2.0, 2.0, 0.0, 14.0, 14.0, 16.0),
                     new VoxelShape[]{box(7.0, 14.0, 5.0, 9.0, 16.0, 11.0), box(7.0, 0.0, 5.0, 9.0, 2.0, 11.0)}
                  );
               case CEILING:
                  yield Shapes.or(
                     box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0),
                     new VoxelShape[]{box(7.0, 5.0, 14.0, 9.0, 11.0, 16.0), box(7.0, 5.0, 0.0, 9.0, 11.0, 2.0)}
                  );
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case EAST -> {
            switch ((AttachFace)state.getValue(FACE)) {
               case FLOOR:
                  yield Shapes.or(
                     box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0),
                     new VoxelShape[]{box(0.0, 5.0, 7.0, 2.0, 11.0, 9.0), box(14.0, 5.0, 7.0, 16.0, 11.0, 9.0)}
                  );
               case WALL:
                  yield Shapes.or(
                     box(0.0, 2.0, 2.0, 16.0, 14.0, 14.0),
                     new VoxelShape[]{box(5.0, 14.0, 7.0, 11.0, 16.0, 9.0), box(5.0, 0.0, 7.0, 11.0, 2.0, 9.0)}
                  );
               case CEILING:
                  yield Shapes.or(
                     box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0),
                     new VoxelShape[]{box(0.0, 5.0, 7.0, 2.0, 11.0, 9.0), box(14.0, 5.0, 7.0, 16.0, 11.0, 9.0)}
                  );
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case WEST -> {
            switch ((AttachFace)state.getValue(FACE)) {
               case FLOOR:
                  yield Shapes.or(
                     box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0),
                     new VoxelShape[]{box(14.0, 5.0, 7.0, 16.0, 11.0, 9.0), box(0.0, 5.0, 7.0, 2.0, 11.0, 9.0)}
                  );
               case WALL:
                  yield Shapes.or(
                     box(0.0, 2.0, 2.0, 16.0, 14.0, 14.0),
                     new VoxelShape[]{box(5.0, 14.0, 7.0, 11.0, 16.0, 9.0), box(5.0, 0.0, 7.0, 11.0, 2.0, 9.0)}
                  );
               case CEILING:
                  yield Shapes.or(
                     box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0),
                     new VoxelShape[]{box(14.0, 5.0, 7.0, 16.0, 11.0, 9.0), box(0.0, 5.0, 7.0, 2.0, 11.0, 9.0)}
                  );
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         default -> {
            switch ((AttachFace)state.getValue(FACE)) {
               case FLOOR:
                  yield Shapes.or(
                     box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0),
                     new VoxelShape[]{box(7.0, 5.0, 0.0, 9.0, 11.0, 2.0), box(7.0, 5.0, 14.0, 9.0, 11.0, 16.0)}
                  );
               case WALL:
                  yield Shapes.or(
                     box(2.0, 2.0, 0.0, 14.0, 14.0, 16.0),
                     new VoxelShape[]{box(7.0, 14.0, 5.0, 9.0, 16.0, 11.0), box(7.0, 0.0, 5.0, 9.0, 2.0, 11.0)}
                  );
               case CEILING:
                  yield Shapes.or(
                     box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0),
                     new VoxelShape[]{box(7.0, 5.0, 0.0, 9.0, 11.0, 2.0), box(7.0, 5.0, 14.0, 9.0, 11.0, 16.0)}
                  );
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

   public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return true;
   }

   public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
      super.onPlace(blockstate, world, pos, oldState, moving);
      world.scheduleTick(pos, this, 1);
   }

   public void neighborChanged(BlockState blockstate, Level world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean moving) {
      super.neighborChanged(blockstate, world, pos, neighborBlock, fromPos, moving);
      if (world.getBestNeighborSignal(pos) > 0) {
         OrdinanceCorePowerProcedure.execute(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), blockstate);
      }
   }

   public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
      super.tick(blockstate, world, pos, random);
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      OrdinanceCoreTriggerProcedure.execute(world, (double)x, (double)y, (double)z, blockstate);
      world.scheduleTick(pos, this, 1);
   }

   public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
      return worldIn.getBlockEntity(pos) instanceof MenuProvider menuProvider ? menuProvider : null;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new OrdinanceCoreBlockEntity(pos, state);
   }

   public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
      super.triggerEvent(state, world, pos, eventID, eventParam);
      BlockEntity blockEntity = world.getBlockEntity(pos);
      return blockEntity == null ? false : blockEntity.triggerEvent(eventID, eventParam);
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.getBlock() != newState.getBlock()) {
         if (world.getBlockEntity(pos) instanceof OrdinanceCoreBlockEntity be) {
            Containers.dropContents(world, pos, be);
            world.updateNeighbourForOutputSignal(pos, this);
         }

         super.onRemove(state, world, pos, newState, isMoving);
      }
   }

   public boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
      return world.getBlockEntity(pos) instanceof OrdinanceCoreBlockEntity be ? AbstractContainerMenu.getRedstoneSignalFromContainer(be) : 0;
   }
}
