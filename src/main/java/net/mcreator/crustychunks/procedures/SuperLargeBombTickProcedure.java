package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SuperLargeBombTickProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         boolean Trigger = false;
         if (OrdinanceTriggerProcedure.execute(world, immediatesourceentity)) {
            SuperLargeBombProjectileHitsBlockProcedure.execute(world, immediatesourceentity);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SuperLargeBombTickProcedure.execute", _wtSafe);
      }
   }
}
