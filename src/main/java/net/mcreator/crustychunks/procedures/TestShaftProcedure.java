package net.mcreator.crustychunks.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class TestShaftProcedure {
   public static double execute(LevelAccessor world, double x, double y, double z, BlockState blockstate, Direction inputdirection) {
      if (inputdirection == null) {
         return 0.0;
      } else {
         boolean allow = false;
         Direction selfdirection = Direction.NORTH;
         Direction targetdirection = Direction.NORTH;
         BlockState targetblock = Blocks.AIR.defaultBlockState();
         double targx = 0.0;
         double targy = 0.0;
         double targz = 0.0;
         double power = 0.0;
         if (inputdirection != null) {
            selfdirection = inputdirection;
         } else {
            selfdirection = (new Object() {
               public Direction getDirection(BlockState _bs) {
                  if (_bs.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty _dp) {
                     return (Direction)_bs.getValue(_dp);
                  } else {
                     if (_bs.getBlock().getStateDefinition().getProperty("axis") instanceof EnumProperty _ep && _ep.getPossibleValues().toArray()[0] instanceof Axis) {
                        return Direction.fromAxisAndDirection((Axis)_bs.getValue(_ep), AxisDirection.POSITIVE);
                     }

                     return Direction.NORTH;
                  }
               }
            }).getDirection(blockstate);
         }

         targetblock = world.getBlockState(
            BlockPos.containing(x - (double)selfdirection.getStepX(), y - (double)selfdirection.getStepY(), z - (double)selfdirection.getStepZ())
         );
         targetdirection = (new Object() {
               public Direction getDirection(BlockState _bs) {
                  if (_bs.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty _dp) {
                     return (Direction)_bs.getValue(_dp);
                  } else {
                     if (_bs.getBlock().getStateDefinition().getProperty("axis") instanceof EnumProperty _ep && _ep.getPossibleValues().toArray()[0] instanceof Axis) {
                        return Direction.fromAxisAndDirection((Axis)_bs.getValue(_ep), AxisDirection.POSITIVE);
                     }

                     return Direction.NORTH;
                  }
               }
            })
            .getDirection(
               world.getBlockState(
                  BlockPos.containing(x - (double)selfdirection.getStepX(), y - (double)selfdirection.getStepY(), z - (double)selfdirection.getStepZ())
               )
            );
         targx = x - (double)selfdirection.getStepX();
         targy = y - (double)selfdirection.getStepY();
         targz = z - (double)selfdirection.getStepZ();
         if (targetblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:kineticoutputtop")))
            && targetblock.getBlock() == world.getBlockState(BlockPos.containing(x, y - 1.0, z)).getBlock()) {
            allow = true;
         }

         if (targetblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:kineticoutputrear"))) && selfdirection == targetdirection.getOpposite()) {
            allow = true;
         }

         if (targetblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:kineticoutputfront"))) && selfdirection == targetdirection) {
            allow = true;
         }

         if (targetblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:kineticoutputbottom")))
            && targetblock.getBlock() == world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock()) {
            allow = true;
         }

         if (targetblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:kineticoutputclock")))
            && selfdirection == targetdirection.getClockWise(Axis.Y)) {
            allow = true;
         }

         if (targetblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:kineticoutputcounter")))
            && selfdirection == targetdirection.getCounterClockWise(Axis.Y)) {
            allow = true;
         }

         if (allow) {
            if (0.0 < (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(targx, targy, targz), "GearboxPower")) {
               power = (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(targx, targy, targz), "GearboxPower");
            } else {
               power = (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(targx, targy, targz), "KineticPower");
            }
         } else {
            power = 0.0;
         }

         return power;
      }
   }
}
