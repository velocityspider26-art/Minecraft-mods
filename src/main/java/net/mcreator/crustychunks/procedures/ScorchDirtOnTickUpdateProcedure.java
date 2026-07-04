package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ScorchDirtOnTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world.getBlockState(BlockPos.containing(x, y - 1.0, z)).getBlock() == CrustyChunksModBlocks.SCORCH_DIRT.get()) {
         world.setBlock(BlockPos.containing(x, y - 1.0, z), ((Block)CrustyChunksModBlocks.HARDDIRT.get()).defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() != Blocks.AIR || 1 == Mth.nextInt(RandomSource.create(), 1, 35)) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.HARDDIRT.get()).defaultBlockState(), 3);
      } else if (1 == Mth.nextInt(RandomSource.create(), 1, 5) && world instanceof ServerLevel _level) {
         _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x, y + 3.0, z, 1, 1.0, 3.0, 1.0, 0.01);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ScorchDirtOnTickUpdateProcedure.execute", _wtSafe);
      }
   }
}
