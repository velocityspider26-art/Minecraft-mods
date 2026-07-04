package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;

public class LargeFragmentProjectileHitsBlockProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         SmallExplosionProcedure.execute(world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
         if (world instanceof Level _level && !_level.isClientSide()) {
            net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ(), 1.0F, ExplosionInteraction.NONE);
         }

         if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("LargeFragmentProjectileHitsBlockProcedure.execute", _wtSafe);
      }
   }
}
