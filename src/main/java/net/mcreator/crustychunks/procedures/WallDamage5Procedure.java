package net.mcreator.crustychunks.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class WallDamage5Procedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (Mth.nextInt(RandomSource.create(), 1, 2) == 2) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.GRAVEL.defaultBlockState(), 3);
      }

      world.levelEvent(2001, BlockPos.containing(x, y, z), Block.getId(Blocks.GRAVEL.defaultBlockState()));
   }
}
