package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.ClusterRocketEntity;
import net.mcreator.crustychunks.entity.DecimatorEntity;
import net.mcreator.crustychunks.entity.IRMissileEntity;
import net.mcreator.crustychunks.entity.IncindiaryRocketProjectileEntity;
import net.mcreator.crustychunks.entity.LargeRocketEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DecimatorAISystemProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         boolean Trigger = false;
         boolean hottarget = false;
         Entity target = null;
         double rev = 0.0;
         double LeadRange = 0.0;
         double leady = 0.0;
         double leadx = 0.0;
         double leadz = 0.0;
         double targetrange = 0.0;
         RandomVoicelinesProcedure.execute(world, x, y, z, entity);
         AutoscoutingProcedure.execute(world, x, y, z, entity);
         if (entity.getPersistentData().getDouble("T") > 0.0) {
            entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") - 1.0);
         }

         if (entity.getPersistentData().getDouble("T2") > 0.0) {
            entity.getPersistentData().putDouble("T2", entity.getPersistentData().getDouble("T2") - 1.0);
         }

         if (1 == Mth.nextInt(RandomSource.create(), 1, 20)) {
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(128.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               if (entityiterator.getY() > y + 25.0
                  && (
                     (new Object() {
                                 public boolean checkGamemode(Entity _ent) {
                                    if (_ent instanceof ServerPlayer _serverPlayer) {
                                       return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
                                    } else {
                                       return _ent.level().isClientSide() && _ent instanceof Player _player
                                          ? Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
                                             && Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.SURVIVAL
                                          : false;
                                    }
                                 }
                              })
                              .checkGamemode(entityiterator)
                           && entityiterator instanceof Player
                        || entityiterator instanceof LargeRocketEntity
                        || entityiterator instanceof IncindiaryRocketProjectileEntity
                        || entityiterator instanceof ClusterRocketEntity
                        || entityiterator instanceof IRMissileEntity
                  )
                  && entity instanceof Mob) {
                  Mob _entity = (Mob)entity;
                  if (entityiterator instanceof LivingEntity _ent) {
                     _entity.setTarget(_ent);
                  }
               }
            }
         }

         if ((entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null) != null) {
            target = entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null;
            hottarget = false;
            Vec3 _center = new Vec3(target.getX(), target.getY(), target.getZ());

            for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(25.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               if (entityiteratorx.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:warm")))
                  && !entityiteratorx.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))
                  && entityiteratorx.getY() > y + 25.0) {
                  leadx = entityiteratorx.getPersistentData().getDouble("Mx");
                  leady = entityiteratorx.getPersistentData().getDouble("My");
                  leadz = entityiteratorx.getPersistentData().getDouble("Mz");
                  hottarget = true;
               } else {
                  leadx = (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getDeltaMovement().x();
                  leady = (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getDeltaMovement().y();
                  leadz = (entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null).getDeltaMovement().z();
               }
            }
         }

         if ((entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null) != null) {
            if (target.isAlive()) {
               if (hottarget && target.getY() > y + 25.0) {
                  targetrange = Math.sqrt(
                     Math.pow(Math.abs(y - target.getY()), 2.0)
                        + Math.pow(Math.abs(z - target.getZ()), 2.0)
                        + Math.pow(Math.abs(x - target.getX()), 2.0)
                  );
                  entity.getPersistentData().putDouble("TargetX", target.getX() + leadx * 1.5 * targetrange * 0.09);
                  entity.getPersistentData()
                     .putDouble("TargetY", Math.max(y + 30.0, target.getY() + 3.0 + leady * 1.5 * targetrange * 0.09 + Math.pow(targetrange / 50.0, 2.0)));
                  entity.getPersistentData().putDouble("TargetZ", target.getZ() + leadz * 1.5 * targetrange * 0.09);
                  entity.getPersistentData().putDouble("Range", targetrange);
                  if (50.0 < targetrange) {
                     CrustyChunksMod.queueServerWork(
                        1,
                        () -> {
                           if (entity.getPersistentData().getDouble("T") < 1.0) {
                              entity.lookAt(
                                 Anchor.EYES,
                                 new Vec3(
                                    entity.getPersistentData().getDouble("TargetX"),
                                    entity.getPersistentData().getDouble("TargetY"),
                                    entity.getPersistentData().getDouble("TargetZ")
                                 )
                              );
                              DecimatorFlakProcedure.execute(world, x, y, z, entity);
                           }
                        }
                     );
                  }
               } else {
                  if (Mth.nextInt(RandomSource.create(), 1, 5) == 1) {
                     entity.lookAt(
                        Anchor.EYES,
                        new Vec3(
                           (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getX(),
                           (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getY() + 1.0,
                           (entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null).getZ()
                        )
                     );
                  }

                  if (entity.getPersistentData().getDouble("T") <= 0.0) {
                     if (!(entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null)
                           .getType()
                           .is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bulletproof")))
                        && Math.abs((entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getY() - y)
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
                        && Math.abs((entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getX() - x)
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
                        && Math.abs((entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null).getZ() - z)
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
                           <= 0.0) {
                        CrustyChunksMod.queueServerWork(
                           20,
                           () -> {
                              if ((entity instanceof Mob _mobEntxxxxx ? _mobEntxxxxx.getTarget() : null) != null) {
                                 entity.lookAt(
                                    Anchor.EYES,
                                    new Vec3(
                                       (entity instanceof Mob _mobEntxxxxxxxx ? _mobEntxxxxxxxx.getTarget() : null).getX(),
                                       (entity instanceof Mob _mobEntxxxxxxx ? _mobEntxxxxxxx.getTarget() : null).getY() + 1.0,
                                       (entity instanceof Mob _mobEntxxxxxx ? _mobEntxxxxxx.getTarget() : null).getZ()
                                    )
                                 );
                                 if (entity.getPersistentData().getDouble("T") <= 0.0) {
                                    DecimatorCoaxSprayProcedure.execute(world, x, y, z, entity);
                                 }
                              }
                           }
                        );
                     } else if (entity.getPersistentData().getBoolean("Cannon")) {
                        CrustyChunksMod.queueServerWork(
                           20,
                           () -> {
                              if ((entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null) != null) {
                                 entity.lookAt(
                                    Anchor.EYES,
                                    new Vec3(
                                       (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getX(),
                                       (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getY() + 1.0,
                                       (entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null).getZ()
                                    )
                                 );
                                 if (entity.getPersistentData().getDouble("T") <= 0.0) {
                                    DecimatorCannonFireProcedure.execute(world, x, y, z, entity);
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
                                       (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getX(),
                                       (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getY() + 1.0,
                                       (entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null).getZ()
                                    )
                                 );
                                 if (entity.getPersistentData().getDouble("T") <= 0.0) {
                                    DecimatorPeelerProcedure.execute(world, x, y, z, entity);
                                 }
                              }
                           }
                        );
                     }
                  }

                  if (Mth.nextInt(RandomSource.create(), 1, 100) == 1) {
                     DecimatorMeleeProcedure.execute(world, x, y, z, entity);
                  }

                  if (Mth.nextInt(RandomSource.create(), 1, 200) == 1 && entity.getPersistentData().getDouble("I") <= 6.0) {
                     if (world instanceof ServerLevel _level) {
                        Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.STRIKER.get())
                           .spawn(_level, BlockPos.containing(x, y + 1.0, z), MobSpawnType.MOB_SUMMONED);
                        if (entityToSpawn != null) {
                           entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
                        }
                     }

                     entity.getPersistentData().putDouble("I", entity.getPersistentData().getDouble("I") + 1.0);
                     if (entity instanceof DecimatorEntity) {
                        ((DecimatorEntity)entity).setAnimation("SmokeDeploy");
                     }

                     entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") + 80.0);
                  }

                  if (Mth.nextInt(RandomSource.create(), 1, 200) == 1) {
                     if (entity.getPersistentData().getBoolean("Cannon")) {
                        entity.getPersistentData().putBoolean("Cannon", false);
                     } else {
                        entity.getPersistentData().putBoolean("Cannon", true);
                     }
                  }

                  if (4.0 <= entity.getPersistentData().getDouble("Rocket")) {
                     entity.getPersistentData().putBoolean("Cannon", true);
                  }

                  if ((entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null)
                        .getType()
                        .is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))
                     && entity instanceof Mob) {
                     try {
                        ((Mob)entity).setTarget(null);
                     } catch (Exception var30) {
                        var30.printStackTrace();
                     }
                  }
               }
            }
         } else if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
               < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F)
            && 1 == Mth.nextInt(RandomSource.create(), 1, 15)) {
            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sparks")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     1.0F
                  );
               } else {
                  _levelx.playLocalSound(
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
               if (world instanceof Level _levelxx) {
                  if (!_levelxx.isClientSide()) {
                     _levelxx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:mechstep")),
                        SoundSource.NEUTRAL,
                        2.5F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.1)
                     );
                  } else {
                     _levelxx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:mechstep")),
                        SoundSource.NEUTRAL,
                        2.5F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.1),
                        false
                     );
                  }
               }

               if (world instanceof ServerLevel _levelxxx) {
                  _levelxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.DUST.get(), x, y + 0.5, z, 7, 3.0, 0.0, 3.0, 0.1);
               }

               rev = 2.0;
            } else {
               rev = 0.0;
            }

            if (world instanceof Level _levelxxx) {
               if (!_levelxxx.isClientSide()) {
                  _levelxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:engine")),
                     SoundSource.NEUTRAL,
                     (float)(2.0 + rev),
                     (float)(Mth.nextDouble(RandomSource.create(), 0.5, 0.6) + rev)
                  );
               } else {
                  _levelxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:engine")),
                     SoundSource.NEUTRAL,
                     (float)(2.0 + rev),
                     (float)(Mth.nextDouble(RandomSource.create(), 0.5, 0.6) + rev),
                     false
                  );
               }
            }

            entity.getPersistentData().putDouble("Cycle", 10.0);
         }

         if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
               < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F) / 2.0F
            && 10 == Mth.nextInt(RandomSource.create(), 1, 15)) {
            world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x, y + 4.0, z, 0.0, 1.0, 0.0);
         }

         if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
            < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F) / 4.0F) {
            if (world instanceof ServerLevel _levelxxxx) {
               _levelxxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SPARKS.get(), x, y + 2.0, z, 2, 1.0, 1.0, 1.0, 1.0);
            }

            if (10 == Mth.nextInt(RandomSource.create(), 1, 80) && world instanceof Level _levelxxxx) {
               if (!_levelxxxx.isClientSide()) {
                  _levelxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                     SoundSource.NEUTRAL,
                     4.0F,
                     0.7F
                  );
               } else {
                  _levelxxxx.playLocalSound(
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

         if (Mth.nextInt(RandomSource.create(), 1, 200) == 1 && 0.0 < entity.getPersistentData().getDouble("Rocket")) {
            entity.getPersistentData().putDouble("Rocket", entity.getPersistentData().getDouble("Rocket") - 1.0);
            if (world instanceof Level _levelxxxxx) {
               if (!_levelxxxxx.isClientSide()) {
                  _levelxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.close")),
                     SoundSource.NEUTRAL,
                     2.5F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.2, 0.4)
                  );
               } else {
                  _levelxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.iron_trapdoor.close")),
                     SoundSource.NEUTRAL,
                     2.5F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.2, 0.4),
                     false
                  );
               }
            }
         }

         if (entity.isInWater()) {
            if (world instanceof Level _levelxxxxxx) {
               if (!_levelxxxxxx.isClientSide()) {
                  _levelxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.6, 0.8)
                  );
               } else {
                  _levelxxxxxx.playLocalSound(
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

            world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SPLASH_PUFF.get(), x, y, z, 0.0, -1.0, 0.0);
            entity.push(entity.getLookAngle().x / 20.0, 0.0, entity.getLookAngle().z / 20.0);
         } else if (!entity.isNoGravity()
            && !world.canSeeSkyFromBelowWater(BlockPos.containing(x, y - 200.0, z))
            && world.canSeeSkyFromBelowWater(BlockPos.containing(x, y - 10.0, z))
            && !entity.onGround()) {
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               entity.getDeltaMovement().x() * 2.0 + 1.0,
               entity.getDeltaMovement().y() * 3.0,
               entity.getDeltaMovement().z() * 2.0 + 1.0
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               entity.getDeltaMovement().x() * 2.0 - 1.0,
               entity.getDeltaMovement().y() * 3.0,
               entity.getDeltaMovement().z() * 2.0 + 1.0
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               entity.getDeltaMovement().x() * 2.0 + 1.0,
               entity.getDeltaMovement().y() * 3.0,
               entity.getDeltaMovement().z() * 2.0 - 1.0
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               entity.getDeltaMovement().x() * 2.0 - 1.0,
               entity.getDeltaMovement().y() * 3.0,
               entity.getDeltaMovement().z() * 2.0 - 1.0
            );
            if (world instanceof Level _levelxxxxxxx) {
               if (!_levelxxxxxxx.isClientSide()) {
                  _levelxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                     SoundSource.NEUTRAL,
                     7.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4)
                  );
               } else {
                  _levelxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                     SoundSource.NEUTRAL,
                     7.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4),
                     false
                  );
               }
            }

            if (world instanceof Level _levelxxxxxxxx) {
               if (!_levelxxxxxxxx.isClientSide()) {
                  _levelxxxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:jetfar")),
                     SoundSource.NEUTRAL,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4)
                  );
               } else {
                  _levelxxxxxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:jetfar")),
                     SoundSource.NEUTRAL,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4),
                     false
                  );
               }
            }

            if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0));
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("DecimatorAISystemProcedure.execute", _wtSafe);
      }
   }
}
