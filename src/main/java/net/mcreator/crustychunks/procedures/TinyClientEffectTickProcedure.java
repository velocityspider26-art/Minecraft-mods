package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class TinyClientEffectTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         double xRadius = 0.0;
         double loop = 0.0;
         double zRadius = 0.0;
         double particleAmount = 0.0;
         immediatesourceentity.noPhysics = true;
         if (!immediatesourceentity.getPersistentData().getBoolean("Used")) {
            for (int index0 = 0; index0 < 15; index0++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(),
                  x,
                  y,
                  z,
                  Mth.nextDouble(RandomSource.create(), -0.6, 0.6),
                  Mth.nextDouble(RandomSource.create(), -1.0, 0.5),
                  Mth.nextDouble(RandomSource.create(), -0.6, 0.6)
               );
            }

            world.addParticle(ParticleTypes.FLASH, x, y + 1.0, z, 0.0, 0.1, 0.0);
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
         net.mcreator.crustychunks.compat.WariumSafety.report("TinyClientEffectTickProcedure.execute", _wtSafe);
      }
   }
}
