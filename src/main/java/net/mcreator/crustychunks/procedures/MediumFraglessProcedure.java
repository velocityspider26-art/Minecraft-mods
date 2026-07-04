package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.SmallClientEffectEntity;
import net.mcreator.crustychunks.entity.SplashEffectClientBypassEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
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

public class MediumFraglessProcedure {
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
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
               SoundSource.MASTER,
               120.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.2)
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
               SoundSource.MASTER,
               120.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.2),
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
               (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
            );
         } else {
            _levelxx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:explosion")),
               SoundSource.MASTER,
               20.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
               false
            );
         }
      }

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
            _entityToSpawn.setPos(x, y + 1.5, z);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.4F, 120.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (world instanceof Level _levelxxx && !_levelxxx.isClientSide()) {
            net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxx, null, x, y, z, 4.0F, ExplosionInteraction.TNT);
         }
      } else if (world instanceof ServerLevel projectileLevel) {
         Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback) {
                  AbstractArrow entityToSpawn = new SmallClientEffectEntity((EntityType<? extends SmallClientEffectEntity>)CrustyChunksModEntities.SMALL_CLIENT_EFFECT.get(), level) {
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
         _entityToSpawn.setPos(x, y + 1.5, z);
         _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.4F, 120.0F);
         projectileLevel.addFreshEntity(_entityToSpawn);
      }

      if (world instanceof Level _levelxxx && !_levelxxx.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxx, null, x, y, z, 5.0F, ExplosionInteraction.TNT);
      }

      if (world instanceof Level _levelxxx && !_levelxxx.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxx, null, x, y, z, 8.0F, ExplosionInteraction.NONE);
      }
   }
}
