package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class MediumBombTickProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         boolean Trigger = false;
         double ypos = 0.0;
         double zpos = 0.0;
         double xpos = 0.0;
         double zvel = 0.0;
         double yvel = 0.0;
         double xvel = 0.0;
         if (OrdinanceTriggerProcedure.execute(world, immediatesourceentity)) {
            MediumBombProjectileHitsBlockProcedure.execute(world, immediatesourceentity);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("MediumBombTickProcedure.execute", _wtSafe);
      }
   }
}
