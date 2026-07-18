package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map.Entry;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class WariumNuclearExplosionShapesProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, double power) {
      double explosionpower = 0.0;
      explosionpower = power / 60.0;
      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rumble")),
               SoundSource.NEUTRAL,
               100.0F,
               1.2F
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rumble")),
               SoundSource.NEUTRAL,
               100.0F,
               1.2F,
               false
            );
         }
      }

      WariumNuclearPlasmaProcedure.execute(world, x, y, z, power);
      WariumNuclearScorchProcedure.execute(world, x, y, z, power);
      int horizontalRadiusSphere = (int)(120.0 * Math.min(2.5, explosionpower)) - 1;
      int verticalRadiusSphere = 39;
      int yIterationsSphere = verticalRadiusSphere;

      for (int i = -verticalRadiusSphere; i <= yIterationsSphere; i++) {
         for (int xi = -horizontalRadiusSphere; xi <= horizontalRadiusSphere; xi++) {
            for (int zi = -horizontalRadiusSphere; zi <= horizontalRadiusSphere; zi++) {
               double distanceSq = (double)(xi * xi) / (double)(horizontalRadiusSphere * horizontalRadiusSphere)
                  + (double)(i * i) / (double)(verticalRadiusSphere * verticalRadiusSphere)
                  + (double)(zi * zi) / (double)(horizontalRadiusSphere * horizontalRadiusSphere);
               if (distanceSq <= 1.0) {
                  if (!world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 1.0, z + (double)zi)).canOcclude()
                     && (
                        world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock() == Blocks.GRASS_BLOCK
                           || world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock() == Blocks.MOSS_BLOCK
                     )) {
                     if (Mth.nextInt(RandomSource.create(), 1, 100) == 1) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.RADIOACTIVE_ASH_FULL_BLOCK.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var22 = _bso.getProperties().iterator();

                        while (var22.hasNext()) {

                           Property<?> entry_prop = var22.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var32) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if ((double)Mth.nextInt(RandomSource.create(), 100, 120) * Math.min(2.5, explosionpower)
                           > Math.sqrt(Math.pow(Math.abs(x + (double)xi - x), 2.0) + Math.pow(Math.abs(z + (double)zi - z), 2.0))
                        && world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).canOcclude()) {
                        MassBurnBlockProcedure.execute(world, x + (double)xi, y + (double)i, z + (double)zi);
                     }
                  } else {
                     MassBurnBlockProcedure.execute(world, x + (double)xi, y + (double)i, z + (double)zi);
                  }

                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:sands")))) {
                     if (!world.getBlockState(BlockPos.containing(x, y + 1.0, z)).canOcclude() && Mth.nextInt(RandomSource.create(), 1, 100) == 1) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.RADIOACTIVE_ASH_FULL_BLOCK.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var55 = _bso.getProperties().iterator();

                        while (var55.hasNext()) {

                           Property<?> entry_prop = var55.next();

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

                     if ((double)Mth.nextInt(RandomSource.create(), 50, 120) * Math.min(2.5, explosionpower)
                           > Math.sqrt(Math.pow(Math.abs(x + (double)xi - x), 2.0) + Math.pow(Math.abs(z + (double)zi - z), 2.0))
                        && world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 1.0, z + (double)zi)).getBlock() == Blocks.AIR) {
                        if (Mth.nextInt(RandomSource.create(), 1, 3) == 1) {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = ((Block)CrustyChunksModBlocks.TRINITITE.get()).defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var56 = _bso.getProperties().iterator();

                           while (var56.hasNext()) {

                              Property<?> entry_prop = var56.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var30) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        } else {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = Blocks.TUFF.defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var57 = _bso.getProperties().iterator();

                           while (var57.hasNext()) {

                              Property<?> entry_prop = var57.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var29) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        }
                     }
                  }

                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock() == Blocks.SNOW) {
                     BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                     BlockState _bs = Blocks.AIR.defaultBlockState();
                     BlockState _bso = world.getBlockState(_bp);
                     java.util.Iterator<Property<?>> var58 = _bso.getProperties().iterator();

                     while (var58.hasNext()) {

                        Property<?> entry_prop = var58.next();

                        Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                        if (_property != null && _bs.getValue(_property) != null) {
                           try {
                              _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                           } catch (Exception var28) {
                           }
                        }
                     }

                     world.setBlock(_bp, _bs, 3);
                  }

                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock() == Blocks.SNOW_BLOCK) {
                     BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                     BlockState _bs = Blocks.AIR.defaultBlockState();
                     BlockState _bso = world.getBlockState(_bp);
                     java.util.Iterator<Property<?>> var59 = _bso.getProperties().iterator();

                     while (var59.hasNext()) {

                        Property<?> entry_prop = var59.next();

                        Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                        if (_property != null && _bs.getValue(_property) != null) {
                           try {
                              _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                           } catch (Exception var27) {
                           }
                        }
                     }

                     world.setBlock(_bp, _bs, 3);
                  }

                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock() == Blocks.ICE) {
                     BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                     BlockState _bs = Blocks.WATER.defaultBlockState();
                     BlockState _bso = world.getBlockState(_bp);
                     java.util.Iterator<Property<?>> var60 = _bso.getProperties().iterator();

                     while (var60.hasNext()) {

                        Property<?> entry_prop = var60.next();

                        Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                        if (_property != null && _bs.getValue(_property) != null) {
                           try {
                              _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                           } catch (Exception var26) {
                           }
                        }
                     }

                     world.setBlock(_bp, _bs, 3);
                  }
               }
            }
         }
      }

      if (world instanceof Level _levelx && !_levelx.isClientSide()) {
         _levelx.explode(null, x, y + 4.0, z, 30.0F, ExplosionInteraction.BLOCK);
      }

      if (world instanceof Level _levelx && !_levelx.isClientSide()) {
         _levelx.explode(null, x, y + 4.0, z, 40.0F, ExplosionInteraction.NONE);
      }
   }
}
