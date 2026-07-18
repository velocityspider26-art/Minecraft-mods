package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;

public class LargeTorpedoHitProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         ExplosionExampleProcedure.execute(
            world, immediatesourceentity.getX(), immediatesourceentity.getY() + 2.0, immediatesourceentity.getZ(), 15.0
         );
         if (world instanceof Level _level && !_level.isClientSide()) {
            net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, immediatesourceentity.getX() + immediatesourceentity.getLookAngle().x * 2.0, immediatesourceentity.getY() + 2.5, immediatesourceentity.getZ() - immediatesourceentity.getLookAngle().z * 2.0, 6.0F, ExplosionInteraction.NONE);
         }

         if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("LargeTorpedoHitProcedure.execute", _wtSafe);
      }
   }
}
