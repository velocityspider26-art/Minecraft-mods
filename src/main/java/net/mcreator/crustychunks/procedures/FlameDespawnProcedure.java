package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class FlameDespawnProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         if (immediatesourceentity.isUnderWater() && !immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }

         if (!immediatesourceentity.getPersistentData().getBoolean("despawntimer")) {
            immediatesourceentity.getPersistentData().putBoolean("despawntimer", true);
            CrustyChunksMod.queueServerWork(60, () -> {
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            });
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FlameDespawnProcedure.execute", _wtSafe);
      }
   }
}
