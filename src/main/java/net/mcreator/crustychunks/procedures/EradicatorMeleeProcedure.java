package net.mcreator.crustychunks.procedures;

import java.util.Map.Entry;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class EradicatorMeleeProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      int horizontalRadiusSquare = 5;
      int verticalRadiusSquare = 3;
      int yIterationsSquare = verticalRadiusSquare;

      for (int i = -verticalRadiusSquare; i <= yIterationsSquare; i++) {
         for (int xi = -horizontalRadiusSquare; xi <= horizontalRadiusSquare; xi++) {
            for (int zi = -horizontalRadiusSquare; zi <= horizontalRadiusSquare; zi++) {
               if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 4.0, z + (double)zi))
                           .getDestroySpeed(world, BlockPos.containing(x + (double)xi, y + (double)i + 4.0, z + (double)zi))
                        < 5.0F
                     && world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 4.0, z + (double)zi))
                           .getDestroySpeed(world, BlockPos.containing(x + (double)xi, y + (double)i + 4.0, z + (double)zi))
                        >= 0.0F
                  || world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 4.0, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:chippable")))
                  || world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 4.0, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:concrete")))) {
                  world.setBlock(BlockPos.containing(x + (double)xi, y + (double)i + 4.0, z + (double)zi), Blocks.AIR.defaultBlockState(), 3);
               }

               if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 3.0, z + (double)zi)).getBlock()
                  == CrustyChunksModBlocks.STRUCTURAL_CONCRETE.get()) {
                  BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i + 3.0, z + (double)zi);
                  BlockState _bs = ((Block)CrustyChunksModBlocks.REENFORCED_CONCRETE.get()).defaultBlockState();
                  BlockState _bso = world.getBlockState(_bp);
                  java.util.Iterator<Property<?>> var16 = _bso.getProperties().iterator();

                  while (var16.hasNext()) {

                     Property<?> entry_prop = var16.next();

                     Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                     if (_property != null && _bs.getValue(_property) != null) {
                        try {
                           _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                        } catch (Exception var20) {
                        }
                     }
                  }

                  world.setBlock(_bp, _bs, 3);
               }

               if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:chippable")))
                  || world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:splinterable")))) {
                  world.setBlock(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi), Blocks.AIR.defaultBlockState(), 3);
               }
            }
         }
      }
   }
}
