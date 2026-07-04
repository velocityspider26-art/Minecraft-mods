package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class ExhaustProjectileTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         CrustyChunksMod.queueServerWork(9, () -> {
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         });
         immediatesourceentity.setInvisible(true);
         immediatesourceentity.setNoGravity(true);
         if (immediatesourceentity.getPersistentData().getDouble("T") == 1.0) {
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x - immediatesourceentity.getDeltaMovement().x() * 4.0,
               y - immediatesourceentity.getDeltaMovement().y() * 4.0,
               z - immediatesourceentity.getDeltaMovement().z() * 4.0,
               immediatesourceentity.getDeltaMovement().x() / 2.0,
               immediatesourceentity.getDeltaMovement().y() / 2.0,
               immediatesourceentity.getDeltaMovement().z() / 2.0
            );
         }

         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         if (immediatesourceentity.getPersistentData().getDouble("T") > 8.0) {
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.PUFF.get(),
               x,
               y,
               z,
               immediatesourceentity.getDeltaMovement().x() * 6.0,
               immediatesourceentity.getDeltaMovement().y() * 6.0,
               immediatesourceentity.getDeltaMovement().z() * 6.0
            );
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ExhaustProjectileTickProcedure.execute", _wtSafe);
      }
   }
}
