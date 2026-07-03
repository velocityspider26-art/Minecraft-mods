package net.mcreator.crustychunks.procedures;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;

public class PhosphorBlockHitProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (Mth.nextInt(RandomSource.create(), 1, 10) == 1) {
         BurnBlockProcedure.execute(world, x, y, z);
      }
   }
}
