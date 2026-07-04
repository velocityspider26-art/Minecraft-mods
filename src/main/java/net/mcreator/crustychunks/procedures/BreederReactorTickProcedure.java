package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModGameRules;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class BreederReactorTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world.getBlockState(BlockPos.containing(x, y - 2.0, z)).getBlock() == CrustyChunksModBlocks.BREEDER_REACTOR_PORT.get()
         && world.getBlockState(BlockPos.containing(x, y - 1.0, z)).getBlock() == CrustyChunksModBlocks.BREEDER_REACTOR_CORE.get()
         && world.getBlockState(BlockPos.containing(x, y - 1.0, z - 2.0)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x, y - 1.0, z + 2.0)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x - 2.0, y - 1.0, z)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x + 2.0, y - 1.0, z)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y - 1.0, z - 2.0), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y - 1.0, z + 2.0), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x - 2.0, y - 1.0, z), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x + 2.0, y - 1.0, z), "Ready")
         && world.getBlockState(BlockPos.containing(x, y, z - 2.0)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x, y, z + 2.0)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x - 2.0, y, z)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && world.getBlockState(BlockPos.containing(x + 2.0, y, z)).getBlock() == CrustyChunksModBlocks.REACTION_CHAMBER.get()
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z - 2.0), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x, y, z + 2.0), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x - 2.0, y, z), "Ready")
         && (new Object() {
            public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
            }
         }).getValue(world, BlockPos.containing(x + 2.0, y, z), "Ready")) {
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).getItem() == CrustyChunksModItems.URANIUM_ENRICHED_DUST.get() && (new Object() {
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
         }).getAmount(world, BlockPos.containing(x, y - 2.0, z), 0) < 64 && ((new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y - 2.0, z), 0).getItem() == CrustyChunksModItems.PLUTONIUM_NUGGET.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y - 2.0, z), 0).getItem() == ItemStack.EMPTY.getItem())) {
            if ((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "T") >= (double)(world.getLevelData().getGameRules().getInt(CrustyChunksModGameRules.ENRICHMENT_TIME) * 2)) {
               if (!world.isClientSide()) {
                  BlockPos _bp = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntity = world.getBlockEntity(_bp);
                  BlockState _bs = world.getBlockState(_bp);
                  if (_blockEntity != null) {
                     _blockEntity.getPersistentData().putDouble("T", 0.0);
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bp, _bs, _bs, 3);
                  }
               }

               BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y - 2.0, z));
               if (_ent != null) {
                  int _slotid = 0;
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.PLUTONIUM_NUGGET.get()).copy();
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
                           .getAmount(world, BlockPos.containing(x, y - 2.0, z), 0)
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

               if (world instanceof ServerLevel _level) {
                  _level.sendParticles(ParticleTypes.SONIC_BOOM, x + 0.5, y + 0.5, z + 0.5, 15, 0.6, 0.6, 0.6, 0.1);
               }

               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.activate")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        3.0F
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.activate")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        3.0F,
                        false
                     );
                  }
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.activate")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        3.0F
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.activate")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        3.0F,
                        false
                     );
                  }
               }
            } else {
               if (!world.isClientSide()) {
                  BlockPos _bpx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                  BlockState _bsx = world.getBlockState(_bpx);
                  if (_blockEntityx != null) {
                     _blockEntityx.getPersistentData().putDouble("T", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "T") + 1.0);
                  }

                  if (world instanceof Level _levelxx) {
                     _levelxx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                  }
               }

               if (world instanceof ServerLevel _levelxx) {
                  _levelxx.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x + 0.5, y + 0.5, z + 0.5, 10, 0.3, 0.6, 0.3, 0.01);
               }

               if (world instanceof Level _levelxx) {
                  if (!_levelxx.isClientSide()) {
                     _levelxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.ambient")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)(
                           1.0
                              + (new Object() {
                                    public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                                       BlockEntity blockEntity = world.getBlockEntity(pos);
                                       return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                                    }
                                 }).getValue(world, BlockPos.containing(x, y, z), "T")
                                 / (double)(world.getLevelData().getGameRules().getInt(CrustyChunksModGameRules.ENRICHMENT_TIME) * 2)
                                 * 2.0
                        )
                     );
                  } else {
                     _levelxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.ambient")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)(
                           1.0
                              + (new Object() {
                                    public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                                       BlockEntity blockEntity = world.getBlockEntity(pos);
                                       return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                                    }
                                 }).getValue(world, BlockPos.containing(x, y, z), "T")
                                 / (double)(world.getLevelData().getGameRules().getInt(CrustyChunksModGameRules.ENRICHMENT_TIME) * 2)
                                 * 2.0
                        ),
                        false
                     );
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
         }).getItemStack(world, BlockPos.containing(x, y, z), 0).getItem() == CrustyChunksModItems.ENRICHED_LITHIUM_INGOT.get() && (new Object() {
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
         }).getAmount(world, BlockPos.containing(x, y - 2.0, z), 0) < 64 && ((new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y - 2.0, z), 0).getItem() == CrustyChunksModItems.TINY_LITHIUM_DEUTERIDE.get() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y - 2.0, z), 0).getItem() == ItemStack.EMPTY.getItem())) {
            if ((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "T") >= (double)(world.getLevelData().getGameRules().getInt(CrustyChunksModGameRules.ENRICHMENT_TIME) * 2)) {
               if (!world.isClientSide()) {
                  BlockPos _bpxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
                  BlockState _bsxx = world.getBlockState(_bpxx);
                  if (_blockEntityxx != null) {
                     _blockEntityxx.getPersistentData().putDouble("T", 0.0);
                  }

                  if (world instanceof Level _levelxxx) {
                     _levelxxx.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
                  }
               }

               BlockEntity _entx = world.getBlockEntity(BlockPos.containing(x, y - 2.0, z));
               if (_entx != null) {
                  int _slotid = 0;
                  ItemStack _setstack = new ItemStack((ItemLike)CrustyChunksModItems.TINY_LITHIUM_DEUTERIDE.get()).copy();
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
                           .getAmount(world, BlockPos.containing(x, y - 2.0, z), 0)
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

               _entx = world.getBlockEntity(BlockPos.containing(x, y, z));
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

               if (world instanceof ServerLevel _levelxxx) {
                  _levelxxx.sendParticles(ParticleTypes.SONIC_BOOM, x + 0.5, y + 0.5, z + 0.5, 15, 0.6, 0.6, 0.6, 0.1);
               }

               if (world instanceof Level _levelxxx) {
                  if (!_levelxxx.isClientSide()) {
                     _levelxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.activate")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        3.0F
                     );
                  } else {
                     _levelxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.activate")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        3.0F,
                        false
                     );
                  }
               }

               if (world instanceof Level _levelxxxx) {
                  if (!_levelxxxx.isClientSide()) {
                     _levelxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.activate")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        3.0F
                     );
                  } else {
                     _levelxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.activate")),
                        SoundSource.NEUTRAL,
                        20.0F,
                        3.0F,
                        false
                     );
                  }
               }
            } else {
               if (!world.isClientSide()) {
                  BlockPos _bpxxx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
                  BlockState _bsxxx = world.getBlockState(_bpxxx);
                  if (_blockEntityxxx != null) {
                     _blockEntityxxx.getPersistentData().putDouble("T", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "T") + 1.0);
                  }

                  if (world instanceof Level _levelxxxxx) {
                     _levelxxxxx.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
                  }
               }

               if (world instanceof ServerLevel _levelxxxxx) {
                  _levelxxxxx.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x + 0.5, y + 0.5, z + 0.5, 10, 0.3, 0.6, 0.3, 0.01);
               }

               if (world instanceof Level _levelxxxxx) {
                  if (!_levelxxxxx.isClientSide()) {
                     _levelxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.ambient")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)(
                           1.0
                              + (new Object() {
                                    public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                                       BlockEntity blockEntity = world.getBlockEntity(pos);
                                       return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                                    }
                                 }).getValue(world, BlockPos.containing(x, y, z), "T")
                                 / (double)(world.getLevelData().getGameRules().getInt(CrustyChunksModGameRules.ENRICHMENT_TIME) * 2)
                                 * 2.0
                        )
                     );
                  } else {
                     _levelxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.beacon.ambient")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)(
                           1.0
                              + (new Object() {
                                    public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                                       BlockEntity blockEntity = world.getBlockEntity(pos);
                                       return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                                    }
                                 }).getValue(world, BlockPos.containing(x, y, z), "T")
                                 / (double)(world.getLevelData().getGameRules().getInt(CrustyChunksModGameRules.ENRICHMENT_TIME) * 2)
                                 * 2.0
                        ),
                        false
                     );
                  }
               }
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("BreederReactorTickProcedure.execute", _wtSafe);
      }
   }
}
