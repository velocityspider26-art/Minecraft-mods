package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class AutoCannonShellTracerProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         boolean trigger = false;
         if ((immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) >= 2.0 && !immediatesourceentity.isNoGravity()) {
            immediatesourceentity.setDeltaMovement(
               new Vec3(
                  immediatesourceentity.getDeltaMovement().x(), immediatesourceentity.getDeltaMovement().y() + 0.01, immediatesourceentity.getDeltaMovement().z()
               )
            );
         }

         world.addParticle(
            (SimpleParticleType)CrustyChunksModParticleTypes.BULLET_TRAIL.get(),
            immediatesourceentity.getX(),
            immediatesourceentity.getY(),
            immediatesourceentity.getZ(),
            0.0,
            0.0,
            0.0
         );
         if (immediatesourceentity.isUnderWater()) {
            trigger = true;
         }

         if (trigger) {
            trigger = false;
            CrustyChunksMod.queueServerWork(
               1,
               () -> {
                  SmallShellHitProcedure.execute(
                     world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ(), immediatesourceentity
                  );
                  if (!immediatesourceentity.level().isClientSide()) {
                     immediatesourceentity.discard();
                  }
               }
            );
         }
      }
   }
}
