package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class FlareTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.FLARE.get(), x, y, z, 0.0, 1.0, 0.0);
         immediatesourceentity.getPersistentData().putDouble("Time", immediatesourceentity.getPersistentData().getDouble("Time") + 1.0);
         if (immediatesourceentity.getPersistentData().getDouble("Time") >= 50.0) {
            immediatesourceentity.setDeltaMovement(
               new Vec3(
                  immediatesourceentity.getDeltaMovement().x(), immediatesourceentity.getDeltaMovement().y() + 0.049, immediatesourceentity.getDeltaMovement().z()
               )
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.CAMP_SMOKE.get(),
               x,
               y,
               z,
               Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
               Mth.nextDouble(RandomSource.create(), 0.7, 0.9),
               Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
            );
         } else {
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.CAMP_SMOKE.get(),
               x,
               y,
               z,
               Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
               Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
               Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
            );
         }

         if (immediatesourceentity.getPersistentData().getDouble("Time") >= 400.0 && !immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FlareTickProcedure.execute", _wtSafe);
      }
   }
}
