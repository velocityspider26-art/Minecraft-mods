package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.Map.Entry;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class FissionEffectsProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
         net.mcreator.crustychunks.compat.WariumNukeEffects.play(world, x, y, z, net.mcreator.crustychunks.compat.WariumNukeEffects.Profile.STANDARD);
      double loop = 0.0;
      CrustyChunksMod.queueServerWork(
         1,
         () -> {
            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rumble")),
                     SoundSource.NEUTRAL,
                     100.0F,
                     1.2F
                  );
               } else {
                  _levelx.playLocalSound(
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

            PlasmaCraterProcedure.execute(world, x, y, z);
            ScorchProcedure.execute(world, x, y, z);
            int horizontalRadiusSphere = 119;
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
                              java.util.Iterator<Property<?>> var18 = _bso.getProperties().iterator();

                              while (var18.hasNext()) {

                                 Property<?> entry_prop = var18.next();

                                 Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                                 if (_property != null && _bs.getValue(_property) != null) {
                                    try {
                                       _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                    } catch (Exception var28) {
                                    }
                                 }
                              }

                              world.setBlock(_bp, _bs, 3);
                           } else if ((double)Mth.nextInt(RandomSource.create(), 100, 120)
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
                              java.util.Iterator<Property<?>> var48 = _bso.getProperties().iterator();

                              while (var48.hasNext()) {

                                 Property<?> entry_prop = var48.next();

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

                           if ((double)Mth.nextInt(RandomSource.create(), 50, 120)
                                 > Math.sqrt(Math.pow(Math.abs(x + (double)xi - x), 2.0) + Math.pow(Math.abs(z + (double)zi - z), 2.0))
                              && world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 1.0, z + (double)zi)).getBlock() == Blocks.AIR) {
                              if (Mth.nextInt(RandomSource.create(), 1, 3) == 1) {
                                 BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                                 BlockState _bs = ((Block)CrustyChunksModBlocks.TRINITITE.get()).defaultBlockState();
                                 BlockState _bso = world.getBlockState(_bp);
                                 java.util.Iterator<Property<?>> var49 = _bso.getProperties().iterator();

                                 while (var49.hasNext()) {

                                    Property<?> entry_prop = var49.next();

                                    Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                                    if (_property != null && _bs.getValue(_property) != null) {
                                       try {
                                          _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                       } catch (Exception var26) {
                                       }
                                    }
                                 }

                                 world.setBlock(_bp, _bs, 3);
                              } else {
                                 BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                                 BlockState _bs = Blocks.TUFF.defaultBlockState();
                                 BlockState _bso = world.getBlockState(_bp);
                                 java.util.Iterator<Property<?>> var50 = _bso.getProperties().iterator();

                                 while (var50.hasNext()) {

                                    Property<?> entry_prop = var50.next();

                                    Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                                    if (_property != null && _bs.getValue(_property) != null) {
                                       try {
                                          _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                       } catch (Exception var25) {
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
                           java.util.Iterator<Property<?>> var51 = _bso.getProperties().iterator();

                           while (var51.hasNext()) {

                              Property<?> entry_prop = var51.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var24) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        }

                        if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock() == Blocks.SNOW_BLOCK) {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = Blocks.AIR.defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var52 = _bso.getProperties().iterator();

                           while (var52.hasNext()) {

                              Property<?> entry_prop = var52.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var23) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        }

                        if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi)).getBlock() == Blocks.ICE) {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = Blocks.WATER.defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var53 = _bso.getProperties().iterator();

                           while (var53.hasNext()) {

                              Property<?> entry_prop = var53.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var22) {
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
      );
      if (world instanceof Level _level && !_level.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, x, y + 4.0, z, 30.0F, ExplosionInteraction.BLOCK);
      }

      if (world instanceof Level _level && !_level.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, x, y + 4.0, z, 40.0F, ExplosionInteraction.NONE);
      }

      for (Entity entityiterator : new ArrayList<>(world.players())) {
         CrustyChunksMod.queueServerWork(
            (int)Math.abs(Math.round(Math.sqrt(Math.pow(x - entityiterator.getX(), 2.0) + Math.pow(z - entityiterator.getZ(), 2.0)) * 0.5)),
            () -> {
               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:fissionblast")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelx.playLocalSound(
                        entityiterator.getX(),
                        entityiterator.getY(),
                        entityiterator.getZ(),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:fissionblast")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }
            }
         );
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FissionEffectsProcedure.execute", _wtSafe);
      }
   }
}
