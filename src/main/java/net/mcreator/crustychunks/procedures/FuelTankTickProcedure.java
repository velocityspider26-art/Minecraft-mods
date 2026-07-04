package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import java.util.concurrent.atomic.AtomicInteger;
import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class FuelTankTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      boolean found = false;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      double amount = 0.0;
      double fluid = 0.0;
      double fillamount = 0.0;
      double tanknumberself = 0.0;
      double tanknumbertarget = 0.0;
      sx = (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "ConnectionX") * -1.0;
      sy = (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "ConnectionY") * -1.0;
      sz = (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "ConnectionZ") * -1.0;
      tanknumberself = (double)(new Object() {
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
      }).getBlockTanks(world, BlockPos.containing(x, y, z));
      tanknumbertarget = (double)(new Object() {
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
      }).getBlockTanks(world, BlockPos.containing(x + sx, y + sy, z + sz));
      if (!(new Object() {
               public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                  BlockEntity blockEntity = level.getBlockEntity(pos);
                  return blockEntity != null
                     ? WariumCaps.fluid(blockEntity, null)
                        .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                        .orElse(FluidStack.EMPTY)
                     : FluidStack.EMPTY;
               }
            })
            .getFluidInTank(world, BlockPos.containing(x, y, z), (int)tanknumberself)
            .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.KEROSENE.get(), 1))
         || !(new Object() {
                  public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                     BlockEntity blockEntity = level.getBlockEntity(pos);
                     return blockEntity != null
                        ? WariumCaps.fluid(blockEntity, null)
                           .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                           .orElse(FluidStack.EMPTY)
                        : FluidStack.EMPTY;
                  }
               })
               .getFluidInTank(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)
               .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.KEROSENE.get(), 1))
            && 0 < (new Object() {
               public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                  }

                  return _retval.get();
               }
            }).getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)) {
         if (!(new Object() {
                  public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                     BlockEntity blockEntity = level.getBlockEntity(pos);
                     return blockEntity != null
                        ? WariumCaps.fluid(blockEntity, null)
                           .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                           .orElse(FluidStack.EMPTY)
                        : FluidStack.EMPTY;
                  }
               })
               .getFluidInTank(world, BlockPos.containing(x, y, z), (int)tanknumberself)
               .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.DIESEL.get(), 1))
            || !(new Object() {
                     public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        return blockEntity != null
                           ? WariumCaps.fluid(blockEntity, null)
                              .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                              .orElse(FluidStack.EMPTY)
                           : FluidStack.EMPTY;
                     }
                  })
                  .getFluidInTank(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)
                  .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.DIESEL.get(), 1))
               && 0 < (new Object() {
                  public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                     AtomicInteger _retval = new AtomicInteger(0);
                     BlockEntity _ent = level.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                     }

                     return _retval.get();
                  }
               }).getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)) {
            if (!(new Object() {
                     public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        return blockEntity != null
                           ? WariumCaps.fluid(blockEntity, null)
                              .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                              .orElse(FluidStack.EMPTY)
                           : FluidStack.EMPTY;
                     }
                  })
                  .getFluidInTank(world, BlockPos.containing(x, y, z), (int)tanknumberself)
                  .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.PETROLIUM.get(), 1))
               || !(new Object() {
                        public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                           BlockEntity blockEntity = level.getBlockEntity(pos);
                           return blockEntity != null
                              ? WariumCaps.fluid(blockEntity, null)
                                 .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                                 .orElse(FluidStack.EMPTY)
                              : FluidStack.EMPTY;
                        }
                     })
                     .getFluidInTank(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)
                     .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.PETROLIUM.get(), 1))
                  && 0
                     < (new Object() {
                           public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = level.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)) {
               if (!(new Object() {
                        public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                           BlockEntity blockEntity = level.getBlockEntity(pos);
                           return blockEntity != null
                              ? WariumCaps.fluid(blockEntity, null)
                                 .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                                 .orElse(FluidStack.EMPTY)
                              : FluidStack.EMPTY;
                        }
                     })
                     .getFluidInTank(world, BlockPos.containing(x, y, z), (int)tanknumberself)
                     .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.HYDRAZINE.get(), 1))
                  || !(new Object() {
                           public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                              BlockEntity blockEntity = level.getBlockEntity(pos);
                              return blockEntity != null
                                 ? WariumCaps.fluid(blockEntity, null)
                                    .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                                    .orElse(FluidStack.EMPTY)
                                 : FluidStack.EMPTY;
                           }
                        })
                        .getFluidInTank(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)
                        .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.HYDRAZINE.get(), 1))
                     && 0
                        < (new Object() {
                              public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                                 AtomicInteger _retval = new AtomicInteger(0);
                                 BlockEntity _ent = level.getBlockEntity(pos);
                                 if (_ent != null) {
                                    {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                                 }

                                 return _retval.get();
                              }
                           })
                           .getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)) {
                  if (!(new Object() {
                           public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                              BlockEntity blockEntity = level.getBlockEntity(pos);
                              return blockEntity != null
                                 ? WariumCaps.fluid(blockEntity, null)
                                    .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                                    .orElse(FluidStack.EMPTY)
                                 : FluidStack.EMPTY;
                           }
                        })
                        .getFluidInTank(world, BlockPos.containing(x, y, z), (int)tanknumberself)
                        .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.LIQUID_OXYGEN.get(), 1))
                     || !(new Object() {
                              public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                                 BlockEntity blockEntity = level.getBlockEntity(pos);
                                 return blockEntity != null
                                    ? WariumCaps.fluid(blockEntity, null)
                                       .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                                       .orElse(FluidStack.EMPTY)
                                    : FluidStack.EMPTY;
                              }
                           })
                           .getFluidInTank(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)
                           .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.LIQUID_OXYGEN.get(), 1))
                        && 0
                           < (new Object() {
                                 public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                                    AtomicInteger _retval = new AtomicInteger(0);
                                    BlockEntity _ent = level.getBlockEntity(pos);
                                    if (_ent != null) {
                                       {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                                    }

                                    return _retval.get();
                                 }
                              })
                              .getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)) {
                     if ((new Object() {
                              public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                                 BlockEntity blockEntity = level.getBlockEntity(pos);
                                 return blockEntity != null
                                    ? WariumCaps.fluid(blockEntity, null)
                                       .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                                       .orElse(FluidStack.EMPTY)
                                    : FluidStack.EMPTY;
                              }
                           })
                           .getFluidInTank(world, BlockPos.containing(x, y, z), (int)tanknumberself)
                           .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.LIQUID_HYDROGEN.get(), 1))
                        && (
                           (new Object() {
                                    public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                                       BlockEntity blockEntity = level.getBlockEntity(pos);
                                       return blockEntity != null
                                          ? WariumCaps.fluid(blockEntity, null)
                                             .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                                             .orElse(FluidStack.EMPTY)
                                          : FluidStack.EMPTY;
                                    }
                                 })
                                 .getFluidInTank(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)
                                 .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.LIQUID_HYDROGEN.get(), 1))
                              || 0
                                 >= (new Object() {
                                       public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                                          AtomicInteger _retval = new AtomicInteger(0);
                                          BlockEntity _ent = level.getBlockEntity(pos);
                                          if (_ent != null) {
                                             {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                                          }

                                          return _retval.get();
                                       }
                                    })
                                    .getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)
                        )
                        && (new Object() {
                                 public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                                    AtomicInteger _retval = new AtomicInteger(0);
                                    BlockEntity _ent = level.getBlockEntity(pos);
                                    if (_ent != null) {
                                       {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                                    }

                                    return _retval.get();
                                 }
                              })
                              .getFluidTankLevel(world, BlockPos.containing(x, y, z), (int)tanknumberself)
                           > (new Object() {
                                 public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                                    AtomicInteger _retval = new AtomicInteger(0);
                                    BlockEntity _ent = level.getBlockEntity(pos);
                                    if (_ent != null) {
                                       {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                                    }

                                    return _retval.get();
                                 }
                              })
                              .getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)) {
                        amount = (double)(new Object() {
                              public int fillTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                                 AtomicInteger _retval = new AtomicInteger(0);
                                 BlockEntity _ent = level.getBlockEntity(pos);
                                 if (_ent != null) {
                                    {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(
                                                capability.fill(
                                                   new FluidStack((Fluid)CrustyChunksModFluids.LIQUID_HYDROGEN.get(), amount), FluidAction.SIMULATE
                                                )
                                             )
                                       ;
}
                                 }

                                 return _retval.get();
                              }
                           })
                           .fillTankSimulate(world, BlockPos.containing(x + sx, y + sy, z + sz), 250);
                        fillamount = (double)(new Object() {
                              public int drainTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                                 AtomicInteger _retval = new AtomicInteger(0);
                                 BlockEntity _ent = level.getBlockEntity(pos);
                                 if (_ent != null) {
                                    {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.drain(amount, FluidAction.SIMULATE).getAmount());
}
                                 }

                                 return _retval.get();
                              }
                           })
                           .drainTankSimulate(world, BlockPos.containing(x, y, z), (int)amount);
                        BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
                        int _amount = (int)fillamount;
                        if (_ent != null) {
                           {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) capability.drain(_amount, FluidAction.EXECUTE);
}
                        }

                        _ent = world.getBlockEntity(BlockPos.containing(x + sx, y + sy, z + sz));
                        _amount = (int)fillamount;
                        if (_ent != null) {
                           {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.LIQUID_HYDROGEN.get(), _amount), FluidAction.EXECUTE)
                              ;
}
                        }
                     }
                  } else if ((new Object() {
                           public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = level.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getFluidTankLevel(world, BlockPos.containing(x, y, z), (int)tanknumberself)
                     > (new Object() {
                           public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = level.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)) {
                     amount = (double)(new Object() {
                           public int fillTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = level.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(
                                             capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.LIQUID_OXYGEN.get(), amount), FluidAction.SIMULATE)
                                          )
                                    ;
}
                              }

                              return _retval.get();
                           }
                        })
                        .fillTankSimulate(world, BlockPos.containing(x + sx, y + sy, z + sz), 250);
                     fillamount = (double)(new Object() {
                           public int drainTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                              AtomicInteger _retval = new AtomicInteger(0);
                              BlockEntity _ent = level.getBlockEntity(pos);
                              if (_ent != null) {
                                 {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.drain(amount, FluidAction.SIMULATE).getAmount());
}
                              }

                              return _retval.get();
                           }
                        })
                        .drainTankSimulate(world, BlockPos.containing(x, y, z), (int)amount);
                     BlockEntity _entx = world.getBlockEntity(BlockPos.containing(x, y, z));
                     int _amountx = (int)fillamount;
                     if (_entx != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_entx, null).orElse(null);
   if (capability != null) capability.drain(_amountx, FluidAction.EXECUTE);
}
                     }

                     _entx = world.getBlockEntity(BlockPos.containing(x + sx, y + sy, z + sz));
                     _amountx = (int)fillamount;
                     if (_entx != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_entx, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.LIQUID_OXYGEN.get(), _amountx), FluidAction.EXECUTE)
                           ;
}
                     }
                  }
               } else if ((new Object() {
                        public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                           AtomicInteger _retval = new AtomicInteger(0);
                           BlockEntity _ent = level.getBlockEntity(pos);
                           if (_ent != null) {
                              {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                           }

                           return _retval.get();
                        }
                     })
                     .getFluidTankLevel(world, BlockPos.containing(x, y, z), (int)tanknumberself)
                  > (new Object() {
                        public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                           AtomicInteger _retval = new AtomicInteger(0);
                           BlockEntity _ent = level.getBlockEntity(pos);
                           if (_ent != null) {
                              {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                           }

                           return _retval.get();
                        }
                     })
                     .getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)) {
                  amount = (double)(new Object() {
                        public int fillTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                           AtomicInteger _retval = new AtomicInteger(0);
                           BlockEntity _ent = level.getBlockEntity(pos);
                           if (_ent != null) {
                              {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(
                                          capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.HYDRAZINE.get(), amount), FluidAction.SIMULATE)
                                       )
                                 ;
}
                           }

                           return _retval.get();
                        }
                     })
                     .fillTankSimulate(world, BlockPos.containing(x + sx, y + sy, z + sz), 250);
                  fillamount = (double)(new Object() {
                        public int drainTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                           AtomicInteger _retval = new AtomicInteger(0);
                           BlockEntity _ent = level.getBlockEntity(pos);
                           if (_ent != null) {
                              {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.drain(amount, FluidAction.SIMULATE).getAmount());
}
                           }

                           return _retval.get();
                        }
                     })
                     .drainTankSimulate(world, BlockPos.containing(x, y, z), (int)amount);
                  BlockEntity _entxx = world.getBlockEntity(BlockPos.containing(x, y, z));
                  int _amountxx = (int)fillamount;
                  if (_entxx != null) {
                     {
   IFluidHandler capability = WariumCaps.fluid(_entxx, null).orElse(null);
   if (capability != null) capability.drain(_amountxx, FluidAction.EXECUTE);
}
                  }

                  _entxx = world.getBlockEntity(BlockPos.containing(x + sx, y + sy, z + sz));
                  _amountxx = (int)fillamount;
                  if (_entxx != null) {
                     {
   IFluidHandler capability = WariumCaps.fluid(_entxx, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.HYDRAZINE.get(), _amountxx), FluidAction.EXECUTE);
}
                  }
               }
            } else if ((new Object() {
                     public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                        AtomicInteger _retval = new AtomicInteger(0);
                        BlockEntity _ent = level.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getFluidTankLevel(world, BlockPos.containing(x, y, z), (int)tanknumberself)
               > (new Object() {
                     public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
                        AtomicInteger _retval = new AtomicInteger(0);
                        BlockEntity _ent = level.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
                        }

                        return _retval.get();
                     }
                  })
                  .getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)) {
               amount = (double)(new Object() {
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
                  .fillTankSimulate(world, BlockPos.containing(x + sx, y + sy, z + sz), 250);
               fillamount = (double)(new Object() {
                     public int drainTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                        AtomicInteger _retval = new AtomicInteger(0);
                        BlockEntity _ent = level.getBlockEntity(pos);
                        if (_ent != null) {
                           {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.drain(amount, FluidAction.SIMULATE).getAmount());
}
                        }

                        return _retval.get();
                     }
                  })
                  .drainTankSimulate(world, BlockPos.containing(x, y, z), (int)amount);
               BlockEntity _entxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
               int _amountxxx = (int)fillamount;
               if (_entxxx != null) {
                  {
   IFluidHandler capability = WariumCaps.fluid(_entxxx, null).orElse(null);
   if (capability != null) capability.drain(_amountxxx, FluidAction.EXECUTE);
}
               }

               _entxxx = world.getBlockEntity(BlockPos.containing(x + sx, y + sy, z + sz));
               _amountxxx = (int)fillamount;
               if (_entxxx != null) {
                  {
   IFluidHandler capability = WariumCaps.fluid(_entxxx, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.PETROLIUM.get(), _amountxxx), FluidAction.EXECUTE);
}
               }
            }
         } else if ((new Object() {
            public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
               }

               return _retval.get();
            }
         }).getFluidTankLevel(world, BlockPos.containing(x, y, z), (int)tanknumberself) > (new Object() {
            public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
               }

               return _retval.get();
            }
         }).getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)) {
            amount = (double)(new Object() {
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
               .fillTankSimulate(world, BlockPos.containing(x + sx, y + sy, z + sz), 250);
            fillamount = (double)(new Object() {
                  public int drainTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                     AtomicInteger _retval = new AtomicInteger(0);
                     BlockEntity _ent = level.getBlockEntity(pos);
                     if (_ent != null) {
                        {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.drain(amount, FluidAction.SIMULATE).getAmount());
}
                     }

                     return _retval.get();
                  }
               })
               .drainTankSimulate(world, BlockPos.containing(x, y, z), (int)amount);
            BlockEntity _entxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            int _amountxxxx = (int)fillamount;
            if (_entxxxx != null) {
               {
   IFluidHandler capability = WariumCaps.fluid(_entxxxx, null).orElse(null);
   if (capability != null) capability.drain(_amountxxxx, FluidAction.EXECUTE);
}
            }

            _entxxxx = world.getBlockEntity(BlockPos.containing(x + sx, y + sy, z + sz));
            _amountxxxx = (int)fillamount;
            if (_entxxxx != null) {
               {
   IFluidHandler capability = WariumCaps.fluid(_entxxxx, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.DIESEL.get(), _amountxxxx), FluidAction.EXECUTE);
}
            }
         }
      } else if ((new Object() {
         public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
            AtomicInteger _retval = new AtomicInteger(0);
            BlockEntity _ent = level.getBlockEntity(pos);
            if (_ent != null) {
               {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
            }

            return _retval.get();
         }
      }).getFluidTankLevel(world, BlockPos.containing(x, y, z), (int)tanknumberself) > (new Object() {
         public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
            AtomicInteger _retval = new AtomicInteger(0);
            BlockEntity _ent = level.getBlockEntity(pos);
            if (_ent != null) {
               {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getFluidInTank(tank).getAmount());
}
            }

            return _retval.get();
         }
      }).getFluidTankLevel(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)tanknumbertarget)) {
         amount = (double)(new Object() {
               public int fillTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.KEROSENE.get(), amount), FluidAction.SIMULATE))
                        ;
}
                  }

                  return _retval.get();
               }
            })
            .fillTankSimulate(world, BlockPos.containing(x + sx, y + sy, z + sz), 250);
         fillamount = (double)(new Object() {
               public int drainTankSimulate(LevelAccessor level, BlockPos pos, int amount) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.drain(amount, FluidAction.SIMULATE).getAmount());
}
                  }

                  return _retval.get();
               }
            })
            .drainTankSimulate(world, BlockPos.containing(x, y, z), (int)amount);
         BlockEntity _entxxxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
         int _amountxxxxx = (int)fillamount;
         if (_entxxxxx != null) {
            {
   IFluidHandler capability = WariumCaps.fluid(_entxxxxx, null).orElse(null);
   if (capability != null) capability.drain(_amountxxxxx, FluidAction.EXECUTE);
}
         }

         _entxxxxx = world.getBlockEntity(BlockPos.containing(x + sx, y + sy, z + sz));
         _amountxxxxx = (int)fillamount;
         if (_entxxxxx != null) {
            {
   IFluidHandler capability = WariumCaps.fluid(_entxxxxx, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.KEROSENE.get(), _amountxxxxx), FluidAction.EXECUTE);
}
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FuelTankTickProcedure.execute", _wtSafe);
      }
   }
}
