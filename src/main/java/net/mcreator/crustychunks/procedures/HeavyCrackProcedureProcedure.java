package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class HeavyCrackProcedureProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.DIORITE) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.GRAVEL.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.DESTROYED_CONCRETE.get()) {
         world.setBlock(BlockPos.containing(x, y, z), Blocks.GRAVEL.defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.DAMAGED_CONCRETE.get()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.DESTROYED_CONCRETE.get()).defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.FRACTURED_CONCRETE.get()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.DAMAGED_CONCRETE.get()).defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.CRACKED_CONCRETE.get()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.FRACTURED_CONCRETE.get()).defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.REENFORCED_CONCRETE.get()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.CRACKED_CONCRETE.get()).defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.DAMAGED_CONCRETE_WALL.get()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.DESTROYED_CONCRETE_WALL.get()).defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.FRACTURED_CONCRETE_WALL.get()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.DAMAGED_CONCRETE_WALL.get()).defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.CRACKED_CONCRETE_WALL.get()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.FRACTURED_CONCRETE_WALL.get()).defaultBlockState(), 3);
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.CONCRETE_WALL.get()) {
         world.setBlock(BlockPos.containing(x, y, z), ((Block)CrustyChunksModBlocks.CRACKED_CONCRETE_WALL.get()).defaultBlockState(), 3);
      }

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

      if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.OBSIDIAN || world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == Blocks.CRYING_OBSIDIAN) {
         world.destroyBlock(BlockPos.containing(x, y, z), false);
         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.iron_golem.damage")),
                  SoundSource.NEUTRAL,
                  1.0F,
                  1.0F
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.iron_golem.damage")),
                  SoundSource.NEUTRAL,
                  1.0F,
                  1.0F,
                  false
               );
            }
         }
      }

      if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:era")))) {
         ERAProcedureProcedure.execute(world, x, y, z);
      }
   }
}
