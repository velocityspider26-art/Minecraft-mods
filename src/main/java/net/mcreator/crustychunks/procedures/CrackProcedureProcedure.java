package net.mcreator.crustychunks.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

public class CrackProcedureProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.DIORITE) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.GRANITE) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.ANDESITE) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.TUFF) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.STONE) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.DEEPSLATE) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.GRASS_BLOCK) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.DIRT.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.CRACKED_STONE_BRICKS) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.STONE_BRICKS) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.MOSSY_STONE_BRICKS) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.CRACKED_DEEPSLATE_BRICKS) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.DEEPSLATE_BRICKS) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.CRACKED_DEEPSLATE_TILES) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.DEEPSLATE_TILES) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.BLACKSTONE.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.POLISHED_BLACKSTONE_BRICKS) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
      }
   }
}
