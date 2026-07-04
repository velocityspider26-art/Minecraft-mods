package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RaidScoutAIProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         boolean schedoodlemode = false;
         double buddydistance = 0.0;
         double distancetotarget = 0.0;
         Entity target = null;
         if ((entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null) != null && (entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null).isAlive()) {
            target = entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null;
         }

         if (1 == Mth.nextInt(RandomSource.create(), 1, 60) && (target != null && !target.isAlive() || target == null)) {
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(128.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               if ((
                     entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robotarget")))
                        || entityiterator instanceof Player
                           && (new Object() {
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
                  )
                  && (target != null && !target.isAlive() || target == null)) {
                  target = entityiterator;
               }
            }
         }

         if (entity.getPersistentData().getDouble("T") > 0.0) {
            entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") - 1.0);
         }

         if (target != null) {
            distancetotarget = Math.sqrt(Math.pow(Math.abs(target.getX() - x), 2.0) + Math.pow(Math.abs(target.getZ() - z), 2.0));
            entity.lookAt(Anchor.EYES, new Vec3(target.getX(), target.getY() + 1.0, target.getZ()));
            entity.setDeltaMovement(
               new Vec3(
                  entity.getDeltaMovement().x() + entity.getLookAngle().x / 18.0,
                  entity.getDeltaMovement().y(),
                  entity.getDeltaMovement().z() + entity.getLookAngle().z / 18.0
               )
            );
            if (Math.abs(target.getY() - y)
                     - (
                        Math.abs(
                              (double)entity.level()
                                    .clip(
                                       new ClipContext(
                                          entity.getEyePosition(1.0F),
                                          entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(200.0)),
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
               && Math.abs(target.getX() - x)
                     - (
                        Math.abs(
                              (double)entity.level()
                                    .clip(
                                       new ClipContext(
                                          entity.getEyePosition(1.0F),
                                          entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(10.0)),
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
               && Math.abs(target.getZ() - z)
                     - (
                        Math.abs(
                              (double)entity.level()
                                    .clip(
                                       new ClipContext(
                                          entity.getEyePosition(1.0F),
                                          entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(10.0)),
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
               && entity.getPersistentData().getDouble("T") <= 0.0) {
               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:beep")),
                        SoundSource.HOSTILE,
                        5.0F,
                        2.0F
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:beep")),
                        SoundSource.HOSTILE,
                        5.0F,
                        2.0F,
                        false
                     );
                  }
               }

               entity.getPersistentData().putDouble("T", 160.0);
               CrustyChunksMod.queueServerWork(
                  80,
                  () -> {
                     Vec3 _center = new Vec3(x, y, z);

                     for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(256.0), e -> true)
                        .stream()
                        .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
                        .toList()) {
                        if (entity.isAlive()
                           && (entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null) != null
                           && (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).isAlive()) {
                           Entity patt6247$temp = entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null;
                           if (patt6247$temp instanceof LivingEntity) {
                              LivingEntity _entityx = (LivingEntity)patt6247$temp;
                              if (!_entityx.level().isClientSide()) {
                                 _entityx.addEffect(new MobEffectInstance(CrustyChunksModMobEffects.IMPENDING_DOOM, 16000, 0, false, false));
                              }
                           }

                           if (!entity.level().isClientSide()) {
                              entity.discard();
                           }

                           if (world instanceof Level) {
                              Level _levelx = (Level)world;
                              if (!_levelx.isClientSide()) {
                                 _levelx.playSound(
                                    null,
                                    BlockPos.containing(x, y, z),
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                                    SoundSource.NEUTRAL,
                                    3.0F,
                                    1.0F
                                 );
                              } else {
                                 _levelx.playLocalSound(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                                    SoundSource.NEUTRAL,
                                    3.0F,
                                    1.0F,
                                    false
                                 );
                              }
                           }

                           world.levelEvent(
                              2001,
                              BlockPos.containing(x, y + 1.0, z),
                              net.minecraft.world.level.block.Block.getId(
                                 ((net.minecraft.world.level.block.Block)CrustyChunksModBlocks.GREEN_ARMOR.get()).defaultBlockState()
                              )
                           );
                           if (world instanceof ServerLevel _levelx) {
                              _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x, y + 1.0, z, 5, 0.0, 0.0, 0.0, 0.5);
                           }

                           if (world instanceof ServerLevel _levelx) {
                              _levelx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SPARKS.get(), x, y + 1.0, z, 25, 0.0, 0.0, 0.0, 0.5);
                           }
                        }
                     }
                  }
               );
            }
         }

         if (entity.getPersistentData().getDouble("Cycle") > 0.0) {
            entity.getPersistentData().putDouble("Cycle", entity.getPersistentData().getDouble("Cycle") - 1.0);
         } else {
            entity.getPersistentData().putDouble("Cycle", 15.0);
            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:drone")),
                     SoundSource.HOSTILE,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:drone")),
                     SoundSource.HOSTILE,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                     false
                  );
               }
            }
         }

         if (entity.getY() < (double)(world.getHeight(Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) + 15)) {
            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x(), entity.getDeltaMovement().y() + 0.01, entity.getDeltaMovement().z()));
         } else if (entity.getY() > (double)(world.getHeight(Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) + 20)) {
            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x(), entity.getDeltaMovement().y() - 0.005, entity.getDeltaMovement().z()));
         }

         entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x() * 0.995, entity.getDeltaMovement().y() * 0.995, entity.getDeltaMovement().z() * 0.995));
         if (target != null && entity instanceof Mob _entity && target instanceof LivingEntity _ent) {
            _entity.setTarget(_ent);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RaidScoutAIProcedure.execute", _wtSafe);
      }
   }
}
