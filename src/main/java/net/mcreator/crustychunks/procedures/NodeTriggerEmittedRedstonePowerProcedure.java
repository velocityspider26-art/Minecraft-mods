package net.mcreator.crustychunks.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NodeTriggerEmittedRedstonePowerProcedure {
   public static double execute(LevelAccessor world, double x, double y, double z) {
      boolean logic = false;
      double number = 0.0;
      if (0.0 < (new Object() {
         public double getValue(LevelAccessor world, BlockPos pos, String tag) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity != null ? blockEntity.getPersistentData().getDouble(tag) : -1.0;
         }
      }).getValue(world, BlockPos.containing(x, y, z), "Trigger")) {
         number = 15.0;
      } else {
         number = 0.0;
      }

      return number;
   }
}
