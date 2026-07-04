package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.CannonMuzzleFlashProducerEntity;
import net.mcreator.crustychunks.entity.EradicatorEntity;
import net.mcreator.crustychunks.entity.LargeHEATFireEntity;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class EradicatorCannonProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         double Barrels = 0.0;
         double Xvector = 0.0;
         double Zvector = 0.0;
         double Pitch = 0.0;
         double mvmultiplier = 0.0;
         mvmultiplier = ProjectilelibsProcedure.execute();
         entity.setYRot(entity.getYRot());
         entity.setXRot(entity.getXRot());
         entity.setYBodyRot(entity.getYRot());
         entity.setYHeadRot(entity.getYRot());
         entity.yRotO = entity.getYRot();
         entity.xRotO = entity.getXRot();
         if (entity instanceof LivingEntity _entity) {
            _entity.yBodyRotO = _entity.getYRot();
            _entity.yHeadRotO = _entity.getYRot();
         }

         Level projectileLevel = entity.level();
         if (!projectileLevel.isClientSide()) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new LargeHEATFireEntity((EntityType<? extends LargeHEATFireEntity>)CrustyChunksModEntities.LARGE_HEAT_FIRE.get(), level) {
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
               .getArrow(projectileLevel, entity, 1.0F, 1);
            _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
            _entityToSpawn.shoot(entity.getLookAngle().x, entity.getLookAngle().y, entity.getLookAngle().z, (float)(7.2 * mvmultiplier), 1.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (world instanceof ServerLevel projectileLevelx) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new CannonMuzzleFlashProducerEntity((EntityType<? extends CannonMuzzleFlashProducerEntity>)CrustyChunksModEntities.CANNON_MUZZLE_FLASH_PRODUCER.get(), level) {
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
               .getArrow(projectileLevelx, entity, 1.0F, 1);
            _entityToSpawn.setPos(
               x + entity.getLookAngle().x * 10.0, y + (double)entity.getEyeHeight() + entity.getLookAngle().y * 7.0, z + entity.getLookAngle().z * 10.0
            );
            _entityToSpawn.shoot(entity.getLookAngle().x, entity.getLookAngle().y, entity.getLookAngle().z, 1.0F, 0.0F);
            projectileLevelx.addFreshEntity(_entityToSpawn);
         }

         entity.getPersistentData().putDouble("T", (double)Mth.nextInt(RandomSource.create(), 68, 70));
         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:autoloader")),
                  SoundSource.NEUTRAL,
                  2.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:autoloader")),
                  SoundSource.NEUTRAL,
                  2.0F,
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
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:battlecannon")),
                  SoundSource.NEUTRAL,
                  15.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
               );
            } else {
               _levelx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:battlecannon")),
                  SoundSource.NEUTRAL,
                  15.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                  false
               );
            }
         }

         if (world instanceof Level _levelxx) {
            if (!_levelxx.isClientSide()) {
               _levelxx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:cannonfar")),
                  SoundSource.NEUTRAL,
                  60.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3)
               );
            } else {
               _levelxx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:cannonfar")),
                  SoundSource.NEUTRAL,
                  60.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3),
                  false
               );
            }
         }

         if (world instanceof Level _levelxxx) {
            if (!_levelxxx.isClientSide()) {
               _levelxxx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
                  SoundSource.NEUTRAL,
                  120.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3)
               );
            } else {
               _levelxxx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
                  SoundSource.NEUTRAL,
                  120.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3),
                  false
               );
            }
         }

         if (entity instanceof EradicatorEntity) {
            ((EradicatorEntity)entity).setAnimation("Fire");
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("EradicatorCannonProcedure.execute", _wtSafe);
      }
   }
}
