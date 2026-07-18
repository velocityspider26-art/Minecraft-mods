package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;

public class FlameTracerProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         world.addParticle(
            (SimpleParticleType)CrustyChunksModParticleTypes.FLAME_PARTICLE.get(),
            x,
            y,
            z,
            immediatesourceentity.getLookAngle().x
               * -1.0
               * (immediatesourceentity instanceof Projectile _projEntxx ? _projEntxx.getDeltaMovement().length() : 0.0),
            immediatesourceentity.getLookAngle().y * -1.0 * (immediatesourceentity instanceof Projectile _projEntx ? _projEntx.getDeltaMovement().length() : 0.0),
            immediatesourceentity.getLookAngle().z * (immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0)
         );
         immediatesourceentity.setInvisible(true);
         world.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.1, 0.0);
         if (immediatesourceentity.isUnderWater() && !immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }

         if (!immediatesourceentity.getPersistentData().getBoolean("despawntimer")) {
            immediatesourceentity.getPersistentData().putBoolean("despawntimer", true);
            CrustyChunksMod.queueServerWork(60, () -> {
               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            });
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FlameTracerProcedure.execute", _wtSafe);
      }
   }
}
