package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class FireClientEffectTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         double xRadius = 0.0;
         double loop = 0.0;
         double zRadius = 0.0;
         double particleAmount = 0.0;
         immediatesourceentity.noPhysics = true;
         if (!immediatesourceentity.getPersistentData().getBoolean("Used")) {
            for (int index0 = 0; index0 < 35; index0++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.FIRE_EXPLOSION.get(),
                  x + Mth.nextDouble(RandomSource.create(), -3.0, 3.0),
                  y + Mth.nextDouble(RandomSource.create(), 0.0, 3.0),
                  z + Mth.nextDouble(RandomSource.create(), -3.0, 3.0),
                  Mth.nextDouble(RandomSource.create(), -0.15, 0.15),
                  Mth.nextDouble(RandomSource.create(), -0.15, 0.15),
                  Mth.nextDouble(RandomSource.create(), -0.15, 0.15)
               );
            }

            for (int index1 = 0; index1 < 35; index1++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.LARGE_SMOKE.get(),
                  x,
                  y,
                  z,
                  Mth.nextDouble(RandomSource.create(), -0.25, 0.25),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.25),
                  Mth.nextDouble(RandomSource.create(), -0.25, 0.25)
               );
            }

            immediatesourceentity.getPersistentData().putBoolean("Used", true);
         } else {
            CrustyChunksMod.queueServerWork(2, () -> {
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            });
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FireClientEffectTickProcedure.execute", _wtSafe);
      }
   }
}
