package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class AimerProcedureProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      if (entity != null) {
         double Multiplier = 0.0;
         if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:cannon")))) {
            if ((new Object() {
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
            }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))) != Direction.NORTH && (new Object() {
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
            }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))) != Direction.EAST) {
               Multiplier = -1.0;
            } else {
               Multiplier = 1.0;
            }

            if (!world.isClientSide()) {
               BlockPos _bp = BlockPos.containing(x, y, z);
               BlockEntity _blockEntity = world.getBlockEntity(_bp);
               BlockState _bs = world.getBlockState(_bp);
               if (_blockEntity != null) {
                  _blockEntity.getPersistentData().putDouble("Pitch", Math.tan(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Pitch") * (Math.PI / 180.0)));
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bp, _bs, _bs, 3);
               }
            }

            if ((new Object() {
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
            }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepX() == 0) {
               if (!world.isClientSide()) {
                  BlockPos _bpx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                  BlockState _bsx = world.getBlockState(_bpx);
                  if (_blockEntityx != null) {
                     _blockEntityx.getPersistentData().putDouble("X", Math.tan(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Yaw") * (Math.PI / 180.0)) * Multiplier);
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxx = BlockPos.containing(x, y, z);
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

            if ((new Object() {
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
            }).getDirection(world.getBlockState(BlockPos.containing(x, y, z))).getStepZ() == 0) {
               if (!world.isClientSide()) {
                  BlockPos _bpxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
                  BlockState _bsxxx = world.getBlockState(_bpxxx);
                  if (_blockEntityxxx != null) {
                     _blockEntityxxx.getPersistentData().putDouble("Z", Math.tan(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Yaw") * (Math.PI / 180.0)) * Multiplier);
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxx = BlockPos.containing(x, y, z);
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
         } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.AIMER_NODE.get()) {
            if (!world.isClientSide()) {
               BlockPos _bpxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
               BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
               if (_blockEntityxxxxx != null) {
                  _blockEntityxxxxx.getPersistentData().putDouble("Yaw", itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Yaw"));
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
               }
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
               BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
               if (_blockEntityxxxxxx != null) {
                  _blockEntityxxxxxx.getPersistentData().putDouble("Pitch", itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Pitch"));
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
               }
            }
         } else {
            DirectionUpdateProcedure.execute(world, entity, itemstack);
         }
      }
   }
}
