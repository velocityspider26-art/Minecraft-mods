package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class ChaffTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SPARKS.get(), x, y, z, 0.0, 0.0, 0.0);
         immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
         if (immediatesourceentity.getPersistentData().getDouble("T") >= 20.0) {
            immediatesourceentity.setDeltaMovement(
               new Vec3(
                  immediatesourceentity.getDeltaMovement().x(), immediatesourceentity.getDeltaMovement().y() + 0.049, immediatesourceentity.getDeltaMovement().z()
               )
            );
         }

         if (immediatesourceentity.getPersistentData().getDouble("T") >= 40.0 && !immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ChaffTickProcedure.execute", _wtSafe);
      }
   }
}
