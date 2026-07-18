package net.mcreator.crustychunks.procedures;

import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class WariumNuclearPlasmaProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, double power) {
      double explosionpower = 0.0;
      double hardness = 0.0;
      explosionpower = power / 60.0;
      int horizontalRadiusSphere = (int)(45.0 * explosionpower) - 1;
      int verticalRadiusSphere = 14;
      int yIterationsSphere = verticalRadiusSphere;

      for (int i = -verticalRadiusSphere; i <= yIterationsSphere; i++) {
         for (int xi = -horizontalRadiusSphere; xi <= horizontalRadiusSphere; xi++) {
            for (int zi = -horizontalRadiusSphere; zi <= horizontalRadiusSphere; zi++) {
               double distanceSq = (double)(xi * xi) / (double)(horizontalRadiusSphere * horizontalRadiusSphere)
                  + (double)(i * i) / (double)(verticalRadiusSphere * verticalRadiusSphere)
                  + (double)(zi * zi) / (double)(horizontalRadiusSphere * horizontalRadiusSphere);
               if (distanceSq <= 1.0 && !world.isEmptyBlock(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))) {
                  hardness = (double)world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                     .getDestroySpeed(world, BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi));
                  if (!world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                        .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:immortal")))
                     && -1.0 < hardness
                     && 1000.0 >= hardness) {
                     BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                     BlockState _bs = Blocks.FIRE.defaultBlockState();
                     BlockState _bso = world.getBlockState(_bp);
                     java.util.Iterator<Property<?>> _bpx = _bso.getProperties().iterator();

                     while (_bpx.hasNext()) {

                        Property<?> entry_prop = _bpx.next();

                        Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                        if (_property != null && _bs.getValue(_property) != null) {
                           try {
                              _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                           } catch (Exception var32) {
                           }
                        }
                     }

                     world.setBlock(_bp, _bs, 3);
                  }
               }
            }
         }
      }

      int horizontalRadiusHemiTop = (int)(45.0 * explosionpower) - 1;
      int verticalRadiusHemiTop = (int)(46.0 * explosionpower);
      int yIterationsHemiTop = verticalRadiusHemiTop;

      for (int i = 0; i < yIterationsHemiTop; i++) {
         if (i != verticalRadiusHemiTop) {
            for (int xi = -horizontalRadiusHemiTop; xi <= horizontalRadiusHemiTop; xi++) {
               for (int zix = -horizontalRadiusHemiTop; zix <= horizontalRadiusHemiTop; zix++) {
                  double distanceSq = (double)(xi * xi) / (double)(horizontalRadiusHemiTop * horizontalRadiusHemiTop)
                     + (double)(i * i) / (double)(verticalRadiusHemiTop * verticalRadiusHemiTop)
                     + (double)(zix * zix) / (double)(horizontalRadiusHemiTop * horizontalRadiusHemiTop);
                  if (distanceSq <= 1.0 && !world.isEmptyBlock(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zix))) {
                     hardness = (double)world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zix))
                        .getDestroySpeed(world, BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zix));
                     if (!world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zix))
                           .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:immortal")))
                        && -1.0 < hardness
                        && 1000.0 >= hardness) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zix);
                        BlockState _bs = Blocks.FIRE.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var27 = _bso.getProperties().iterator();

                        while (var27.hasNext()) {

                           Property<?> entry_prop = var27.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var31) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     }
                  }
               }
            }
         }
      }
   }
}
