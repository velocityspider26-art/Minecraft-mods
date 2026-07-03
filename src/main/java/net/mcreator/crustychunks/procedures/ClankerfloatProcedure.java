package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class ClankerfloatProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (entity.isInWater()) {
            entity.push(entity.getLookAngle().x * 0.1, 0.1, entity.getLookAngle().z * 0.1);
            world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SMALL_SPLASH_PUFF.get(), x, y, z, 0.0, -1.0, 0.0);
         }
      }
   }
}
