package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.SmallAIBulletEntity;
import net.mcreator.crustychunks.entity.SmallMuzzleFlashProducerEntity;
import net.mcreator.crustychunks.entity.WorkerEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class WorkerRifleProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         double mvmultiplier = 0.0;
         mvmultiplier = ProjectileLibsSmallArmsProcedure.execute();
         if (entity.getPersistentData().getDouble("Mag") < 20.0) {
            if (entity instanceof Mob _entity) {
               _entity.getNavigation().stop();
            }

            Level projectileLevel = entity.level();
            if (!projectileLevel.isClientSide()) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new SmallAIBulletEntity((EntityType<? extends SmallAIBulletEntity>)CrustyChunksModEntities.SMALL_AI_BULLET.get(), level) {
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
                        return entityToSpawn;
                     }
                  })
                  .getArrow(projectileLevel, entity, 0.1F, 0, (byte)3);
               _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
               _entityToSpawn.shoot(entity.getLookAngle().x, entity.getLookAngle().y, entity.getLookAngle().z, (float)(6.0 * mvmultiplier), 1.0F);
               projectileLevel.addFreshEntity(_entityToSpawn);
            }

            if (world instanceof ServerLevel projectileLevelx) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                        AbstractArrow entityToSpawn = new SmallMuzzleFlashProducerEntity((EntityType<? extends SmallMuzzleFlashProducerEntity>)CrustyChunksModEntities.SMALL_MUZZLE_FLASH_PRODUCER.get(), level) {
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
                        return entityToSpawn;
                     }
                  })
                  .getArrow(projectileLevelx, entity, 0.0F, 0);
               _entityToSpawn.setPos(
                  x + entity.getLookAngle().x * 2.25,
                  y + (double)entity.getEyeHeight() + entity.getLookAngle().y * 2.25,
                  z + entity.getLookAngle().z * 2.25
               );
               _entityToSpawn.shoot(entity.getLookAngle().x, entity.getLookAngle().y, entity.getLookAngle().z, 0.0F, 0.0F);
               projectileLevelx.addFreshEntity(_entityToSpawn);
            }

            if (entity instanceof Mob _entity) {
               _entity.getNavigation().stop();
            }

            entity.getPersistentData().putDouble("T", 3.0);
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:distantgunfire")),
                     SoundSource.NEUTRAL,
                     80.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:distantgunfire")),
                     SoundSource.NEUTRAL,
                     80.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9),
                     false
                  );
               }
            }

            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:mediumshot")),
                     SoundSource.NEUTRAL,
                     10.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.8)
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:mediumshot")),
                     SoundSource.NEUTRAL,
                     10.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.7, 0.8),
                     false
                  );
               }
            }

            if (entity instanceof WorkerEntity) {
               ((WorkerEntity)entity).setAnimation("fire");
            }

            entity.getPersistentData().putDouble("Mag", entity.getPersistentData().getDouble("Mag") + 1.0);
            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:mediumcasing")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2)
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:mediumcasing")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.2),
                     false
                  );
               }
            }
         } else {
            entity.getPersistentData().putDouble("T", (double)Mth.nextInt(RandomSource.create(), 90, 120));
            entity.getPersistentData().putDouble("Mag", 0.0);
            if (world instanceof Level _levelxxx) {
               if (!_levelxxx.isClientSide()) {
                  _levelxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                  );
               } else {
                  _levelxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:boltreload")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                     false
                  );
               }
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("WorkerRifleProcedure.execute", _wtSafe);
      }
   }
}
