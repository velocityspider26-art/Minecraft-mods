package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import java.util.Map.Entry;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ScorchProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      Vec3 _center = new Vec3(x, y, z);

      for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(50.0), e -> true)
         .stream()
         .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
         .toList()) {
         entityiterator.hurt(
            new DamageSource(
               world.registryAccess()
                  .registryOrThrow(Registries.DAMAGE_TYPE)
                  .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("crusty_chunks:vaporized")))
            ),
            4000.0F
         );
         entityiterator.igniteForSeconds(30);
      }

      int horizontalRadiusSphere = 49;
      int verticalRadiusSphere = 19;
      int yIterationsSphere = verticalRadiusSphere;

      for (int i = -verticalRadiusSphere; i <= yIterationsSphere; i++) {
         for (int xi = -horizontalRadiusSphere; xi <= horizontalRadiusSphere; xi++) {
            for (int zi = -horizontalRadiusSphere; zi <= horizontalRadiusSphere; zi++) {
               double distanceSq = (double)(xi * xi) / (double)(horizontalRadiusSphere * horizontalRadiusSphere)
                  + (double)(i * i) / (double)(verticalRadiusSphere * verticalRadiusSphere)
                  + (double)(zi * zi) / (double)(horizontalRadiusSphere * horizontalRadiusSphere);
               if (distanceSq <= 1.0) {
                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                        .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:stone")))
                     || world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                        .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:crushable")))) {
                     if (1 == Mth.nextInt(RandomSource.create(), 1, 50)
                        && !world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 1.0, z + (double)zi)).canOcclude()) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.RADIOACTIVE_ASH_FULL_BLOCK.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var90 = _bso.getProperties().iterator();

                        while (var90.hasNext()) {

                           Property<?> entry_prop = var90.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var36) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if ((double)Mth.nextInt(RandomSource.create(), 5, 15)
                        > Math.sqrt(Math.pow(Math.abs(x + (double)xi - x), 2.0) + Math.pow(Math.abs(z + (double)zi - z), 2.0))) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.OBSIDIAN.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var18 = _bso.getProperties().iterator();

                        while (var18.hasNext()) {

                           Property<?> entry_prop = var18.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var35) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if ((double)Mth.nextInt(RandomSource.create(), 15, 25)
                        > Math.sqrt(Math.pow(Math.abs(x + (double)xi - x), 2.0) + Math.pow(Math.abs(z + (double)zi - z), 2.0))) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.BLACKSTONE.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var83 = _bso.getProperties().iterator();

                        while (var83.hasNext()) {

                           Property<?> entry_prop = var83.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var34) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if ((double)Mth.nextInt(RandomSource.create(), 25, 35)
                        > Math.sqrt(Math.pow(Math.abs(x + (double)xi - x), 2.0) + Math.pow(Math.abs(z + (double)zi - z), 2.0))) {
                        if (Math.random() < 0.5) {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = Blocks.BLACKSTONE.defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var84 = _bso.getProperties().iterator();

                           while (var84.hasNext()) {

                              Property<?> entry_prop = var84.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var33) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        } else {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = Blocks.SMOOTH_BASALT.defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var85 = _bso.getProperties().iterator();

                           while (var85.hasNext()) {

                              Property<?> entry_prop = var85.next();

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
                     } else if ((double)Mth.nextInt(RandomSource.create(), 35, 45)
                        > Math.sqrt(Math.pow(Math.abs(x + (double)xi - x), 2.0) + Math.pow(Math.abs(z + (double)zi - z), 2.0))) {
                        if (Math.random() < 0.5) {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var86 = _bso.getProperties().iterator();

                           while (var86.hasNext()) {

                              Property<?> entry_prop = var86.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var31) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        } else {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = Blocks.SMOOTH_BASALT.defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var87 = _bso.getProperties().iterator();

                           while (var87.hasNext()) {

                              Property<?> entry_prop = var87.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var30) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        }
                     } else if (Math.random() < 0.5) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var88 = _bso.getProperties().iterator();

                        while (var88.hasNext()) {

                           Property<?> entry_prop = var88.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var29) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.TUFF.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var89 = _bso.getProperties().iterator();

                        while (var89.hasNext()) {

                           Property<?> entry_prop = var89.next();

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
                  }

                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:dirts")))) {
                     if (1 == Mth.nextInt(RandomSource.create(), 1, 50)
                        && !world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 1.0, z + (double)zi)).canOcclude()) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.RADIOACTIVE_ASH_FULL_BLOCK.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var93 = _bso.getProperties().iterator();

                        while (var93.hasNext()) {

                           Property<?> entry_prop = var93.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var27) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if (Math.random() < 0.5) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.PACKED_MUD.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var91 = _bso.getProperties().iterator();

                        while (var91.hasNext()) {

                           Property<?> entry_prop = var91.next();

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
                        BlockState _bs = Blocks.TERRACOTTA.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var92 = _bso.getProperties().iterator();

                        while (var92.hasNext()) {

                           Property<?> entry_prop = var92.next();

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

                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:sands")))) {
                     if (1 == Mth.nextInt(RandomSource.create(), 1, 50)
                        && !world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 1.0, z + (double)zi)).canOcclude()) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.RADIOACTIVE_ASH_FULL_BLOCK.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var96 = _bso.getProperties().iterator();

                        while (var96.hasNext()) {

                           Property<?> entry_prop = var96.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var24) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if (Math.random() < 0.5) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.TRINITITE.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var95 = _bso.getProperties().iterator();

                        while (var95.hasNext()) {

                           Property<?> entry_prop = var95.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var23) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var94 = _bso.getProperties().iterator();

                        while (var94.hasNext()) {

                           Property<?> entry_prop = var94.next();

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
   }
}
