package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map.Entry;
import net.mcreator.crustychunks.entity.DebrisEntity;
import net.mcreator.crustychunks.entity.GiantShockExplosionBypassEntity;
import net.mcreator.crustychunks.entity.HVParticleProjectileEntity;
import net.mcreator.crustychunks.entity.ShockClientsideBypassEntity;
import net.mcreator.crustychunks.entity.SplashEffectClientBypassEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

public class GiantExplosionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      boolean found = false;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      double xRadius = 0.0;
      double loop = 0.0;
      double zRadius = 0.0;
      double particleAmount = 0.0;
      sx = -20.0;
      found = false;

      for (int index0 = 0; index0 < 40; index0++) {
         sy = -9.0;

         for (int index1 = 0; index1 < 8; index1++) {
            sz = -20.0;

            for (int index2 = 0; index2 < 40; index2++) {
               if (Math.sqrt(Math.pow(sz, 2.0) + Math.pow(sx, 2.0)) < 20.0
                  && world.getBlockState(BlockPos.containing(x + sx, y + sy + 1.0, z + sz)).getBlock() != Blocks.AIR
                  && world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz)).getBlock() == Blocks.DIRT) {
                  if (Math.sqrt(Math.pow(sz, 2.0) + Math.pow(sx, 2.0)) < 6.0) {
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
                     java.util.Iterator<Property<?>> var28 = _bso.getProperties().iterator();

                     while (var28.hasNext()) {

                        Property<?> entry_prop = var28.next();

                        Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                        if (_property != null && _bs.getValue(_property) != null) {
                           try {
                              _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                           } catch (Exception var33) {
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

      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
               SoundSource.MASTER,
               140.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.0)
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
               SoundSource.MASTER,
               140.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.0),
               false
            );
         }
      }

      if (world instanceof Level _levelx) {
         if (!_levelx.isClientSide()) {
            _levelx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:huge_explosion_distant")),
               SoundSource.MASTER,
               60.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9)
            );
         } else {
            _levelx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:huge_explosion_distant")),
               SoundSource.MASTER,
               60.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9),
               false
            );
         }
      }

      if (world instanceof Level _levelxx) {
         if (!_levelxx.isClientSide()) {
            _levelxx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion")),
               SoundSource.MASTER,
               20.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9)
            );
         } else {
            _levelxx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion")),
               SoundSource.MASTER,
               20.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9),
               false
            );
         }
      }

      if (world instanceof Level _levelxxx) {
         if (!_levelxxx.isClientSide()) {
            _levelxxx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion")),
               SoundSource.MASTER,
               20.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.8)
            );
         } else {
            _levelxxx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion")),
               SoundSource.MASTER,
               20.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.8),
               false
            );
         }
      }

      if (world instanceof Level _levelxxxx) {
         if (!_levelxxxx.isClientSide()) {
            _levelxxxx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion")),
               SoundSource.MASTER,
               20.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9)
            );
         } else {
            _levelxxxx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion")),
               SoundSource.MASTER,
               20.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9),
               false
            );
         }
      }

      if (world.getFluidState(BlockPos.containing(x, y + 1.0, z)).createLegacyBlock().getBlock() instanceof LiquidBlock) {
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
               .getArrow(projectileLevel, 16.0F, 4, (byte)2);
            _entityToSpawn.setPos(x + 0.5, y + 1.5, z + 0.5);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.0F, 0.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (world instanceof Level _levelxxxxx && !_levelxxxxx.isClientSide()) {
            net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxxxx, null, x + 0.5, y + 1.0, z + 0.5, 10.0F, ExplosionInteraction.TNT);
         }
      } else {
         if (world instanceof ServerLevel _levelxxxxx) {
            _levelxxxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.LARGE_SMOKE.get(), x, y + 0.5, z, 5, 4.0, 4.0, 4.0, 1.5);
         }

         if (world instanceof ServerLevel _levelxxxxx) {
            _levelxxxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.FIREBALL.get(), x, y + 1.5, z, 5, 2.0, 1.5, 2.0, 1.0);
         }
      }

      if (world instanceof Level _levelxxxxx && !_levelxxxxx.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxxxx, null, x + 0.5, y + 0.5, z + 0.5, 22.0F, ExplosionInteraction.TNT);
      }

      if (world instanceof Level _levelxxxxx && !_levelxxxxx.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxxxx, null, x + 0.5 + 6.0, y + 0.5, z + 0.5, 8.0F, ExplosionInteraction.TNT);
      }

      if (world instanceof Level _levelxxxxx && !_levelxxxxx.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxxxx, null, x + 0.5 - 6.0, y + 0.5, z + 0.5, 8.0F, ExplosionInteraction.TNT);
      }

      if (world instanceof Level _levelxxxxx && !_levelxxxxx.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxxxx, null, x + 0.5, y + 0.5, z + 0.5 + 6.0, 8.0F, ExplosionInteraction.TNT);
      }

      if (world instanceof Level _levelxxxxx && !_levelxxxxx.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxxxx, null, x + 0.5, y + 0.5, z + 0.5 - 6.0, 8.0F, ExplosionInteraction.TNT);
      }

      if (world instanceof Level _levelxxxxx && !_levelxxxxx.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxxxx, null, x + 0.5, y + 0.5, z + 0.5, 25.0F, ExplosionInteraction.NONE);
      }

      if (world instanceof ServerLevel projectileLevel) {
         Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                  AbstractArrow entityToSpawn = new GiantShockExplosionBypassEntity((EntityType<? extends GiantShockExplosionBypassEntity>)CrustyChunksModEntities.GIANT_SHOCK_EXPLOSION_BYPASS.get(), level) {
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
            .getArrow(projectileLevel, 16.0F, 4, (byte)2);
         _entityToSpawn.setPos(x + 0.5, y + 1.0, z + 0.5);
         _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.0F, 0.0F);
         projectileLevel.addFreshEntity(_entityToSpawn);
      }

      if (world instanceof ServerLevel projectileLevel) {
         Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                  AbstractArrow entityToSpawn = new ShockClientsideBypassEntity((EntityType<? extends ShockClientsideBypassEntity>)CrustyChunksModEntities.SHOCK_CLIENTSIDE_BYPASS.get(), level) {
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
            .getArrow(projectileLevel, 16.0F, 4, (byte)2);
         _entityToSpawn.setPos(x + 0.5, y + 1.5, z + 0.5);
         _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.0F, 0.0F);
         projectileLevel.addFreshEntity(_entityToSpawn);
      }

      for (int index3 = 0; index3 < 190; index3++) {
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
               .getArrow(projectileLevel, 16.0F, 4, (byte)2);
            _entityToSpawn.setPos(x + 0.5, y + 0.9, z + 0.5);
            _entityToSpawn.shoot(
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
               0.1,
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
               (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.3),
               90.0F
            );
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }

      for (int index4 = 0; index4 < 25; index4++) {
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
            }).getArrow(projectileLevel, 16.0F, 4, (byte)2);
            _entityToSpawn.setPos(x + 0.5, y + 0.9, z + 0.5);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, (float)Mth.nextDouble(RandomSource.create(), 1.3, 1.5), 45.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("GiantExplosionProcedure.execute", _wtSafe);
      }
   }
}
