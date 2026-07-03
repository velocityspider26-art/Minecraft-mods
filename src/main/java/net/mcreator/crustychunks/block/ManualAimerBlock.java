package net.mcreator.crustychunks.block;

import net.mcreator.crustychunks.block.entity.ManualAimerBlockEntity;
import net.mcreator.crustychunks.procedures.ManualAimerEmittedRedstonePowerProcedure;
import net.mcreator.crustychunks.procedures.ManualAimerProcedureProcedure;
import net.mcreator.crustychunks.procedures.PassengerSeatOnBlockRightClickedProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ManualAimerBlock extends Block implements EntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final IntegerProperty FIRING = IntegerProperty.create("firing", 0, 5);

   public ManualAimerBlock() {
      super(Properties.of().sound(SoundType.WOOL).strength(1.0F, 10.0F).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(FIRING, 0));
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
         box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
         new VoxelShape[]{
            box(7.0, 0.0, 0.0, 9.0, 6.0, 2.0),
            box(-0.25, 7.75, -0.25, 2.25, 15.25, 2.25),
            box(13.75, 7.75, -0.25, 16.25, 15.25, 2.25),
            box(0.0, 6.0, 0.0, 16.0, 8.0, 2.0)
         }
      );
         case EAST -> Shapes.or(
         box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
         new VoxelShape[]{
            box(14.0, 0.0, 7.0, 16.0, 6.0, 9.0),
            box(13.75, 7.75, -0.25, 16.25, 15.25, 2.25),
            box(13.75, 7.75, 13.75, 16.25, 15.25, 16.25),
            box(14.0, 6.0, 0.0, 16.0, 8.0, 16.0)
         }
      );
         case WEST -> Shapes.or(
         box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
         new VoxelShape[]{
            box(0.0, 0.0, 7.0, 2.0, 6.0, 9.0),
            box(-0.25, 7.75, 13.75, 2.25, 15.25, 16.25),
            box(-0.25, 7.75, -0.25, 2.25, 15.25, 2.25),
            box(0.0, 6.0, 0.0, 2.0, 8.0, 16.0)
         }
      );
         default -> Shapes.or(
         box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
         new VoxelShape[]{
            box(7.0, 0.0, 14.0, 9.0, 6.0, 16.0),
            box(13.75, 7.75, 13.75, 16.25, 15.25, 16.25),
            box(-0.25, 7.75, 13.75, 2.25, 15.25, 16.25),
            box(0.0, 6.0, 14.0, 16.0, 8.0, 16.0)
         }
      );
      };
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FACING, FIRING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)((BlockState)super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite())).setValue(FIRING, 0);
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   public boolean isSignalSource(BlockState state) {
      return true;
   }

   public int getSignal(BlockState blockstate, BlockGetter blockAccess, BlockPos pos, Direction direction) {
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      Level world = (Level)blockAccess;
      return (int)ManualAimerEmittedRedstonePowerProcedure.execute(blockstate);
   }

   public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
      super.onPlace(blockstate, world, pos, oldState, moving);
      world.scheduleTick(pos, this, 1);
   }

   public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
      super.tick(blockstate, world, pos, random);
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      ManualAimerProcedureProcedure.execute(world, (double)x, (double)y, (double)z, blockstate);
      world.scheduleTick(pos, this, 1);
   }

   public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
      InteractionHand hand = InteractionHand.MAIN_HAND;
      super.useWithoutItem(blockstate, world, pos, entity, hit);
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      double hitX = hit.getLocation().x;
      double hitY = hit.getLocation().y;
      double hitZ = hit.getLocation().z;
      Direction direction = hit.getDirection();
      PassengerSeatOnBlockRightClickedProcedure.execute(world, (double)x, (double)y, (double)z, entity);
      return InteractionResult.SUCCESS;
   }

   public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
      return worldIn.getBlockEntity(pos) instanceof MenuProvider menuProvider ? menuProvider : null;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ManualAimerBlockEntity(pos, state);
   }

   public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
      super.triggerEvent(state, world, pos, eventID, eventParam);
      BlockEntity blockEntity = world.getBlockEntity(pos);
      return blockEntity == null ? false : blockEntity.triggerEvent(eventID, eventParam);
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.getBlock() != newState.getBlock()) {
         if (world.getBlockEntity(pos) instanceof ManualAimerBlockEntity be) {
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
      return world.getBlockEntity(pos) instanceof ManualAimerBlockEntity be ? AbstractContainerMenu.getRedstoneSignalFromContainer(be) : 0;
   }
}
