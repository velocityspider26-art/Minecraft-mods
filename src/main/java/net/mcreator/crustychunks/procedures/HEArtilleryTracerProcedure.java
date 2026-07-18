package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;

public class HEArtilleryTracerProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         world.addParticle(
            (SimpleParticleType)CrustyChunksModParticleTypes.TRACER.get(),
            x,
            y,
            z,
            immediatesourceentity.getLookAngle().x
               * -1.0
               * (immediatesourceentity instanceof Projectile _projEntxx ? _projEntxx.getDeltaMovement().length() : 0.0),
            immediatesourceentity.getLookAngle().y * -1.0 * (immediatesourceentity instanceof Projectile _projEntx ? _projEntx.getDeltaMovement().length() : 0.0),
            immediatesourceentity.getLookAngle().z * (immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0)
         );
         world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.CAMP_SMOKE.get(), x, y, z, 0.0, 0.0, 0.0);
         if (OrdinanceTriggerProcedure.execute(world, immediatesourceentity)) {
            ArtilleryHitProcedure.execute(world, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("HEArtilleryTracerProcedure.execute", _wtSafe);
      }
   }
}
