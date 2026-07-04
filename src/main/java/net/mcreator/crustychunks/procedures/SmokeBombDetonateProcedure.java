package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public class SmokeBombDetonateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double particleRadius = 0.0;
      double particleAmount = 0.0;
      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.SMOKE_BOMB.get()) {
         world.destroyBlock(BlockPos.containing(x, y, z), false);
      }

      SmokeExplosionProcedure.execute(world, x, y, z);
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SmokeBombDetonateProcedure.execute", _wtSafe);
      }
   }
}
