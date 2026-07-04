package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;

public class BreechingProjectileTickProcedure {
   public static void execute(Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         if (immediatesourceentity.getPersistentData().getDouble("T") >= 2.0 && !immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("BreechingProjectileTickProcedure.execute", _wtSafe);
      }
   }
}
