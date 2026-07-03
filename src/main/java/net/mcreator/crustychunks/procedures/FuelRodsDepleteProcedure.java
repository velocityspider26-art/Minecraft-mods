package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

public class FuelRodsDepleteProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (CrustyChunksModBlocks.FUEL_RODS_1.get() == world.getBlockState(BlockPos.containing(x, y, z)).getBlock()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.EMPTY_FUEL_RODS.get()).defaultBlockState(), 3);
      }

      if (CrustyChunksModBlocks.FUEL_RODS_2.get() == world.getBlockState(BlockPos.containing(x, y, z)).getBlock()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.FUEL_RODS_1.get()).defaultBlockState(), 3);
      }

      if (CrustyChunksModBlocks.FUEL_RODS_3.get() == world.getBlockState(BlockPos.containing(x, y, z)).getBlock()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.FUEL_RODS_2.get()).defaultBlockState(), 3);
      }

      if (CrustyChunksModBlocks.FUEL_RODS_4.get() == world.getBlockState(BlockPos.containing(x, y, z)).getBlock()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.FUEL_RODS_3.get()).defaultBlockState(), 3);
      }
   }
}
