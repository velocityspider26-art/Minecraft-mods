package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.IRMissileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;

public class ArtilleryHitProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         if (immediatesourceentity instanceof IRMissileEntity) {
            if (immediatesourceentity.getPersistentData().getDouble("Time") > 2.0) {
               MediumExplosionProcedure.execute(world, x, y, z);
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }

               if (world instanceof Level _level && !_level.isClientSide()) {
                  net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, x, y, z, 5.0F, ExplosionInteraction.BLOCK);
               }

               if (world instanceof Level _level && !_level.isClientSide()) {
                  net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ(), 6.0F, ExplosionInteraction.BLOCK);
               }
            }
         } else {
            CrustyChunksMod.queueServerWork(1, () -> {
               MediumExplosionProcedure.execute(world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            });
         }
      }
   }
}
