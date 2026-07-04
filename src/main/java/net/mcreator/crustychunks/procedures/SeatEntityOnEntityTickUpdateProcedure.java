package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SeatEntityOnEntityTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if (!entity.isVehicle()) {
            if (!entity.level().isClientSide()) {
               entity.discard();
            }
         } else if ((entity.getFirstPassenger() instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.AIMER.get()
            || world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.MANUAL_AIMER.get()) {
            CrustyChunksMod.queueServerWork(
               1,
               () -> {
                  if (entity.isVehicle()) {
                     if ((entity.getFirstPassenger() instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getItem()
                           == CrustyChunksModItems.AIMER.get()
                        && (entity.getFirstPassenger() instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Mode")) {
                        { final var _fvcc1 = (double)Math.min(Math.max(-22.0F, (entity.getFirstPassenger().getXRot() - entity.getXRot()) * -1.0F), 45.0F); CustomData.update(DataComponents.CUSTOM_DATA, (entity.getFirstPassenger() instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Pitch", _fvcc1)); }
                        if (180.0F < entity.getFirstPassenger().getYRot() - entity.getYRot()) {
                           { final var _fvcc1 = (double)Math.min(Math.max(-30.0F, (360.0F - (entity.getFirstPassenger().getYRot() - entity.getYRot())) * -1.0F), 30.0F); CustomData.update(DataComponents.CUSTOM_DATA, (entity.getFirstPassenger() instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Yaw", _fvcc1)); }
                        } else {
                           { final var _fvcc1 = (double)Math.min(Math.max(-30.0F, entity.getFirstPassenger().getYRot() - entity.getYRot()), 30.0F); CustomData.update(DataComponents.CUSTOM_DATA, (entity.getFirstPassenger() instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Yaw", _fvcc1)); }
                        }
                     }

                     if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.MANUAL_AIMER.get()) {
                        if (!world.isClientSide()) {
                           BlockPos _bp = BlockPos.containing(x, y, z);
                           BlockEntity _blockEntity = world.getBlockEntity(_bp);
                           BlockState _bs = world.getBlockState(_bp);
                           if (_blockEntity != null) {
                              _blockEntity.getPersistentData()
                                 .putDouble("Pitch", (double)Math.min(Math.max(-22.0F, (entity.getFirstPassenger().getXRot() - entity.getXRot()) * -1.0F), 45.0F));
                           }

                           if (world instanceof Level _level) {
                              _level.sendBlockUpdated(_bp, _bs, _bs, 3);
                           }
                        }

                        if (!world.isClientSide()) {
                           BlockPos _bpx = BlockPos.containing(x, y, z);
                           BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                           BlockState _bsx = world.getBlockState(_bpx);
                           if (_blockEntityx != null) {
                              _blockEntityx.getPersistentData().putDouble("Updated", 5.0);
                           }

                           if (world instanceof Level _level) {
                              _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                           }
                        }

                        if (180.0F < entity.getFirstPassenger().getYRot() - entity.getYRot()) {
                           if (!world.isClientSide()) {
                              BlockPos _bpxx = BlockPos.containing(x, y, z);
                              BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
                              BlockState _bsxx = world.getBlockState(_bpxx);
                              if (_blockEntityxx != null) {
                                 _blockEntityxx.getPersistentData()
                                    .putDouble(
                                       "Yaw",
                                       (double)Math.min(Math.max(-30.0F, (360.0F - (entity.getFirstPassenger().getYRot() - entity.getYRot())) * -1.0F), 30.0F)
                                    );
                              }

                              if (world instanceof Level _level) {
                                 _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
                              }
                           }
                        } else if (!world.isClientSide()) {
                           BlockPos _bpxxx = BlockPos.containing(x, y, z);
                           BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
                           BlockState _bsxxx = world.getBlockState(_bpxxx);
                           if (_blockEntityxxx != null) {
                              _blockEntityxxx.getPersistentData()
                                 .putDouble("Yaw", (double)Math.min(Math.max(-30.0F, entity.getFirstPassenger().getYRot() - entity.getYRot()), 30.0F));
                           }

                           if (world instanceof Level _level) {
                              _level.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
                           }
                        }
                     }
                  }
               }
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SeatEntityOnEntityTickUpdateProcedure.execute", _wtSafe);
      }
   }
}
