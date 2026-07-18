package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import java.util.Map.Entry;
import net.mcreator.crustychunks.entity.DebrisEntity;
import net.mcreator.crustychunks.entity.GlareEffectEntity;
import net.mcreator.crustychunks.entity.HVParticleProjectileEntity;
import net.mcreator.crustychunks.entity.SplashEffectClientBypassEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class WariumExplosionServerProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, double power) {
      boolean found = false;
      double explosionpower = 0.0;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      BlockState targetblock = Blocks.AIR.defaultBlockState();
      explosionpower = power;
      if (1.0 < power) {
         sx = -4.0 * Math.sqrt(power);

         for (int index0 = 0; index0 < (int)(8.0 * Math.sqrt(explosionpower)); index0++) {
            sy = -2.0 * Math.sqrt(explosionpower);

            for (int index1 = 0; index1 < (int)(2.0 * Math.sqrt(explosionpower)); index1++) {
               sz = -4.0 * Math.sqrt(explosionpower);

               for (int index2 = 0; index2 < (int)(8.0 * Math.sqrt(explosionpower)); index2++) {
                  if (Math.sqrt(Math.pow(sz, 2.0) + Math.pow(sx, 2.0)) < 5.0 * Math.sqrt(explosionpower)
                     && world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz)).canOcclude()) {
                     targetblock = world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz));
                     if (targetblock.getBlock() == Blocks.DIRT) {
                        if (Math.sqrt(Math.pow(sz, 2.0) + Math.pow(sx, 2.0)) < 2.0 * Math.sqrt(explosionpower)) {
                           BlockPos _bp = BlockPos.containing(x + sx, y + sy, z + sz);
                           BlockState _bs = ((Block)CrustyChunksModBlocks.SCORCH_DIRT.get()).defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var67 = _bso.getProperties().iterator();

                           while (var67.hasNext()) {

                              Property<?> entry_prop = var67.next();

                              Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                              if (_property != null && _bs.getValue(_property) != null) {
                                 try {
                                    _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                                 } catch (Exception var32) {
                                 }
                              }
                           }

                           world.setBlock(_bp, _bs, 3);
                        } else {
                           BlockPos _bp = BlockPos.containing(x + sx, y + sy, z + sz);
                           BlockState _bs = ((Block)CrustyChunksModBlocks.HARDDIRT.get()).defaultBlockState();
                           BlockState _bso = world.getBlockState(_bp);
                           java.util.Iterator<Property<?>> var66 = _bso.getProperties().iterator();

                           while (var66.hasNext()) {

                              Property<?> entry_prop = var66.next();

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
                     } else if (targetblock.getBlock() == Blocks.SAND) {
                        BlockPos _bp = BlockPos.containing(x + sx, y + sy, z + sz);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.IMMUNITY_SAND.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var65 = _bso.getProperties().iterator();

                        while (var65.hasNext()) {

                           Property<?> entry_prop = var65.next();

                           Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                           if (_property != null && _bs.getValue(_property) != null) {
                              try {
                                 _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                              } catch (Exception var30) {
                              }
                           }
                        }

                        world.setBlock(_bp, _bs, 3);
                     } else if (targetblock.getBlock() == Blocks.RED_SAND) {
                        BlockPos _bp = BlockPos.containing(x + sx, y + sy, z + sz);
                        BlockState _bs = ((Block)CrustyChunksModBlocks.IMMUNITY_RED_SAND.get()).defaultBlockState();
                        BlockState _bso = world.getBlockState(_bp);
                        java.util.Iterator<Property<?>> var25 = _bso.getProperties().iterator();

                        while (var25.hasNext()) {

                           Property<?> entry_prop = var25.next();

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

                  sz++;
               }

               sy++;
            }

            sx++;
         }
      }

      if (world instanceof Level _level && !_level.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, x, y, z, (float)explosionpower, ExplosionInteraction.BLOCK);
      }

      if (world instanceof Level _level && !_level.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, x, y, z, (float)(explosionpower * 2.0), ExplosionInteraction.NONE);
      }

      for (int index3 = 0; index3 < (int)Math.min(350.0, 25.0 + explosionpower * 15.0); index3++) {
         if (explosionpower <= 4.0) {
            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new HVParticleProjectileEntity((EntityType<? extends HVParticleProjectileEntity>)CrustyChunksModEntities.HV_PARTICLE_PROJECTILE.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
                  .getArrow(projectileLevel, 0.0F, 3, (byte)2);
               _entityToSpawn.setPos(
                  x + Mth.nextDouble(RandomSource.create(), -0.5, 0.5),
                  y + Mth.nextDouble(RandomSource.create(), -0.5, 0.5),
                  z + Mth.nextDouble(RandomSource.create(), -0.5, 0.5)
               );
               _entityToSpawn.shoot(
                  Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                  Mth.nextDouble(RandomSource.create(), -0.1, 1.0),
                  Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                  (float)Mth.nextDouble(RandomSource.create(), 1.5, 2.0),
                  25.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         } else if (explosionpower <= 15.0) {
            if (1 == Mth.nextInt(RandomSource.create(), 1, 5)) {
               if (world instanceof ServerLevel projectileLevel) {
                  Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new DebrisEntity((EntityType<? extends DebrisEntity>)CrustyChunksModEntities.DEBRIS.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
                  }).getArrow(projectileLevel, 0.0F, 3, (byte)2);
                  _entityToSpawn.setPos(
                     x + Mth.nextDouble(RandomSource.create(), -0.5, 0.5),
                     y + Mth.nextDouble(RandomSource.create(), -0.5, 0.5),
                     z + Mth.nextDouble(RandomSource.create(), -0.5, 0.5)
                  );
                  _entityToSpawn.shoot(
                     Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                     0.25,
                     Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                     (float)Mth.nextDouble(RandomSource.create(), 1.5, 2.0),
                     15.0F
                  );
                  projectileLevel.addFreshEntity(_entityToSpawn);
               }
            } else if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new HVParticleProjectileEntity((EntityType<? extends HVParticleProjectileEntity>)CrustyChunksModEntities.HV_PARTICLE_PROJECTILE.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
                  .getArrow(projectileLevel, 0.0F, 3, (byte)2);
               _entityToSpawn.setPos(
                  x + Mth.nextDouble(RandomSource.create(), -0.5, 0.5),
                  y + Mth.nextDouble(RandomSource.create(), -0.5, 0.5),
                  z + Mth.nextDouble(RandomSource.create(), -0.5, 0.5)
               );
               _entityToSpawn.shoot(
                  Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                  Mth.nextDouble(RandomSource.create(), -0.1, 1.0),
                  Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                  (float)Mth.nextDouble(RandomSource.create(), 2.0, 3.0),
                  25.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         } else if (1 == Mth.nextInt(RandomSource.create(), 1, 4)) {
            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                     AbstractArrow entityToSpawn = new DebrisEntity((EntityType<? extends DebrisEntity>)CrustyChunksModEntities.DEBRIS.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
               }).getArrow(projectileLevel, 0.0F, 3, (byte)2);
               _entityToSpawn.setPos(
                  x + Mth.nextDouble(RandomSource.create(), -0.5, 0.5),
                  y + Mth.nextDouble(RandomSource.create(), -0.5, 0.5),
                  z + Mth.nextDouble(RandomSource.create(), -0.5, 0.5)
               );
               _entityToSpawn.shoot(
                  Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                  0.25,
                  Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                  (float)Mth.nextDouble(RandomSource.create(), 2.0, 3.0),
                  15.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         } else if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                     AbstractArrow entityToSpawn = new HVParticleProjectileEntity((EntityType<? extends HVParticleProjectileEntity>)CrustyChunksModEntities.HV_PARTICLE_PROJECTILE.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
               .getArrow(projectileLevel, 0.0F, 3, (byte)2);
            _entityToSpawn.setPos(
               x + Mth.nextDouble(RandomSource.create(), -0.5, 0.5),
               y + Mth.nextDouble(RandomSource.create(), -0.5, 0.5),
               z + Mth.nextDouble(RandomSource.create(), -0.5, 0.5)
            );
            _entityToSpawn.shoot(
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
               Mth.nextDouble(RandomSource.create(), -0.1, 1.0),
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
               (float)Mth.nextDouble(RandomSource.create(), 3.0, 4.0),
               25.0F
            );
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }

      if (world.getFluidState(BlockPos.containing(x, y + 1.0, z)).createLegacyBlock().getBlock() instanceof LiquidBlock
         || world.getFluidState(BlockPos.containing(x, y, z)).createLegacyBlock().getBlock() instanceof LiquidBlock) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                     AbstractArrow entityToSpawn = new SplashEffectClientBypassEntity((EntityType<? extends SplashEffectClientBypassEntity>)CrustyChunksModEntities.SPLASH_EFFECT_CLIENT_BYPASS.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
                     entityToSpawn.setCritArrow(true);
                     return entityToSpawn;
                  }
               })
               .getArrow(projectileLevel, 14.0F, 4, (byte)2);
            _entityToSpawn.setPos(x, y, z);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.0F, 120.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (world instanceof Level _level && !_level.isClientSide()) {
            net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, x, y, z, (float)(explosionpower * 1.1), ExplosionInteraction.BLOCK);
         }
      }

      if (world instanceof ServerLevel projectileLevel) {
         Projectile _entityToSpawn = (new Object() {
            public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
               AbstractArrow entityToSpawn = new GlareEffectEntity((EntityType<? extends GlareEffectEntity>)CrustyChunksModEntities.GLARE_EFFECT.get(), level) {
               @Override
               public byte getPierceLevel() {
                  return piercing;
               }

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
               entityToSpawn.setCritArrow(true);
               return entityToSpawn;
            }
         }).getArrow(projectileLevel, 14.0F, 4, (byte)2);
         _entityToSpawn.setPos(x, y, z);
         _entityToSpawn.shoot(0.0, 0.0, 0.0, 0.0F, 0.0F);
         projectileLevel.addFreshEntity(_entityToSpawn);
      }
   }
}
