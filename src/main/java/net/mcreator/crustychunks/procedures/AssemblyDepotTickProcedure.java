package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class AssemblyDepotTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      try {
      new JsonObject();
      boolean Success = false;
      boolean tagged = false;
      double inputslot = 0.0;
      double OffsetX = 0.0;
      double OffsetZ = 0.0;
      double runs = 0.0;
      double outputslot = 0.0;
      double Power = 0.0;
      double specificindex = 0.0;
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
      if (0.0 < TestShaftProcedure.execute(world, x, y + 1.0, z, blockstate, (new Object() {
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
      }).getDirection(world.getBlockState(BlockPos.containing(x, y + 1.0, z))))) {
         Power = TestShaftProcedure.execute(world, x, y + 1.0, z, blockstate, (new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y + 1.0, z))));
      } else {
         Power = 0.0;
      }

      if (0.0 < Power
         && world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == CrustyChunksModBlocks.ASSEMBLY_MACHINE.get()
         && world.getBlockState(BlockPos.containing(x - OffsetX, y, z - OffsetZ)).getBlock() == CrustyChunksModBlocks.PRODUCTION_INPUT.get()
         && world.getBlockState(BlockPos.containing(x + OffsetX, y, z + OffsetZ)).getBlock() == CrustyChunksModBlocks.PRODUCTION_OUTPUT.get()
         && ItemStack.EMPTY.getItem() != (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x - OffsetX, y, z - OffsetZ), 0).getItem()
         && ItemStack.EMPTY.getItem() != (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x, y + 1.0, z), 0).getItem()) {
         Success = false;

         for (int index0 = 0; index0 < CrustyChunksModVariables.recipesloaded.size(); index0++) {
            JsonObject Recipe = CrustyChunksModVariables.recipesloaded.get((int)specificindex).getAsJsonObject();
            if (Recipe.get("type").getAsString().equals("crusty_chunks:assembly")) {
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
                     .getItemStack(world, BlockPos.containing(x - OffsetX, y, z - OffsetZ), 0)
                     .getItem()
                  == BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("item").getAsString().toLowerCase(Locale.ENGLISH)))) {
                  tagged = false;
               } else if (Recipe.get("item").getAsString().contains("c:")
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
                     .getItemStack(world, BlockPos.containing(x - OffsetX, y, z - OffsetZ), 0)
                     .is(ItemTags.create(ResourceLocation.parse(Recipe.get("item").getAsString().toLowerCase(Locale.ENGLISH))))) {
                  tagged = true;
               }

               if ((
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
                              .getItemStack(world, BlockPos.containing(x - OffsetX, y, z - OffsetZ), 0)
                              .getItem()
                           == BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("item").getAsString().toLowerCase(Locale.ENGLISH)))
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
                              .getItemStack(world, BlockPos.containing(x - OffsetX, y, z - OffsetZ), 0)
                              .is(ItemTags.create(ResourceLocation.parse(Recipe.get("item").getAsString().toLowerCase(Locale.ENGLISH))))
                           && tagged
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
                        .getItemStack(world, BlockPos.containing(x, y + 1.0, z), 0)
                        .getItem()
                     == BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("process").getAsString().toLowerCase(Locale.ENGLISH)))
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
                              .getItemStack(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), (int)outputslot)
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
                                 .getItemStack(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), (int)outputslot)
                                 .getItem()
                              == BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("result").getAsString().toLowerCase(Locale.ENGLISH)))
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
                                 .getAmount(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), (int)outputslot)
                              < new ItemStack(
                                    (ItemLike)BuiltInRegistries.ITEM
                                       .get(ResourceLocation.parse(Recipe.get("result").getAsString().toLowerCase(Locale.ENGLISH)))
                                 )
                                 .getMaxStackSize()
                  )
                  && BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("result").getAsString().toLowerCase(Locale.ENGLISH)))
                     != Blocks.AIR.asItem()
                  && (
                     BuiltInRegistries.ITEM
                                 .getOrCreateTag(ItemTags.create(ResourceLocation.parse(Recipe.get("item").getAsString().toLowerCase(Locale.ENGLISH))))
                                 .getRandomElement(RandomSource.create()).map(_h -> (net.minecraft.world.item.Item) _h.value()).orElse(Items.AIR)
                              != Blocks.AIR.asItem()
                           && tagged
                        || BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("item").getAsString().toLowerCase(Locale.ENGLISH)))
                              != Blocks.AIR.asItem()
                           && !tagged
                  )) {
                  runs = Recipe.get("runs").getAsDouble();
                  if ((new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "work") >= runs) {
                     BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x - OffsetX, y, z - OffsetZ));
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

                     _ent = world.getBlockEntity(BlockPos.containing(x, y + 1.0, z));
                     if (_ent != null) {
                        int _slotid = 0;
                        int _amount = 1;
                        {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                           if (capability instanceof IItemHandlerModifiable) {
                              ItemStack _stk = capability.getStackInSlot(0).copy();
                              if (world instanceof ServerLevel _srvlvl) _stk.hurtAndBreak(1, _srvlvl, null, _itmcns -> {});

                              ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                           }
                        }
}
                     }

                     _ent = world.getBlockEntity(BlockPos.containing(x + OffsetX, y, z + OffsetZ));
                     if (_ent != null) {
                        int _slotid = (int)outputslot;
                        ItemStack _setstack = new ItemStack(
                              (ItemLike)BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("result").getAsString().toLowerCase(Locale.ENGLISH)))
                           )
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
                                 .getAmount(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), (int)outputslot)
                              + 1
                        );
                        {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                           if (capability instanceof IItemHandlerModifiable) {
                              ((IItemHandlerModifiable)capability).setStackInSlot(_slotid, _setstack);
                           }
                        }
}
                     }

                     if (!world.isClientSide()) {
                        BlockPos _bp = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntity = world.getBlockEntity(_bp);
                        BlockState _bs = world.getBlockState(_bp);
                        if (_blockEntity != null) {
                           _blockEntity.getPersistentData().putDouble("work", 0.0);
                        }

                        if (world instanceof Level _level) {
                           _level.sendBlockUpdated(_bp, _bs, _bs, 3);
                        }
                     }

                     if (BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("process").getAsString().toLowerCase(Locale.ENGLISH)))
                        == CrustyChunksModItems.MECHANICAL_PRESS.get()) {
                        if (world instanceof Level _level) {
                           if (!_level.isClientSide()) {
                              _level.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 0.8F
                              );
                           } else {
                              _level.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 0.8F,
                                 false
                              );
                           }
                        }
                     } else if (BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("process").getAsString().toLowerCase(Locale.ENGLISH)))
                        == CrustyChunksModItems.MECHANICAL_SHEAR.get()) {
                        if (world instanceof Level _levelx) {
                           if (!_levelx.isClientSide()) {
                              _levelx.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ui.stonecutter.take_result")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 0.8F
                              );
                           } else {
                              _levelx.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ui.stonecutter.take_result")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 0.8F,
                                 false
                              );
                           }
                        }
                     } else if (BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("process").getAsString().toLowerCase(Locale.ENGLISH)))
                        == CrustyChunksModItems.MECHANICAL_BORE.get()) {
                        if (world instanceof Level _levelxx) {
                           if (!_levelxx.isClientSide()) {
                              _levelxx.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.scrape")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 0.8F
                              );
                           } else {
                              _levelxx.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.scrape")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 0.8F,
                                 false
                              );
                           }
                        }
                     } else if (BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("process").getAsString().toLowerCase(Locale.ENGLISH)))
                           == CrustyChunksModItems.MECHANICAL_EXTRUDER.get()
                        && world instanceof Level _levelxxx) {
                        if (!_levelxxx.isClientSide()) {
                           _levelxxx.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.grindstone.use")),
                              SoundSource.NEUTRAL,
                              2.0F,
                              0.8F
                           );
                        } else {
                           _levelxxx.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.grindstone.use")),
                              SoundSource.NEUTRAL,
                              2.0F,
                              0.8F,
                              false
                           );
                        }
                     }

                     if (world instanceof ServerLevel _levelxxxx) {
                        _levelxxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SPARKS.get(), x + 0.5, y + 0.9, z + 0.5, 5, 0.1, 0.1, 0.1, 0.5);
                     }
                  } else {
                     if (!world.isClientSide()) {
                        BlockPos _bpx = BlockPos.containing(x, y, z);
                        BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                        BlockState _bsx = world.getBlockState(_bpx);
                        if (_blockEntityx != null) {
                           _blockEntityx.getPersistentData().putDouble("work", 1.0 * (Power / 25.0) + (new Object() {
                              public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                                 BlockEntity blockEntity = world.getBlockEntity(pos);
                                 return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                              }
                           }).getValue(world, BlockPos.containing(x, y, z), "work"));
                        }

                        if (world instanceof Level _levelxxxx) {
                           _levelxxxx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                        }
                     }

                     BlockEntity _entx = world.getBlockEntity(BlockPos.containing(x, y + 1.0, z));
                     if (_entx != null) {
                        int _slotid = 0;
                        int _amount = 1;
                        {
   IItemHandler capability = WariumCaps.itemHandler(_entx, null).orElse(null);
   if (capability != null) {
                           if (capability instanceof IItemHandlerModifiable) {
                              ItemStack _stk = capability.getStackInSlot(0).copy();
                              if (world instanceof ServerLevel _srvlvl) _stk.hurtAndBreak(1, _srvlvl, null, _itmcns -> {});

                              ((IItemHandlerModifiable)capability).setStackInSlot(0, _stk);
                           }
                        }
}
                     }

                     if (BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("process").getAsString().toLowerCase(Locale.ENGLISH)))
                        == CrustyChunksModItems.MECHANICAL_PRESS.get()) {
                        if (world instanceof Level _levelxxxx) {
                           if (!_levelxxxx.isClientSide()) {
                              _levelxxxx.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 (float)(0.1 + runs / 10.0)
                              );
                           } else {
                              _levelxxxx.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 (float)(0.1 + runs / 10.0),
                                 false
                              );
                           }
                        }
                     } else if (BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("process").getAsString().toLowerCase(Locale.ENGLISH)))
                        == CrustyChunksModItems.MECHANICAL_SHEAR.get()) {
                        if (world instanceof Level _levelxxxxx) {
                           if (!_levelxxxxx.isClientSide()) {
                              _levelxxxxx.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ui.stonecutter.take_result")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 (float)(0.1 + runs / 10.0)
                              );
                           } else {
                              _levelxxxxx.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("ui.stonecutter.take_result")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 (float)(0.1 + runs / 10.0),
                                 false
                              );
                           }
                        }
                     } else if (BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("process").getAsString().toLowerCase(Locale.ENGLISH)))
                        == CrustyChunksModItems.MECHANICAL_BORE.get()) {
                        if (world instanceof Level _levelxxxxxx) {
                           if (!_levelxxxxxx.isClientSide()) {
                              _levelxxxxxx.playSound(
                                 null,
                                 BlockPos.containing(x, y, z),
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.scrape")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 (float)(0.1 + runs / 10.0)
                              );
                           } else {
                              _levelxxxxxx.playLocalSound(
                                 x,
                                 y,
                                 z,
                                 (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.axe.scrape")),
                                 SoundSource.NEUTRAL,
                                 2.0F,
                                 (float)(0.1 + runs / 10.0),
                                 false
                              );
                           }
                        }
                     } else if (BuiltInRegistries.ITEM.get(ResourceLocation.parse(Recipe.get("process").getAsString().toLowerCase(Locale.ENGLISH)))
                           == CrustyChunksModItems.MECHANICAL_EXTRUDER.get()
                        && world instanceof Level _levelxxxxxxx) {
                        if (!_levelxxxxxxx.isClientSide()) {
                           _levelxxxxxxx.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.grindstone.use")),
                              SoundSource.NEUTRAL,
                              2.0F,
                              (float)(0.1 + runs / 10.0)
                           );
                        } else {
                           _levelxxxxxxx.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.grindstone.use")),
                              SoundSource.NEUTRAL,
                              2.0F,
                              (float)(0.1 + runs / 10.0),
                              false
                           );
                        }
                     }

                     if (world instanceof ServerLevel _levelxxxxxxxx) {
                        _levelxxxxxxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SPARKS.get(), x + 0.5, y + 0.9, z + 0.5, 5, 0.1, 0.1, 0.1, 0.5);
                     }
                  }

                  Success = true;
               }
            }

            specificindex++;
         }

         if (!Success && !world.isClientSide()) {
            BlockPos _bpxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
            BlockState _bsxx = world.getBlockState(_bpxx);
            if (_blockEntityxx != null) {
               _blockEntityxx.getPersistentData().putDouble("work", 0.0);
            }

            if (world instanceof Level _levelxxxxxxxx) {
               _levelxxxxxxxx.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("AssemblyDepotTickProcedure.execute", _wtSafe);
      }
   }
}
