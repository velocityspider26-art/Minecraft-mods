package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.ClusterRocketEntity;
import net.mcreator.crustychunks.entity.IRMissileEntity;
import net.mcreator.crustychunks.entity.IncindiaryRocketProjectileEntity;
import net.mcreator.crustychunks.entity.LargeRocketEntity;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CIWSAIProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         Entity target = null;
         boolean hottarget = false;
         double rev = 0.0;
         double targetrange = 0.0;
         double LeadRange = 0.0;
         double leadx = 0.0;
         double leady = 0.0;
         double leadz = 0.0;
         double locationy = 0.0;
         double distancewithvector = 0.0;
         double locationz = 0.0;
         double distance = 0.0;
         double locationx = 0.0;
         double mvmultiplier = 0.0;
         mvmultiplier = ProjectileLibsSmallArmsProcedure.execute();
         if (entity.getPersistentData().getDouble("T") > -1.0) {
            entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") - 1.0);
         }

         Vec3 _center = new Vec3(x, y + 256.0, z);

         for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(200.0), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
            .toList()) {
            if (entityiterator != null) {
               distance = Math.sqrt(
                  Math.pow(x - entityiterator.getX(), 2.0) + Math.pow(y - entityiterator.getY(), 2.0) + Math.pow(z - entityiterator.getZ(), 2.0)
               );
               distancewithvector = Math.sqrt(
                  Math.pow(x - (entityiterator.getX() + entityiterator.getDeltaMovement().x()), 2.0)
                     + Math.pow(y - (entityiterator.getY() + entityiterator.getDeltaMovement().y()), 2.0)
                     + Math.pow(z - (entityiterator.getZ() + entityiterator.getDeltaMovement().z()), 2.0)
               );
               if ((new Object() {
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
                  || (
                        entityiterator instanceof LargeRocketEntity
                           || entityiterator instanceof IncindiaryRocketProjectileEntity
                           || entityiterator instanceof ClusterRocketEntity
                           || entityiterator instanceof IRMissileEntity
                     )
                     && distancewithvector < distance) {
                  target = entityiterator;
               }
            }
         }

         if (target != null) {
            hottarget = false;
            Vec3 _center1 = new Vec3(target.getX(), target.getY(), target.getZ());

            for (Entity entityiteratorx : world.getEntitiesOfClass(Entity.class, new AABB(_center1, _center1).inflate(25.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center1)))
               .toList()) {
               if (entityiteratorx.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:warm")))
                  && !entityiteratorx.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))) {
                  if (entityiteratorx.getPersistentData().getDouble("Mx") == 0.0
                     && entityiteratorx.getPersistentData().getDouble("My") == 0.0
                     && entityiteratorx.getPersistentData().getDouble("Mz") == 0.0) {
                     leadx = entityiteratorx.getDeltaMovement().x();
                     leady = entityiteratorx.getDeltaMovement().y();
                     leadz = entityiteratorx.getDeltaMovement().z();
                  } else {
                     leadx = entityiteratorx.getPersistentData().getDouble("Mx");
                     leady = entityiteratorx.getPersistentData().getDouble("My");
                     leadz = entityiteratorx.getPersistentData().getDouble("Mz");
                  }

                  hottarget = true;
               } else {
                  leadx = target.getDeltaMovement().x();
                  leady = target.getDeltaMovement().y();
                  leadz = target.getDeltaMovement().z();
               }
            }

            if (target.getY() > y + 25.0) {
               targetrange = Math.sqrt(
                  Math.pow(Math.abs(y - target.getY()), 2.0)
                     + Math.pow(Math.abs(z - target.getZ()), 2.0)
                     + Math.pow(Math.abs(x - target.getX()), 2.0)
               );
               LeadRange = (targetrange + Math.pow(targetrange, 2.0) / 500.0) / 2.0;
               entity.getPersistentData().putDouble("TargX", target.getX() + leadx / mvmultiplier * LeadRange * 0.17);
               entity.getPersistentData().putDouble("TargY", Math.max(y + 25.0, target.getY() + 2.0 + leady / mvmultiplier * LeadRange * 0.17));
               entity.getPersistentData().putDouble("TargZ", target.getZ() + leadz / mvmultiplier * LeadRange * 0.17);
               entity.lookAt(
                  Anchor.EYES,
                  new Vec3(
                     entity.getPersistentData().getDouble("TargX"),
                     entity.getPersistentData().getDouble("TargY"),
                     entity.getPersistentData().getDouble("TargZ")
                  )
               );
               if (hottarget || !target.onGround()) {
                  if (hottarget && entity.getPersistentData().getDouble("Rocket") < 3.0) {
                     CrustyChunksMod.queueServerWork(
                        1,
                        () -> {
                           entity.lookAt(
                              Anchor.EYES,
                              new Vec3(
                                 entity.getPersistentData().getDouble("TargX"),
                                 entity.getPersistentData().getDouble("TargY"),
                                 entity.getPersistentData().getDouble("TargZ")
                              )
                           );
                           if (40.0
                                 < Math.sqrt(
                                    Math.pow(
                                          (double)entity.level()
                                             .clip(
                                                new ClipContext(
                                                   entity.getEyePosition(1.0F),
                                                   entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(45.0)),
                                                   Block.COLLIDER,
                                                   Fluid.ANY,
                                                   entity
                                                )
                                             )
                                             .getBlockPos()
                                             .getX(),
                                          2.0
                                       )
                                       + Math.pow(
                                          (double)entity.level()
                                             .clip(
                                                new ClipContext(
                                                   entity.getEyePosition(1.0F),
                                                   entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(45.0)),
                                                   Block.COLLIDER,
                                                   Fluid.ANY,
                                                   entity
                                                )
                                             )
                                             .getBlockPos()
                                             .getY(),
                                          2.0
                                       )
                                       + Math.pow(
                                          (double)entity.level()
                                             .clip(
                                                new ClipContext(
                                                   entity.getEyePosition(1.0F),
                                                   entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(45.0)),
                                                   Block.COLLIDER,
                                                   Fluid.ANY,
                                                   entity
                                                )
                                             )
                                             .getBlockPos()
                                             .getZ(),
                                          2.0
                                       )
                                 )
                              && entity.getPersistentData().getDouble("T") < 0.0) {
                              CIWSSAMProcedure.execute(world, x, y, z, entity);
                              entity.getPersistentData().putDouble("T", 20.0);
                           }
                        }
                     );
                  } else {
                     CrustyChunksMod.queueServerWork(
                        1,
                        () -> {
                           entity.lookAt(
                              Anchor.EYES,
                              new Vec3(
                                 entity.getPersistentData().getDouble("TargX"),
                                 entity.getPersistentData().getDouble("TargY"),
                                 entity.getPersistentData().getDouble("TargZ")
                              )
                           );
                           if (40.0
                                 < Math.sqrt(
                                    Math.pow(
                                          (double)entity.level()
                                             .clip(
                                                new ClipContext(
                                                   entity.getEyePosition(1.0F),
                                                   entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(45.0)),
                                                   Block.COLLIDER,
                                                   Fluid.ANY,
                                                   entity
                                                )
                                             )
                                             .getBlockPos()
                                             .getX(),
                                          2.0
                                       )
                                       + Math.pow(
                                          (double)entity.level()
                                             .clip(
                                                new ClipContext(
                                                   entity.getEyePosition(1.0F),
                                                   entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(45.0)),
                                                   Block.COLLIDER,
                                                   Fluid.ANY,
                                                   entity
                                                )
                                             )
                                             .getBlockPos()
                                             .getY(),
                                          2.0
                                       )
                                       + Math.pow(
                                          (double)entity.level()
                                             .clip(
                                                new ClipContext(
                                                   entity.getEyePosition(1.0F),
                                                   entity.getEyePosition(1.0F).add(entity.getViewVector(1.0F).scale(45.0)),
                                                   Block.COLLIDER,
                                                   Fluid.ANY,
                                                   entity
                                                )
                                             )
                                             .getBlockPos()
                                             .getZ(),
                                          2.0
                                       )
                                 )
                              && entity.getPersistentData().getDouble("T") < 0.0) {
                              CIWSGunProcedure.execute(world, x, y, z, entity);
                           }
                        }
                     );
                  }
               }
            }
         }

         if (entity.getPersistentData().getDouble("Rocket") > 0.0 && 1 == Mth.nextInt(RandomSource.create(), 1, 800)) {
            entity.getPersistentData().putDouble("Rocket", entity.getPersistentData().getDouble("Rocket") - 1.0);
         }

         if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
               < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F) / 2.0F
            && 10 == Mth.nextInt(RandomSource.create(), 1, 15)) {
            world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x, y + 4.0, z, 0.0, 1.0, 0.0);
         }

         if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
            < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F) / 4.0F) {
            if (world instanceof ServerLevel _level) {
               _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SPARKS.get(), x, y + 2.0, z, 2, 1.0, 1.0, 1.0, 1.0);
            }

            if (10 == Mth.nextInt(RandomSource.create(), 1, 80) && world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                     SoundSource.NEUTRAL,
                     4.0F,
                     0.7F
                  );
               } else {
                  _level.playLocalSound(
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
            entity.setDeltaMovement(new Vec3(0.0, 0.1, 0.0));
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("CIWSAIProcedure.execute", _wtSafe);
      }
   }
}
