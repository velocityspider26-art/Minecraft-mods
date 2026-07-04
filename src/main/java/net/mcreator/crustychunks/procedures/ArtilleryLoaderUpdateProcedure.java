package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class ArtilleryLoaderUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      boolean found = false;
      double Ammunition = 0.0;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      if (!world.isClientSide()) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockEntity _blockEntity = world.getBlockEntity(_bp);
         BlockState _bs = world.getBlockState(_bp);
         if (_blockEntity != null) {
            _blockEntity.getPersistentData().putBoolean("Greenlight", true);
         }

         if (world instanceof Level _level) {
            _level.sendBlockUpdated(_bp, _bs, _bs, 3);
         }
      }

      if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == CrustyChunksModBlocks.ARTILLERYBREECH.get() && !(new Object() {
         public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
         }
      }).getValue(world, BlockPos.containing(x, y + 1.0, z), "Loaded")) {
         if ((new Object() {
            public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
               }

               return _retval.get();
            }
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).getItem() == CrustyChunksModItems.ARTILLERY_SHELL.get()) {
            if (!world.isClientSide()) {
               BlockPos _bpx = BlockPos.containing(x, y + 1.0, z);
               BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
               BlockState _bsx = world.getBlockState(_bpx);
               if (_blockEntityx != null) {
                  _blockEntityx.getPersistentData().putBoolean("Loaded", true);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
               }
            }

            if (!world.isClientSide()) {
               BlockPos _bpxx = BlockPos.containing(x, y + 1.0, z);
               BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
               BlockState _bsxx = world.getBlockState(_bpxx);
               if (_blockEntityxx != null) {
                  _blockEntityxx.getPersistentData().putBoolean("HE", true);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
               }
            }

            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.piston.extend")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.piston.extend")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 0;
               int _amount = 1;
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(1);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }
         } else if ((new Object() {
            public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
               }

               return _retval.get();
            }
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).getItem() == CrustyChunksModItems.ARTILLERY_SOLID_SHELL.get()) {
            if (!world.isClientSide()) {
               BlockPos _bpxxx = BlockPos.containing(x, y + 1.0, z);
               BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
               BlockState _bsxxx = world.getBlockState(_bpxxx);
               if (_blockEntityxxx != null) {
                  _blockEntityxxx.getPersistentData().putBoolean("Loaded", true);
               }

               if (world instanceof Level _levelx) {
                  _levelx.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
               }
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxx = BlockPos.containing(x, y + 1.0, z);
               BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
               BlockState _bsxxxx = world.getBlockState(_bpxxxx);
               if (_blockEntityxxxx != null) {
                  _blockEntityxxxx.getPersistentData().putBoolean("AP", true);
               }

               if (world instanceof Level _levelx) {
                  _levelx.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
               }
            }

            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.piston.extend")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.piston.extend")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 0;
               int _amount = 1;
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(1);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }
         } else if ((new Object() {
            public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
               }

               return _retval.get();
            }
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).getItem() == CrustyChunksModItems.GAS_ARTILLERY_SHELL.get()) {
            if (!world.isClientSide()) {
               BlockPos _bpxxxxx = BlockPos.containing(x, y + 1.0, z);
               BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
               BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
               if (_blockEntityxxxxx != null) {
                  _blockEntityxxxxx.getPersistentData().putBoolean("Loaded", true);
               }

               if (world instanceof Level _levelxx) {
                  _levelxx.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
               }
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxx = BlockPos.containing(x, y + 1.0, z);
               BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
               BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
               if (_blockEntityxxxxxx != null) {
                  _blockEntityxxxxxx.getPersistentData().putBoolean("GAS", true);
               }

               if (world instanceof Level _levelxx) {
                  _levelxx.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
               }
            }

            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.piston.extend")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.piston.extend")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 0;
               int _amount = 1;
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(1);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }
         } else if ((new Object() {
            public ItemStack getItemStack(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicReference<ItemStack> _retval = new AtomicReference<>(ItemStack.EMPTY);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).copy());
}
               }

               return _retval.get();
            }
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).getItem() == CrustyChunksModItems.FIRE_ARTILLERY_SHELL.get()) {
            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxx = BlockPos.containing(x, y + 1.0, z);
               BlockEntity _blockEntityxxxxxxx = world.getBlockEntity(_bpxxxxxxx);
               BlockState _bsxxxxxxx = world.getBlockState(_bpxxxxxxx);
               if (_blockEntityxxxxxxx != null) {
                  _blockEntityxxxxxxx.getPersistentData().putBoolean("Loaded", true);
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_bpxxxxxxx, _bsxxxxxxx, _bsxxxxxxx, 3);
               }
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxx = BlockPos.containing(x, y + 1.0, z);
               BlockEntity _blockEntityxxxxxxxx = world.getBlockEntity(_bpxxxxxxxx);
               BlockState _bsxxxxxxxx = world.getBlockState(_bpxxxxxxxx);
               if (_blockEntityxxxxxxxx != null) {
                  _blockEntityxxxxxxxx.getPersistentData().putBoolean("FIRE", true);
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_bpxxxxxxxx, _bsxxxxxxxx, _bsxxxxxxxx, 3);
               }
            }

            if (world instanceof Level _levelxxx) {
               if (!_levelxxx.isClientSide()) {
                  _levelxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.piston.extend")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.piston.extend")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 0;
               int _amount = 1;
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(1);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ArtilleryLoaderUpdateProcedure.execute", _wtSafe);
      }
   }
}
