package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ArtilleryReloadScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if (!(new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Loaded")) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.ARTILLERY_SHELL.get()) {
               (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).setCount(0);
               if (!world.isClientSide()) {
                  BlockPos _bp = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntity = world.getBlockEntity(_bp);
                  BlockState _bs = world.getBlockState(_bp);
                  if (_blockEntity != null) {
                     _blockEntity.getPersistentData().putBoolean("Loaded", true);
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
                     _blockEntityx.getPersistentData().putBoolean("HE", true);
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                  }
               }

               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.4F
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.4F,
                        false
                     );
                  }
               }
            } else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.ARTILLERY_SOLID_SHELL.get()) {
               (entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).setCount(0);
               if (!world.isClientSide()) {
                  BlockPos _bpxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
                  BlockState _bsxx = world.getBlockState(_bpxx);
                  if (_blockEntityxx != null) {
                     _blockEntityxx.getPersistentData().putBoolean("Loaded", true);
                  }

                  if (world instanceof Level _levelx) {
                     _levelx.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
                  BlockState _bsxxx = world.getBlockState(_bpxxx);
                  if (_blockEntityxxx != null) {
                     _blockEntityxxx.getPersistentData().putBoolean("AP", true);
                  }

                  if (world instanceof Level _levelx) {
                     _levelx.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
                  }
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.4F
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.4F,
                        false
                     );
                  }
               }
            } else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.GAS_ARTILLERY_SHELL.get()) {
               (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).setCount(0);
               if (!world.isClientSide()) {
                  BlockPos _bpxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
                  BlockState _bsxxxx = world.getBlockState(_bpxxxx);
                  if (_blockEntityxxxx != null) {
                     _blockEntityxxxx.getPersistentData().putBoolean("Loaded", true);
                  }

                  if (world instanceof Level _levelxx) {
                     _levelxx.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
                  BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
                  if (_blockEntityxxxxx != null) {
                     _blockEntityxxxxx.getPersistentData().putBoolean("GAS", true);
                  }

                  if (world instanceof Level _levelxx) {
                     _levelxx.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
                  }
               }

               if (world instanceof Level _levelxx) {
                  if (!_levelxx.isClientSide()) {
                     _levelxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.4F
                     );
                  } else {
                     _levelxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.4F,
                        false
                     );
                  }
               }
            } else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
               == CrustyChunksModItems.FIRE_ARTILLERY_SHELL.get()) {
               (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).setCount(0);
               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
                  BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
                  if (_blockEntityxxxxxx != null) {
                     _blockEntityxxxxxx.getPersistentData().putBoolean("Loaded", true);
                  }

                  if (world instanceof Level _levelxxx) {
                     _levelxxx.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxxx = world.getBlockEntity(_bpxxxxxxx);
                  BlockState _bsxxxxxxx = world.getBlockState(_bpxxxxxxx);
                  if (_blockEntityxxxxxxx != null) {
                     _blockEntityxxxxxxx.getPersistentData().putBoolean("FIRE", true);
                  }

                  if (world instanceof Level _levelxxx) {
                     _levelxxx.sendBlockUpdated(_bpxxxxxxx, _bsxxxxxxx, _bsxxxxxxx, 3);
                  }
               }

               if (world instanceof Level _levelxxx) {
                  if (!_levelxxx.isClientSide()) {
                     _levelxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.4F
                     );
                  } else {
                     _levelxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.open")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.4F,
                        false
                     );
                  }
               }
            }
         } else if (!(new Object() {
               public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Charge")
            && (new Object() {
               public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Loaded")
            && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.POWDER_CHARGE.get()) {
            (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).setCount(0);
            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxx = world.getBlockEntity(_bpxxxxxxxx);
               BlockState _bsxxxxxxxx = world.getBlockState(_bpxxxxxxxx);
               if (_blockEntityxxxxxxxx != null) {
                  _blockEntityxxxxxxxx.getPersistentData().putBoolean("Charge", true);
               }

               if (world instanceof Level _levelxxxx) {
                  _levelxxxx.sendBlockUpdated(_bpxxxxxxxx, _bsxxxxxxxx, _bsxxxxxxxx, 3);
               }
            }

            if (world instanceof Level _levelxxxx) {
               if (!_levelxxxx.isClientSide()) {
                  _levelxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.open")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F
                  );
               } else {
                  _levelxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_door.open")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.4F,
                     false
                  );
               }
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ArtilleryReloadScriptProcedure.execute", _wtSafe);
      }
   }
}
