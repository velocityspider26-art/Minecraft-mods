package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class GreenBulletTracerProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         world.addParticle(
            (SimpleParticleType)CrustyChunksModParticleTypes.BULLET_TRAIL.get(),
            immediatesourceentity.getX(),
            immediatesourceentity.getY(),
            immediatesourceentity.getZ(),
            0.0,
            0.0,
            0.0
         );
         if ((immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) >= 2.0 && !immediatesourceentity.isNoGravity()) {
            immediatesourceentity.setDeltaMovement(
               new Vec3(
                  immediatesourceentity.getDeltaMovement().x(), immediatesourceentity.getDeltaMovement().y() + 0.04, immediatesourceentity.getDeltaMovement().z()
               )
            );
         }

         if ((immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) <= 0.75
            && !immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }

         if (!immediatesourceentity.getPersistentData().getBoolean("despawntimer")) {
            immediatesourceentity.getPersistentData().putBoolean("despawntimer", true);
            CrustyChunksMod.queueServerWork(200, () -> {
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            });
         }
      }
   }
}
