package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class OilFireboxUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if ((new Object() {
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
      }).getFluidTankLevel(world, BlockPos.containing(x, y, z), (new Object() {
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
      }).getBlockTanks(world, BlockPos.containing(x, y, z))) > 0) {
         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                  SoundSource.BLOCKS,
                  3.0F,
                  1.0F
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                  SoundSource.BLOCKS,
                  3.0F,
                  1.0F,
                  false
               );
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bp = BlockPos.containing(x + 1.0, y, z);
            BlockEntity _blockEntity = world.getBlockEntity(_bp);
            BlockState _bs = world.getBlockState(_bp);
            if (_blockEntity != null) {
               _blockEntity.getPersistentData().putDouble("Heat", (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x + 1.0, y, z), "Heat") + 5.0);
            }

            if (world instanceof Level _levelx) {
               _levelx.sendBlockUpdated(_bp, _bs, _bs, 3);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpx = BlockPos.containing(x - 1.0, y, z);
            BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
            BlockState _bsx = world.getBlockState(_bpx);
            if (_blockEntityx != null) {
               _blockEntityx.getPersistentData().putDouble("Heat", (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x - 1.0, y, z), "Heat") + 5.0);
            }

            if (world instanceof Level _levelx) {
               _levelx.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpxx = BlockPos.containing(x, y, z + 1.0);
            BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
            BlockState _bsxx = world.getBlockState(_bpxx);
            if (_blockEntityxx != null) {
               _blockEntityxx.getPersistentData().putDouble("Heat", (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z + 1.0), "Heat") + 5.0);
            }

            if (world instanceof Level _levelx) {
               _levelx.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpxxx = BlockPos.containing(x, y, z - 1.0);
            BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
            BlockState _bsxxx = world.getBlockState(_bpxxx);
            if (_blockEntityxxx != null) {
               _blockEntityxxx.getPersistentData().putDouble("Heat", (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z - 1.0), "Heat") + 5.0);
            }

            if (world instanceof Level _levelx) {
               _levelx.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpxxxx = BlockPos.containing(x, y + 1.0, z);
            BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
            BlockState _bsxxxx = world.getBlockState(_bpxxxx);
            if (_blockEntityxxxx != null) {
               _blockEntityxxxx.getPersistentData().putDouble("Heat", (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y + 1.0, z), "Heat") + 5.0);
            }

            if (world instanceof Level _levelx) {
               _levelx.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
            }
         }

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles(ParticleTypes.FLAME, x + 0.5, y + 0.5, z + 0.5, 10, 0.2, 0.3, 0.2, 0.01);
         }

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + 0.5, y + 0.5, z + 0.5, 10, 0.2, 0.3, 0.2, 0.01);
         }

         BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
         int _amount = (new Object() {
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
            .drainTankSimulate(world, BlockPos.containing(x, y, z), 2);
         if (_ent != null) {
            {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) capability.drain(_amount, FluidAction.EXECUTE);
}
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("OilFireboxUpdateProcedure.execute", _wtSafe);
      }
   }
}
