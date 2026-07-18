package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.IRMissileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class ArtilleryHitProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         if (immediatesourceentity instanceof IRMissileEntity) {
            if (immediatesourceentity.getPersistentData().getDouble("Time") > 2.0) {
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }

               ExplosionExampleProcedure.execute(
                  world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ(), 6.0
               );
            }
         } else {
            CrustyChunksMod.queueServerWork(
               1,
               () -> {
                  if (!immediatesourceentity.level().isClientSide()) {
                     immediatesourceentity.discard();
                  }

                  ExplosionExampleProcedure.execute(
                     world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ(), 6.0
                  );
               }
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ArtilleryHitProcedure.execute", _wtSafe);
      }
   }
}
