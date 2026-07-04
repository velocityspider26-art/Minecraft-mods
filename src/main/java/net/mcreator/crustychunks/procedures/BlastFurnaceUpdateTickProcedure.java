package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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

public class BlastFurnaceUpdateTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == CrustyChunksModBlocks.BLAST_FUNNEL.get()
         && world.getBlockState(BlockPos.containing(x, y + 2.0, z)).getBlock() == CrustyChunksModBlocks.BLAST_FUNNEL.get()
         && world.getBlockState(BlockPos.containing(x, y + 3.0, z)).getBlock() == CrustyChunksModBlocks.BLAST_FUNNEL.get()
         && (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 200.0) {
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
         }).getAmount(world, BlockPos.containing(x, y, z), 2) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).getItem() == CrustyChunksModItems.COMPRESSED_ADVANCED_MIXTURE.get() && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).getItem() == CrustyChunksModItems.COMPRESSED_ADVANCED_MIXTURE.get() && ((new Object() {
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
         }).getAmount(world, BlockPos.containing(x, y, z), 2) <= 63 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.ADVANCED_ALLOY_INGOT.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == ItemStack.EMPTY.getItem())) {
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            if (world instanceof ServerLevel _levelx) {
               _levelx.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + 0.5, y + 6.0, z + 0.5, 5, 0.0, 3.0, 0.0, 0.01);
            }

            if ((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Progress") <= 0.0) {
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

               _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
               if (_ent != null) {
                  int _slotid = 1;
                  int _amount = 1;
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ItemStack _stk = capability.getStackInSlot(1).copy();
                        _stk.shrink(1);
                        ((IItemHandlerModifiable)capability).setStackInSlot(1, _stk);
                     }
                  }
}
               }

               _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
               if (_ent != null) {
                  int _slotid = 2;
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.ADVANCED_ALLOY_INGOT.get()).copy();
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
                           .getAmount(world, BlockPos.containing(x, y, z), 2)
                        + 1
                  );
                  {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ((IItemHandlerModifiable)capability).setStackInSlot(2, _setstack);
                     }
                  }
}
               }

               if (!world.isClientSide()) {
                  BlockPos _bp = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntity = world.getBlockEntity(_bp);
                  BlockState _bs = world.getBlockState(_bp);
                  if (_blockEntity != null) {
                     _blockEntity.getPersistentData().putDouble("Progress", 4.0);
                  }

                  if (world instanceof Level _levelx) {
                     _levelx.sendBlockUpdated(_bp, _bs, _bs, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                  BlockState _bsx = world.getBlockState(_bpx);
                  if (_blockEntityx != null) {
                     _blockEntityx.getPersistentData().putDouble("Heat", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 20.0);
                  }

                  if (world instanceof Level _levelx) {
                     _levelx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                  }
               }
            } else if (!world.isClientSide()) {
               BlockPos _bpxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
               BlockState _bsxx = world.getBlockState(_bpxx);
               if (_blockEntityxx != null) {
                  _blockEntityxx.getPersistentData().putDouble("Progress", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Progress") - 1.0);
               }

               if (world instanceof Level _levelx) {
                  _levelx.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
               }
            }
         }

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
         }).getAmount(world, BlockPos.containing(x, y, z), 2) < 64 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:dusts/iron"))) && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).is(ItemTags.create(ResourceLocation.parse("c:dusts/iron"))) && ((new Object() {
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
         }).getAmount(world, BlockPos.containing(x, y, z), 2) <= 63 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.STEEL_INGOT.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == ItemStack.EMPTY.getItem())) {
            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            if (world instanceof ServerLevel _levelxx) {
               _levelxx.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + 0.5, y + 6.0, z + 0.5, 5, 0.0, 3.0, 0.0, 0.01);
            }

            if ((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Progress") <= 0.0) {
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

               _entx = world.getBlockEntity(BlockPos.containing(x, y, z));
               if (_entx != null) {
                  int _slotid = 1;
                  int _amount = 1;
                  {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ItemStack _stk = capability.getStackInSlot(1).copy();
                        _stk.shrink(1);
                        ((IItemHandlerModifiable)capability).setStackInSlot(1, _stk);
                     }
                  }
}
               }

               _entx = world.getBlockEntity(BlockPos.containing(x, y, z));
               if (_entx != null) {
                  int _slotid = 2;
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.STEEL_INGOT.get()).copy();
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
                           .getAmount(world, BlockPos.containing(x, y, z), 2)
                        + 1
                  );
                  {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ((IItemHandlerModifiable)capability).setStackInSlot(2, _setstack);
                     }
                  }
}
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
                  BlockState _bsxxx = world.getBlockState(_bpxxx);
                  if (_blockEntityxxx != null) {
                     _blockEntityxxx.getPersistentData().putDouble("Progress", 4.0);
                  }

                  if (world instanceof Level _levelxx) {
                     _levelxx.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
                  BlockState _bsxxxx = world.getBlockState(_bpxxxx);
                  if (_blockEntityxxxx != null) {
                     _blockEntityxxxx.getPersistentData().putDouble("Heat", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 20.0);
                  }

                  if (world instanceof Level _levelxx) {
                     _levelxx.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
                  }
               }
            } else if (!world.isClientSide()) {
               BlockPos _bpxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
               BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
               if (_blockEntityxxxxx != null) {
                  _blockEntityxxxxx.getPersistentData().putDouble("Progress", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Progress") - 1.0);
               }

               if (world instanceof Level _levelxx) {
                  _levelxx.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
               }
            }
         }

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
         }).getAmount(world, BlockPos.containing(x, y, z), 2) < 64 && ((new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:dusts/zinc"))) && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).is(ItemTags.create(ResourceLocation.parse("c:dusts/copper"))) || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).is(ItemTags.create(ResourceLocation.parse("c:dusts/copper"))) && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 1).is(ItemTags.create(ResourceLocation.parse("c:dusts/zinc")))) && ((new Object() {
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
         }).getAmount(world, BlockPos.containing(x, y, z), 2) <= 63 && (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == CrustyChunksModItems.BRASS_INGOT.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 2).getItem() == ItemStack.EMPTY.getItem())) {
            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            if (world instanceof ServerLevel _levelxxx) {
               _levelxxx.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + 0.5, y + 6.0, z + 0.5, 5, 0.0, 3.0, 0.0, 0.01);
            }

            if ((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Progress") <= 0.0) {
               BlockEntity _entxx = world.getBlockEntity(BlockPos.containing(x, y, z));
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

               _entxx = world.getBlockEntity(BlockPos.containing(x, y, z));
               if (_entxx != null) {
                  int _slotid = 1;
                  int _amount = 1;
                  {
   IItemHandler capability = WariumCaps.itemHandler(_entxx, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ItemStack _stk = capability.getStackInSlot(1).copy();
                        _stk.shrink(1);
                        ((IItemHandlerModifiable)capability).setStackInSlot(1, _stk);
                     }
                  }
}
               }

               _entxx = world.getBlockEntity(BlockPos.containing(x, y, z));
               if (_entxx != null) {
                  int _slotid = 2;
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.BRASS_INGOT.get()).copy();
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
                           .getAmount(world, BlockPos.containing(x, y, z), 2)
                        + 1
                  );
                  {
   IItemHandler capability = WariumCaps.itemHandler(_entxx, null).orElse(null);
   if (capability != null) {
                     if (capability instanceof IItemHandlerModifiable) {
                        ((IItemHandlerModifiable)capability).setStackInSlot(2, _setstack);
                     }
                  }
}
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
                  BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
                  if (_blockEntityxxxxxx != null) {
                     _blockEntityxxxxxx.getPersistentData().putDouble("Progress", 4.0);
                  }

                  if (world instanceof Level _levelxxx) {
                     _levelxxx.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
                  }
               }

               if (!world.isClientSide()) {
                  BlockPos _bpxxxxxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxxxxxx = world.getBlockEntity(_bpxxxxxxx);
                  BlockState _bsxxxxxxx = world.getBlockState(_bpxxxxxxx);
                  if (_blockEntityxxxxxxx != null) {
                     _blockEntityxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 20.0);
                  }

                  if (world instanceof Level _levelxxx) {
                     _levelxxx.sendBlockUpdated(_bpxxxxxxx, _bsxxxxxxx, _bsxxxxxxx, 3);
                  }
               }
            } else if (!world.isClientSide()) {
               BlockPos _bpxxxxxxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxxxxxxx = world.getBlockEntity(_bpxxxxxxxx);
               BlockState _bsxxxxxxxx = world.getBlockState(_bpxxxxxxxx);
               if (_blockEntityxxxxxxxx != null) {
                  _blockEntityxxxxxxxx.getPersistentData().putDouble("Progress", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Progress") - 1.0);
               }

               if (world instanceof Level _levelxxx) {
                  _levelxxx.sendBlockUpdated(_bpxxxxxxxx, _bsxxxxxxxx, _bsxxxxxxxx, 3);
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
         BlockPos _bpxxxxxxxxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxx);
         BlockState _bsxxxxxxxxx = world.getBlockState(_bpxxxxxxxxx);
         if (_blockEntityxxxxxxxxx != null) {
            _blockEntityxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 1.0);
         }

         if (world instanceof Level _levelxxx) {
            _levelxxx.sendBlockUpdated(_bpxxxxxxxxx, _bsxxxxxxxxx, _bsxxxxxxxxx, 3);
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Heat") >= 1500.0 && !world.isClientSide()) {
         BlockPos _bpxxxxxxxxxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxxxxxxxxxx = world.getBlockEntity(_bpxxxxxxxxxx);
         BlockState _bsxxxxxxxxxx = world.getBlockState(_bpxxxxxxxxxx);
         if (_blockEntityxxxxxxxxxx != null) {
            _blockEntityxxxxxxxxxx.getPersistentData().putDouble("Heat", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Heat") - 10.0);
         }

         if (world instanceof Level _levelxxx) {
            _levelxxx.sendBlockUpdated(_bpxxxxxxxxxx, _bsxxxxxxxxxx, _bsxxxxxxxxxx, 3);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("BlastFurnaceUpdateTickProcedure.execute", _wtSafe);
      }
   }
}
