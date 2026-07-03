package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class FuelTankDamageTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (0 < (new Object() {
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
      }).getBlockTanks(world, BlockPos.containing(x, y, z)))) {
         if (world instanceof ServerLevel _level) {
            _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x + 0.5, y + 0.5, z + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
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
            .drainTankSimulate(world, BlockPos.containing(x, y, z), 10);
         if (_ent != null) {
            {
   IFluidHandler capability = WariumCaps.fluid(_ent, null).orElse(null);
   if (capability != null) capability.drain(_amount, FluidAction.EXECUTE);
}
         }

         if ((new Object() {
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
               .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.PETROLIUM.get(), 1))) {
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     (float)(
                        2
                           - (new Object() {
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
                              / 1000
                     )
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     (float)(
                        2
                           - (new Object() {
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
                              / 1000
                     ),
                     false
                  );
               }
            }

            if (world instanceof ServerLevel _levelx) {
               _levelx.sendParticles(ParticleTypes.FLAME, x + 0.5, y + 0.5, z + 0.5, 4, 0.2, 0.2, 0.2, 0.0);
            }

            if (world instanceof ServerLevel _levelx) {
               _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.RISING_FLAME.get(), x + 0.5, y + 0.5, z + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
            }

            DamagesProcedure.execute(
               world,
               x + (double)Mth.nextInt(RandomSource.create(), -1, 1),
               y + (double)Mth.nextInt(RandomSource.create(), -1, 1),
               z + (double)Mth.nextInt(RandomSource.create(), -1, 1)
            );
         }
      }
   }
}
