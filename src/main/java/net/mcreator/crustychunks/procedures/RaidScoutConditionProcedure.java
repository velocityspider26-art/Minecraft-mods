package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public class RaidScoutConditionProcedure {
   public static boolean execute(LevelAccessor world, double x, double y, double z) {
      boolean logic = false;
      if (world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.APOCALYPSE_MODE) && world.canSeeSkyFromBelowWater(BlockPos.containing(x, y, z))) {
         logic = true;
      } else {
         logic = false;
      }

      return logic;
   }
}
