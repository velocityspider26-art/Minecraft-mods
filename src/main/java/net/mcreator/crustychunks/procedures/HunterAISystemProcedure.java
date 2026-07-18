package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.HunterEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class HunterAISystemProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         Entity target = null;
         boolean Trigger = false;
         boolean detonate = false;
         boolean schedoodlemode = false;
         double leadvariable = 0.0;
         double distancetotarget = 0.0;
         double speed = 0.0;
         double targetrange = 0.0;
         double buddydistance = 0.0;
         CrustyChunksMod.queueServerWork(1, () -> AutoscoutingProcedure.execute(world, x, y, z, entity));
         if ((entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null) != null && (entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null).isAlive()) {
            target = entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null;
         }

         if (entity.getPersistentData().getDouble("T") > 0.0) {
            entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") - 1.0);
         }

         if (1 == Mth.nextInt(RandomSource.create(), 1, 160)) {
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
                  && world.canSeeSkyFromBelowWater(BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()))
                  && (target != null && target.isAlive() || target == null)) {
                  target = entityiterator;
               }
            }
         }

         if (1 == Mth.nextInt(RandomSource.create(), 1, 5)) {
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(17.5), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               if (entityiteratorx instanceof HunterEntity && entityiteratorx != entity) {
                  schedoodlemode = true;
                  buddydistance = Math.sqrt(Math.pow(Math.abs(entityiteratorx.getX() - x), 2.0) + Math.pow(Math.abs(entityiteratorx.getZ() - z), 2.0));
                  entity.setDeltaMovement(
                     new Vec3(
                        entity.getDeltaMovement().x() + (entityiteratorx.getX() - x) / buddydistance / -18.0,
                        entity.getDeltaMovement().y(),
                        entity.getDeltaMovement().z() + (entityiteratorx.getZ() - z) / buddydistance / -18.0
                     )
                  );
               } else {
                  schedoodlemode = false;
               }
            }
         }

         if (target != null) {
            distancetotarget = Math.sqrt(Math.pow(Math.abs(target.getX() - x), 2.0) + Math.pow(Math.abs(target.getZ() - z), 2.0));
            entity.lookAt(Anchor.EYES, new Vec3(target.getX(), target.getY() + 1.0, target.getZ()));
            if (!schedoodlemode) {
               if (distancetotarget > 10.0) {
                  entity.setDeltaMovement(
                     new Vec3(
                        entity.getDeltaMovement().x() + entity.getLookAngle().x / 18.0,
                        entity.getDeltaMovement().y(),
                        entity.getDeltaMovement().z() + entity.getLookAngle().z / 18.0
                     )
                  );
                  entity.getPersistentData().putBoolean("Weapon", false);
               } else {
                  entity.getPersistentData().putBoolean("Weapon", true);
               }
            }

            if (entity.getPersistentData().getDouble("T") <= 0.0
               && (entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F) > 0.0F
               && (target instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1.0F) > 0.0F) {
               if (entity.getPersistentData().getBoolean("Weapon")) {
                  HunterGrenadeLauncherProcedure.execute(world, x, y, z, entity);
               } else {
                  HunterCannonProcedure.execute(world, x, y, z, entity);
               }
            }

            if (target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))) {
               target = null;
            }
         }

         if (entity.getPersistentData().getDouble("Cycle") > 0.0) {
            entity.getPersistentData().putDouble("Cycle", entity.getPersistentData().getDouble("Cycle") - 1.0);
         } else {
            entity.getPersistentData().putDouble("Cycle", 25.0);
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:hunternear")),
                     SoundSource.HOSTILE,
                     10.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:hunternear")),
                     SoundSource.HOSTILE,
                     10.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                     false
                  );
               }
            }

            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:hunterfar")),
                     SoundSource.HOSTILE,
                     30.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:hunterfar")),
                     SoundSource.HOSTILE,
                     30.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                     false
                  );
               }
            }
         }

         if (entity.getY() < (double)(world.getHeight(Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) + 35)) {
            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x(), entity.getDeltaMovement().y() + 0.01, entity.getDeltaMovement().z()));
         } else if (entity.getY() > (double)(world.getHeight(Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) + 40)) {
            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x(), entity.getDeltaMovement().y() - 0.005, entity.getDeltaMovement().z()));
         }

         entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x() * 0.995, entity.getDeltaMovement().y() * 0.995, entity.getDeltaMovement().z() * 0.995));
         if (target != null && entity instanceof Mob _entity && target instanceof LivingEntity _ent) {
            _entity.setTarget(_ent);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("HunterAISystemProcedure.execute", _wtSafe);
      }
   }
}
