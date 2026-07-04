package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class PhosphorTrailProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         if (40.0 >= immediatesourceentity.getPersistentData().getDouble("T")) {
            world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.PHOSPHORUS_TRAIL.get(), x, y, z, 0.0, 0.0, 0.0);
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.AERIAL_SPARKS.get(),
               x,
               y,
               z,
               immediatesourceentity.getDeltaMovement().x() + Mth.nextDouble(RandomSource.create(), -0.2, 0.2),
               immediatesourceentity.getDeltaMovement().y() + Mth.nextDouble(RandomSource.create(), -0.2, 0.2),
               immediatesourceentity.getDeltaMovement().z() + Mth.nextDouble(RandomSource.create(), -0.2, 0.2)
            );
         } else if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }

         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("PhosphorTrailProcedure.execute", _wtSafe);
      }
   }
}
