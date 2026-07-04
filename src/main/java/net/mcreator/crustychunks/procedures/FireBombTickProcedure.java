package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class FireBombTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         if (immediatesourceentity.isUnderWater()) {
            FireBombProjectileHitProcedure.execute(world, x, y, z, immediatesourceentity);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FireBombTickProcedure.execute", _wtSafe);
      }
   }
}
