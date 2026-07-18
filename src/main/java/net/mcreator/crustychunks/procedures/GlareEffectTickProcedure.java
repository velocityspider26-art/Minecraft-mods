package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class GlareEffectTickProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         double xRadius = 0.0;
         double loop = 0.0;
         double zRadius = 0.0;
         double particleAmount = 0.0;
         immediatesourceentity.noPhysics = true;
         if (!immediatesourceentity.getPersistentData().getBoolean("Used")) {
            immediatesourceentity.getPersistentData().putBoolean("Used", true);
         } else {
            CrustyChunksMod.queueServerWork(2, () -> {
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            });
         }
      }
   }
}
