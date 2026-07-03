package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import net.mcreator.crustychunks.entity.EmberParticleProjectileEntity;
import net.mcreator.crustychunks.entity.FireClientEffectEntity;
import net.mcreator.crustychunks.entity.FlameThrowerEmberEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SmallfirexplosionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      boolean found = false;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      world.destroyBlock(BlockPos.containing(x, y, z), false);
      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:huge_explosion_distant")),
               SoundSource.MASTER,
               40.0F,
               (float)Mth.nextDouble(RandomSource.create(), -0.9, 1.1)
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:huge_explosion_distant")),
               SoundSource.MASTER,
               40.0F,
               (float)Mth.nextDouble(RandomSource.create(), -0.9, 1.1),
               false
            );
         }
      }

      if (world instanceof Level _levelx) {
         if (!_levelx.isClientSide()) {
            _levelx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion")),
               SoundSource.MASTER,
               6.0F,
               (float)(0.5 + Mth.nextDouble(RandomSource.create(), -0.1, 0.1))
            );
         } else {
            _levelx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion")),
               SoundSource.MASTER,
               6.0F,
               (float)(0.5 + Mth.nextDouble(RandomSource.create(), -0.1, 0.1)),
               false
            );
         }
      }

      if (world.getFluidState(BlockPos.containing(x, y + 1.0, z)).createLegacyBlock().getBlock() instanceof LiquidBlock) {
         if (world instanceof Level _levelxx && !_levelxx.isClientSide()) {
            _levelxx.explode(null, x, y, z, 2.0F, ExplosionInteraction.NONE);
         }
      } else if (world instanceof ServerLevel projectileLevel) {
         Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                  AbstractArrow entityToSpawn = new FireClientEffectEntity((EntityType<? extends FireClientEffectEntity>)CrustyChunksModEntities.FIRE_CLIENT_EFFECT.get(), level) {
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
            .getArrow(projectileLevel, 11.0F, 3, (byte)2);
         _entityToSpawn.setPos(x, y + 1.0, z);
         _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.0F, 90.0F);
         projectileLevel.addFreshEntity(_entityToSpawn);
      }

      if (world instanceof Level _levelxx && !_levelxx.isClientSide()) {
         _levelxx.explode(null, x, y + 1.0, z, 2.0F, ExplosionInteraction.TNT);
      }

      for (int index0 = 0; index0 < 60; index0++) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                     AbstractArrow entityToSpawn = new EmberParticleProjectileEntity((EntityType<? extends EmberParticleProjectileEntity>)CrustyChunksModEntities.EMBER_PARTICLE_PROJECTILE.get(), level) {
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
                     entityToSpawn.igniteForSeconds(100);
                     return entityToSpawn;
                  }
               })
               .getArrow(projectileLevel, 11.0F, 3, (byte)2);
            _entityToSpawn.setPos(x, y + 1.0, z);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, (float)Mth.nextDouble(RandomSource.create(), 0.6, 0.7), 90.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }

      for (int index1 = 0; index1 < 40; index1++) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                     AbstractArrow entityToSpawn = new FlameThrowerEmberEntity((EntityType<? extends FlameThrowerEmberEntity>)CrustyChunksModEntities.FLAME_THROWER_EMBER.get(), level) {
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
               .getArrow(projectileLevel, 11.0F, 3, (byte)2);
            _entityToSpawn.setPos(x, y + 1.0, z);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, (float)Mth.nextDouble(RandomSource.create(), 0.6, 0.7), 90.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }

      sx = -5.0;
      found = false;

      for (int index2 = 0; index2 < 10; index2++) {
         sy = -3.0;

         for (int index3 = 0; index3 < 6; index3++) {
            sz = -5.0;

            for (int index4 = 0; index4 < 10; index4++) {
               if (Math.sqrt(Math.pow(sz, 2.0) + Math.pow(sx, 2.0)) < 8.0) {
                  BurnBlockProcedure.execute(world, x + sx, y + sy, z + sz);
               }

               sz++;
            }

            sy++;
         }

         sx++;
      }

      Vec3 _center = new Vec3(x, y, z);

      for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(6.0), e -> true)
         .stream()
         .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
         .toList()) {
         if (entityiterator instanceof LivingEntity) {
            LivingEntity _entity = (LivingEntity)entityiterator;
            if (!_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(CrustyChunksModMobEffects.FLAMMABLE, 1000, 3));
            }
         }
      }
   }
}
