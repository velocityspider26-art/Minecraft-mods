package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class LargeRocketHitProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         CrustyChunksMod.queueServerWork(
            1,
            () -> {
               ExplosionExampleProcedure.execute(
                  world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ(), 10.0
               );
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            }
         );
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("LargeRocketHitProcedure.execute", _wtSafe);
      }
   }
}
