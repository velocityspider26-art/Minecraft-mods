package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.LevelAccessor;

public class EradicatorTurretFlightProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x, y, z, 0.0, 0.0, 0.0);
   }
}
