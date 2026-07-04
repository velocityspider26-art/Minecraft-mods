package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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

public class ReaperAIProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         Entity target = null;
         boolean schedoodlemode = false;
         boolean Trigger = false;
         boolean detonate = false;
         double distancetotarget = 0.0;
         double buddydistance = 0.0;
         double distancefromhome = 0.0;
         double leadvariable = 0.0;
         double mx = 0.0;
         double my = 0.0;
         double mz = 0.0;
         double speed = 0.0;
         double Limiter = 0.0;
         double rev = 0.0;
         if ((entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null) != null && (entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null).isAlive()) {
            target = entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null;
         }

         if (0.0 == entity.getPersistentData().getDouble("Direction")) {
            entity.getPersistentData().putDouble("Direction", 1.0);
         }

         if (1 == Mth.nextInt(RandomSource.create(), 1, 400)) {
            entity.getPersistentData().putDouble("Direction", entity.getPersistentData().getDouble("Direction") * -1.0);
         }

         if (0.0 == entity.getPersistentData().getDouble("X")) {
            entity.getPersistentData().putDouble("X", x);
         }

         if (0.0 == entity.getPersistentData().getDouble("Z")) {
            entity.getPersistentData().putDouble("Z", z);
         }

         distancefromhome = Math.sqrt(
            Math.pow(Math.abs(entity.getPersistentData().getDouble("X") - x), 2.0) + Math.pow(Math.abs(entity.getPersistentData().getDouble("Z") - z), 2.0)
         );
         if (entity.getPersistentData().getDouble("T") > 0.0) {
            entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") - 1.0);
         }

         if (1 == Mth.nextInt(RandomSource.create(), 1, 160) && (target == null || !target.isAlive())) {
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(200.0), e -> true)
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
                  && world.canSeeSkyFromBelowWater(BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()))
                  && (target != null && target.isAlive() || target == null)) {
                  target = entityiterator;
               }
            }
         }

         if (target != null
            && (entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null) != null
            && Math.sqrt(Math.pow(target.getY() - y, 2.0) + Math.pow(target.getX() - x, 2.0) + Math.pow(target.getZ() - z, 2.0))
                  - Math.sqrt(
                     Math.pow(target.getY() - (y + entity.getDeltaMovement().y()), 2.0)
                        + Math.pow(target.getX() - (x + (entity.getLookAngle().x + entity.getDeltaMovement().x()) / 2.0), 2.0)
                        + Math.pow(target.getZ() - (z + (entity.getLookAngle().z + entity.getDeltaMovement().z()) / 2.0), 2.0)
                  )
               > 1.15) {
            distancetotarget = Math.sqrt(Math.pow(Math.abs(target.getX() - x), 2.0) + Math.pow(Math.abs(target.getZ() - z), 2.0));
            entity.getPersistentData().putDouble("TargetRange", distancetotarget);
            if ((entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null) != null) {
               entity.lookAt(
                  Anchor.EYES,
                  new Vec3(
                     (entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null).getX(),
                     (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getY() + 1.0,
                     (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getZ()
                  )
               );
            }

            if ((
                  (entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null)
                        .getType()
                        .is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bulletproof")))
                     || !(
                        Math.abs((entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getY() - y)
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
                     )
                     || !(
                        Math.abs((entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getX() - x)
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
                     )
                     || !(
                        Math.abs((entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null).getZ() - z)
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
                     )
               )
               && entity.getPersistentData().getDouble("Rocket") < 28.0) {
               CrustyChunksMod.queueServerWork(
                  1,
                  () -> {
                     if ((entity instanceof Mob _mobEntxxxxx ? _mobEntxxxxx.getTarget() : null) != null) {
                        entity.lookAt(
                           Anchor.EYES,
                           new Vec3(
                              (entity instanceof Mob _mobEntxxxxxxxx ? _mobEntxxxxxxxx.getTarget() : null).getX(),
                              (entity instanceof Mob _mobEntxxxxxxx ? _mobEntxxxxxxx.getTarget() : null).getY()
                                 + Math.pow(entity.getPersistentData().getDouble("TargetRange") / 35.0, 2.0),
                              (entity instanceof Mob _mobEntxxxxxx ? _mobEntxxxxxx.getTarget() : null).getZ()
                           )
                        );
                        if (entity.getPersistentData().getDouble("T") <= 0.0) {
                           ReaperPeelerProcedure.execute(world, x, y, z, entity);
                        }
                     }
                  }
               );
            } else {
               CrustyChunksMod.queueServerWork(
                  1,
                  () -> {
                     if ((entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null) != null) {
                        entity.lookAt(
                           Anchor.EYES,
                           new Vec3(
                              (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getX(),
                              2.0
                                 + (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getY()
                                 + Math.pow(entity.getPersistentData().getDouble("TargetRange") / 75.0, 2.0),
                              (entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null).getZ()
                           )
                        );
                        if (entity.getPersistentData().getDouble("T") <= 0.0) {
                           ReaperCannonProcedure.execute(world, x, y, z, entity);
                        }
                     }
                  }
               );
            }
         } else {
            CrustyChunksMod.queueServerWork(1, () -> {
               entity.setYRot((float)((double)entity.getYRot() - entity.getPersistentData().getDouble("Direction")));
               entity.setXRot(entity.getXRot());
               entity.setYBodyRot(entity.getYRot());
               entity.setYHeadRot(entity.getYRot());
               entity.yRotO = entity.getYRot();
               entity.xRotO = entity.getXRot();
               if (entity instanceof LivingEntity _entityx) {
                  _entityx.yBodyRotO = _entityx.getYRot();
                  _entityx.yHeadRotO = _entityx.getYRot();
               }
            });
         }

         if (entity.getPersistentData().getDouble("Cycle") > 0.0) {
            entity.getPersistentData().putDouble("Cycle", entity.getPersistentData().getDouble("Cycle") - 1.0);
         } else {
            entity.getPersistentData().putDouble("Cycle", 7.0);
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:jetidle")),
                     SoundSource.HOSTILE,
                     10.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:jetidle")),
                     SoundSource.HOSTILE,
                     10.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.95, 1.05),
                     false
                  );
               }
            }

            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:jetfar")),
                     SoundSource.HOSTILE,
                     30.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.25)
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:jetfar")),
                     SoundSource.HOSTILE,
                     30.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.25),
                     false
                  );
               }
            }
         }

         if (entity.getY() < (double)(world.getHeight(Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) + 45)) {
            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x(), entity.getDeltaMovement().y() + 0.04, entity.getDeltaMovement().z()));
         } else if (entity.getY() > (double)(world.getHeight(Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) + 55)) {
            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x(), entity.getDeltaMovement().y() - 0.005, entity.getDeltaMovement().z()));
         }

         entity.setDeltaMovement(
            new Vec3(
               entity.getDeltaMovement().x() * 0.995 + entity.getLookAngle().x / 5.0 + (entity.getPersistentData().getDouble("X") - x) / 2500.0,
               entity.getDeltaMovement().y() * 0.995 + entity.getLookAngle().y / 25.0,
               entity.getDeltaMovement().z() * 0.995 + entity.getLookAngle().z / 5.0 + (entity.getPersistentData().getDouble("Z") - z) / 2500.0
            )
         );
         world.addParticle(
            (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
            x - entity.getLookAngle().x * 4.0,
            y + 1.25 - entity.getLookAngle().y,
            z - entity.getLookAngle().z * 4.0,
            entity.getDeltaMovement().x(),
            entity.getDeltaMovement().y(),
            entity.getDeltaMovement().z()
         );
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1.0F) > 20.0F) {
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.CAMP_SMOKE.get(),
               x - entity.getLookAngle().x * 7.0,
               y + 1.25,
               z - entity.getLookAngle().z * 7.0,
               0.0,
               0.0,
               0.0
            );
         } else {
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(),
               x - entity.getLookAngle().x * 7.0,
               y + 1.25,
               z - entity.getLookAngle().z * 7.0,
               0.0,
               0.0,
               0.0
            );
         }

         if (target != null && entity instanceof Mob _entity && target instanceof LivingEntity _ent) {
            _entity.setTarget(_ent);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ReaperAIProcedure.execute", _wtSafe);
      }
   }
}
