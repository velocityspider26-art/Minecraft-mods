package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.SmallBombProjectileEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ClusterRocketFlightTickProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         boolean Trigger = false;
         double mvmultiplier = 0.0;
         mvmultiplier = ProjectilelibsProcedure.execute();
         immediatesourceentity.getPersistentData().putDouble("MaxTime", 30.0);
         immediatesourceentity.getPersistentData().putDouble("Time", immediatesourceentity.getPersistentData().getDouble("Time") + 1.0);
         if (immediatesourceentity.getPersistentData().getDouble("Time") <= 30.0) {
            for (int index0 = 0; index0 < 4; index0++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.CAMP_SMOKE.get(),
                  x + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  y + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  z + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
               );
            }

            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.ROCKET_FLAME.get(),
               x,
               y,
               z,
               immediatesourceentity.getDeltaMovement().x(),
               immediatesourceentity.getDeltaMovement().y(),
               immediatesourceentity.getDeltaMovement().z()
            );
            if (immediatesourceentity.getPersistentData().getDouble("Time") / 10.0
               == (double)Math.round(immediatesourceentity.getPersistentData().getDouble("Time") / 10.0)) {
               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                        SoundSource.NEUTRAL,
                        15.0F,
                        (float)(1.4 - immediatesourceentity.getPersistentData().getDouble("Time") / 30.0)
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                        SoundSource.NEUTRAL,
                        15.0F,
                        (float)(1.4 - immediatesourceentity.getPersistentData().getDouble("Time") / 30.0),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketfar")),
                        SoundSource.NEUTRAL,
                        40.0F,
                        (float)(1.4 - immediatesourceentity.getPersistentData().getDouble("Time") / 30.0)
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketfar")),
                        SoundSource.NEUTRAL,
                        40.0F,
                        (float)(1.4 - immediatesourceentity.getPersistentData().getDouble("Time") / 30.0),
                        false
                     );
                  }
               }
            }

            if (world instanceof ServerLevel _levelxx) {
               _levelxx.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
            }

            Vec3 motion = immediatesourceentity.getDeltaMovement().scale(1.0 + 0.02 * mvmultiplier);
            immediatesourceentity.setDeltaMovement(motion);
         } else {
            for (int index1 = 0; index1 < 4; index1++) {
               if (world instanceof ServerLevel projectileLevel) {
                  Projectile _entityToSpawn = (new Object() {
                        public Projectile getArrow(Level level, float damage, int knockback) {
                           AbstractArrow entityToSpawn = new SmallBombProjectileEntity((EntityType<? extends SmallBombProjectileEntity>)CrustyChunksModEntities.SMALL_BOMB_PROJECTILE.get(), level) {
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
                     .getArrow(projectileLevel, 5.0F, 1);
                  _entityToSpawn.setPos(x, y, z);
                  _entityToSpawn.shoot(
                     immediatesourceentity.getDeltaMovement().x(),
                     immediatesourceentity.getDeltaMovement().y(),
                     immediatesourceentity.getDeltaMovement().z(),
                     (float)(immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0),
                     12.0F
                  );
                  projectileLevel.addFreshEntity(_entityToSpawn);
               }
            }

            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }

         if (immediatesourceentity.isUnderWater()) {
            ClusterRocketHitProcedure.execute(world, immediatesourceentity);
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }
         }

         Vec3 _center = new Vec3(x, y, z);

         for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1.25), e -> true)
            .stream()
            .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
            .toList()) {
            if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:bullet")))) {
               if (!entityiterator.level().isClientSide()) {
                  entityiterator.discard();
               }

               Trigger = true;
            }
         }

         if (Trigger) {
            if (!immediatesourceentity.level().isClientSide()) {
               immediatesourceentity.discard();
            }

            CrustyChunksMod.queueServerWork(1, () -> ClusterRocketHitProcedure.execute(world, immediatesourceentity));
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ClusterRocketFlightTickProcedure.execute", _wtSafe);
      }
   }
}
