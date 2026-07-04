package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class EnergyNodeTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      try {
      boolean targetinputonly = false;
      boolean targetoutputonly = false;
      double TestNumber = 0.0;
      double TestNumber2 = 0.0;
      double powerx = 0.0;
      double powery = 0.0;
      double powerz = 0.0;
      double targetx = 0.0;
      double targety = 0.0;
      double targetz = 0.0;
      powerx = (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "PowerX");
      powery = (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "PowerY");
      powerz = (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "PowerZ");
      targetx = x - (double)(new Object() {
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
      targety = y - (double)(new Object() {
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
      targetz = z - (double)(new Object() {
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
      if ((powerx != 0.0 || powery != 0.0 || powerz != 0.0)
         && world.getBlockState(BlockPos.containing(powerx, powery, powerz)).getBlock() == CrustyChunksModBlocks.ENERGY_NODE.get()
         && (new Object() {
            public int getEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
               }

               return _retval.get();
            }
         }).getEnergyStored(world, BlockPos.containing(x, y, z)) > (new Object() {
            public int getEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
               }

               return _retval.get();
            }
         }).getEnergyStored(world, BlockPos.containing(powerx, powery, powerz))) {
         TestNumber = (double)(new Object() {
            public int receiveEnergySimulate(LevelAccessor level, BlockPos pos, int _amount) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.receiveEnergy(_amount, true));
}
               }

               return _retval.get();
            }
         }).receiveEnergySimulate(world, BlockPos.containing(powerx, powery, powerz), (new Object() {
            public int getEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
               }

               return _retval.get();
            }
         }).getEnergyStored(world, BlockPos.containing(x, y, z)));
         TestNumber2 = (double)(new Object() {
            public int extractEnergySimulate(LevelAccessor level, BlockPos pos, int _amount) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.extractEnergy(_amount, true));
}
               }

               return _retval.get();
            }
         }).extractEnergySimulate(world, BlockPos.containing(x, y, z), (int)TestNumber);
         BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
         int _amount = (int)(TestNumber2 / 2.0);
         if (_ent != null) {
            {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) capability.extractEnergy(_amount, false);
}
         }

         _ent = world.getBlockEntity(BlockPos.containing(powerx, powery, powerz));
         _amount = (int)(TestNumber2 / 2.0);
         if (_ent != null) {
            {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) capability.receiveEnergy(_amount, false);
}
         }
      }

      if (((new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(powerx, powery, powerz), "PowerX") != x || (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(powerx, powery, powerz), "PowerY") != y || (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(powerx, powery, powerz), "PowerZ") != z)
         && world.getBlockState(BlockPos.containing(powerx, powery, powerz)).getBlock() == CrustyChunksModBlocks.ENERGY_NODE.get()
         && world.getBlockState(BlockPos.containing(powerx, powery, powerz)).getBlock() == CrustyChunksModBlocks.ENERGY_NODE.get()) {
         if (!world.isClientSide()) {
            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockEntity _blockEntity = world.getBlockEntity(_bp);
            BlockState _bs = world.getBlockState(_bp);
            if (_blockEntity != null) {
               _blockEntity.getPersistentData().putDouble("PowerX", 0.0);
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bp, _bs, _bs, 3);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
            BlockState _bsx = world.getBlockState(_bpx);
            if (_blockEntityx != null) {
               _blockEntityx.getPersistentData().putDouble("PowerY", 0.0);
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
            BlockState _bsxx = world.getBlockState(_bpxx);
            if (_blockEntityxx != null) {
               _blockEntityxx.getPersistentData().putDouble("Powerz", 0.0);
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpxxx = BlockPos.containing(powerx, powery, powerz);
            BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
            BlockState _bsxxx = world.getBlockState(_bpxxx);
            if (_blockEntityxxx != null) {
               _blockEntityxxx.getPersistentData().putDouble("PowerX", 0.0);
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpxxxx = BlockPos.containing(powerx, powery, powerz);
            BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
            BlockState _bsxxxx = world.getBlockState(_bpxxxx);
            if (_blockEntityxxxx != null) {
               _blockEntityxxxx.getPersistentData().putDouble("PowerY", 0.0);
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpxxxxx = BlockPos.containing(powerx, powery, powerz);
            BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
            BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
            if (_blockEntityxxxxx != null) {
               _blockEntityxxxxx.getPersistentData().putDouble("Powerz", 0.0);
            }

            if (world instanceof Level _level) {
               _level.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
            }
         }

         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                  SoundSource.BLOCKS,
                  1.0F,
                  1.0F
               );
            } else {
               _level.playLocalSound(
                  x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")), SoundSource.BLOCKS, 1.0F, 1.0F, false
               );
            }
         }
      }

      if (world.getBlockState(BlockPos.containing(targetx, targety, targetz)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:poweroutputonly")))) {
         targetoutputonly = true;
      }

      if (world.getBlockState(BlockPos.containing(targetx, targety, targetz)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:powerinputonly")))) {
         targetinputonly = true;
      }

      if ((new Object() {
         public boolean canReceiveEnergy(LevelAccessor level, BlockPos pos) {
            AtomicBoolean _retval = new AtomicBoolean(false);
            BlockEntity _ent = level.getBlockEntity(pos);
            if (_ent != null) {
               {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.canReceive());
}
            }

            return _retval.get();
         }
      }).canReceiveEnergy(world, BlockPos.containing(targetx, targety, targetz)) || (new Object() {
         public boolean canExtractEnergy(LevelAccessor level, BlockPos pos) {
            AtomicBoolean _retval = new AtomicBoolean(false);
            BlockEntity _ent = level.getBlockEntity(pos);
            if (_ent != null) {
               {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.canExtract());
}
            }

            return _retval.get();
         }
      }).canExtractEnergy(world, BlockPos.containing(targetx, targety, targetz))) {
         if ((targetoutputonly || (new Object() {
            public int getEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
               }

               return _retval.get();
            }
         }).getEnergyStored(world, BlockPos.containing(x, y, z)) < (new Object() {
            public int getMaxEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getMaxEnergyStored());
}
               }

               return _retval.get();
            }
         }).getMaxEnergyStored(world, BlockPos.containing(x, y, z)) / 2 && (new Object() {
            public int getEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
               }

               return _retval.get();
            }
         }).getEnergyStored(world, BlockPos.containing(x, y, z)) < (new Object() {
            public int getEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
               }

               return _retval.get();
            }
         }).getEnergyStored(world, BlockPos.containing(targetx, targety, targetz)) && !targetinputonly) && (new Object() {
            public boolean canExtractEnergy(LevelAccessor level, BlockPos pos) {
               AtomicBoolean _retval = new AtomicBoolean(false);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.canExtract());
}
               }

               return _retval.get();
            }
         }).canExtractEnergy(world, BlockPos.containing(targetx, targety, targetz))) {
            TestNumber = (double)(new Object() {
               public int receiveEnergySimulate(LevelAccessor level, BlockPos pos, int _amount) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.receiveEnergy(_amount, true));
}
                  }

                  return _retval.get();
               }
            }).receiveEnergySimulate(world, BlockPos.containing(x, y, z), (new Object() {
               public int getEnergyStored(LevelAccessor level, BlockPos pos) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
                  }

                  return _retval.get();
               }
            }).getEnergyStored(world, BlockPos.containing(targetx, targety, targetz)));
            TestNumber2 = (double)(new Object() {
               public int extractEnergySimulate(LevelAccessor level, BlockPos pos, int _amount) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.extractEnergy(_amount, true));
}
                  }

                  return _retval.get();
               }
            }).extractEnergySimulate(world, BlockPos.containing(targetx, targety, targetz), (int)TestNumber);
            BlockEntity _entx = world.getBlockEntity(BlockPos.containing(targetx, targety, targetz));
            int _amountx = (int)(TestNumber2 / 2.0);
            if (_entx != null) {
               {
   IEnergyStorage capability = WariumCaps.energy(_entx, null).orElse(null);
   if (capability != null) capability.extractEnergy(_amountx, false);
}
            }

            _entx = world.getBlockEntity(BlockPos.containing(x, y, z));
            _amountx = (int)(TestNumber2 / 2.0);
            if (_entx != null) {
               {
   IEnergyStorage capability = WariumCaps.energy(_entx, null).orElse(null);
   if (capability != null) capability.receiveEnergy(_amountx, false);
}
            }
         } else if ((targetinputonly || ((new Object() {
            public int getEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
               }

               return _retval.get();
            }
         }).getEnergyStored(world, BlockPos.containing(x, y, z)) > (new Object() {
            public int getEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
               }

               return _retval.get();
            }
         }).getEnergyStored(world, BlockPos.containing(targetx, targety, targetz)) || (new Object() {
            public int getEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
               }

               return _retval.get();
            }
         }).getEnergyStored(world, BlockPos.containing(targetx, targety, targetz)) < (new Object() {
            public int getMaxEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getMaxEnergyStored());
}
               }

               return _retval.get();
            }
         }).getMaxEnergyStored(world, BlockPos.containing(targetx, targety, targetz))) && !targetoutputonly) && (new Object() {
            public boolean canReceiveEnergy(LevelAccessor level, BlockPos pos) {
               AtomicBoolean _retval = new AtomicBoolean(false);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.canReceive());
}
               }

               return _retval.get();
            }
         }).canReceiveEnergy(world, BlockPos.containing(targetx, targety, targetz))) {
            TestNumber = (double)(new Object() {
               public int receiveEnergySimulate(LevelAccessor level, BlockPos pos, int _amountx) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.receiveEnergy(_amountx, true));
}
                  }

                  return _retval.get();
               }
            }).receiveEnergySimulate(world, BlockPos.containing(targetx, targety, targetz), (new Object() {
               public int getEnergyStored(LevelAccessor level, BlockPos pos) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
                  }

                  return _retval.get();
               }
            }).getEnergyStored(world, BlockPos.containing(x, y, z)));
            TestNumber2 = (double)(new Object() {
               public int extractEnergySimulate(LevelAccessor level, BlockPos pos, int _amountx) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.extractEnergy(_amountx, true));
}
                  }

                  return _retval.get();
               }
            }).extractEnergySimulate(world, BlockPos.containing(x, y, z), (int)TestNumber);
            BlockEntity _entxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            int _amountxx = (int)(TestNumber2 / 2.0);
            if (_entxx != null) {
               {
   IEnergyStorage capability = WariumCaps.energy(_entxx, null).orElse(null);
   if (capability != null) capability.extractEnergy(_amountxx, false);
}
            }

            _entxx = world.getBlockEntity(BlockPos.containing(targetx, targety, targetz));
            _amountxx = (int)(TestNumber2 / 2.0);
            if (_entxx != null) {
               {
   IEnergyStorage capability = WariumCaps.energy(_entxx, null).orElse(null);
   if (capability != null) capability.receiveEnergy(_amountxx, false);
}
            }
         } else if ((new Object() {
            public int getEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
               }

               return _retval.get();
            }
         }).getEnergyStored(world, BlockPos.containing(x, y, z)) > (new Object() {
            public int getMaxEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getMaxEnergyStored());
}
               }

               return _retval.get();
            }
         }).getMaxEnergyStored(world, BlockPos.containing(x, y, z)) / 2 && (new Object() {
            public boolean canReceiveEnergy(LevelAccessor level, BlockPos pos) {
               AtomicBoolean _retval = new AtomicBoolean(false);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.canReceive());
}
               }

               return _retval.get();
            }
         }).canReceiveEnergy(world, BlockPos.containing(targetx, targety, targetz)) && (new Object() {
            public int getEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
               }

               return _retval.get();
            }
         }).getEnergyStored(world, BlockPos.containing(targetx, targety, targetz)) < (new Object() {
            public int getMaxEnergyStored(LevelAccessor level, BlockPos pos) {
               AtomicInteger _retval = new AtomicInteger(0);
               BlockEntity _ent = level.getBlockEntity(pos);
               if (_ent != null) {
                  {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getMaxEnergyStored());
}
               }

               return _retval.get();
            }
         }).getMaxEnergyStored(world, BlockPos.containing(targetx, targety, targetz))) {
            TestNumber = (double)(new Object() {
               public int receiveEnergySimulate(LevelAccessor level, BlockPos pos, int _amountxx) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.receiveEnergy(_amountxx, true));
}
                  }

                  return _retval.get();
               }
            }).receiveEnergySimulate(world, BlockPos.containing(targetx, targety, targetz), (new Object() {
               public int getEnergyStored(LevelAccessor level, BlockPos pos) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.getEnergyStored());
}
                  }

                  return _retval.get();
               }
            }).getEnergyStored(world, BlockPos.containing(x, y, z)));
            TestNumber2 = (double)(new Object() {
               public int extractEnergySimulate(LevelAccessor level, BlockPos pos, int _amountxx) {
                  AtomicInteger _retval = new AtomicInteger(0);
                  BlockEntity _ent = level.getBlockEntity(pos);
                  if (_ent != null) {
                     {
   IEnergyStorage capability = WariumCaps.energy(_ent, null).orElse(null);
   if (capability != null) _retval.set(capability.extractEnergy(_amountxx, true));
}
                  }

                  return _retval.get();
               }
            }).extractEnergySimulate(world, BlockPos.containing(x, y, z), (int)TestNumber);
            BlockEntity _entxxx = world.getBlockEntity(BlockPos.containing(x, y, z));
            int _amountxxx = (int)(TestNumber2 / 2.0);
            if (_entxxx != null) {
               {
   IEnergyStorage capability = WariumCaps.energy(_entxxx, null).orElse(null);
   if (capability != null) capability.extractEnergy(_amountxxx, false);
}
            }

            _entxxx = world.getBlockEntity(BlockPos.containing(targetx, targety, targetz));
            _amountxxx = (int)(TestNumber2 / 2.0);
            if (_entxxx != null) {
               {
   IEnergyStorage capability = WariumCaps.energy(_entxxx, null).orElse(null);
   if (capability != null) capability.receiveEnergy(_amountxxx, false);
}
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("EnergyNodeTickUpdateProcedure.execute", _wtSafe);
      }
   }
}
