package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SmokeLaunchTickProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         double RocketVelocity = 0.0;
         if (immediatesourceentity.isUnderWater() || immediatesourceentity.getPersistentData().getDouble("T") >= 25.0) {
            SmokeMortarHitProcedure.execute(world, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }

         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SmokeLaunchTickProcedure.execute", _wtSafe);
      }
   }
}
