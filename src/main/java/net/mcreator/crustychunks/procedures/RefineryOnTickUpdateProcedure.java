package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class RefineryOnTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if ((new Object() {
                  public int getAmountInTank(LevelAccessor level, BlockPos pos, int tank) {
                     BlockEntity blockEntity = level.getBlockEntity(pos);
                     return blockEntity != null
                        ? WariumCaps.fluid(blockEntity, null)
                           .map(fluidHandler -> fluidHandler.getFluidInTank(tank).getAmount())
                           .orElse(0)
                        : 0;
                  }
               })
               .getAmountInTank(world, BlockPos.containing(x, y + 1.0, z), (new Object() {
                  public int getBlockTanks(LevelAccessor level, BlockPos pos) {
                     AtomicInteger _retval = new AtomicInteger(0);
                     BlockEntity _ent = level.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getTanks());
}
                     }

                     return _retval.get();
                  }
               }).getBlockTanks(world, BlockPos.containing(x, y + 1.0, z)))
            <= 15750
         && (new Object() {
                  public int getAmountInTank(LevelAccessor level, BlockPos pos, int tank) {
                     BlockEntity blockEntity = level.getBlockEntity(pos);
                     return blockEntity != null
                        ? WariumCaps.fluid(blockEntity, null)
                           .map(fluidHandler -> fluidHandler.getFluidInTank(tank).getAmount())
                           .orElse(0)
                        : 0;
                  }
               })
               .getAmountInTank(world, BlockPos.containing(x, y + 2.0, z), (new Object() {
                  public int getBlockTanks(LevelAccessor level, BlockPos pos) {
                     AtomicInteger _retval = new AtomicInteger(0);
                     BlockEntity _ent = level.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getTanks());
}
                     }

                     return _retval.get();
                  }
               }).getBlockTanks(world, BlockPos.containing(x, y + 2.0, z)))
            <= 15750
         && (new Object() {
                  public int getAmountInTank(LevelAccessor level, BlockPos pos, int tank) {
                     BlockEntity blockEntity = level.getBlockEntity(pos);
                     return blockEntity != null
                        ? WariumCaps.fluid(blockEntity, null)
                           .map(fluidHandler -> fluidHandler.getFluidInTank(tank).getAmount())
                           .orElse(0)
                        : 0;
                  }
               })
               .getAmountInTank(world, BlockPos.containing(x, y + 3.0, z), (new Object() {
                  public int getBlockTanks(LevelAccessor level, BlockPos pos) {
                     AtomicInteger _retval = new AtomicInteger(0);
                     BlockEntity _ent = level.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getTanks());
}
                     }

                     return _retval.get();
                  }
               }).getBlockTanks(world, BlockPos.containing(x, y + 3.0, z)))
            <= 15750
         && (new Object() {
                  public int getAmountInTank(LevelAccessor level, BlockPos pos, int tank) {
                     BlockEntity blockEntity = level.getBlockEntity(pos);
                     return blockEntity != null
                        ? WariumCaps.fluid(blockEntity, null)
                           .map(fluidHandler -> fluidHandler.getFluidInTank(tank).getAmount())
                           .orElse(0)
                        : 0;
                  }
               })
               .getAmountInTank(world, BlockPos.containing(x, y + 4.0, z), (new Object() {
                  public int getBlockTanks(LevelAccessor level, BlockPos pos) {
                     AtomicInteger _retval = new AtomicInteger(0);
                     BlockEntity _ent = level.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getTanks());
}
                     }

                     return _retval.get();
                  }
               }).getBlockTanks(world, BlockPos.containing(x, y + 4.0, z)))
            <= 15750) {
         if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == CrustyChunksModBlocks.REFINERY_TOWER.get()) {
            if (!world.isClientSide()) {
               BlockPos _bp = BlockPos.containing(x, y + 1.0, z);
               BlockEntity _blockEntity = world.getBlockEntity(_bp);
               BlockState _bs = world.getBlockState(_bp);
               if (_blockEntity != null) {
                  _blockEntity.getPersistentData().putString("Fluid", "Oil");
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bp, _bs, _bs, 3);
               }
            }

            if (world.getBlockState(BlockPos.containing(x, y + 2.0, z)).getBlock() == CrustyChunksModBlocks.REFINERY_TOWER.get()) {
               if (!world.isClientSide()) {
                  BlockPos _bpx = BlockPos.containing(x, y + 2.0, z);
                  BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                  BlockState _bsx = world.getBlockState(_bpx);
                  if (_blockEntityx != null) {
                     _blockEntityx.getPersistentData().putString("Fluid", "Diesel");
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                  }
               }

               if (world.getBlockState(BlockPos.containing(x, y + 3.0, z)).getBlock() == CrustyChunksModBlocks.REFINERY_TOWER.get()) {
                  if (!world.isClientSide()) {
                     BlockPos _bpxx = BlockPos.containing(x, y + 3.0, z);
                     BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
                     BlockState _bsxx = world.getBlockState(_bpxx);
                     if (_blockEntityxx != null) {
                        _blockEntityxx.getPersistentData().putString("Fluid", "Kerosene");
                     }

                     if (world instanceof Level _level) {
                        _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
                     }
                  }

                  if (world.getBlockState(BlockPos.containing(x, y + 4.0, z)).getBlock() == CrustyChunksModBlocks.REFINERY_TOWER.get()) {
                     if (!world.isClientSide()) {
                        BlockPos _bpxxx = BlockPos.containing(x, y + 4.0, z);
                        BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
                        BlockState _bsxxx = world.getBlockState(_bpxxx);
                        if (_blockEntityxxx != null) {
                           _blockEntityxxx.getPersistentData().putString("Fluid", "Petrolium");
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
                           _blockEntityxxxx.getPersistentData().putBoolean("Ready", true);
                        }

                        if (world instanceof Level _level) {
                           _level.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
                        }
                     }
                  } else if (!world.isClientSide()) {
                     BlockPos _bpxxxxx = BlockPos.containing(x, y, z);
                     BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
                     BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
                     if (_blockEntityxxxxx != null) {
                        _blockEntityxxxxx.getPersistentData().putBoolean("Ready", false);
                     }

                     if (world instanceof Level _level) {
                        _level.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
                     }
                  }
               } else if (!world.isClientSide()) {
                  BlockPos _bpxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
                  BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
                  if (_blockEntityxxxxxx != null) {
                     _blockEntityxxxxxx.getPersistentData().putBoolean("Ready", false);
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
                  }
               }
            } else if (!world.isClientSide()) {
               BlockPos _bpxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxx = world.getBlockEntity(_bpxxxxxxx);
               BlockState _bsxxxxxxx = world.getBlockState(_bpxxxxxxx);
               if (_blockEntityxxxxxxx != null) {
                  _blockEntityxxxxxxx.getPersistentData().putBoolean("Ready", false);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpxxxxxxx, _bsxxxxxxx, _bsxxxxxxx, 3);
               }
            }
         } else if (!world.isClientSide()) {
            BlockPos _bpxxxxxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxxxxxx = world.getBlockEntity(_bpxxxxxxxx);
            BlockState _bsxxxxxxxx = world.getBlockState(_bpxxxxxxxx);
            if (_blockEntityxxxxxxxx != null) {
               _blockEntityxxxxxxxx.getPersistentData().putBoolean("Ready", false);
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxxxxx, _bsxxxxxxxx, _bsxxxxxxxx, 3);
            }
         }

         if ((new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 200.0 && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Ready") && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).getItem() == CrustyChunksModItems.SHALE_OIL.get()) {
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
               BlockPos _bpxxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxx);
               BlockState _bsxxxxxxxxx = world.getBlockState(_bpxxxxxxxxx);
               if (_blockEntityxxxxxxxxx != null) {
                  _blockEntityxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 5.0);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpxxxxxxxxx, _bsxxxxxxxxx, _bsxxxxxxxxx, 3);
               }
            }

            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y + 6.0, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.NEUTRAL,
                     20.0F,
                     0.2F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y + 6.0,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.NEUTRAL,
                     20.0F,
                     0.2F,
                     false
                  );
               }
            }

            if (world instanceof ServerLevel _levelx) {
               _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.RISING_FLAME.get(), x + 0.5, y + 6.0, z + 0.5, 1, 0.0, 0.0, 0.0, 0.1);
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y + 1.0, z));
            int _amount = (new Object() {
                  public int fillTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                     AtomicInteger _retval = new AtomicInteger(0);
                     BlockEntity _ent = level.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.OIL.get(), amount), FluidAction.SIMULATE))
                           ;
}
                     }

                     return _retval.get();
                  }
               })
               .fillTankSimulate(world, BlockPos.containing(x, y + 1.0, z), 250);
            if (_ent != null) {
               {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.OIL.get(), _amount), FluidAction.EXECUTE);
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y + 2.0, z));
            _amount = (new Object() {
                  public int fillTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                     AtomicInteger _retval = new AtomicInteger(0);
                     BlockEntity _ent = level.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(
                                    capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.DIESEL.get(), amount), FluidAction.SIMULATE)
                                 )
                           ;
}
                     }

                     return _retval.get();
                  }
               })
               .fillTankSimulate(world, BlockPos.containing(x, y + 2.0, z), 250);
            if (_ent != null) {
               {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.DIESEL.get(), _amount), FluidAction.EXECUTE);
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y + 3.0, z));
            _amount = (new Object() {
                  public int fillTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                     AtomicInteger _retval = new AtomicInteger(0);
                     BlockEntity _ent = level.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(
                                    capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.KEROSENE.get(), amount), FluidAction.SIMULATE)
                                 )
                           ;
}
                     }

                     return _retval.get();
                  }
               })
               .fillTankSimulate(world, BlockPos.containing(x, y + 3.0, z), 250);
            if (_ent != null) {
               {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.KEROSENE.get(), _amount), FluidAction.EXECUTE);
}
            }

            _ent = world.getBlockEntity(BlockPos.containing(x, y + 4.0, z));
            _amount = (new Object() {
                  public int fillTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                     AtomicInteger _retval = new AtomicInteger(0);
                     BlockEntity _ent = level.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(
                                    capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.PETROLIUM.get(), amount), FluidAction.SIMULATE)
                                 )
                           ;
}
                     }

                     return _retval.get();
                  }
               })
               .fillTankSimulate(world, BlockPos.containing(x, y + 4.0, z), 250);
            if (_ent != null) {
               {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.PETROLIUM.get(), _amount), FluidAction.EXECUTE);
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
         BlockPos _bpxxxxxxxxxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxx);
         BlockState _bsxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxx);
         if (_blockEntityxxxxxxxxxx != null) {
            _blockEntityxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 1.0);
         }

         if (world instanceof Level _levelx) {
            _levelx.sendBlockUpdated(_bpxxxxxxxxxx, _bsxxxxxxxxxx, _bsxxxxxxxxxx, 3);
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 1500.0 && !world.isClientSide()) {
         BlockPos _bpxxxxxxxxxxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxxx);
         BlockState _bsxxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxxx);
         if (_blockEntityxxxxxxxxxxx != null) {
            _blockEntityxxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 10.0);
         }

         if (world instanceof Level _levelx) {
            _levelx.sendBlockUpdated(_bpxxxxxxxxxxx, _bsxxxxxxxxxxx, _bsxxxxxxxxxxx, 3);
         }
      }
   }
}
