package net.mcreator.crustychunks.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

public class RawLithiumBlockNeighbourBlockChangesProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).getBlock() == Blocks.WATER
         || world.getBlockState(BlockPos.containing(x + 1.0, y, z)).getBlock() == Blocks.WATER
         || world.getBlockState(BlockPos.containing(x, y, z + 1.0)).getBlock() == Blocks.WATER
         || world.getBlockState(BlockPos.containing(x - 1.0, y, z)).getBlock() == Blocks.WATER
         || world.getBlockState(BlockPos.containing(x, y, z - 1.0)).getBlock() == Blocks.WATER) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
         ExplosionExampleProcedure.execute(world, x + 0.5, y + 0.5, z + 0.5, 3.0);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RawLithiumBlockNeighbourBlockChangesProcedure.execute", _wtSafe);
      }
   }
}
