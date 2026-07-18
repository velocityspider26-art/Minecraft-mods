package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import java.util.Comparator;
import java.util.Map.Entry;
import net.mcreator.crustychunks.entity.FusionHeatWaveEntity;
import net.mcreator.crustychunks.entity.NuclearThermalRadEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WariumNuclearScorchProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, double power) {
      double explosionpower = 0.0;
      explosionpower = power / 60.0;
      Vec3 _center = new Vec3(x, y, z);

      for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(100.0 * explosionpower / 2.0), e -> true)
         .stream()
         .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
         .toList()) {
         entityiterator.hurt(
            new DamageSource(
               world.registryAccess()
                  .registryOrThrow(Registries.DAMAGE_TYPE)
                  .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("crusty_chunks:vaporized")))
            ),
            (float)(4000.0 * explosionpower)
         );
         entityiterator.igniteForSeconds(30);
      }

      int horizontalRadiusSphere = (int)(50.0 * explosionpower) - 1;
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
                        java.util.Iterator<Property<?>> var99 = _bso.getProperties().iterator();

                        while (var99.hasNext()) {

                           Property<?> entry_prop = var99.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var40) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if ((double)Mth.nextInt(RandomSource.create(), 5, 15) * explosionpower
                        > Math.sqrt(Math.pow(Math.abs(x + (double)xi - x), 2.0) + Math.pow(Math.abs(z + (double)zi - z), 2.0))) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.OBSIDIAN.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var22 = _bso.getProperties().iterator();

                        while (var22.hasNext()) {

                           Property<?> entry_prop = var22.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var39) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if ((double)Mth.nextInt(RandomSource.create(), 15, 25) * explosionpower
                        > Math.sqrt(Math.pow(Math.abs(x + (double)xi - x), 2.0) + Math.pow(Math.abs(z + (double)zi - z), 2.0))) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.BLACKSTONE.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var92 = _bso.getProperties().iterator();

                        while (var92.hasNext()) {

                           Property<?> entry_prop = var92.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var38) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if ((double)Mth.nextInt(RandomSource.create(), 25, 35) * explosionpower
                        > Math.sqrt(Math.pow(Math.abs(x + (double)xi - x), 2.0) + Math.pow(Math.abs(z + (double)zi - z), 2.0))) {
                        if (Math.random() < 0.5) {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = Blocks.BLACKSTONE.defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var93 = _bso.getProperties().iterator();

                           while (var93.hasNext()) {

                              Property<?> entry_prop = var93.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var37) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        } else {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = Blocks.SMOOTH_BASALT.defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var94 = _bso.getProperties().iterator();

                           while (var94.hasNext()) {

                              Property<?> entry_prop = var94.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var36) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        }
                     } else if ((double)Mth.nextInt(RandomSource.create(), 35, 45) * explosionpower
                        > Math.sqrt(Math.pow(Math.abs(x + (double)xi - x), 2.0) + Math.pow(Math.abs(z + (double)zi - z), 2.0))) {
                        if (Math.random() < 0.5) {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var95 = _bso.getProperties().iterator();

                           while (var95.hasNext()) {

                              Property<?> entry_prop = var95.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var35) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        } else {
                           BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                           BlockState _bs = Blocks.SMOOTH_BASALT.defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var96 = _bso.getProperties().iterator();

                           while (var96.hasNext()) {

                              Property<?> entry_prop = var96.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var34) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        }
                     } else if (Math.random() < 0.5) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var97 = _bso.getProperties().iterator();

                        while (var97.hasNext()) {

                           Property<?> entry_prop = var97.next();

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
                        BlockState _bs = Blocks.TUFF.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var98 = _bso.getProperties().iterator();

                        while (var98.hasNext()) {

                           Property<?> entry_prop = var98.next();

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

                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:dirts")))) {
                     if (1 == Mth.nextInt(RandomSource.create(), 1, 50)
                        && !world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 1.0, z + (double)zi)).canOcclude()) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.RADIOACTIVE_ASH_FULL_BLOCK.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var102 = _bso.getProperties().iterator();

                        while (var102.hasNext()) {

                           Property<?> entry_prop = var102.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var31) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if (Math.random() < 0.5) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.PACKED_MUD.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var100 = _bso.getProperties().iterator();

                        while (var100.hasNext()) {

                           Property<?> entry_prop = var100.next();

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
                        BlockState _bs = Blocks.TERRACOTTA.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var101 = _bso.getProperties().iterator();

                        while (var101.hasNext()) {

                           Property<?> entry_prop = var101.next();

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

                  if (world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi))
                     .is(BlockTags.create(ResourceLocation.parse("crusty_chunks:sands")))) {
                     if (1 == Mth.nextInt(RandomSource.create(), 1, 50)
                        && !world.getBlockState(BlockPos.containing(x + (double)xi, y + (double)i + 1.0, z + (double)zi)).canOcclude()) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.RADIOACTIVE_ASH_FULL_BLOCK.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var105 = _bso.getProperties().iterator();

                        while (var105.hasNext()) {

                           Property<?> entry_prop = var105.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var28) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if (Math.random() < 0.5) {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.TRINITITE.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var104 = _bso.getProperties().iterator();

                        while (var104.hasNext()) {

                           Property<?> entry_prop = var104.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var27) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else {
                        BlockPos _bp = BlockPos.containing(x + (double)xi, y + (double)i, z + (double)zi);
                        BlockState _bs = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var103 = _bso.getProperties().iterator();

                        while (var103.hasNext()) {

                           Property<?> entry_prop = var103.next();

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
      }

      if (1.0 < explosionpower) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new FusionHeatWaveEntity((EntityType<? extends FusionHeatWaveEntity>)CrustyChunksModEntities.FUSION_HEAT_WAVE.get(), level) {
               @Override
               protected void doKnockback(LivingEntity livingEntity, DamageSource damageSource) {
                  if (knockback > 0) {
                     double _kbres = Math.max(0.0, 1.0 - livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                     Vec3 _kbvec = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(knockback * 0.6 * _kbres);
                     if (_kbvec.lengthSqr() > 0.0) {
                        livingEntity.push(_kbvec.x, 0.1, _kbvec.z);
                     }
                  }
               }
            };
                     entityToSpawn.setBaseDamage((double)damage);
                     entityToSpawn.setSilent(true);
                     return entityToSpawn;
                  }
               })
               .getArrow(projectileLevel, 5.0F, 1);
            _entityToSpawn.setPos(x, y + 7.0, z);
            _entityToSpawn.shoot(0.0, 0.0, 0.0, 0.0F, 0.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      } else if (world instanceof ServerLevel projectileLevel) {
         Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback) {
                  AbstractArrow entityToSpawn = new NuclearThermalRadEntity((EntityType<? extends NuclearThermalRadEntity>)CrustyChunksModEntities.NUCLEAR_THERMAL_RAD.get(), level) {
               @Override
               protected void doKnockback(LivingEntity livingEntity, DamageSource damageSource) {
                  if (knockback > 0) {
                     double _kbres = Math.max(0.0, 1.0 - livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                     Vec3 _kbvec = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(knockback * 0.6 * _kbres);
                     if (_kbvec.lengthSqr() > 0.0) {
                        livingEntity.push(_kbvec.x, 0.1, _kbvec.z);
                     }
                  }
               }
            };
                  entityToSpawn.setBaseDamage((double)damage);
                  entityToSpawn.setSilent(true);
                  return entityToSpawn;
               }
            })
            .getArrow(projectileLevel, 5.0F, 1);
         _entityToSpawn.setPos(x, y + 7.0, z);
         _entityToSpawn.shoot(0.0, 0.0, 0.0, 0.0F, 0.0F);
         projectileLevel.addFreshEntity(_entityToSpawn);
      }
   }
}
