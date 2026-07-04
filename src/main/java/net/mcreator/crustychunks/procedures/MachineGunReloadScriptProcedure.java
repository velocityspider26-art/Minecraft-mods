package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MachineGunReloadScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.HEAVY_MACHINE_GUN.get()) {
            if (!(new Object() {
               public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Loaded")) {
               if ((entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize") == 1.0
                  && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
                     == CrustyChunksModItems.MACHINE_GUN_BOX.get()) {
                  if (world instanceof Level _level) {
                     if (!_level.isClientSide()) {
                        _level.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           0.8F
                        );
                     } else {
                        _level.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           0.8F,
                           false
                        );
                     }
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bp = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntity = world.getBlockEntity(_bp);
                     BlockState _bs = world.getBlockState(_bp);
                     if (_blockEntity != null) {
                        _blockEntity.getPersistentData()
                           .putDouble(
                              "Ammo", (entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           );
                     }

                     if (world instanceof Level _levelx) {
                        _levelx.sendBlockUpdated(_bp, _bs, _bs, 3);
                     }
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bpx = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                     BlockState _bsx = world.getBlockState(_bpx);
                     if (_blockEntityx != null) {
                        _blockEntityx.getPersistentData().putBoolean("Loaded", true);
                     }

                     if (world instanceof Level _levelx) {
                        _levelx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                     }
                  }

                  (entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).setCount(0);
                  if (entity instanceof Player _player) {
                     _player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
                  }
               }
            } else if ((new Object() {
                  public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Loaded")
               && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getCount() == 0) {
               if (!world.isClientSide()) {
                  BlockPos _bpxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
                  BlockState _bsxx = world.getBlockState(_bpxx);
                  if (_blockEntityxx != null) {
                     _blockEntityxx.getPersistentData().putBoolean("Loaded", false);
                  }

                  if (world instanceof Level _levelx) {
                     _levelx.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
                  }
               }

               if (entity instanceof Player _player) {
                  _player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
               }

               if (entity instanceof LivingEntity _entity) {
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.MACHINE_GUN_BOX.get()).copy();
                  _setstack.setCount(1);
                  _entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
                  if (_entity instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               { final var _fvcc1 = (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo"); CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
               if (!world.isClientSide()) {
                  BlockPos _bpxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
                  BlockState _bsxxx = world.getBlockState(_bpxxx);
                  if (_blockEntityxxx != null) {
                     _blockEntityxxx.getPersistentData().putDouble("Ammo", 0.0);
                  }

                  if (world instanceof Level _levelx) {
                     _levelx.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
                  }
               }

               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("AmmoSize", 1.0));
               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.8F
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.8F,
                        false
                     );
                  }
               }
            }
         } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.MACHINE_GUN.get()
            || world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.MINIGUN.get()) {
            if (!(new Object() {
               public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Loaded")) {
               if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                     == CrustyChunksModItems.MACHINE_GUN_BOX.get()
                  && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize") == 0.0) {
                  if (world instanceof Level _levelxx) {
                     if (!_levelxx.isClientSide()) {
                        _levelxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           0.8F
                        );
                     } else {
                        _levelxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           0.8F,
                           false
                        );
                     }
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bpxxxx = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
                     BlockState _bsxxxx = world.getBlockState(_bpxxxx);
                     if (_blockEntityxxxx != null) {
                        _blockEntityxxxx.getPersistentData()
                           .putDouble(
                              "Ammo", (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           );
                     }

                     if (world instanceof Level _levelxxx) {
                        _levelxxx.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
                     }
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bpxxxxx = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
                     BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
                     if (_blockEntityxxxxx != null) {
                        _blockEntityxxxxx.getPersistentData().putBoolean("Loaded", true);
                     }

                     if (world instanceof Level _levelxxx) {
                        _levelxxx.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
                     }
                  }

                  (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).setCount(0);
                  if (entity instanceof Player _player) {
                     _player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
                  }
               }
            } else if ((new Object() {
                  public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Loaded")
               && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getCount() == 0) {
               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
                  BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
                  if (_blockEntityxxxxxx != null) {
                     _blockEntityxxxxxx.getPersistentData().putBoolean("Loaded", false);
                  }

                  if (world instanceof Level _levelxxx) {
                     _levelxxx.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
                  }
               }

               if (entity instanceof Player _player) {
                  _player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
               }

               if (entity instanceof LivingEntity _entityx) {
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.MACHINE_GUN_BOX.get()).copy();
                  _setstack.setCount(1);
                  _entityx.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
                  if (_entityx instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               { final var _fvcc1 = (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo"); CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("AmmoSize", 0.0));
               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxxx = world.getBlockEntity(_bpxxxxxxx);
                  BlockState _bsxxxxxxx = world.getBlockState(_bpxxxxxxx);
                  if (_blockEntityxxxxxxx != null) {
                     _blockEntityxxxxxxx.getPersistentData().putDouble("Ammo", 0.0);
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
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.8F
                     );
                  } else {
                     _levelxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.8F,
                        false
                     );
                  }
               }
            }
         } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.LIGHT_MACHINE_GUN.get()) {
            if (!(new Object() {
               public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Loaded")) {
               if ((entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).getItem()
                     == CrustyChunksModItems.MACHINE_GUN_BOX.get()
                  && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoSize") == -1.0) {
                  if (world instanceof Level _levelxxxx) {
                     if (!_levelxxxx.isClientSide()) {
                        _levelxxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           0.8F
                        );
                     } else {
                        _levelxxxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                           SoundSource.NEUTRAL,
                           1.0F,
                           0.8F,
                           false
                        );
                     }
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bpxxxxxxxx = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntityxxxxxxxx = world.getBlockEntity(_bpxxxxxxxx);
                     BlockState _bsxxxxxxxx = world.getBlockState(_bpxxxxxxxx);
                     if (_blockEntityxxxxxxxx != null) {
                        _blockEntityxxxxxxxx.getPersistentData()
                           .putDouble(
                              "Ammo", (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                           );
                     }

                     if (world instanceof Level _levelxxxxx) {
                        _levelxxxxx.sendBlockUpdated(_bpxxxxxxxx, _bsxxxxxxxx, _bsxxxxxxxx, 3);
                     }
                  }

                  if (!world.isClientSide()) {
                     BlockPos _bpxxxxxxxxx = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntityxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxx);
                     BlockState _bsxxxxxxxxx = world.getBlockState(_bpxxxxxxxxx);
                     if (_blockEntityxxxxxxxxx != null) {
                        _blockEntityxxxxxxxxx.getPersistentData().putBoolean("Loaded", true);
                     }

                     if (world instanceof Level _levelxxxxx) {
                        _levelxxxxx.sendBlockUpdated(_bpxxxxxxxxx, _bsxxxxxxxxx, _bsxxxxxxxxx, 3);
                     }
                  }

                  (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).setCount(0);
                  if (entity instanceof Player _player) {
                     _player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
                  }
               }
            } else if ((new Object() {
                  public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Loaded")
               && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getCount() == 0) {
               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxx);
                  BlockState _bsxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxx);
                  if (_blockEntityxxxxxxxxxx != null) {
                     _blockEntityxxxxxxxxxx.getPersistentData().putBoolean("Loaded", false);
                  }

                  if (world instanceof Level _levelxxxxx) {
                     _levelxxxxx.sendBlockUpdated(_bpxxxxxxxxxx, _bsxxxxxxxxxx, _bsxxxxxxxxxx, 3);
                  }
               }

               if (entity instanceof Player _player) {
                  _player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
               }

               if (entity instanceof LivingEntity _entityxx) {
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.MACHINE_GUN_BOX.get()).copy();
                  _setstack.setCount(1);
                  _entityxx.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
                  if (_entityxx instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               { final var _fvcc1 = (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Ammo"); CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("AmmoSize", -1.0));
               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxx);
                  BlockState _bsxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxx);
                  if (_blockEntityxxxxxxxxxxx != null) {
                     _blockEntityxxxxxxxxxxx.getPersistentData().putDouble("Ammo", 0.0);
                  }

                  if (world instanceof Level _levelxxxxx) {
                     _levelxxxxx.sendBlockUpdated(_bpxxxxxxxxxxx, _bsxxxxxxxxxxx, _bsxxxxxxxxxxx, 3);
                  }
               }

               if (world instanceof Level _levelxxxxx) {
                  if (!_levelxxxxx.isClientSide()) {
                     _levelxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.8F
                     );
                  } else {
                     _levelxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:pistolaction")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        0.8F,
                        false
                     );
                  }
               }
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("MachineGunReloadScriptProcedure.execute", _wtSafe);
      }
   }
}
