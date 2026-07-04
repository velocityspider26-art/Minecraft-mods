package net.mcreator.crustychunks.procedures;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class GrenadeProjectileWhileProjectileFlyingTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         immediatesourceentity.getPersistentData().putDouble("t", immediatesourceentity.getPersistentData().getDouble("t") + 1.0);
         if (80.0 < immediatesourceentity.getPersistentData().getDouble("t")) {
            TinyExplosionProcedure.execute(world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }

         world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.1, 0.0);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("GrenadeProjectileWhileProjectileFlyingTickProcedure.execute", _wtSafe);
      }
   }
}
