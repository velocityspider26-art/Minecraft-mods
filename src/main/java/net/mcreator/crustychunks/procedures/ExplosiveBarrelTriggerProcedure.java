package net.mcreator.crustychunks.procedures;

import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ExplosiveBarrelTriggerProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, BlockState blockstate) {
      try {
      double hardness = 0.0;
      if (blockstate.getBlock() == CrustyChunksModBlocks.EXPLOSIVE_BARREL.get()) {
         world.destroyBlock(BlockPos.containing(x, y, z), false);
         ExplosionExampleProcedure.execute(world, x + 0.5, y + 0.5, z + 0.5, 3.5);
      } else {
         world.destroyBlock(BlockPos.containing(x, y, z), false);
         ExplosionExampleProcedure.execute(world, x + 0.5, y + 0.5, z + 0.5, 6.0);
         int horizontalRadiusSquare = 5;
         int verticalRadiusSquare = 5;
         int yIterationsSquare = verticalRadiusSquare;

         for (int i = -verticalRadiusSquare; i <= yIterationsSquare; i++) {
            for (int xi = -horizontalRadiusSquare; xi <= horizontalRadiusSquare; xi++) {
               for (int zi = -horizontalRadiusSquare; zi <= horizontalRadiusSquare; zi++) {
                  if (!world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                        .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:immortal")))
                     && -1.0 < hardness
                     && 15.0 >= hardness) {
                     if (!world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                           .is(BlockTags.create(ResourceLocation.parse("minecraft:mineable/pickaxe")))
                        && !world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                           .is(BlockTags.create(ResourceLocation.parse("minecraft:mineable/axe")))
                        && !world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                           .is(BlockTags.create(ResourceLocation.parse("minecraft:mineable/shovel")))) {
                        world.setBlock(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi), Blocks.AIR.defaultBlockState(), 3);
                     } else {
                        BlockPos _pos = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        Block.dropResources(world.getBlockState(_pos), world, BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi), null);
                        world.destroyBlock(_pos, false);
                     }
                  }
               }
            }
         }
      }

      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ExplosiveBarrelTriggerProcedure.execute", _wtSafe);
      }
   }
}
