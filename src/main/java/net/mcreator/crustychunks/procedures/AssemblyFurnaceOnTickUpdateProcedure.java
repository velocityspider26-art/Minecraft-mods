package net.mcreator.crustychunks.procedures;

import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class AssemblyFurnaceOnTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      try {
      double XTrigger = 0.0;
      double ZTrigger = 0.0;
      boolean sufficientheat = false;
      ItemStack result = ItemStack.EMPTY;
      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 200.0) {
         sufficientheat = true;
      } else {
         sufficientheat = false;
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 5.0 && !world.isClientSide()) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockEntity _blockEntity = world.getBlockEntity(_bp);
         BlockState _bs = world.getBlockState(_bp);
         if (_blockEntity != null) {
            _blockEntity.getPersistentData().putDouble("Heat", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 1.0);
         }

         if (world instanceof Level _level) {
            _level.sendBlockUpdated(_bp, _bs, _bs, 3);
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 1500.0 && !world.isClientSide()) {
         BlockPos _bpx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
         BlockState _bsx = world.getBlockState(_bpx);
         if (_blockEntityx != null) {
            _blockEntityx.getPersistentData().putDouble("Heat", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 10.0);
         }

         if (world instanceof Level _level) {
            _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
         }
      }

      XTrigger = (double)(new Object() {
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
      ZTrigger = (double)(new Object() {
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
      if (world.getBlockState(BlockPos.containing(XTrigger + x, y, ZTrigger + z)).getBlock() == CrustyChunksModBlocks.PRODUCTION_OUTPUT.get()
         && world.getBlockState(BlockPos.containing(x - XTrigger, y, z - ZTrigger)).getBlock() == CrustyChunksModBlocks.PRODUCTION_INPUT.get()
         && sufficientheat
         && world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == CrustyChunksModBlocks.BLAST_FUNNEL.get()
         && world.getBlockState(BlockPos.containing(x, y + 2.0, z)).getBlock() == CrustyChunksModBlocks.BLAST_FUNNEL.get()
         && world.getBlockState(BlockPos.containing(x, y + 3.0, z)).getBlock() == CrustyChunksModBlocks.BLAST_FUNNEL.get()
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
         }).getAmount(world, BlockPos.containing(XTrigger + x, y, ZTrigger + z), 0) < 64
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
         }).getItemStack(world, BlockPos.containing(x - XTrigger, y, z - ZTrigger), 0).getItem() != ItemStack.EMPTY.getItem()
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
                  }).getAmount(world, BlockPos.containing(XTrigger + x, y, ZTrigger + z), 0) < (new Object() {
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
                  }).getItemStack(world, BlockPos.containing(XTrigger + x, y, ZTrigger + z), 0).getMaxStackSize()
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
                     }).getItemStack(world, BlockPos.containing(XTrigger + x, y, ZTrigger + z), 0).getItem()
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
                                          .getItemStack(world, BlockPos.containing(XTrigger - 1.0, y, ZTrigger - 1.0), 0)
                                    
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
               }).getItemStack(world, BlockPos.containing(XTrigger + x, y, ZTrigger + z), 0).getItem() == ItemStack.EMPTY.getItem()
         )) {
         if ((world instanceof Level _lvlSmeltResultx
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
                                 .getItemStack(world, BlockPos.containing(x - XTrigger, y, z - ZTrigger), 0)
                           
                        ),
                        _lvlSmeltResultx
                     )
                     .map(recipe -> recipe.value().getResultItem(_lvlSmeltResultx.registryAccess()).copy())
                     .orElse(ItemStack.EMPTY)
                  : ItemStack.EMPTY)
               .getItem()
            == Blocks.AIR.asItem()) {
            result = (new Object() {
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
               .getItemStack(world, BlockPos.containing(x - XTrigger, y, z - ZTrigger), 0)
               .copy();
         } else {
            result = (world instanceof Level _lvlSmeltResultx
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
                                 .getItemStack(world, BlockPos.containing(x - XTrigger, y, z - ZTrigger), 0)
                           
                        ),
                        _lvlSmeltResultx
                     )
                     .map(recipe -> recipe.value().getResultItem(_lvlSmeltResultx.registryAccess()).copy())
                     .orElse(ItemStack.EMPTY)
                  : ItemStack.EMPTY)
               .copy();
            if (!world.isClientSide()) {
               BlockPos _bpxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
               BlockState _bsxx = world.getBlockState(_bpxx);
               if (_blockEntityxx != null) {
                  _blockEntityxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 20.0);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
               }
            }
         }

         BlockEntity _ent = world.getBlockEntity(BlockPos.containing(XTrigger + x, y, ZTrigger + z));
         if (_ent != null) {
            int _slotid = 0;
            ItemStack _setstack = result.copy();
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
                     .getAmount(world, BlockPos.containing(XTrigger + x, y, ZTrigger + z), 0)
                  + 1
            );
            {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
               if (capability instanceof IItemHandlerModifiable) {
                  ((IItemHandlerModifiable)capability).setStackInSlot(0, _setstack);
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

         _ent = world.getBlockEntity(BlockPos.containing(x - XTrigger, y, z - ZTrigger));
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
            _levelx.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + 0.5, y + 10.0, z + 0.5, 10, 0.0, 3.0, 0.0, 0.01);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("AssemblyFurnaceOnTickUpdateProcedure.execute", _wtSafe);
      }
   }
}
