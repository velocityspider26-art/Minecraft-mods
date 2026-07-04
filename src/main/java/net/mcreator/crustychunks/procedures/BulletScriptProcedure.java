package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class BulletScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         if ((immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) >= 2.0) {
            immediatesourceentity.setDeltaMovement(
               new Vec3(
                  immediatesourceentity.getDeltaMovement().x(), immediatesourceentity.getDeltaMovement().y() + 0.04, immediatesourceentity.getDeltaMovement().z()
               )
            );
         }

         world.addParticle(
            (SimpleParticleType)CrustyChunksModParticleTypes.BULLET_TRAIL.get(),
            x,
            y,
            z,
            immediatesourceentity.getDeltaMovement().x() * 0.1,
            immediatesourceentity.getDeltaMovement().y() * 0.1,
            immediatesourceentity.getDeltaMovement().z() * 0.1
         );
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("BulletScriptProcedure.execute", _wtSafe);
      }
   }
}
