package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

public class FuelTankExplodedProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.DAMAGEDFUELTANK.get()).defaultBlockState(), 3);
      if (world instanceof ServerLevel _level) {
         _level.sendParticles(ParticleTypes.FLAME, x + 0.5, y + 0.5, z + 0.5, 5, 0.25, 0.25, 0.25, 0.1);
      }
   }
}
