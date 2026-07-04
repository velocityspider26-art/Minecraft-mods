package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class ConveyorUpdateTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      try {
      double ZOffset = 0.0;
      double checkslotID = 0.0;
      double ItemCount = 0.0;
      double YOffset = 0.0;
      double XOffset = 0.0;
      XOffset = (double)(new Object() {
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
      YOffset = (double)(new Object() {
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
      }).getDirection(blockstate).getStepY();
      ZOffset = (double)(new Object() {
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
      if ((new Object() {
         public int getContainerSize(LevelAccessor world, BlockPos pos) {
            BlockEntity _ent = world.getBlockEntity(pos);
            return _ent != null && _ent instanceof BaseContainerBlockEntity _block ? _block.getContainerSize() : 0;
         }

         public int getAmount(LevelAccessor world, BlockPos pos) {
            Block block = world.getBlockState(pos).getBlock();
            if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
               boolean var10000;
               label17: {
                  if (block.getStateDefinition().getProperty("type") instanceof EnumProperty _getep5 && world.getBlockState(pos).getValue(_getep5).toString().equals("SINGLE")) {
                     var10000 = true;
                     break label17;
                  }

                  var10000 = false;
               }

               boolean isSingle = var10000;
               if (!isSingle) {
                  return this.getContainerSize(world, pos) * 2;
               }
            }

            return this.getContainerSize(world, pos);
         }
      }).getAmount(world, new BlockPos((int)(x - XOffset), (int)(y - YOffset), (int)(z - ZOffset))) > 0) {
         checkslotID = 0.0;

         for (int index0 = 0;
            index0
               < (new Object() {
                     public int getContainerSize(LevelAccessor world, BlockPos pos) {
                        BlockEntity _ent = world.getBlockEntity(pos);
                        return _ent != null && _ent instanceof BaseContainerBlockEntity _block ? _block.getContainerSize() : 0;
                     }

                     public int getAmount(LevelAccessor world, BlockPos pos) {
                        Block block = world.getBlockState(pos).getBlock();
                        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
                           boolean var10000;
                           label17: {
                              if (block.getStateDefinition().getProperty("type") instanceof EnumProperty _getep5
                                 && world.getBlockState(pos).getValue(_getep5).toString().equals("SINGLE")) {
                                 var10000 = true;
                                 break label17;
                              }

                              var10000 = false;
                           }

                           boolean isSingle = var10000;
                           if (!isSingle) {
                              return this.getContainerSize(world, pos) * 2;
                           }
                        }

                        return this.getContainerSize(world, pos);
                     }
                  })
                  .getAmount(world, new BlockPos((int)(x - XOffset), (int)(y - YOffset), (int)(z - ZOffset)));
            index0++
         ) {
            if ((
                  world.getBlockState(BlockPos.containing(x - XOffset, y - YOffset, z - ZOffset))
                        .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:extractable")))
                     || (new Object() {
                        public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
                        }
                     }).getValue(world, BlockPos.containing(x - XOffset, y - YOffset, z - ZOffset), "Greenlight")
               )
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
                  == 0) {
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
                     .getItemStack(world, BlockPos.containing(x - XOffset, y - YOffset, z - ZOffset), (int)checkslotID)
                     .getItem()
                  != ItemStack.EMPTY.getItem()) {
                  BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
                  if (_ent != null) {
                     int _slotid = 0;
                     ItemStack _setstack = (new Object() {
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
                        .getItemStack(world, BlockPos.containing(x - XOffset, y - YOffset, z - ZOffset), (int)checkslotID)
                        .copy();
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
                           .getItemStack(world, BlockPos.containing(x - XOffset, y - YOffset, z - ZOffset), (int)checkslotID)
                           .getCount()
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

                  _ent = world.getBlockEntity(BlockPos.containing(x - XOffset, y - YOffset, z - ZOffset));
                  if (_ent != null) {
                     int _slotid = (int)checkslotID;
                     ItemStack _setstack = (new Object() {
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
                        .getItemStack(world, BlockPos.containing(x - XOffset, y - YOffset, z - ZOffset), (int)checkslotID)
                        .copy();
                     _setstack.setCount(0);
                     {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                        if (capability instanceof IItemHandlerModifiable) {
                           ((IItemHandlerModifiable)capability).setStackInSlot(_slotid, _setstack);
                        }
                     }
}
                  }
                  break;
               }

               checkslotID++;
            }
         }
      }

      if ((new Object() {
         public int getContainerSize(LevelAccessor world, BlockPos pos) {
            BlockEntity _ent = world.getBlockEntity(pos);
            return _ent != null && _ent instanceof BaseContainerBlockEntity _block ? _block.getContainerSize() : 0;
         }

         public int getAmount(LevelAccessor world, BlockPos pos) {
            Block block = world.getBlockState(pos).getBlock();
            if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
               boolean var10000;
               label17: {
                  if (block.getStateDefinition().getProperty("type") instanceof EnumProperty _getep5 && world.getBlockState(pos).getValue(_getep5).toString().equals("SINGLE")) {
                     var10000 = true;
                     break label17;
                  }

                  var10000 = false;
               }

               boolean isSingle = var10000;
               if (!isSingle) {
                  return this.getContainerSize(world, pos) * 2;
               }
            }

            return this.getContainerSize(world, pos);
         }
      }).getAmount(world, new BlockPos((int)(x + XOffset), (int)(y + YOffset), (int)(z + ZOffset))) > 0) {
         checkslotID = 0.0;

         for (int index1 = 0;
            index1
               < (new Object() {
                     public int getContainerSize(LevelAccessor world, BlockPos pos) {
                        BlockEntity _ent = world.getBlockEntity(pos);
                        return _ent != null && _ent instanceof BaseContainerBlockEntity _block ? _block.getContainerSize() : 0;
                     }

                     public int getAmount(LevelAccessor world, BlockPos pos) {
                        Block block = world.getBlockState(pos).getBlock();
                        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
                           boolean var10000;
                           label17: {
                              if (block.getStateDefinition().getProperty("type") instanceof EnumProperty _getep5
                                 && world.getBlockState(pos).getValue(_getep5).toString().equals("SINGLE")) {
                                 var10000 = true;
                                 break label17;
                              }

                              var10000 = false;
                           }

                           boolean isSingle = var10000;
                           if (!isSingle) {
                              return this.getContainerSize(world, pos) * 2;
                           }
                        }

                        return this.getContainerSize(world, pos);
                     }
                  })
                  .getAmount(world, new BlockPos((int)(x + XOffset), (int)(y + YOffset), (int)(z + ZOffset)));
            index1++
         ) {
            if ((
                  world.getBlockState(BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset))
                        .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:insertable")))
                     || (new Object() {
                        public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
                        }
                     }).getValue(world, BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset), "Greenlight")
               )
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
                     .getCount()
                  != 0) {
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
                     .getItemStack(world, BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset), (int)checkslotID)
                     .getItem()
                  == ItemStack.EMPTY.getItem()) {
                  BlockEntity _entx = world.getBlockEntity(BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset));
                  if (_entx != null) {
                     int _slotid = (int)checkslotID;
                     ItemStack _setstack = (new Object() {
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
                        .copy();
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
                           .getItemStack(world, BlockPos.containing(x, y, z), 0)
                           .getCount()
                     );
                     {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                        if (capability instanceof IItemHandlerModifiable) {
                           ((IItemHandlerModifiable)capability).setStackInSlot(_slotid, _setstack);
                        }
                     }
}
                  }

                  _entx = world.getBlockEntity(BlockPos.containing(x, y, z));
                  if (_entx != null) {
                     int _slotid = 0;
                     ItemStack _setstack = (new Object() {
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
                        .copy();
                     _setstack.setCount(0);
                     {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                        if (capability instanceof IItemHandlerModifiable) {
                           ((IItemHandlerModifiable)capability).setStackInSlot(0, _setstack);
                        }
                     }
}
                  }
                  break;
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
                        .getItemStack(world, BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset), (int)checkslotID)
                        .getItem()
                     == (new Object() {
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
                           .getItemStack(world, BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset), (int)checkslotID)
                           .getMaxStackSize()
                        - (new Object() {
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
                           .getItemStack(world, BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset), (int)checkslotID)
                           .getCount()
                     > 0) {
                  ItemCount = (double)Math.min(
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
                           .getItemStack(world, BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset), (int)checkslotID)
                           .getMaxStackSize()
                        - (new Object() {
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
                           .getItemStack(world, BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset), (int)checkslotID)
                           .getCount(),
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
                        .getCount()
                  );
                  BlockEntity _entxx = world.getBlockEntity(BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset));
                  if (_entxx != null) {
                     int _slotid = (int)checkslotID;
                     ItemStack _setstack = (new Object() {
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
                        .copy();
                     _setstack.setCount(
                        (int)(
                           (double)(new Object() {
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
                                 .getItemStack(world, BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset), (int)checkslotID)
                                 .getCount()
                              + ItemCount
                        )
                     );
                     {
   IItemHandler capability = WariumCaps.itemHandler(_entxx, null).orElse(null);
   if (capability != null) {
                        if (capability instanceof IItemHandlerModifiable) {
                           ((IItemHandlerModifiable)capability).setStackInSlot(_slotid, _setstack);
                        }
                     }
}
                  }

                  _entxx = world.getBlockEntity(BlockPos.containing(x, y, z));
                  if (_entxx != null) {
                     int _slotid = 0;
                     ItemStack _setstack = (new Object() {
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
                        .copy();
                     _setstack.setCount(
                        (int)(
                           (double)(new Object() {
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
                                 .getCount()
                              - ItemCount
                        )
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
                  break;
               }

               checkslotID++;
            }
         }
      } else if (world.getBlockState(BlockPos.containing(x + XOffset, y + YOffset, z + ZOffset)).getBlock() == Blocks.AIR && (new Object() {
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
      }).getItemStack(world, BlockPos.containing(x, y, z), 0).getCount() >= 1) {
         if (world instanceof ServerLevel _level) {
            ItemEntity entityToSpawn = new ItemEntity(
               _level,
               x + XOffset + 0.5,
               y + YOffset + 0.5,
               z + ZOffset + 0.5,
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
            );
            entityToSpawn.setPickUpDelay(10);
            _level.addFreshEntity(entityToSpawn);
         }

         BlockEntity _entxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
         if (_entxxx != null) {
            int _slotid = 0;
            ItemStack _setstack = (new Object() {
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
               .copy();
            _setstack.setCount(0);
            {
   IItemHandler capability = WariumCaps.itemHandler(_entxxx, null).orElse(null);
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
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.puffer_fish.blow_out")),
                  SoundSource.NEUTRAL,
                  1.0F,
                  1.0F
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.puffer_fish.blow_out")),
                  SoundSource.NEUTRAL,
                  1.0F,
                  1.0F,
                  false
               );
            }
         }

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles(ParticleTypes.POOF, x + XOffset + 0.5, y + YOffset + 0.5, z + ZOffset + 0.5, 5, 0.5, 0.5, 0.5, 0.02);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ConveyorUpdateTickProcedure.execute", _wtSafe);
      }
   }
}
