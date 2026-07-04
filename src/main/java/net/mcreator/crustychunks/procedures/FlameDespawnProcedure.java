package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;

public class FlameDespawnProcedure {
   public static void execute(Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         if (immediatesourceentity.isUnderWater() && !immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FlameDespawnProcedure.execute", _wtSafe);
      }
   }
}
