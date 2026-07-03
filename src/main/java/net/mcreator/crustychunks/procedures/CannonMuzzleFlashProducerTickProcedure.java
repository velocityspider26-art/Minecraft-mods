package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class CannonMuzzleFlashProducerTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         immediatesourceentity.noPhysics = true;
         immediatesourceentity.setNoGravity(true);
         if (0.0 == immediatesourceentity.getPersistentData().getDouble("Tagged")) {
            for (int index0 = 0; index0 < 25; index0++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.PUFF.get(),
                  x,
                  y,
                  z,
                  (immediatesourceentity.getDeltaMovement().x() + Mth.nextDouble(RandomSource.create(), -1.5, 1.5)) * 1.0,
                  (immediatesourceentity.getDeltaMovement().y() + Mth.nextDouble(RandomSource.create(), -1.5, 1.5)) * 1.0,
                  (immediatesourceentity.getDeltaMovement().z() + Mth.nextDouble(RandomSource.create(), -1.5, 1.5)) * 1.0
               );
            }
         }

         if (1.0 > immediatesourceentity.getPersistentData().getDouble("Tagged")) {
            immediatesourceentity.getPersistentData().putDouble("Tagged", immediatesourceentity.getPersistentData().getDouble("Tagged") + 1.0);
         } else if (1.0 == immediatesourceentity.getPersistentData().getDouble("Tagged")) {
            CrustyChunksMod.queueServerWork(2, () -> {
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            });
         }
      }
   }
}
