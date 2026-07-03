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

public class SpaceScorchProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, double scale) {
      Vec3 _center = new Vec3(x, y, z);

      for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(60.0 * scale / 2.0), e -> true)
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

      int horizontalRadiusSphere = (int)(60.0 * scale) - 1;
      int verticalRadiusSphere = (int)(60.0 * scale) - 1;
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
                        java.util.Iterator<Property<?>> var62 = _bso.getProperties().iterator();

                        while (var62.hasNext()) {

                           Property<?> entry_prop = var62.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var32) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if (Math.random() < 0.5) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var20 = _bso.getProperties().iterator();

                        while (var20.hasNext()) {

                           Property<?> entry_prop = var20.next();

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
                        BlockState _bs = Blocks.TUFF.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var61 = _bso.getProperties().iterator();

                        while (var61.hasNext()) {

                           Property<?> entry_prop = var61.next();

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
                  }

                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:dirts")))) {
                     if (1 == Mth.nextInt(RandomSource.create(), 1, 50)
                        && !world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 1.0, z + (double)zi)).canOcclude()) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.RADIOACTIVE_ASH_FULL_BLOCK.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var65 = _bso.getProperties().iterator();

                        while (var65.hasNext()) {

                           Property<?> entry_prop = var65.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var29) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if (Math.random() < 0.5) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.PACKED_MUD.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var63 = _bso.getProperties().iterator();

                        while (var63.hasNext()) {

                           Property<?> entry_prop = var63.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var28) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.TERRACOTTA.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var64 = _bso.getProperties().iterator();

                        while (var64.hasNext()) {

                           Property<?> entry_prop = var64.next();

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
                  }

                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:sands")))) {
                     if (1 == Mth.nextInt(RandomSource.create(), 1, 50)
                        && !world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 1.0, z + (double)zi)).canOcclude()) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.RADIOACTIVE_ASH_FULL_BLOCK.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var68 = _bso.getProperties().iterator();

                        while (var68.hasNext()) {

                           Property<?> entry_prop = var68.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var26) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if (Math.random() < 0.5) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.TRINITITE.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var67 = _bso.getProperties().iterator();

                        while (var67.hasNext()) {

                           Property<?> entry_prop = var67.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var25) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var66 = _bso.getProperties().iterator();

                        while (var66.hasNext()) {

                           Property<?> entry_prop = var66.next();

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
                  }
               }
            }
         }
      }
   }
}
