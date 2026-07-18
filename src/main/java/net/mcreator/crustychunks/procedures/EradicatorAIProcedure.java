package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.EradicatorEntity;
import net.mcreator.crustychunks.entity.FlameThrowerEmberEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class EradicatorAIProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         Entity target = null;
         double rev = 0.0;
         double LeadRange = 0.0;
         double targetrange = 0.0;
         RandomVoicelinesProcedure.execute(world, x, y, z, entity);
         CrustyChunksMod.queueServerWork(1, () -> AutoscoutingProcedure.execute(world, x, y, z, entity));
         if (entity.isAlive()) {
            if (entity.getPersistentData().getDouble("T") > 0.0) {
               entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") - 1.0);
            }

            if (entity.getPersistentData().getDouble("T2") > 0.0) {
               entity.getPersistentData().putDouble("T2", entity.getPersistentData().getDouble("T2") - 1.0);
            }

            if ((entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null) != null && (entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null).isAlive()) {
               if (Mth.nextInt(RandomSource.create(), 1, 5) == 1) {
                  entity.lookAt(
                     Anchor.EYES,
                     new Vec3(
                        (entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null).getX(),
                        (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getY() + 1.0,
                        (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getZ()
                     )
                  );
               }

               if (entity.getPersistentData().getDouble("T") <= 0.0) {
                  if ((entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getY() > y + 30.0) {
                     targetrange = Math.sqrt(
                        Math.pow(Math.abs(y - (entity instanceof Mob _mobEntxxxxx ? _mobEntxxxxx.getTarget() : null).getY()), 2.0)
                           + Math.pow(Math.abs(z - (entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null).getZ()), 2.0)
                           + Math.pow(Math.abs(x - (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getX()), 2.0)
                     );
                     LeadRange = targetrange + Math.pow(targetrange, 2.0) / 225.0;
                     entity.lookAt(
                        Anchor.EYES,
                        new Vec3(
                           (entity instanceof Mob _mobEntxxxxxxxxxxx ? _mobEntxxxxxxxxxxx.getTarget() : null).getX()
                              + (entity instanceof Mob _mobEntxxxxxxxxxx ? _mobEntxxxxxxxxxx.getTarget() : null).getDeltaMovement().x() * 1.25 * LeadRange * 0.09,
                           Math.max(
                              y + 25.0,
                              (entity instanceof Mob _mobEntxxxxxxxxx ? _mobEntxxxxxxxxx.getTarget() : null).getY()
                                 + 3.0
                                 + (entity instanceof Mob _mobEntxxxxxxxx ? _mobEntxxxxxxxx.getTarget() : null).getDeltaMovement().y() * 1.25 * LeadRange * 0.09
                           ),
                           (entity instanceof Mob _mobEntxxxxxxx ? _mobEntxxxxxxx.getTarget() : null).getZ()
                              + (entity instanceof Mob _mobEntxxxxxx ? _mobEntxxxxxx.getTarget() : null).getDeltaMovement().z() * 1.25 * LeadRange * 0.09
                        )
                     );
                     CrustyChunksMod.queueServerWork(1, () -> EradicatorFlakProcedure.execute(world, x, y, z, entity));
                  } else if (!(entity instanceof Mob _mobEntxxxxx ? _mobEntxxxxx.getTarget() : null)
                        .getType()
                        .is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bulletproof")))
                     && (
                        Math.abs((entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null).getY() - y)
                                    - (
                                       Math.abs(
                                             (double)entity.level()
                                                   .clip(
                                                      new ClipContext(
                                                         entity.getEyePosition(1.0F),
                                                         entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(512.0)),
                                                         Block.COLLIDER,
                                                         Fluid.ANY,
                                                         entity
                                                      )
                                                   )
                                                   .getBlockPos()
                                                   .getY()
                                                - y
                                          )
                                          + 0.5
                                    )
                                 <= 0.0
                              && Math.abs((entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getX() - x)
                                    - (
                                       Math.abs(
                                             (double)entity.level()
                                                   .clip(
                                                      new ClipContext(
                                                         entity.getEyePosition(1.0F),
                                                         entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(512.0)),
                                                         Block.COLLIDER,
                                                         Fluid.ANY,
                                                         entity
                                                      )
                                                   )
                                                   .getBlockPos()
                                                   .getX()
                                                - x
                                          )
                                          + 0.5
                                    )
                                 <= 0.0
                              && Math.abs((entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getZ() - z)
                                    - (
                                       Math.abs(
                                             (double)entity.level()
                                                   .clip(
                                                      new ClipContext(
                                                         entity.getEyePosition(1.0F),
                                                         entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(512.0)),
                                                         Block.COLLIDER,
                                                         Fluid.ANY,
                                                         entity
                                                      )
                                                   )
                                                   .getBlockPos()
                                                   .getZ()
                                                - z
                                          )
                                          + 0.5
                                    )
                                 <= 0.0
                           || world.getBlockState(
                                    new BlockPos(
                                       entity.level()
                                          .clip(
                                             new ClipContext(
                                                entity.getEyePosition(1.0F),
                                                entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(7.0)),
                                                Block.COLLIDER,
                                                Fluid.ANY,
                                                entity
                                             )
                                          )
                                          .getBlockPos()
                                          .getX(),
                                       entity.level()
                                          .clip(
                                             new ClipContext(
                                                entity.getEyePosition(1.0F),
                                                entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(7.0)),
                                                Block.COLLIDER,
                                                Fluid.ANY,
                                                entity
                                             )
                                          )
                                          .getBlockPos()
                                          .getY(),
                                       entity.level()
                                          .clip(
                                             new ClipContext(
                                                entity.getEyePosition(1.0F),
                                                entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(7.0)),
                                                Block.COLLIDER,
                                                Fluid.ANY,
                                                entity
                                             )
                                          )
                                          .getBlockPos()
                                          .getZ()
                                    )
                                 )
                                 .getBlock()
                              != Blocks.AIR
                     )) {
                     CrustyChunksMod.queueServerWork(
                        20,
                        () -> {
                           if ((entity instanceof Mob _mobEntxxxxxx ? _mobEntxxxxxx.getTarget() : null) != null) {
                              entity.lookAt(
                                 Anchor.EYES,
                                 new Vec3(
                                    (entity instanceof Mob _mobEntxxxxxxxxx ? _mobEntxxxxxxxxx.getTarget() : null).getX(),
                                    (entity instanceof Mob _mobEntxxxxxxxx ? _mobEntxxxxxxxx.getTarget() : null).getY() + 1.0,
                                    (entity instanceof Mob _mobEntxxxxxxx ? _mobEntxxxxxxx.getTarget() : null).getZ()
                                 )
                              );
                              if (entity.getPersistentData().getDouble("T") <= 0.0) {
                                 EradicatorMinigunProcedure.execute(world, x, y, z, entity);
                              }
                           }
                        }
                     );
                  } else {
                     CrustyChunksMod.queueServerWork(
                        20,
                        () -> {
                           if ((entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null) != null) {
                              entity.lookAt(
                                 Anchor.EYES,
                                 new Vec3(
                                    (entity instanceof Mob _mobEntxxxxx ? _mobEntxxxxx.getTarget() : null).getX(),
                                    (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getY() + 3.0,
                                    (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getZ()
                                 )
                              );
                              if (entity.getPersistentData().getDouble("T") <= 0.0) {
                                 EradicatorCannonProcedure.execute(world, x, y, z, entity);
                              }
                           }
                        }
                     );
                  }

                  if (1 == Mth.nextInt(RandomSource.create(), 1, 200)) {
                     CrustyChunksMod.queueServerWork(1, () -> DroneLaunchProcedure.execute(world, x, y, z, entity));
                  }
               }

               if ((entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null)
                     .getType()
                     .is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))
                  && entity instanceof Mob) {
                  try {
                     ((Mob)entity).setTarget(null);
                  } catch (Exception var24) {
                     var24.printStackTrace();
                  }
               }
            } else if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
                  < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F)
               && 1 == Mth.nextInt(RandomSource.create(), 1, 15)) {
               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sparks")),
                        SoundSource.NEUTRAL,
                        2.0F,
                        1.0F
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sparks")),
                        SoundSource.NEUTRAL,
                        2.0F,
                        1.0F,
                        false
                     );
                  }
               }

               if (entity instanceof LivingEntity _entity) {
                  _entity.setHealth((entity instanceof LivingEntity _livEntxx ? _livEntxx.getHealth() : -1.0F) + 1.0F);
               }
            }

            if (entity.getPersistentData().getDouble("Cycle") > 0.0) {
               entity.getPersistentData().putDouble("Cycle", entity.getPersistentData().getDouble("Cycle") - 1.0);
            } else {
               if (entity.getDeltaMovement().x() != 0.0 && entity.getDeltaMovement().z() != 0.0) {
                  if (entity instanceof EradicatorEntity && world instanceof Level _levelx) {
                     if (!_levelx.isClientSide()) {
                        _levelx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:megamechstep")),
                           SoundSource.HOSTILE,
                           8.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 0.5, 0.6)
                        );
                     } else {
                        _levelx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:megamechstep")),
                           SoundSource.HOSTILE,
                           8.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 0.5, 0.6),
                           false
                        );
                     }
                  }

                  if (world instanceof ServerLevel _levelxx) {
                     _levelxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.DUST.get(), x, y + 0.5, z, 14, 5.0, 0.0, 5.0, 0.1);
                  }

                  rev = 1.4;
                  if (Mth.nextInt(RandomSource.create(), 1, 3) == 1) {
                     CrustyChunksMod.queueServerWork(1, () -> EradicatorMeleeProcedure.execute(world, x, y, z));
                  }
               } else {
                  rev = 0.7;
               }

               if (world instanceof Level _levelxx) {
                  if (!_levelxx.isClientSide()) {
                     _levelxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:turbine")),
                        SoundSource.NEUTRAL,
                        (float)(3.0 + rev),
                        (float)(Mth.nextDouble(RandomSource.create(), 0.6, 0.7) + rev)
                     );
                  } else {
                     _levelxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:turbine")),
                        SoundSource.NEUTRAL,
                        (float)(3.0 + rev),
                        (float)(Mth.nextDouble(RandomSource.create(), 0.6, 0.7) + rev),
                        false
                     );
                  }
               }

               entity.getPersistentData().putDouble("Cycle", 7.0);
            }

            if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
                  < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F) / 2.0F
               && 10 == Mth.nextInt(RandomSource.create(), 1, 15)) {
               world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x, y + 4.0, z, 0.0, 1.0, 0.0);
            }

            if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
               < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F) / 4.0F) {
               if (world instanceof ServerLevel _levelxxx) {
                  _levelxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SPARKS.get(), x, y + 2.0, z, 2, 1.0, 1.0, 1.0, 1.0);
               }

               if (10 == Mth.nextInt(RandomSource.create(), 1, 80) && world instanceof Level _levelxxx) {
                  if (!_levelxxx.isClientSide()) {
                     _levelxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                        SoundSource.NEUTRAL,
                        4.0F,
                        0.7F
                     );
                  } else {
                     _levelxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                        SoundSource.NEUTRAL,
                        4.0F,
                        0.7F,
                        false
                     );
                  }
               }
            }

            if (entity.isInWater()) {
               if (world instanceof Level _levelxxxx) {
                  if (!_levelxxxx.isClientSide()) {
                     _levelxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                        SoundSource.NEUTRAL,
                        2.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.6, 0.8)
                     );
                  } else {
                     _levelxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                        SoundSource.NEUTRAL,
                        2.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.6, 0.8),
                        false
                     );
                  }
               }

               world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SPLASH_PUFF.get(), x, y, z, 1.0, -1.0, 1.0);
               world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SPLASH_PUFF.get(), x, y, z, -1.0, -1.0, -1.0);
               world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SPLASH_PUFF.get(), x, y, z, -1.0, -1.0, 1.0);
               world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SPLASH_PUFF.get(), x, y, z, 1.0, -1.0, -1.0);
               entity.push(entity.getLookAngle().x / 20.0, 0.0, entity.getLookAngle().z / 20.0);
            }
         } else {
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.AERIAL_SPARKS.get(),
               x,
               y + 3.0,
               z,
               Mth.nextDouble(RandomSource.create(), -0.7, 0.7),
               4.0,
               Mth.nextDouble(RandomSource.create(), -0.7, 0.7)
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.AERIAL_SPARKS.get(),
               x,
               y + 3.0,
               z,
               Mth.nextDouble(RandomSource.create(), -0.7, 0.7),
               4.0,
               Mth.nextDouble(RandomSource.create(), -0.7, 0.7)
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.RISING_FLAME.get(),
               x,
               y + 3.0,
               z,
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
               4.0,
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0)
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.ROCKET_FLAME.get(),
               x,
               y + 3.0,
               z,
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
               2.0,
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0)
            );
            if (world instanceof Level _levelxxxxx) {
               if (!_levelxxxxx.isClientSide()) {
                  _levelxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.NEUTRAL,
                     10.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.0, 2.0)
                  );
               } else {
                  _levelxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                     SoundSource.NEUTRAL,
                     10.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.0, 2.0),
                     false
                  );
               }
            }

            if (1 == Mth.nextInt(RandomSource.create(), 1, 6)) {
               if (world instanceof Level _levelxxxxxx) {
                  if (!_levelxxxxxx.isClientSide()) {
                     _levelxxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 1.5, 2.0)
                     );
                  } else {
                     _levelxxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 1.5, 2.0),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelxxxxxxx) {
                  if (!_levelxxxxxxx.isClientSide()) {
                     _levelxxxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallfarblast")),
                        SoundSource.NEUTRAL,
                        30.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 1.5, 2.0)
                     );
                  } else {
                     _levelxxxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallfarblast")),
                        SoundSource.NEUTRAL,
                        30.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 1.5, 2.0),
                        false
                     );
                  }
               }
            } else if (1 == Mth.nextInt(RandomSource.create(), 1, 4)) {
               if (world instanceof Level _levelxxxxxxxx) {
                  if (!_levelxxxxxxxx.isClientSide()) {
                     _levelxxxxxxxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.blast")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                     );
                  } else {
                     _levelxxxxxxxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.firework_rocket.blast")),
                        SoundSource.NEUTRAL,
                        10.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                        false
                     );
                  }
               }

               if (world instanceof ServerLevel projectileLevel) {
                  Projectile _entityToSpawn = (new Object() {
                        public Projectile getArrow(Level level, float damage, int knockback) {
                           AbstractArrow entityToSpawn = new FlameThrowerEmberEntity((EntityType<? extends FlameThrowerEmberEntity>)CrustyChunksModEntities.FLAME_THROWER_EMBER.get(), level) {
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
                     .getArrow(projectileLevel, 0.0F, 1);
                  _entityToSpawn.setPos(x, y + 3.0, z);
                  _entityToSpawn.shoot(0.0, 1.0, 0.0, (float)Mth.nextDouble(RandomSource.create(), 0.4, 1.0), 45.0F);
                  projectileLevel.addFreshEntity(_entityToSpawn);
               }
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("EradicatorAIProcedure.execute", _wtSafe);
      }
   }
}
