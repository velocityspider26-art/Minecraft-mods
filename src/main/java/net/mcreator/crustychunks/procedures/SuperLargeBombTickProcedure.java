package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SuperLargeBombTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         if (immediatesourceentity.isUnderWater()) {
            SuperLargeBombProjectileHitsBlockProcedure.execute(world, x, y, z, immediatesourceentity);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SuperLargeBombTickProcedure.execute", _wtSafe);
      }
   }
}
