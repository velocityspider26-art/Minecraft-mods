package net.mcreator.crustychunks.procedures;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class ManualAimerEmittedRedstonePowerProcedure {
   public static double execute(BlockState blockstate) {
      double returnvalue = 0.0;
      if (0 < (blockstate.getBlock().getStateDefinition().getProperty("firing") instanceof IntegerProperty _getip1 ? (Integer)blockstate.getValue(_getip1) : -1)) {
         returnvalue = 15.0;
      } else {
         returnvalue = 0.0;
      }

      return returnvalue;
   }
}
