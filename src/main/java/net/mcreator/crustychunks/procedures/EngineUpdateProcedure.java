package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class EngineUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      String direction = "";
      boolean found = false;
      double power = 0.0;
      double multiplier = 0.0;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      double maxpower = 0.0;
      double fuelusage = 0.0;
      EngineThrottleSystemProcedure.execute(world, x, y, z);
      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.SMALL_DIESEL_ENGINE.get()) {
         maxpower = 35.0;
         fuelusage = 0.6;
      } else if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.MEDIUM_DIESEL_ENGINE.get()) {
         maxpower = 50.0;
         fuelusage = 1.0;
      }

      power = 0.0;
      if (!world.isClientSide()) {
         BlockPos _bp = BlockPos.containing(x, y, z);
         BlockEntity _blockEntity = world.getBlockEntity(_bp);
         BlockState _bs = world.getBlockState(_bp);
         if (_blockEntity != null) {
            _blockEntity.getPersistentData().putString("FuelType", "Diesel");
         }

         if (world instanceof Level _level) {
            _level.sendBlockUpdated(_bp, _bs, _bs, 3);
         }
      }

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
         }).getBlockTanks(world, BlockPos.containing(x, y, z))) > 0
         && (new Object() {
            public String getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
            }
         }).getValue(world, BlockPos.containing(x, y, z), "FuelType").equals("Diesel")
         && (new Object() {
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
            .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.DIESEL.get(), 1))) {
         label120: {
            if (world instanceof Level _level12 && _level12.hasNeighborSignal(BlockPos.containing(x, y, z))) {
               power = maxpower;
               break label120;
            }

            if ((new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Throttle") > 0.0) {
               power = (new Object() {
                  public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Throttle") * (maxpower / 10.0);
            } else {
               power = 0.0;
            }
         }

         if (0.0 >= (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "FuelQue")) {
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
               .drainTankSimulate(world, BlockPos.containing(x + sx, y + sy, z + sz), (int)(power / 5.0 * fuelusage));
            if (_ent != null) {
               {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) capability.drain(_amount, FluidAction.EXECUTE);
}
            }

            if (!world.isClientSide()) {
               BlockPos _bpx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityx = world.getBlockEntity(_bpx);
               BlockState _bsx = world.getBlockState(_bpx);
               if (_blockEntityx != null) {
                  _blockEntityx.getPersistentData().putDouble("FuelQue", 100.0);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpx, _bsx, _bsx, 3);
               }
            }
         }
      }

      if (0.0 < power) {
         if (2.0 >= (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Stage")) {
            if (!world.isClientSide()) {
               BlockPos _bpxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxx = world.getBlockEntity(_bpxx);
               BlockState _bsxx = world.getBlockState(_bpxx);
               if (_blockEntityxx != null) {
                  _blockEntityxx.getPersistentData().putDouble("Stage", (new Object() {
                     public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
                     }
                  }).getValue(world, BlockPos.containing(x, y, z), "Stage") + 1.0);
               }

               if (world instanceof Level _level) {
                  _level.sendBlockUpdated(_bpxx, _bsxx, _bsxx, 3);
               }
            }
         } else if (2.0 < (new Object() {
            public double getValue(LevelAccessor world, BlockPos pos, String tag) {
               BlockEntity blockEntity = world.getBlockEntity(pos);
               return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
            }
         }).getValue(world, BlockPos.containing(x, y, z), "Stage")) {
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:engine")),
                     SoundSource.BLOCKS,
                     (float)(power / 10.0),
                     (float)(0.8 + power / maxpower + Mth.nextDouble(RandomSource.create(), -0.05, 0.05))
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:engine")),
                     SoundSource.BLOCKS,
                     (float)(power / 10.0),
                     (float)(0.8 + power / maxpower + Mth.nextDouble(RandomSource.create(), -0.05, 0.05)),
                     false
                  );
               }
            }

            if (!world.isClientSide()) {
               BlockPos _bpxxx = BlockPos.containing(x, y, z);
               BlockEntity _blockEntityxxx = world.getBlockEntity(_bpxxx);
               BlockState _bsxxx = world.getBlockState(_bpxxx);
               if (_blockEntityxxx != null) {
                  _blockEntityxxx.getPersistentData().putDouble("Stage", 0.0);
               }

               if (world instanceof Level _levelx) {
                  _levelx.sendBlockUpdated(_bpxxx, _bsxxx, _bsxxx, 3);
               }
            }
         }

         if (!world.isClientSide()) {
            BlockPos _bpxxxx = BlockPos.containing(x, y, z);
            BlockEntity _blockEntityxxxx = world.getBlockEntity(_bpxxxx);
            BlockState _bsxxxx = world.getBlockState(_bpxxxx);
            if (_blockEntityxxxx != null) {
               _blockEntityxxxx.getPersistentData().putDouble("KineticPower", power);
            }

            if (world instanceof Level _levelx) {
               _levelx.sendBlockUpdated(_bpxxxx, _bsxxxx, _bsxxxx, 3);
            }
         }
      } else if (!world.isClientSide()) {
         BlockPos _bpxxxxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxxxxx = world.getBlockEntity(_bpxxxxx);
         BlockState _bsxxxxx = world.getBlockState(_bpxxxxx);
         if (_blockEntityxxxxx != null) {
            _blockEntityxxxxx.getPersistentData().putDouble("KineticPower", 0.0);
         }

         if (world instanceof Level _levelx) {
            _levelx.sendBlockUpdated(_bpxxxxx, _bsxxxxx, _bsxxxxx, 3);
         }
      }

      if (0.0 < (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "FuelQue") && !world.isClientSide()) {
         BlockPos _bpxxxxxx = BlockPos.containing(x, y, z);
         BlockEntity _blockEntityxxxxxx = world.getBlockEntity(_bpxxxxxx);
         BlockState _bsxxxxxx = world.getBlockState(_bpxxxxxx);
         if (_blockEntityxxxxxx != null) {
            _blockEntityxxxxxx.getPersistentData().putDouble("FuelQue", (new Object() {
               public double getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
               }
            }).getValue(world, BlockPos.containing(x, y, z), "FuelQue") - 1.0);
         }

         if (world instanceof Level _levelx) {
            _levelx.sendBlockUpdated(_bpxxxxxx, _bsxxxxxx, _bsxxxxxx, 3);
         }
      }

      if ((new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Damage") >= 2.0 && world instanceof ServerLevel _levelx) {
         _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x + 0.5, y + 1.1, z + 0.5, 1, 0.0, 1.0, 0.0, 0.1);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("EngineUpdateProcedure.execute", _wtSafe);
      }
   }
}
