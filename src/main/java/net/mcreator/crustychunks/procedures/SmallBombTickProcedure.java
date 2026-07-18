package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SmallBombTickProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         boolean Trigger = false;
         double xpos = 0.0;
         double ypos = 0.0;
         double zpos = 0.0;
         double xvel = 0.0;
         double yvel = 0.0;
         double zvel = 0.0;
         if (OrdinanceTriggerProcedure.execute(world, immediatesourceentity)) {
            SmallBombProjectileHitsBlockProcedure.execute(world, immediatesourceentity);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SmallBombTickProcedure.execute", _wtSafe);
      }
   }
}
