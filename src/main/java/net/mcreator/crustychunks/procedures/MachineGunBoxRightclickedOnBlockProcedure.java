package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class MachineGunBoxRightclickedOnBlockProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() != CrustyChunksModBlocks.MACHINE_GUN.get()
            && world.getBlockState(BlockPos.containing(x, y, z)).getBlock() != CrustyChunksModBlocks.HEAVY_MACHINE_GUN.get()) {
            MGBoxScriptProcedure.execute(world, x, y, z, entity);
         }
      }
   }
}
