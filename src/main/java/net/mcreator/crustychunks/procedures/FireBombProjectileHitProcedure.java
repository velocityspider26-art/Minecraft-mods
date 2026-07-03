package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class FireBombProjectileHitProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         IncindiaryExplosionProcedure.execute(
            world,
            x + immediatesourceentity.getLookAngle().x * 2.0,
            y + immediatesourceentity.getLookAngle().y * 2.0,
            z - immediatesourceentity.getLookAngle().z * 2.0
         );
         if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   }
}
