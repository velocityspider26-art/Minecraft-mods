package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class ConveyorSplitterUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      try {
      Direction behind = Direction.NORTH;
      Direction ahead = Direction.NORTH;
      Direction orange = Direction.NORTH;
      Direction magenta = Direction.NORTH;
      ItemStack input = ItemStack.EMPTY;
      double OffsetX = 0.0;
      double OffsetZ = 0.0;
      double Power = 0.0;
      double outputslot = 0.0;
      double slotlog = 0.0;
      boolean forward = false;
      boolean orangesend = false;
      boolean magentasend = false;
      outputslot = (double)Mth.nextInt(RandomSource.create(), 0, 3);
      OffsetX = (double)(new Object() {
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
      OffsetZ = (double)(new Object() {
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
      ahead = (new Object() {
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
      }).getDirection(blockstate);
      orange = (new Object() {
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
      }).getDirection(blockstate).getCounterClockWise(Axis.Y);
      magenta = (new Object() {
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
      }).getDirection(blockstate).getClockWise(Axis.Y);
      input = (new Object() {
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
      }).getItemStack(world, BlockPos.containing(x - OffsetX, y, z - OffsetZ), 0).copy();
      if (Blocks.AIR.asItem() != input.getItem()) {
         for (int index0 = 0; index0 < 6; index0++) {
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
                  .getItemStack(world, BlockPos.containing(x, y, z), (int)slotlog)
                  .getItem()
               == input.getItem()) {
               orangesend = true;
            }

            slotlog++;
         }

         if (!orangesend) {
            for (int index1 = 0; index1 < 6; index1++) {
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
                     .getItemStack(world, BlockPos.containing(x, y, z), (int)slotlog)
                     .getItem()
                  == input.getItem()) {
                  magentasend = true;
               }

               slotlog++;
            }

            if (!magentasend) {
               forward = true;
            }
         }

         if (orangesend) {
            if (world.getBlockState(BlockPos.containing(x + (double)orange.getStepX(), y, z + (double)orange.getStepZ())).getBlock()
                  == CrustyChunksModBlocks.CONVEYOR.get()
               && (
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
                           .getItemStack(
                              world, BlockPos.containing(x + (double)orange.getStepX(), y + (double)orange.getStepY(), z + (double)orange.getStepZ()), 0
                           )
                           .getItem()
                        == ItemStack.EMPTY.getItem()
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
                              .getItemStack(
                                 world, BlockPos.containing(x + (double)orange.getStepX(), y + (double)orange.getStepY(), z + (double)orange.getStepZ()), 0
                              )
                              .getItem()
                           == input.getItem()
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
                              .getItemStack(
                                 world, BlockPos.containing(x + (double)orange.getStepX(), y + (double)orange.getStepY(), z + (double)orange.getStepZ()), 0
                              )
                              .getMaxStackSize()
                           > (new Object() {
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
                              .getItemStack(
                                 world, BlockPos.containing(x + (double)orange.getStepX(), y + (double)orange.getStepY(), z + (double)orange.getStepZ()), 0
                              )
                              .getCount()
               )) {
               BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x + (double)orange.getStepX(), y, z + (double)orange.getStepZ()));
               if (_ent != null) {
                  int _slotid = 0;
                  ItemStack _setstack = input.copy();
                  _setstack.setCount(
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
                           .getItemStack(
                              world, BlockPos.containing(x + (double)orange.getStepX(), y + (double)orange.getStepY(), z + (double)orange.getStepZ()), 0
                           )
                           .getCount()
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
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.amethyst_cluster.place")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.5F
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.amethyst_cluster.place")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.5F,
                        false
                     );
                  }
               }

               _ent = world.getBlockEntity(BlockPos.containing(x - OffsetX, y, z - OffsetZ));
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
         } else if (magentasend) {
            if (world.getBlockState(BlockPos.containing(x + (double)magenta.getStepX(), y, z + (double)magenta.getStepZ())).getBlock()
                  == CrustyChunksModBlocks.CONVEYOR.get()
               && (
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
                           .getItemStack(
                              world, BlockPos.containing(x + (double)magenta.getStepX(), y + (double)magenta.getStepY(), z + (double)magenta.getStepZ()), 0
                           )
                           .getItem()
                        == ItemStack.EMPTY.getItem()
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
                              .getItemStack(
                                 world,
                                 BlockPos.containing(x + (double)magenta.getStepX(), y + (double)magenta.getStepY(), z + (double)magenta.getStepZ()),
                                 0
                              )
                              .getItem()
                           == input.getItem()
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
                              .getItemStack(
                                 world,
                                 BlockPos.containing(x + (double)magenta.getStepX(), y + (double)magenta.getStepY(), z + (double)magenta.getStepZ()),
                                 0
                              )
                              .getMaxStackSize()
                           > (new Object() {
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
                              .getItemStack(
                                 world,
                                 BlockPos.containing(x + (double)magenta.getStepX(), y + (double)magenta.getStepY(), z + (double)magenta.getStepZ()),
                                 0
                              )
                              .getCount()
               )) {
               BlockEntity _entx = world.getBlockEntity(BlockPos.containing(x + (double)magenta.getStepX(), y, z + (double)magenta.getStepZ()));
               if (_entx != null) {
                  int _slotid = 0;
                  ItemStack _setstack = input.copy();
                  _setstack.setCount(
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
                           .getItemStack(
                              world, BlockPos.containing(x + (double)magenta.getStepX(), y + (double)magenta.getStepY(), z + (double)magenta.getStepZ()), 0
                           )
                           .getCount()
                        + 1
                  );
                  {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ((IItemHandlerModifiable)capability).setStackInSlot(0, _setstack);
                     }
                  }
}
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.amethyst_cluster.place")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.amethyst_cluster.place")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }

               _entx = world.getBlockEntity(BlockPos.containing(x - OffsetX, y, z - OffsetZ));
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
            }
         } else if (world.getBlockState(BlockPos.containing(x + (double)ahead.getStepX(), y, z + (double)ahead.getStepZ())).getBlock()
               == CrustyChunksModBlocks.CONVEYOR.get()
            && (
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
                        .getItemStack(world, BlockPos.containing(x + (double)ahead.getStepX(), y + (double)ahead.getStepY(), z + (double)ahead.getStepZ()), 0)
                        .getItem()
                     == ItemStack.EMPTY.getItem()
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
                           .getItemStack(
                              world, BlockPos.containing(x + (double)ahead.getStepX(), y + (double)ahead.getStepY(), z + (double)ahead.getStepZ()), 0
                           )
                           .getItem()
                        == input.getItem()
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
                           .getItemStack(
                              world, BlockPos.containing(x + (double)ahead.getStepX(), y + (double)ahead.getStepY(), z + (double)ahead.getStepZ()), 0
                           )
                           .getMaxStackSize()
                        > (new Object() {
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
                           .getItemStack(
                              world, BlockPos.containing(x + (double)ahead.getStepX(), y + (double)ahead.getStepY(), z + (double)ahead.getStepZ()), 0
                           )
                           .getCount()
            )) {
            BlockEntity _entxx = world.getBlockEntity(BlockPos.containing(x + (double)ahead.getStepX(), y, z + (double)ahead.getStepZ()));
            if (_entxx != null) {
               int _slotid = 0;
               ItemStack _setstack = input.copy();
               _setstack.setCount(
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
                        .getItemStack(world, BlockPos.containing(x + (double)ahead.getStepX(), y + (double)ahead.getStepY(), z + (double)ahead.getStepZ()), 0)
                        .getCount()
                     + 1
               );
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxx, null).orElse(null);
   if (capability != null) {
                  if (capability instanceof IItemHandlerModifiable) {
                     ((IItemHandlerModifiable)capability).setStackInSlot(0, _setstack);
                  }
               }
}
            }

            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.amethyst_cluster.place")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.5F
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.amethyst_cluster.place")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     0.5F,
                     false
                  );
               }
            }

            _entxx = world.getBlockEntity(BlockPos.containing(x - OffsetX, y, z - OffsetZ));
            if (_entxx != null) {
               int _slotid = 0;
               int _amount = 1;
               {
   IItemHandler capability = WariumCaps.itemHandler(_entxx, null).orElse(null);
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
         net.mcreator.crustychunks.compat.WariumSafety.report("ConveyorSplitterUpdateProcedure.execute", _wtSafe);
      }
   }
}
