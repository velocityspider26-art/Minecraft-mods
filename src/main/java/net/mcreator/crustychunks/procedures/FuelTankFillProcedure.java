package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;
import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class FuelTankFillProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)
            .is(ItemTags.create(ResourceLocation.parse("c:buckets/diesel")))) {
            if ((
                  (new Object() {
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
                           .getFluidTankLevel(
                              world,
                              BlockPos.containing(x, y, z),
                              (new Object() {
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
                                 .getFluidTankLevel(world, BlockPos.containing(x, y, z), (new Object() {
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
                                 }).getBlockTanks(world, BlockPos.containing(x, y, z)))
                           )
                        == 0
                     || (new Object() {
                           public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                              BlockEntity blockEntity = level.getBlockEntity(pos);
                              return blockEntity != null
                                 ? WariumCaps.fluid(blockEntity, null)
                                    .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                                    .orElse(FluidStack.EMPTY)
                                 : FluidStack.EMPTY;
                           }
                        })
                        .getFluidInTank(world, BlockPos.containing(x, y, z), (new Object() {
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
                        }).getBlockTanks(world, BlockPos.containing(x, y, z)))
                        .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.DIESEL.get(), 1))
               )
               && 900
                  <= (new Object() {
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
                     .fillTankSimulate(world, BlockPos.containing(x, y, z), 1000)) {
               BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
               int _amount = (new Object() {
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
                  .fillTankSimulate(world, BlockPos.containing(x, y, z), 1000);
               if (_ent != null) {
                  {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.DIESEL.get(), _amount), FluidAction.EXECUTE);
}
               }

               if (entity instanceof LivingEntity _entity) {
                  ItemStack _setstack = new ItemStack(Items.BUCKET).copy();
                  _setstack.setCount(1);
                  _entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
                  if (_entity instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }
            }
         } else if ((entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY)
            .is(ItemTags.create(ResourceLocation.parse("c:buckets/kerosene")))) {
            if ((
                  (new Object() {
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
                           .getFluidTankLevel(
                              world,
                              BlockPos.containing(x, y, z),
                              (new Object() {
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
                                 .getFluidTankLevel(world, BlockPos.containing(x, y, z), (new Object() {
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
                                 }).getBlockTanks(world, BlockPos.containing(x, y, z)))
                           )
                        == 0
                     || (new Object() {
                           public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                              BlockEntity blockEntity = level.getBlockEntity(pos);
                              return blockEntity != null
                                 ? WariumCaps.fluid(blockEntity, null)
                                    .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                                    .orElse(FluidStack.EMPTY)
                                 : FluidStack.EMPTY;
                           }
                        })
                        .getFluidInTank(world, BlockPos.containing(x, y, z), (new Object() {
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
                        }).getBlockTanks(world, BlockPos.containing(x, y, z)))
                        .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.KEROSENE.get(), 1))
               )
               && 900
                  <= (new Object() {
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
                     .fillTankSimulate(world, BlockPos.containing(x, y, z), 1000)) {
               BlockEntity _entx = world.getBlockEntity(BlockPos.containing(x, y, z));
               int _amountx = (new Object() {
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
                  .fillTankSimulate(world, BlockPos.containing(x, y, z), 1000);
               if (_entx != null) {
                  {
   IFluidHandler capability = WariumCaps.fluid(_entx, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.KEROSENE.get(), _amountx), FluidAction.EXECUTE);
}
               }

               if (entity instanceof LivingEntity _entityx) {
                  ItemStack _setstack = new ItemStack(Items.BUCKET).copy();
                  _setstack.setCount(1);
                  _entityx.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
                  if (_entityx instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }
            }
         } else if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY)
            .is(ItemTags.create(ResourceLocation.parse("c:buckets/gasoline")))) {
            if ((
                  (new Object() {
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
                           .getFluidTankLevel(
                              world,
                              BlockPos.containing(x, y, z),
                              (new Object() {
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
                                 .getFluidTankLevel(world, BlockPos.containing(x, y, z), (new Object() {
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
                                 }).getBlockTanks(world, BlockPos.containing(x, y, z)))
                           )
                        == 0
                     || (new Object() {
                           public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                              BlockEntity blockEntity = level.getBlockEntity(pos);
                              return blockEntity != null
                                 ? WariumCaps.fluid(blockEntity, null)
                                    .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                                    .orElse(FluidStack.EMPTY)
                                 : FluidStack.EMPTY;
                           }
                        })
                        .getFluidInTank(world, BlockPos.containing(x, y, z), (new Object() {
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
                        }).getBlockTanks(world, BlockPos.containing(x, y, z)))
                        .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.PETROLIUM.get(), 1))
               )
               && 900
                  <= (new Object() {
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
                     .fillTankSimulate(world, BlockPos.containing(x, y, z), 1000)) {
               BlockEntity _entxx = world.getBlockEntity(BlockPos.containing(x, y, z));
               int _amountxx = (new Object() {
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
                  .fillTankSimulate(world, BlockPos.containing(x, y, z), 1000);
               if (_entxx != null) {
                  {
   IFluidHandler capability = WariumCaps.fluid(_entxx, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.PETROLIUM.get(), _amountxx), FluidAction.EXECUTE);
}
               }

               if (entity instanceof LivingEntity _entityxx) {
                  ItemStack _setstack = new ItemStack(Items.BUCKET).copy();
                  _setstack.setCount(1);
                  _entityxx.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
                  if (_entityxx instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               if (world instanceof Level _levelxx) {
                  if (!_levelxx.isClientSide()) {
                     _levelxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }
            }
         } else if ((entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY)
            .is(ItemTags.create(ResourceLocation.parse("c:buckets/hydrazine")))) {
            if ((
                  (new Object() {
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
                           .getFluidTankLevel(
                              world,
                              BlockPos.containing(x, y, z),
                              (new Object() {
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
                                 .getFluidTankLevel(world, BlockPos.containing(x, y, z), (new Object() {
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
                                 }).getBlockTanks(world, BlockPos.containing(x, y, z)))
                           )
                        == 0
                     || (new Object() {
                           public FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank) {
                              BlockEntity blockEntity = level.getBlockEntity(pos);
                              return blockEntity != null
                                 ? WariumCaps.fluid(blockEntity, null)
                                    .map(fluidHandler -> fluidHandler.getFluidInTank(tank).copy())
                                    .orElse(FluidStack.EMPTY)
                                 : FluidStack.EMPTY;
                           }
                        })
                        .getFluidInTank(world, BlockPos.containing(x, y, z), (new Object() {
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
                        }).getBlockTanks(world, BlockPos.containing(x, y, z)))
                        .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.HYDRAZINE.get(), 1))
               )
               && 900
                  <= (new Object() {
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
                     .fillTankSimulate(world, BlockPos.containing(x, y, z), 1000)) {
               BlockEntity _entxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
               int _amountxxx = (new Object() {
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
                  .fillTankSimulate(world, BlockPos.containing(x, y, z), 1000);
               if (_entxxx != null) {
                  {
   IFluidHandler capability = WariumCaps.fluid(_entxxx, null).orElse(null);
   if (capability != null) capability.fill(new FluidStack((Fluid)CrustyChunksModFluids.HYDRAZINE.get(), _amountxxx), FluidAction.EXECUTE);
}
               }

               if (entity instanceof LivingEntity _entityxxx) {
                  ItemStack _setstack = new ItemStack(Items.BUCKET).copy();
                  _setstack.setCount(1);
                  _entityxxx.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
                  if (_entityxxx instanceof Player _player) {
                     _player.getInventory().setChanged();
                  }
               }

               if (world instanceof Level _levelxxx) {
                  if (!_levelxxx.isClientSide()) {
                     _levelxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }
            }
         } else if ((entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).getItem()
            == CrustyChunksModItems.FUEL_HOSE.get()) {
            HoseConnectionProcedure.execute(world, x, y, z, entity, entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY);
         }

         if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(
               Component.literal(
                  new DecimalFormat("####")
                        .format(
                           (long)(new Object() {
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
                              .getFluidTankLevel(world, BlockPos.containing(x, y, z), (new Object() {
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
                              }).getBlockTanks(world, BlockPos.containing(x, y, z)))
                        )
                     + "/"
                     + new DecimalFormat("####").format((long)(new Object() {
                        public int getFluidTankCapacity(LevelAccessor level, BlockPos pos, int tank) {
                           AtomicInteger _retval = new AtomicInteger(0);
                           BlockEntity _ent = level.getBlockEntity(pos);
                           if (_ent != null) {
                              {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getTankCapacity(tank));
}
                           }

                           return _retval.get();
                        }
                     }).getFluidTankCapacity(world, BlockPos.containing(x, y, z), (new Object() {
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
                     }).getBlockTanks(world, BlockPos.containing(x, y, z))))
               ),
               true
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FuelTankFillProcedure.execute", _wtSafe);
      }
   }
}
