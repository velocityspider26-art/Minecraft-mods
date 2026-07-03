package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SmokeGrenadeFlightProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         immediatesourceentity.getPersistentData().putDouble("t", immediatesourceentity.getPersistentData().getDouble("t") + 1.0);
         if (80.0 < immediatesourceentity.getPersistentData().getDouble("t")) {
            SmokeGrenadeExplosionProcedure.execute(world, x, y, z, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }

         world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.PHOSPHORUS_TRAIL.get(), x, y, z, 0.0, 0.1, 0.0);
      }
   }
}
