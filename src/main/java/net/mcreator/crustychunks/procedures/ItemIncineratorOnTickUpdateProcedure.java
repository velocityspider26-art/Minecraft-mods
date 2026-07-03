package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.mcreator.crustychunks.utils.WariumCaps;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class ItemIncineratorOnTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
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
      }).getAmount(world, BlockPos.containing(x, y, z), 0) > 0) {
         BlockEntity _ent = world.getBlockEntity(BlockPos.containing(x, y, z));
         if (_ent != null) {
            int _slotid = 0;
            {
   IItemHandler capability = WariumCaps.itemHandler(_ent, null).orElse(null);
   if (capability != null) {
               if (capability instanceof IItemHandlerModifiable) {
                  ((IItemHandlerModifiable)capability).setStackInSlot(0, ItemStack.EMPTY);
               }
            }
}
         }

         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                  SoundSource.NEUTRAL,
                  1.0F,
                  1.0F
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                  SoundSource.NEUTRAL,
                  1.0F,
                  1.0F,
                  false
               );
            }
         }

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x + 0.5, y + 1.5, z + 0.5, 2, 0.0, 0.0, 0.0, 0.1);
         }

         if (world instanceof ServerLevel _levelx) {
            _levelx.sendParticles(ParticleTypes.FLAME, x + 0.5, y + 1.5, z + 0.5, 5, 0.0, 0.0, 0.0, 0.1);
         }
      }

      if ((new Object() {
               public int getAmountInTank(LevelAccessor level, BlockPos pos, int tank) {
                  BlockEntity blockEntity = level.getBlockEntity(pos);
                  return blockEntity != null
                     ? WariumCaps.fluid(blockEntity, null)
                        .map(fluidHandler -> fluidHandler.getFluidInTank(tank).getAmount())
                        .orElse(0)
                     : 0;
               }
            })
            .getAmountInTank(
               world,
               BlockPos.containing(x, y, z),
               (new Object() {
                     public int getTanks(LevelAccessor level, BlockPos pos) {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        return blockEntity != null
                           ? WariumCaps.fluid(blockEntity, null).map(fluidHandler -> fluidHandler.getTanks()).orElse(0)
                           : 0;
                     }
                  })
                  .getTanks(world, BlockPos.containing(x, y, z))
            )
         > 0) {
         int _drain = 100;
         BlockEntity blockEntity = world.getBlockEntity(BlockPos.containing(x, y, z));
         if (blockEntity != null) {
            {
   IFluidHandler capability = WariumCaps.fluid(blockEntity, null).orElse(null);
   if (capability != null) capability.drain(_drain, FluidAction.EXECUTE);
}
         }

         if (world instanceof Level _levelx) {
            if (!_levelx.isClientSide()) {
               _levelx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lava.pop")),
                  SoundSource.NEUTRAL,
                  1.0F,
                  1.0F
               );
            } else {
               _levelx.playLocalSound(
                  x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.lava.pop")), SoundSource.NEUTRAL, 1.0F, 1.0F, false
               );
            }
         }

         if (world instanceof ServerLevel _levelxx) {
            _levelxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x + 0.5, y + 1.5, z + 0.5, 2, 0.0, 0.0, 0.0, 0.1);
         }

         if (world instanceof ServerLevel _levelxx) {
            _levelxx.sendParticles(ParticleTypes.FLAME, x + 0.5, y + 1.5, z + 0.5, 5, 0.0, 0.0, 0.0, 0.1);
         }
      }
   }
}
