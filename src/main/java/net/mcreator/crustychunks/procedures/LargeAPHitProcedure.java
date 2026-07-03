package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.HVParticleProjectileEntity;
import net.mcreator.crustychunks.entity.HugeFragmentEntity;
import net.mcreator.crustychunks.entity.LargeAPFireEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LargeAPHitProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         double Power = 0.0;
         immediatesourceentity.getPersistentData()
            .putDouble("Power", immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0);
         CrustyChunksMod.queueServerWork(
            1,
            () -> {
               DamagesProcedure.execute(world, x, y, z);
               if (world instanceof ServerLevel _level) {
                  _level.sendParticles(ParticleTypes.FLASH, x + 0.5, y + 0.5, z + 0.5, 5, 0.5, 0.5, 0.5, 0.05);
               }

               if (world instanceof ServerLevel _level) {
                  _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SPARKS.get(), x + 0.5, y + 0.5, z + 0.5, 40, 0.6, 0.6, 0.6, 1.2);
               }

               HugeFragmentHitProcedure.execute(world, x, y, z, immediatesourceentity);
               if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:pennable")))
                  || world.getBlockState(BlockPos.containing(x, y, z)).getDestroySpeed(world, BlockPos.containing(x, y, z)) <= 50.0F
                     && world.getBlockState(BlockPos.containing(x, y, z)).getDestroySpeed(world, BlockPos.containing(x, y, z)) >= 0.0F) {
                  if ((
                        world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:metal")))
                           || world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:breakable_metal")))
                     )
                     && world instanceof Level _level) {
                     if (!_level.isClientSide()) {
                        _level.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                           SoundSource.NEUTRAL,
                           10.0F,
                           0.8F
                        );
                     } else {
                        _level.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                           SoundSource.NEUTRAL,
                           10.0F,
                           0.8F,
                           false
                        );
                     }
                  }

                  if (10.0 > immediatesourceentity.getPersistentData().getDouble("Hits")) {
                     if (world instanceof ServerLevel projectileLevel) {
                        Projectile _entityToSpawn = (new Object() {
                              public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                                 AbstractArrow entityToSpawn = new LargeAPFireEntity((EntityType<? extends LargeAPFireEntity>)CrustyChunksModEntities.LARGE_AP_FIRE.get(), level) {
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
                                 entityToSpawn.setOwner(shooter);
                                 entityToSpawn.setBaseDamage((double)damage);
                                 entityToSpawn.setSilent(true);
                                 entityToSpawn.setCritArrow(true);
                                 return entityToSpawn;
                              }
                           })
                           .getArrow(projectileLevel, immediatesourceentity, 25.0F, 1, (byte)50);
                        _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
                        _entityToSpawn.shoot(
                           immediatesourceentity.getDeltaMovement().x(),
                           immediatesourceentity.getDeltaMovement().y(),
                           immediatesourceentity.getDeltaMovement().z(),
                           5.0F,
                           4.0F
                        );
                        projectileLevel.addFreshEntity(_entityToSpawn);
                     }

                     Vec3 _center = new Vec3(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());

                     for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(0.5), e -> true)
                        .stream()
                        .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
                        .toList()) {
                        if (entityiterator instanceof LargeAPFireEntity) {
                           entityiterator.getPersistentData()
                              .putDouble(
                                 "Hits",
                                 immediatesourceentity.getPersistentData().getDouble("Hits")
                                    + (double)Math.round(1.0F + world.getBlockState(BlockPos.containing(x, y, z)).getDestroySpeed(world, BlockPos.containing(x, y, z)) / 4.0F)
                              );
                        }
                     }

                     if (world instanceof ServerLevel _levelx) {
                        _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SPARKS.get(), x + 0.5, y + 0.5, z + 0.5, 5, 0.0, 0.0, 0.0, 1.0);
                     }

                     for (int index0 = 0; index0 < (int)world.getBlockState(BlockPos.containing(x, y, z)).getDestroySpeed(world, BlockPos.containing(x, y, z)); index0++) {
                        if (world instanceof ServerLevel projectileLevel) {
                           Projectile _entityToSpawn = (new Object() {
                                 public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                                    AbstractArrow entityToSpawn = new HVParticleProjectileEntity((EntityType<? extends HVParticleProjectileEntity>)CrustyChunksModEntities.HV_PARTICLE_PROJECTILE.get(), level) {
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
                                    entityToSpawn.setOwner(shooter);
                                    entityToSpawn.setBaseDamage((double)damage);
                                    entityToSpawn.setSilent(true);
                                    entityToSpawn.setCritArrow(true);
                                    return entityToSpawn;
                                 }
                              })
                              .getArrow(projectileLevel, immediatesourceentity, 0.5F, 1);
                           _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
                           _entityToSpawn.shoot(
                              immediatesourceentity.getDeltaMovement().x(),
                              immediatesourceentity.getDeltaMovement().y(),
                              immediatesourceentity.getDeltaMovement().z(),
                              2.5F,
                              12.0F
                           );
                           projectileLevel.addFreshEntity(_entityToSpawn);
                        }
                     }

                     for (int index1 = 0; index1 < (int)world.getBlockState(BlockPos.containing(x, y, z)).getDestroySpeed(world, BlockPos.containing(x, y, z)); index1++) {
                        if (world instanceof ServerLevel projectileLevel) {
                           Projectile _entityToSpawn = (new Object() {
                                 public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                                    AbstractArrow entityToSpawn = new HugeFragmentEntity((EntityType<? extends HugeFragmentEntity>)CrustyChunksModEntities.HUGE_FRAGMENT.get(), level) {
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
                                    entityToSpawn.setOwner(shooter);
                                    entityToSpawn.setBaseDamage((double)damage);
                                    entityToSpawn.setSilent(true);
                                    entityToSpawn.setCritArrow(true);
                                    return entityToSpawn;
                                 }
                              })
                              .getArrow(projectileLevel, immediatesourceentity, 3.0F, 1);
                           _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
                           _entityToSpawn.shoot(
                              immediatesourceentity.getDeltaMovement().x(),
                              immediatesourceentity.getDeltaMovement().y(),
                              immediatesourceentity.getDeltaMovement().z(),
                              3.0F,
                              8.0F
                           );
                           projectileLevel.addFreshEntity(_entityToSpawn);
                        }
                     }
                  } else {
                     if (world instanceof Level _levelx) {
                        if (!_levelx.isClientSide()) {
                           _levelx.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
                              SoundSource.NEUTRAL,
                              5.0F,
                              1.3F
                           );
                        } else {
                           _levelx.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
                              SoundSource.NEUTRAL,
                              5.0F,
                              1.3F,
                              false
                           );
                        }
                     }

                     if (world instanceof Level _levelxx) {
                        if (!_levelxx.isClientSide()) {
                           _levelxx.playSound(
                              null,
                              BlockPos.containing(x, y, z),
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:medium_small_explosion_distant")),
                              SoundSource.NEUTRAL,
                              40.0F,
                              1.3F
                           );
                        } else {
                           _levelxx.playLocalSound(
                              x,
                              y,
                              z,
                              (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:medium_small_explosion_distant")),
                              SoundSource.NEUTRAL,
                              40.0F,
                              1.3F,
                              false
                           );
                        }
                     }

                     for (int index2 = 0; index2 < 5; index2++) {
                        if (world instanceof ServerLevel projectileLevel) {
                           Projectile _entityToSpawn = (new Object() {
                                 public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                                    AbstractArrow entityToSpawn = new HVParticleProjectileEntity((EntityType<? extends HVParticleProjectileEntity>)CrustyChunksModEntities.HV_PARTICLE_PROJECTILE.get(), level) {
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
                                    entityToSpawn.setOwner(shooter);
                                    entityToSpawn.setBaseDamage((double)damage);
                                    entityToSpawn.setSilent(true);
                                    entityToSpawn.setCritArrow(true);
                                    return entityToSpawn;
                                 }
                              })
                              .getArrow(projectileLevel, immediatesourceentity, 0.5F, 1);
                           _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
                           _entityToSpawn.shoot(
                              immediatesourceentity.getDeltaMovement().x(),
                              immediatesourceentity.getDeltaMovement().y(),
                              immediatesourceentity.getDeltaMovement().z(),
                              2.5F,
                              12.0F
                           );
                           projectileLevel.addFreshEntity(_entityToSpawn);
                        }
                     }

                     for (int index3 = 0; index3 < 5; index3++) {
                        if (world instanceof ServerLevel projectileLevel) {
                           Projectile _entityToSpawn = (new Object() {
                                 public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                                    AbstractArrow entityToSpawn = new HugeFragmentEntity((EntityType<? extends HugeFragmentEntity>)CrustyChunksModEntities.HUGE_FRAGMENT.get(), level) {
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
                                    entityToSpawn.setOwner(shooter);
                                    entityToSpawn.setBaseDamage((double)damage);
                                    entityToSpawn.setSilent(true);
                                    entityToSpawn.setCritArrow(true);
                                    return entityToSpawn;
                                 }
                              })
                              .getArrow(projectileLevel, immediatesourceentity, 3.0F, 1);
                           _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
                           _entityToSpawn.shoot(
                              immediatesourceentity.getDeltaMovement().x(),
                              immediatesourceentity.getDeltaMovement().y(),
                              immediatesourceentity.getDeltaMovement().z(),
                              3.0F,
                              8.0F
                           );
                           projectileLevel.addFreshEntity(_entityToSpawn);
                        }
                     }
                  }

                  world.destroyBlock(BlockPos.containing(x, y, z), false);
               }

               if (world instanceof ServerLevel _levelxxx) {
                  _levelxxx.sendParticles(ParticleTypes.FLASH, x + 0.5, y + 0.5, z + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
               }

               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            }
         );
      }
   }
}
