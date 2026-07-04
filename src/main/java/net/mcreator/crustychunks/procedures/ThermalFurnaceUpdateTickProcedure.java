package net.mcreator.crustychunks.procedures;

import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class ThermalFurnaceUpdateTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double previousRecipe = 0.0;
      if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == CrustyChunksModBlocks.BLAST_FUNNEL.get()
         && world.getBlockState(BlockPos.containing(x, y + 2.0, z)).getBlock() == CrustyChunksModBlocks.BLAST_FUNNEL.get()
         && world.getBlockState(BlockPos.containing(x, y + 3.0, z)).getBlock() == CrustyChunksModBlocks.BLAST_FUNNEL.get()
         && (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 200.0
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
         }).getAmount(world, BlockPos.containing(x, y, z), 1) < 64
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).getItem() != ItemStack.EMPTY.getItem()
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
                  }).getAmount(world, BlockPos.containing(x, y, z), 1) < (new Object() {
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
                  }).getItemStack(world, BlockPos.containing(x, y, z), 1).getMaxStackSize()
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
                     }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem()
                     == (world instanceof Level _lvlSmeltResult
                           ? _lvlSmeltResult.getRecipeManager()
                              .getRecipeFor(
                                 RecipeType.SMELTING,
                                 new SingleRecipeInput(
                                       (new Object() {
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
                                    
                                 ),
                                 _lvlSmeltResult
                              )
                              .map(recipe -> recipe.value().getResultItem(_lvlSmeltResult.registryAccess()).copy())
                              .orElse(ItemStack.EMPTY)
                           : ItemStack.EMPTY)
                        .getItem()
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
               }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == ItemStack.EMPTY.getItem()
         )) {
         BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
         if (_ent != null) {
            int _slotid = 1;
            ItemStack _setstack = (world instanceof Level _lvlSmeltResultx
                  ? _lvlSmeltResultx.getRecipeManager()
                     .getRecipeFor(
                        RecipeType.SMELTING,
                        new SingleRecipeInput(
                              (new Object() {
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
                           
                        ),
                        _lvlSmeltResultx
                     )
                     .map(recipe -> recipe.value().getResultItem(_lvlSmeltResultx.registryAccess()).copy())
                     .orElse(ItemStack.EMPTY)
                  : ItemStack.EMPTY)
               .copy();
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
                  5.0F,
                  1.0F
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.furnace.fire_crackle")),
                  SoundSource.BLOCKS,
                  5.0F,
                  1.0F,
                  false
               );
            }
         }

         _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
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

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + 0.5, y + 6.0, z + 0.5, 5, 0.0, 3.0, 0.0, 0.01);
         }

         if (!world.isClientSide()) {
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockEntity _blockEntity = world.getBlockEntity(_bp);
            BlockState _bs = world.getBlockState(_bp);
            if (_blockEntity != null) {
               _blockEntity.getPersistentData().putDouble("Heat", (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 40.0);
            }

            if (world instanceof Level _levelx) {
               _levelx.sendBlockUpdated(_bp, _bs, _bs, 3);
            }
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 5.0 && !world.isClientSide()) {
         BlockPos _bpx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
         BlockState _bsx = world.getBlockState(_bpx);
         if (_blockEntityx != null) {
            _blockEntityx.getPersistentData().putDouble("Heat", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 1.0);
         }

         if (world instanceof Level _levelx) {
            _levelx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 1500.0 && !world.isClientSide()) {
         BlockPos _bpxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
         BlockState _bsxx = world.getBlockState(_bpxx);
         if (_blockEntityxx != null) {
            _blockEntityxx.getPersistentData().putDouble("Heat", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 10.0);
         }

         if (world instanceof Level _levelx) {
            _levelx.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ThermalFurnaceUpdateTickProcedure.execute", _wtSafe);
      }
   }
}
