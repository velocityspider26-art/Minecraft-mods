package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SmokeStackSmokeWhileProjectileFlyingTickProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }

         world.addParticle(
            (SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(),
            immediatesourceentity.getX(),
            immediatesourceentity.getY(),
            immediatesourceentity.getZ(),
            0.0,
            1.0,
            0.0
         );
         CrustyChunksMod.queueServerWork(1, () -> {
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         });
      }
   }
}
