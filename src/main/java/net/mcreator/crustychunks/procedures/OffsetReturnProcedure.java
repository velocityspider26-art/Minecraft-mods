package net.mcreator.crustychunks.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;

public class OffsetReturnProcedure {
   public static double execute(LevelAccessor world, double x, double y, double z) {
      double launchoffsety = 0.0;
      if (world.getBlockState(BlockPos.containing(x, y - 1.0, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:bombbay")))) {
         launchoffsety = -2.5;
      } else if (world.getBlockState(BlockPos.containing(x, y - 2.0, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:bombbay")))) {
         launchoffsety = -3.5;
      } else if (world.getBlockState(BlockPos.containing(x, y - 3.0, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:bombbay")))) {
         launchoffsety = -4.5;
      } else if (world.getBlockState(BlockPos.containing(x, y - 1.0, z)).canOcclude()) {
         launchoffsety = 0.5;
      } else if (world.getBlockState(BlockPos.containing(x, y + 1.0, z)).canOcclude()) {
         launchoffsety = -0.75;
      } else {
         launchoffsety = -0.5;
      }

      return launchoffsety;
   }
}
