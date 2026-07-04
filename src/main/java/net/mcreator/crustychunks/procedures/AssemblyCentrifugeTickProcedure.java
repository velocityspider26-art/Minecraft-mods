package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModGameRules;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

public class AssemblyCentrifugeTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      try {
      boolean sufficientheat = false;
      ItemStack result = ItemStack.EMPTY;
      ItemStack input = ItemStack.EMPTY;
      ItemStack Result4 = ItemStack.EMPTY;
      ItemStack Result3 = ItemStack.EMPTY;
      ItemStack Result2 = ItemStack.EMPTY;
      ItemStack Result1 = ItemStack.EMPTY;
      ItemStack catalyst = ItemStack.EMPTY;
      BlockState bottomblock = Blocks.AIR.defaultBlockState();
      double XTrigger = 0.0;
      double ZTrigger = 0.0;
      double Chance4 = 0.0;
      double OffsetX = 0.0;
      double OffsetZ = 0.0;
      double Chance3 = 0.0;
      double Chance2 = 0.0;
      double Chance1 = 0.0;
      double kineticpower = 0.0;
      double Power = 0.0;
      double passes = 0.0;
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
      if (0.0 < (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x - (double)(new Object() {
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
      }).getDirection(world.getBlockState(BlockPos.containing(x, y - 1.0, z))).getStepX(), y - 1.0, z - (double)(new Object() {
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
      }).getDirection(world.getBlockState(BlockPos.containing(x, y - 1.0, z))).getStepZ()), "KineticPower") && (new Object() {
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
      }).getDirection(world.getBlockState(BlockPos.containing(x - (double)(new Object() {
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
      }).getDirection(world.getBlockState(BlockPos.containing(x, y - 1.0, z))).getStepX(), y - 1.0, z - (double)(new Object() {
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
      }).getDirection(world.getBlockState(BlockPos.containing(x, y - 1.0, z))).getStepZ()))) == (new Object() {
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
      }).getDirection(world.getBlockState(BlockPos.containing(x, y - 1.0, z)))) {
         Power = (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x - (double)(new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y - 1.0, z))).getStepX(), y - 1.0, z - (double)(new Object() {
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
         }).getDirection(world.getBlockState(BlockPos.containing(x, y - 1.0, z))).getStepZ()), "KineticPower");
      } else {
         Power = 0.0;
      }

      if (world.getBlockState(BlockPos.containing(x - OffsetX, y, z - OffsetZ)).getBlock() == CrustyChunksModBlocks.PRODUCTION_INPUT.get()
         && world.getBlockState(BlockPos.containing(x + OffsetX, y, z + OffsetZ)).getBlock() == CrustyChunksModBlocks.PRODUCTION_OUTPUT.get()
         && 30.0 < Power
         && world.getBlockState(BlockPos.containing(x, y - 1.0, z)).getBlock() == CrustyChunksModBlocks.ASSEMBLY_CENTRIFUGE_BOTTOM.get()
         && world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == CrustyChunksModBlocks.ASSEMBLY_CENTRIFUGE_TOP.get()
         && world.getBlockState(BlockPos.containing(x, y - 2.0, z)).getBlock() == CrustyChunksModBlocks.GIANT_COIL.get()) {
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
         if (input.getItem() == CrustyChunksModItems.URANIUM_NEUTRAL_DUST.get()) {
            Result1 = new ItemStack((ItemLike)CrustyChunksModItems.URANIUM_ENRICHED_TINY_DUST.get()).copy();
            Chance1 = 1.0;
            Result2 = new ItemStack((ItemLike)CrustyChunksModItems.URANIUM_DEPLETED_DUST.get()).copy();
            Chance2 = 0.8;
            Result3 = new ItemStack((ItemLike)CrustyChunksModItems.LEAD_DUST.get()).copy();
            Chance3 = 0.05;
            passes = (double)world.getLevelData().getGameRules().getInt(CrustyChunksModGameRules.ENRICHMENT_TIME);
         } else if (input.getItem() == CrustyChunksModItems.LITHIUM_DUST.get()) {
            Result1 = new ItemStack((ItemLike)CrustyChunksModItems.ENRICHED_LITHIUM_NUGGET.get()).copy();
            Chance1 = 1.0;
            passes = (double)world.getLevelData().getGameRules().getInt(CrustyChunksModGameRules.ENRICHMENT_TIME);
         } else if (input.getItem() == CrustyChunksModItems.ALUMINATE_DUST.get()) {
            Result1 = new ItemStack((ItemLike)CrustyChunksModItems.FILTERED_ALUMINATE_DUST.get()).copy();
            Chance1 = 1.0;
            Result2 = new ItemStack(Blocks.RED_SAND).copy();
            Chance2 = 0.25;
            passes = 4.0;
         } else if (input.getItem() == CrustyChunksModItems.FILTERED_PYROCHLORE_DUST.get()) {
            Result1 = new ItemStack((ItemLike)CrustyChunksModItems.NIOBIUM_TINY_DUST.get()).copy();
            Chance1 = 1.0;
            Result2 = new ItemStack((ItemLike)CrustyChunksModItems.NIOBIUM_TINY_DUST.get()).copy();
            Chance2 = 0.25;
            Result3 = new ItemStack((ItemLike)CrustyChunksModItems.SULFUR.get()).copy();
            Chance3 = 0.1;
            Result4 = new ItemStack(Items.QUARTZ).copy();
            Chance4 = 0.1;
            passes = 80.0;
         } else {
            Result1 = input.copy();
            Chance1 = 1.0;
            passes = 0.0;
         }

         if (input.getItem() != ItemStack.EMPTY.getItem() && ((new Object() {
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
         }).getItemStack(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 0).getItem() == ItemStack.EMPTY.getItem() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 0).getItem() == Result1.getItem() && (new Object() {
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
         }).getAmount(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 0) < Result1.getMaxStackSize()) && ((new Object() {
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
         }).getItemStack(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 1).getItem() == ItemStack.EMPTY.getItem() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 1).getItem() == Result2.getItem() && (new Object() {
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
         }).getAmount(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 1) < Result2.getMaxStackSize()) && ((new Object() {
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
         }).getItemStack(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 2).getItem() == ItemStack.EMPTY.getItem() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 2).getItem() == Result3.getItem() && (new Object() {
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
         }).getAmount(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 2) < Result3.getMaxStackSize()) && ((new Object() {
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
         }).getItemStack(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 3).getItem() == ItemStack.EMPTY.getItem() || (new Object() {
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
         }).getItemStack(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 3).getItem() == Result4.getItem() && (new Object() {
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
         }).getAmount(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 3) < Result4.getMaxStackSize())) {
            if (passes > (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "progress")) {
               if (!world.isClientSide()) {
                  BlockPos _bp = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntity = world.getBlockEntity(_bp);
                  BlockState _bs = world.getBlockState(_bp);
                  if (_blockEntity != null) {
                     _blockEntity.getPersistentData().putDouble("progress", (new Object() {
                        public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                           BlockEntity blockEntity = world.getBlockEntity(pos);
                           return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                        }
                     }).getValue(world, BlockPos.containing(x, y, z), "progress") + 1.0);
                  }

                  if (world instanceof Level _level) {
                     _level.sendBlockUpdated(_bp, _bs, _bs, 3);
                  }
               }

               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.conduit.ambient")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)(1.0 + (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Progress") / passes * 3.0)
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.conduit.ambient")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)(1.0 + (new Object() {
                           public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                              BlockEntity blockEntity = world.getBlockEntity(pos);
                              return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                           }
                        }).getValue(world, BlockPos.containing(x, y, z), "Progress") / passes * 3.0),
                        false
                     );
                  }
               }

               if (world instanceof ServerLevel _levelx) {
                  _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SPARKS.get(), x + 0.5, y + 0.9, z + 0.5, 5, 0.1, 0.1, 0.1, 0.5);
               }
            } else {
               if (!world.isClientSide()) {
                  BlockPos _bpx = BlockPos.containing(x, y, z);
                  BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
                  BlockState _bsx = world.getBlockState(_bpx);
                  if (_blockEntityx != null) {
                     _blockEntityx.getPersistentData().putDouble("progress", 0.0);
                  }

                  if (world instanceof Level _levelx) {
                     _levelx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
                  }
               }

               if (Result1.getItem() != ItemStack.EMPTY.getItem()) {
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

                  if (Mth.nextDouble(RandomSource.create(), 0.0, 1.0) < Chance1) {
                     _ent = world.getBlockEntity(BlockPos.containing(x + OffsetX, y, z + OffsetZ));
                     if (_ent != null) {
                        int _slotid = 0;
                        ItemStack _setstack = Result1.copy();
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
                                 .getAmount(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 0)
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
                  }

                  if (Result2.getItem() != ItemStack.EMPTY.getItem() && Mth.nextDouble(RandomSource.create(), 0.0, 1.0) < Chance2) {
                     _ent = world.getBlockEntity(BlockPos.containing(x + OffsetX, y, z + OffsetZ));
                     if (_ent != null) {
                        int _slotid = 1;
                        ItemStack _setstack = Result2.copy();
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
                                 .getAmount(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 1)
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
                  }

                  if (Result3.getItem() != ItemStack.EMPTY.getItem() && Mth.nextDouble(RandomSource.create(), 0.0, 1.0) < Chance3) {
                     _ent = world.getBlockEntity(BlockPos.containing(x + OffsetX, y, z + OffsetZ));
                     if (_ent != null) {
                        int _slotid = 2;
                        ItemStack _setstack = Result3.copy();
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
                                 .getAmount(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 2)
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
                  }

                  if (Result4.getItem() != ItemStack.EMPTY.getItem() && Mth.nextDouble(RandomSource.create(), 0.0, 1.0) < Chance4) {
                     _ent = world.getBlockEntity(BlockPos.containing(x + OffsetX, y, z + OffsetZ));
                     if (_ent != null) {
                        int _slotid = 3;
                        ItemStack _setstack = Result4.copy();
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
                                 .getAmount(world, BlockPos.containing(x + OffsetX, y, z + OffsetZ), 3)
                              + 1
                        );
                        {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
                           if (capability instanceof IItemHandlerModifiable) {
                              ((IItemHandlerModifiable)capability).setStackInSlot(3, _setstack);
                           }
                        }
}
                     }
                  }

                  if (input.getItem() != Result1.getItem()) {
                     if (world instanceof ServerLevel _levelx) {
                        _levelx.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x + 0.5, y + 0.5, z + 0.5, 10, 0.6, 0.6, 0.6, 0.1);
                     }

                     if (world instanceof Level _levelx) {
                        if (!_levelx.isClientSide()) {
                           _levelx.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.conduit.activate")),
                              SoundSource.NEUTRAL,
                              20.0F,
                              3.0F
                           );
                        } else {
                           _levelx.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.conduit.activate")),
                              SoundSource.NEUTRAL,
                              20.0F,
                              3.0F,
                              false
                           );
                        }
                     }

                     if (world instanceof Level _levelxx) {
                        if (!_levelxx.isClientSide()) {
                           _levelxx.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.conduit.deactivate")),
                              SoundSource.NEUTRAL,
                              20.0F,
                              3.0F
                           );
                        } else {
                           _levelxx.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.conduit.deactivate")),
                              SoundSource.NEUTRAL,
                              20.0F,
                              3.0F,
                              false
                           );
                        }
                     }
                  }
               }
            }
         }
      }

      if (!world.isClientSide()) {
         BlockPos _bpxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
         BlockState _bsxx = world.getBlockState(_bpxx);
         if (_blockEntityxx != null) {
            _blockEntityxx.getPersistentData().putDouble("ProgressFraction", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "progress") / Math.max(1.0, passes));
         }

         if (world instanceof Level _levelxxx) {
            _levelxxx.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("AssemblyCentrifugeTickProcedure.execute", _wtSafe);
      }
   }
}
