package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class FluidDrainProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         ItemStack Bucket = ItemStack.EMPTY;
         if ((new Object() {
               public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Fluid").equals("Oil")
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
               .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.OIL.get(), 1))) {
            Bucket = new ItemStack((ItemLike)CrustyChunksModItems.OIL_BUCKET.get()).copy();
         } else if ((new Object() {
               public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Fluid").equals("Diesel")
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
            Bucket = new ItemStack((ItemLike)CrustyChunksModItems.DIESEL_BUCKET.get()).copy();
         } else if ((new Object() {
               public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Fluid").equals("Kerosene")
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
               .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.KEROSENE.get(), 1))) {
            Bucket = new ItemStack((ItemLike)CrustyChunksModItems.KEROSENE_BUCKET.get()).copy();
         } else if ((new Object() {
               public String getValue(LevelAccessor world, BlockPos pos, String tag) {
                  BlockEntity blockEntity = world.getBlockEntity(pos);
                  return blockEntity != null ? blockEntity.getPersistentData().getString(tag) : "";
               }
            }).getValue(world, BlockPos.containing(x, y, z), "Fluid").equals("Petrolium")
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
               .isFluidEqual(new FluidStack((Fluid)CrustyChunksModFluids.PETROLIUM.get(), 1))) {
            Bucket = new ItemStack((ItemLike)CrustyChunksModItems.PETROLIUM_BUCKET.get()).copy();
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
                  .getAmount()
               >= 1000
            && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == Items.BUCKET) {
            (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).shrink(1);
            if (world instanceof ServerLevel _level) {
               ItemEntity entityToSpawn = new ItemEntity(_level, entity.getX(), entity.getY(), entity.getZ(), Bucket);
               entityToSpawn.setPickUpDelay(5);
               _level.addFreshEntity(entityToSpawn);
            }

            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            int _drain = 1000;
            BlockEntity blockEntity = world.getBlockEntity(BlockPos.containing(x, y, z));
            if (blockEntity != null) {
               {
   IFluidHandler capability = WariumCaps.fluid(blockEntity, null).orElse(null);
   if (capability != null) capability.drain(_drain, FluidAction.EXECUTE);
}
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FluidDrainProcedure.execute", _wtSafe);
      }
   }
}
