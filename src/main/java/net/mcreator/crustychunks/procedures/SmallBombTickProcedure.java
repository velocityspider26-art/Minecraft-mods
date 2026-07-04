package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SmallBombTickProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         if (immediatesourceentity.isUnderWater()) {
            SmallBombProjectileHitsBlockProcedure.execute(world, immediatesourceentity);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SmallBombTickProcedure.execute", _wtSafe);
      }
   }
}
