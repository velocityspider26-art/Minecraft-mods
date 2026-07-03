package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;

public class FlameDespawnProcedure {
   public static void execute(Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         if (immediatesourceentity.isUnderWater() && !immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   }
}
