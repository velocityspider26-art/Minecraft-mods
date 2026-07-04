package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;

public class CharredBlockOnRandomClientDisplayTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (5 == Mth.nextInt(RandomSource.create(), 1, 5)) {
         world.addParticle(
            (SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(),
            x + Mth.nextDouble(RandomSource.create(), 0.2, 0.8),
            y + Mth.nextDouble(RandomSource.create(), 0.2, 0.8),
            z + Mth.nextDouble(RandomSource.create(), 0.2, 0.8),
            0.0,
            1.0,
            0.0
         );
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("CharredBlockOnRandomClientDisplayTickProcedure.execute", _wtSafe);
      }
   }
}
