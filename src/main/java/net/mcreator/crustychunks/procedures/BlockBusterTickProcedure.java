package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class BlockBusterTickProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         if (OrdinanceTriggerProcedure.execute(world, immediatesourceentity)) {
            BlockBusterHitProcedure.execute(world, immediatesourceentity);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("BlockBusterTickProcedure.execute", _wtSafe);
      }
   }
}
