package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.DebrisEntity;
import net.mcreator.crustychunks.entity.ShockClientsideBypassEntity;
import net.mcreator.crustychunks.entity.SplashEffectClientBypassEntity;
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

public class LargeFraglessProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      boolean found = false;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      double xRadius = 0.0;
      double loop = 0.0;
      double zRadius = 0.0;
      double particleAmount = 0.0;
      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
               SoundSource.MASTER,
               120.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.0, 1.1)
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
               SoundSource.MASTER,
               120.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.0, 1.1),
               false
            );
         }
      }

      if (world instanceof Level _levelx) {
         if (!_levelx.isClientSide()) {
            _levelx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:huge_explosion_distant")),
               SoundSource.MASTER,
               60.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.3)
            );
         } else {
            _levelx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:huge_explosion_distant")),
               SoundSource.MASTER,
               60.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.3),
               false
            );
         }
      }

      if (world instanceof Level _levelxx) {
         if (!_levelxx.isClientSide()) {
            _levelxx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion")),
               SoundSource.MASTER,
               20.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.2)
            );
         } else {
            _levelxx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion")),
               SoundSource.MASTER,
               20.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.2),
               false
            );
         }
      }

      if (world.getFluidState(BlockPos.containing(x, y + 1.0, z)).createLegacyBlock().getBlock() instanceof LiquidBlock
         || world.getFluidState(BlockPos.containing(x, y, z)).createLegacyBlock().getBlock() instanceof LiquidBlock) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                     AbstractArrow entityToSpawn = new SplashEffectClientBypassEntity((EntityType<? extends SplashEffectClientBypassEntity>)CrustyChunksModEntities.SPLASH_EFFECT_CLIENT_BYPASS.get(), level) {
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
                     entityToSpawn.setCritArrow(true);
                     return entityToSpawn;
                  }
               })
               .getArrow(projectileLevel, 14.0F, 4, (byte)2);
            _entityToSpawn.setPos(x + 0.5, y + 1.5, z + 0.5);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.0F, 120.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (world instanceof Level _levelxxx && !_levelxxx.isClientSide()) {
            _levelxxx.explode(null, x + 0.5, y + 1.5, z + 0.5, 6.0F, ExplosionInteraction.TNT);
         }
      }

      if (world instanceof ServerLevel _levelxxx) {
         _levelxxx.sendParticles(ParticleTypes.FLASH, x, y, z, 5, 3.0, 3.0, 3.0, 1.0);
      }

      if (world instanceof Level _levelxxx && !_levelxxx.isClientSide()) {
         _levelxxx.explode(null, x + 0.5, y + 0.5, z + 0.5, 9.0F, ExplosionInteraction.TNT);
      }

      if (world instanceof Level _levelxxx && !_levelxxx.isClientSide()) {
         _levelxxx.explode(null, x + 0.5, y + 0.5, z + 0.5, 10.0F, ExplosionInteraction.NONE);
      }

      for (int index0 = 0; index0 < 10; index0++) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                  AbstractArrow entityToSpawn = new DebrisEntity((EntityType<? extends DebrisEntity>)CrustyChunksModEntities.DEBRIS.get(), level) {
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
            }).getArrow(projectileLevel, 14.0F, 4, (byte)2);
            _entityToSpawn.setPos(x + 0.5, y + 0.9, z + 0.5);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4), 45.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }

      if (world instanceof ServerLevel projectileLevel) {
         Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                  AbstractArrow entityToSpawn = new ShockClientsideBypassEntity((EntityType<? extends ShockClientsideBypassEntity>)CrustyChunksModEntities.SHOCK_CLIENTSIDE_BYPASS.get(), level) {
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
                  entityToSpawn.setCritArrow(true);
                  return entityToSpawn;
               }
            })
            .getArrow(projectileLevel, 14.0F, 4, (byte)2);
         _entityToSpawn.setPos(x + 0.5, y + 1.5, z + 0.5);
         _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.0F, 120.0F);
         projectileLevel.addFreshEntity(_entityToSpawn);
      }
   }
}
