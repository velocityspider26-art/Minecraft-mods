package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class FoundryUpdateTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.COMPONENT_FOUNDRY_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockEntity _blockEntity = world.getBlockEntity(_bp);
            BlockState _bs = world.getBlockState(_bp);
            if (_blockEntity != null) {
               _blockEntity.getPersistentData().putString("Produce", "Component");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bp, _bs, _bs, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.CYLINDER_FOUNDRY_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
            BlockState _bsx = world.getBlockState(_bpx);
            if (_blockEntityx != null) {
               _blockEntityx.getPersistentData().putString("Produce", "Cylinder");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.SMALL_PROJECTILE_FOUNDRY_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
            BlockState _bsxx = world.getBlockState(_bpxx);
            if (_blockEntityxx != null) {
               _blockEntityxx.getPersistentData().putString("Produce", "PR1");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.MEDIUM_PROJECTILE_FOUNDRY_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
            BlockState _bsxxx = world.getBlockState(_bpxxx);
            if (_blockEntityxxx != null) {
               _blockEntityxxx.getPersistentData().putString("Produce", "PR2");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.LARGE_PROJECTILE_FOUNDRY_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
            BlockState _bsxxxx = world.getBlockState(_bpxxxx);
            if (_blockEntityxxxx != null) {
               _blockEntityxxxx.getPersistentData().putString("Produce", "PR3");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.EXTRA_LARGE_PROJECTILE_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
            BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
            if (_blockEntityxxxxx != null) {
               _blockEntityxxxxx.getPersistentData().putString("Produce", "PR4");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.HUGE_PROJECTILE_FOUNDRY_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
            BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
            if (_blockEntityxxxxxx != null) {
               _blockEntityxxxxxx.getPersistentData().putString("Produce", "PR5");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.SMALL_BARREL_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxx = world.getBlockEntity(_bpxxxxxxx);
            BlockState _bsxxxxxxx = world.getBlockState(_bpxxxxxxx);
            if (_blockEntityxxxxxxx != null) {
               _blockEntityxxxxxxx.getPersistentData().putString("Produce", "BR1");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxxxx, _bsxxxxxxx, _bsxxxxxxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.MEDIUM_BARREL_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxxx = world.getBlockEntity(_bpxxxxxxxx);
            BlockState _bsxxxxxxxx = world.getBlockState(_bpxxxxxxxx);
            if (_blockEntityxxxxxxxx != null) {
               _blockEntityxxxxxxxx.getPersistentData().putString("Produce", "BR2");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxxxxx, _bsxxxxxxxx, _bsxxxxxxxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.LARGE_BARREL_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxx);
            BlockState _bsxxxxxxxxx = world.getBlockState(_bpxxxxxxxxx);
            if (_blockEntityxxxxxxxxx != null) {
               _blockEntityxxxxxxxxx.getPersistentData().putString("Produce", "BR3");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxxxxxx, _bsxxxxxxxxx, _bsxxxxxxxxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.HUGE_BARREL_FOUNDRY_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxxxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxx);
            BlockState _bsxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxx);
            if (_blockEntityxxxxxxxxxx != null) {
               _blockEntityxxxxxxxxxx.getPersistentData().putString("Produce", "BR4");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxxxxxxx, _bsxxxxxxxxxx, _bsxxxxxxxxxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.SMALL_CANNON_FOUNDRY_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxxxxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxx);
            BlockState _bsxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxx);
            if (_blockEntityxxxxxxxxxxx != null) {
               _blockEntityxxxxxxxxxxx.getPersistentData().putString("Produce", "BR5");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxxxxxxxx, _bsxxxxxxxxxxx, _bsxxxxxxxxxxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.MEDIUM_CANNON_FOUNDRY_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxxxxxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxx);
            BlockState _bsxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxx);
            if (_blockEntityxxxxxxxxxxxx != null) {
               _blockEntityxxxxxxxxxxxx.getPersistentData().putString("Produce", "BR6");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxxxxxxxxx, _bsxxxxxxxxxxxx, _bsxxxxxxxxxxxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.LARGE_CANNON_FOUNDRY_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxx);
            BlockState _bsxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxx);
            if (_blockEntityxxxxxxxxxxxxx != null) {
               _blockEntityxxxxxxxxxxxxx.getPersistentData().putString("Produce", "BR7");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxxxxxxxxxx, _bsxxxxxxxxxxxxx, _bsxxxxxxxxxxxxx, 3);
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.HUGE_CANNON_FOUNDRY_TEMPLATE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bpxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxx);
            BlockState _bsxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxx);
            if (_blockEntityxxxxxxxxxxxxxx != null) {
               _blockEntityxxxxxxxxxxxxxx.getPersistentData().putString("Produce", "BR8");
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxx, 3);
            }
         }
      } else if (!world.isClientSide()) {
         BlockPos _bpxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxx);
         BlockState _bsxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxx);
         if (_blockEntityxxxxxxxxxxxxxxx != null) {
            _blockEntityxxxxxxxxxxxxxxx.getPersistentData().putString("Produce", "None");
         }

         if (world instanceof Level _level) {
            _level.sendBlockUpdated(_bpxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxx, 3);
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 200.0) {
         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("Cylinder") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:ingots/steel"))) && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 15 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.STEEL_CYLINDER.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
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

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 10.0);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxx, 3);
               }
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.STEEL_CYLINDER.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("Component")) {
            if ((new Object() {
                        public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                           AtomicInteger _retval = new AtomicInteger(0);
                           BlockEntity _ent = world.getBlockEntity(pos);
                           if (_ent != null) {
                              {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                           }

                           return _retval.get();
                        }
                     })
                     .getAmount(world, BlockPos.containing(x, y, z), 1)
                  < 64
               && (new Object() {
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
                  })
                  .getItemStack(world, BlockPos.containing(x, y, z), 0)
                  .is(ItemTags.create(ResourceLocation.parse("c:ingots/steel")))
               && (
                  (new Object() {
                                 public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                                    AtomicInteger _retval = new AtomicInteger(0);
                                    BlockEntity _ent = world.getBlockEntity(pos);
                                    if (_ent != null) {
                                       {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                                    }

                                    return _retval.get();
                                 }
                              })
                              .getAmount(world, BlockPos.containing(x, y, z), 1)
                           <= 15
                        && (new Object() {
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
                              })
                              .getItemStack(world, BlockPos.containing(x, y, z), 1)
                              .getItem()
                           == CrustyChunksModItems.CAST_COMPONENT.get()
                     || (new Object() {
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
                           })
                           .getItemStack(world, BlockPos.containing(x, y, z), 1)
                           .getItem()
                        == ItemStack.EMPTY.getItem()
               )) {
               BlockEntity _entx = world.getBlockEntity(BlockPos.containing(x, y, z));
               if (_entx != null) {
                  int _slotid = 0;
                  int _amount = 1;
                  {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ItemStack _stk = capability.getStackInSlot(0).copy();
                        _stk.shrink(1);
                        ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                     }
                  }
}
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxx);
                  BlockState _bsxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxx);
                  if (_blockEntityxxxxxxxxxxxxxxxxx != null) {
                     _blockEntityxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 10.0);
                  }

                  if (world instanceof Level _levelx) {
                     _levelx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxx, 3);
                  }
               }

               _entx = world.getBlockEntity(BlockPos.containing(x, y, z));
               if (_entx != null) {
                  int _slotid = 1;
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.CAST_COMPONENT.get()).copy();
                  _setstack.setCount(
                     (new Object() {
                              public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                                 AtomicInteger _retval = new AtomicInteger(0);
                                 BlockEntity _ent = world.getBlockEntity(pos);
                                 if (_ent != null) {
                                    {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                                 }

                                 return _retval.get();
                              }
                           })
                           .getAmount(world, BlockPos.containing(x, y, z), 1)
                        + 1
                  );
                  {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                     }
                  }
}
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F,
                        false
                     );
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
                     })
                     .getItemStack(world, BlockPos.containing(x, y, z), 0)
                     .getItem()
                  == CrustyChunksModItems.PLUTONIUM_INGOT.get()
               && (new Object() {
                        public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                           AtomicInteger _retval = new AtomicInteger(0);
                           BlockEntity _ent = world.getBlockEntity(pos);
                           if (_ent != null) {
                              {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                           }

                           return _retval.get();
                        }
                     })
                     .getAmount(world, BlockPos.containing(x, y, z), 0)
                  >= 4
               && (new Object() {
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
                     })
                     .getItemStack(world, BlockPos.containing(x, y, z), 1)
                     .getItem()
                  == ItemStack.EMPTY.getItem()) {
               BlockEntity _entxx = world.getBlockEntity(BlockPos.containing(x, y, z));
               if (_entxx != null) {
                  int _slotid = 0;
                  int _amount = 4;
                  {
   IItemHandler capability = WariumCaps.itemHandler(_entxx, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ItemStack _stk = capability.getStackInSlot(0).copy();
                        _stk.shrink(4);
                        ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                     }
                  }
}
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxx);
                  BlockState _bsxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxx);
                  if (_blockEntityxxxxxxxxxxxxxxxxxx != null) {
                     _blockEntityxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 200.0);
                  }

                  if (world instanceof Level _levelxx) {
                     _levelxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxx, 3);
                  }
               }

               _entxx = world.getBlockEntity(BlockPos.containing(x, y, z));
               if (_entxx != null) {
                  int _slotid = 1;
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.PLUTONIUM_CORE.get()).copy();
                  _setstack.setCount(
                     (new Object() {
                              public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                                 AtomicInteger _retval = new AtomicInteger(0);
                                 BlockEntity _ent = world.getBlockEntity(pos);
                                 if (_ent != null) {
                                    {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                                 }

                                 return _retval.get();
                              }
                           })
                           .getAmount(world, BlockPos.containing(x, y, z), 1)
                        + 1
                  );
                  {
   IItemHandler capability = WariumCaps.itemHandler(_entxx, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                     }
                  }
}
               }

               if (world instanceof Level _levelxx) {
                  if (!_levelxx.isClientSide()) {
                     _levelxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lava.extinguish")),
                        SoundSource.BLOCKS,
                        20.0F,
                        0.3F
                     );
                  } else {
                     _levelxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lava.extinguish")),
                        SoundSource.BLOCKS,
                        20.0F,
                        0.3F,
                        false
                     );
                  }
               }

               if (world instanceof ServerLevel _levelxxx) {
                  _levelxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.AERIAL_SPARKS.get(), x + 0.5, y + 0.5, z + 0.5, 25, 1.0, 1.0, 1.0, 0.3);
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("PR1") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:nuggets/lead"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 2 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 63 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.SMALL_PROJECTILE.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxx != null) {
               int _slotid = 0;
               int _amount = 2;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(2);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 5.0);
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.SMALL_PROJECTILE.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxx) {
               if (!_levelxxx.isClientSide()) {
                  _levelxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("PR2") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:nuggets/lead"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 4 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 63 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.MEDIUM_PROJECTILE.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxx != null) {
               int _slotid = 0;
               int _amount = 4;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(4);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 15.0);
               }

               if (world instanceof Level _levelxxxx) {
                  _levelxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.MEDIUM_PROJECTILE.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("PR3") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:nuggets/lead"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 6 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 63 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.LARGE_PROJECTILE.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxx != null) {
               int _slotid = 0;
               int _amount = 6;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(6);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 15.0);
               }

               if (world instanceof Level _levelxxxx) {
                  _levelxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.LARGE_PROJECTILE.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxxx) {
               if (!_levelxxxx.isClientSide()) {
                  _levelxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("PR4") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:ingots/lead"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 2 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 63 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.EXTRA_LARGE_PROJECTILE.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxx != null) {
               int _slotid = 0;
               int _amount = 2;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(2);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 20.0);
               }

               if (world instanceof Level _levelxxxxx) {
                  _levelxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.EXTRA_LARGE_PROJECTILE.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxxxx) {
               if (!_levelxxxxx.isClientSide()) {
                  _levelxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("PR5") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:ingots/lead"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 3 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 63 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.HUGE_PROJECTILE.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxx != null) {
               int _slotid = 0;
               int _amount = 3;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(3);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 25.0);
               }

               if (world instanceof Level _levelxxxxxx) {
                  _levelxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.HUGE_PROJECTILE.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxxxxx) {
               if (!_levelxxxxxx.isClientSide()) {
                  _levelxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("BR1") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:ingots/steel"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 1 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 15 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.SMALL_UNBORED_BARREL.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxx != null) {
               int _slotid = 0;
               int _amount = 1;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(1);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 5.0);
               }

               if (world instanceof Level _levelxxxxxxx) {
                  _levelxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.SMALL_UNBORED_BARREL.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxxxxxx) {
               if (!_levelxxxxxxx.isClientSide()) {
                  _levelxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("BR2") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:ingots/steel"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 3 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 15 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.MEDIUM_UNBORED_BARREL.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxx != null) {
               int _slotid = 0;
               int _amount = 3;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(3);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 10.0);
               }

               if (world instanceof Level _levelxxxxxxxx) {
                  _levelxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.MEDIUM_UNBORED_BARREL.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxxxxxxx) {
               if (!_levelxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("BR3") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:ingots/steel"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 4 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 7 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.LARGE_UNBORED_BARREL.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxx != null) {
               int _slotid = 0;
               int _amount = 4;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(4);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 15.0);
               }

               if (world instanceof Level _levelxxxxxxxxx) {
                  _levelxxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.LARGE_UNBORED_BARREL.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxxxxxxxx) {
               if (!_levelxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("BR4") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:ingots/steel"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 6 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 7 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.HUGE_UNBORED_BARREL.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxxx != null) {
               int _slotid = 0;
               int _amount = 6;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(6);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 20.0);
               }

               if (world instanceof Level _levelxxxxxxxxxx) {
                  _levelxxxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.HUGE_UNBORED_BARREL.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxxxxxxxxx) {
               if (!_levelxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("BR5") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:ingots/steel"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 6 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 0 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.SMALL_UNBORED_CANNON_BARREL.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxxxx != null) {
               int _slotid = 0;
               int _amount = 6;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(6);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 25.0);
               }

               if (world instanceof Level _levelxxxxxxxxxxx) {
                  _levelxxxxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.SMALL_UNBORED_CANNON_BARREL.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("BR6") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:ingots/steel"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 8 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 0 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.MEDIUM_UNBORED_CANNON_BARREL.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxxxxx != null) {
               int _slotid = 0;
               int _amount = 8;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(8);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 30.0);
               }

               if (world instanceof Level _levelxxxxxxxxxxxx) {
                  _levelxxxxxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.MEDIUM_UNBORED_CANNON_BARREL.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("BR7") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:ingots/steel"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 8 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 0 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.LARGE_UNBORED_CANNON_BARREL.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxxxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxxxxxx != null) {
               int _slotid = 0;
               int _amount = 8;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(8);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 35.0);
               }

               if (world instanceof Level _levelxxxxxxxxxxxxx) {
                  _levelxxxxxxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxxxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxxxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.LARGE_UNBORED_CANNON_BARREL.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }

         if ((new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Produce").equals("BR8") && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:ingots/steel"))) && (new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 0) >= 9 && ((new Object() {
            public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = world.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
               }

               return _retval.get();
            }
         }).getAmount(world, BlockPos.containing(x, y, z), 1) <= 0 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.HUGE_UNBORED_CANNON_BARREL.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem())) {
            BlockEntity _entxxxxxxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxxxxxxx != null) {
               int _slotid = 0;
               int _amount = 9;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ItemStack _stk = capability.getStackInSlot(0).copy();
                     _stk.shrink(9);
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                  }
               }
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
               BlockState _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
               if (_blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 40.0);
               }

               if (world instanceof Level _levelxxxxxxxxxxxxxx) {
                  _levelxxxxxxxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, 3);
               }
            }

            _entxxxxxxxxxxxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_entxxxxxxxxxxxxxxx != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.HUGE_UNBORED_CANNON_BARREL.get()).copy();
               _setstack.setCount(
                  (new Object() {
                           public int getAmount(LevelAccessor world, BlockPos pos, int slotid) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = world.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getStackInSlot(slotid).getCount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getAmount(world, BlockPos.containing(x, y, z), 1)
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxxxxxxxxxxxxxxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxxxxxxxxxxxxxx) {
               if (!_levelxxxxxxxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxxxxxxxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 5.0 && !world.isClientSide()) {
         BlockPos _bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
         BlockState _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
         if (_blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx != null) {
            _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 1.0);
         }

         if (world instanceof Level _levelxxxxxxxxxxxxxxx) {
            _levelxxxxxxxxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, 3);
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 1000.0 && !world.isClientSide()) {
         BlockPos _bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
         BlockState _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);
         if (_blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx != null) {
            _blockEntityxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 10.0);
         }

         if (world instanceof Level _levelxxxxxxxxxxxxxxx) {
            _levelxxxxxxxxxxxxxxx.sendBlockUpdated(_bpxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, _bsxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, 3);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FoundryUpdateTickProcedure.execute", _wtSafe);
      }
   }
}
