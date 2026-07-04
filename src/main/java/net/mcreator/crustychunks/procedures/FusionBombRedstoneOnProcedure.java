package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.init.CrustyChunksModGameRules;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class FusionBombRedstoneOnProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.STRATEGIC_WEAPONS) && (new Object() {
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 0).getItem() == CrustyChunksModItems.FISSION_CORE.get() && (new Object() {
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.IMPLOSION_LENS.get() && (new Object() {
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.IMPLOSION_LENS.get() && (new Object() {
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 3).getItem() == CrustyChunksModItems.IMPLOSION_LENS.get() && (new Object() {
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 4).getItem() == CrustyChunksModItems.IMPLOSION_LENS.get() && (new Object() {
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 5).getItem() == CrustyChunksModItems.IMPLOSION_LENS.get() && (new Object() {
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 6).getItem() == CrustyChunksModItems.IMPLOSION_LENS.get()) {
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
            }).getItemStack(world, BlockPos.containing(x, y, z), 7).getItem() == CrustyChunksModItems.IMPACT_FUZE.get()
            && world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.ALLOW_IMPACT_FUZE)) {
            BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 0;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 1;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(1, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 2;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(2, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 3;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(3, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 4;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(4, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 5;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(5, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 6;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(6, _setstack);
                  }
               }
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (_ent != null) {
               int _slotid = 7;
               ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
               _setstack.setCount(0);
               {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(7, _setstack);
                  }
               }
}
            }

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
                  })
                  .getItemStack(world, BlockPos.containing(x, y, z), 8)
                  .getItem()
               == CrustyChunksModItems.FUSION_CORE.get()) {
               _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
               if (_ent != null) {
                  int _slotid = 8;
                  ItemStack _setstack = new ItemStack(Blocks.AIR).copy();
                  _setstack.setCount(0);
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ((IItemHandlerModifiable)capability).setStackInSlot(8, _setstack);
                     }
                  }
}
               }

               FusionExplosionProcedure.execute(world, x, y, z);
            } else {
               FissionExplosionProcedure.execute(world, x, y, z);
            }

            world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 7).getItem() == CrustyChunksModItems.TIMED_FUZE.get() && !world.isClientSide()) {
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockEntity _blockEntity = world.getBlockEntity(_bp);
            BlockState _bs = world.getBlockState(_bp);
            if (_blockEntity != null) {
               _blockEntity.getPersistentData().putBoolean("Triggered", true);
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bp, _bs, _bs, 3);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FusionBombRedstoneOnProcedure.execute", _wtSafe);
      }
   }
}
