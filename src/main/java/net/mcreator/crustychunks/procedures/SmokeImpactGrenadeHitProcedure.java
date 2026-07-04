package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SmokeImpactGrenadeHitProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         SmallSmokeExplosionProcedure.execute(world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
         if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SmokeImpactGrenadeHitProcedure.execute", _wtSafe);
      }
   }
}
