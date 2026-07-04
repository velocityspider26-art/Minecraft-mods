package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.ParticleProjectileEntity;
import net.mcreator.crustychunks.entity.SplashEffectClientBypassEntity;
import net.mcreator.crustychunks.entity.TinyClientEffectEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.LiquidBlock;

public class MicroExplosionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      boolean found = false;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      if (world.getFluidState(BlockPos.containing(x, y + 1.0, z)).createLegacyBlock().getBlock() instanceof LiquidBlock) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new SplashEffectClientBypassEntity((EntityType<? extends SplashEffectClientBypassEntity>)CrustyChunksModEntities.SPLASH_EFFECT_CLIENT_BYPASS.get(), level) {
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
               .getArrow(projectileLevel, 0.0F, 0);
            _entityToSpawn.setPos(x, y, z);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.3F, 180.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (world instanceof Level _level && !_level.isClientSide()) {
            net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, x, y, z, 1.0F, ExplosionInteraction.TNT);
         }
      } else if (world instanceof ServerLevel projectileLevel) {
         Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback) {
                  AbstractArrow entityToSpawn = new TinyClientEffectEntity((EntityType<? extends TinyClientEffectEntity>)CrustyChunksModEntities.TINY_CLIENT_EFFECT.get(), level) {
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
            .getArrow(projectileLevel, 0.0F, 0);
         _entityToSpawn.setPos(x, y, z);
         _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.3F, 180.0F);
         projectileLevel.addFreshEntity(_entityToSpawn);
      }

      CrustyChunksMod.queueServerWork(2, () -> {
         if (world instanceof Level _levelxx && !_levelxx.isClientSide()) {
            net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxx, null, x + 0.0, y + 0.0, z + 0.0, 2.0F, ExplosionInteraction.TNT);
         }

         if (world instanceof Level _levelx && !_levelx.isClientSide()) {
            net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelx, null, x + 0.0, y + 0.0, z + 0.0, 3.0F, ExplosionInteraction.NONE);
         }
      });
      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:medium_small_explosion_distant")),
               SoundSource.MASTER,
               80.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:medium_small_explosion_distant")),
               SoundSource.MASTER,
               80.0F,
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
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
               SoundSource.MASTER,
               15.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.3, 1.5)
            );
         } else {
            _levelx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
               SoundSource.MASTER,
               15.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.3, 1.5),
               false
            );
         }
      }

      for (int index0 = 0; index0 < 10; index0++) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                     AbstractArrow entityToSpawn = new ParticleProjectileEntity((EntityType<? extends ParticleProjectileEntity>)CrustyChunksModEntities.PARTICLE_PROJECTILE.get(), level) {
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
                     entityToSpawn.setBaseDamage((double)damage);
                     entityToSpawn.setSilent(true);
                     return entityToSpawn;
                  }
               })
               .getArrow(projectileLevel, 0.0F, 3, (byte)2);
            _entityToSpawn.setPos(x, y, z);
            _entityToSpawn.shoot(
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
               0.2,
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
               (float)Mth.nextDouble(RandomSource.create(), 0.6, 0.7),
               45.0F
            );
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }

      if (world instanceof ServerLevel _levelxx) {
         _levelxx.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.5, 0.5, 0.5, 0.6);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("MicroExplosionProcedure.execute", _wtSafe);
      }
   }
}
