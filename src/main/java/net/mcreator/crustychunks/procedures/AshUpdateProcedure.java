package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;

public class AshUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (1 == Mth.nextInt(RandomSource.create(), 1, 3)) {
         world.destroyBlock(BlockPos.containing(x, y, z), false);
         if (world instanceof ServerLevel _level) {
            _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x, y, z, 3, 0.0, 0.0, 0.0, 0.3);
         }
      }
   }
}
