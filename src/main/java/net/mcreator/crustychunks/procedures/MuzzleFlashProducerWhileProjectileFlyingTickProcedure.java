package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class MuzzleFlashProducerWhileProjectileFlyingTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         immediatesourceentity.noPhysics = true;
         immediatesourceentity.setNoGravity(true);
         CrustyChunksMod.queueServerWork(2, () -> {
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         });

         for (int index0 = 0; index0 < 3; index0++) {
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.GUN_SMOKE.get(),
               x,
               y,
               z,
               Mth.nextDouble(RandomSource.create(), -0.2, 0.2),
               Mth.nextDouble(RandomSource.create(), -0.2, 0.2),
               Mth.nextDouble(RandomSource.create(), -0.2, 0.2)
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("MuzzleFlashProducerWhileProjectileFlyingTickProcedure.execute", _wtSafe);
      }
   }
}
