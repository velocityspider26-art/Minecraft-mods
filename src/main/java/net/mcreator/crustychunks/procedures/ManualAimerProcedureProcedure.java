package net.mcreator.crustychunks.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class ManualAimerProcedureProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      try {
      boolean found = false;
      double FX = 0.0;
      double FY = 0.0;
      double FZ = 0.0;
      double sx = 0.0;
      double Xvector = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      double Zvector = 0.0;
      double Multiplier = 0.0;
      double Pitch = 0.0;
      Direction fdirection = Direction.NORTH;
      found = false;
      sx = (double)(new Object() {
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
      }).getDirection(blockstate).getStepX();
      sy = (double)(new Object() {
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
      }).getDirection(blockstate).getStepY();
      sz = (double)(new Object() {
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
      }).getDirection(blockstate).getStepZ();
      if (world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:cannon")))) {
         FX = x + sx;
         FY = y + sy;
         FZ = z + sz;
         fdirection = (new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(FX, FY, FZ)));
         if (fdirection != Direction.NORTH && fdirection != Direction.EAST) {
            Multiplier = -1.0;
         } else {
            Multiplier = 1.0;
         }

         if (!world.isClientSide()) {
            BlockPos _bp = BlockPos.containing(FX, FY, FZ);
            BlockEntity _blockEntity = world.getBlockEntity(_bp);
            BlockState _bs = world.getBlockState(_bp);
            if (_blockEntity != null) {
               _blockEntity.getPersistentData().putDouble("Pitch", Math.tan((new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Pitch") * (Math.PI / 180.0)));
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bp, _bs, _bs, 3);
            }
         }

         if (fdirection.getStepX() == 0) {
            if (!world.isClientSide()) {
               BlockPos _bpx = BlockPos.containing(FX, FY, FZ);
               BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
               BlockState _bsx = world.getBlockState(_bpx);
               if (_blockEntityx != null) {
                  _blockEntityx.getPersistentData().putDouble("X", Math.tan((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Yaw") * (Math.PI / 180.0)) * Multiplier);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
               }
            }

            if (!world.isClientSide()) {
               BlockPos _bpxx = BlockPos.containing(FX, FY, FZ);
               BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
               BlockState _bsxx = world.getBlockState(_bpxx);
               if (_blockEntityxx != null) {
                  _blockEntityxx.getPersistentData().putDouble("Z", 0.0);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
               }
            }
         }

         if (fdirection.getStepZ() == 0) {
            if (!world.isClientSide()) {
               BlockPos _bpxxx = BlockPos.containing(FX, FY, FZ);
               BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
               BlockState _bsxxx = world.getBlockState(_bpxxx);
               if (_blockEntityxxx != null) {
                  _blockEntityxxx.getPersistentData().putDouble("Z", Math.tan((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Yaw") * (Math.PI / 180.0)) * Multiplier);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
               }
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxx = BlockPos.containing(FX, FY, FZ);
               BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
               BlockState _bsxxxx = world.getBlockState(_bpxxxx);
               if (_blockEntityxxxx != null) {
                  _blockEntityxxxx.getPersistentData().putDouble("X", 0.0);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
               }
            }
         }

         if (1.0 >= Math.abs((double)fdirection.getStepX() - (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(FX, FY, FZ), "X")) || 0.5 >= Math.abs((double)fdirection.getStepZ() - (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(FX, FY, FZ), "Z"))) {
            Pitch = (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(FX, FY, FZ), "Pitch") + 0.05;
            Xvector = (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(FX, FY, FZ), "X") + (double)fdirection.getStepX();
            Zvector = (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(FX, FY, FZ), "Z") + (double)fdirection.getStepZ();
         }

         if (0.0 == Xvector * Zvector) {
            Xvector = (double)fdirection.getStepX();
            Zvector = (double)fdirection.getStepZ();
         }
      }

      if ((blockstate.getBlock().getStateDefinition().getProperty("firing") instanceof IntegerProperty _getip37 ? (Integer)blockstate.getValue(_getip37) : -1) > 0) {
         int _value = (blockstate.getBlock().getStateDefinition().getProperty("firing") instanceof IntegerProperty _getip39 ? (Integer)blockstate.getValue(_getip39) : -1)
            - 1;
         BlockPos _pos = BlockPos.containing(x, y, z);
         BlockState _bsxxxxx = world.getBlockState(_pos);
         if (_bsxxxxx.getBlock().getStateDefinition().getProperty("firing") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value)) {
            world.setBlock(_pos, (BlockState)_bsxxxxx.setValue(_integerProp, _value), 3);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ManualAimerProcedureProcedure.execute", _wtSafe);
      }
   }
}
