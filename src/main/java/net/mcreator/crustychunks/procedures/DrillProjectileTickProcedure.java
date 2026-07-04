package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class DrillProjectileTickProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         if (immediatesourceentity.getPersistentData().getDouble("T") >= 3.0) {
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }

            DrillProjectileHitProcedure.execute(world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("DrillProjectileTickProcedure.execute", _wtSafe);
      }
   }
}
