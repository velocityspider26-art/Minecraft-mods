package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AutoscoutingProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         Entity targetcandidate = null;
         double oldyaw = 0.0;
         double oldpitch = 0.0;
         if (1 == Mth.nextInt(RandomSource.create(), 1, 200) && (entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null) == null) {
            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(200.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
               .toList()) {
               if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robotarget")))
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
                        .checkGamemode(entityiterator)) {
                  targetcandidate = entityiterator;
               }
            }

            if (targetcandidate != null) {
               oldyaw = (double)entity.getYRot();
               oldpitch = (double)entity.getXRot();
               entity.lookAt(Anchor.EYES, new Vec3(targetcandidate.getX(), targetcandidate.getY() + 1.0, targetcandidate.getZ()));
               if (!(
                     Math.abs(targetcandidate.getY() - y)
                           - (
                              Math.abs(
                                    (double)entity.level()
                                          .clip(
                                             new ClipContext(
                                                entity.getEyePosition(1.0F),
                                                entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(256.0)),
                                                Block.VISUAL,
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
                     Math.abs(targetcandidate.getX() - x)
                           - (
                              Math.abs(
                                    (double)entity.level()
                                          .clip(
                                             new ClipContext(
                                                entity.getEyePosition(1.0F),
                                                entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(256.0)),
                                                Block.VISUAL,
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
                     Math.abs(targetcandidate.getZ() - z)
                           - (
                              Math.abs(
                                    (double)entity.level()
                                          .clip(
                                             new ClipContext(
                                                entity.getEyePosition(1.0F),
                                                entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(256.0)),
                                                Block.VISUAL,
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
                  )) {
                  entity.setYRot((float)oldyaw);
                  entity.setXRot((float)oldpitch);
                  entity.setYBodyRot(entity.getYRot());
                  entity.setYHeadRot(entity.getYRot());
                  entity.yRotO = entity.getYRot();
                  entity.xRotO = entity.getXRot();
                  if (entity instanceof LivingEntity _entity) {
                     _entity.yBodyRotO = _entity.getYRot();
                     _entity.yHeadRotO = _entity.getYRot();
                  }
               } else if (entity instanceof Mob _entity && targetcandidate instanceof LivingEntity _ent) {
                  _entity.setTarget(_ent);
               }
            }
         } else if (1 == Mth.nextInt(RandomSource.create(), 1, 10) && (entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null) != null) {
            CrustyChunksMod.queueServerWork(
               1,
               () -> {
                  if ((entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null) != null) {
                     Vec3 _center = new Vec3(x, y, z);

                     for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(100.0), e -> true)
                        .stream()
                        .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
                        .toList()) {
                        if (entityiteratorx.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))
                           && (entityiteratorx instanceof Mob _mobEntx ? _mobEntx.getTarget() : null) == null
                           && entityiteratorx instanceof Mob) {
                           Mob _entity = (Mob)entityiteratorx;
                           Entity patt5170$temp = entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null;
                           if (patt5170$temp instanceof LivingEntity _entx) {
                              _entity.setTarget(_entx);
                           }
                        }
                     }
                  }
               }
            );
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("AutoscoutingProcedure.execute", _wtSafe);
      }
   }
}
