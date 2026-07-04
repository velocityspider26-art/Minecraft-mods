package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.PhosphorusParticleEntity;
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

public class SmallSmokeExplosionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      double particleRadius = 0.0;
      double particleAmount = 0.0;
      if (world instanceof Level _level && !_level.isClientSide()) {
         net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, x, y, z, 2.0F, ExplosionInteraction.NONE);
      }

      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
               SoundSource.NEUTRAL,
               5.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.6, 1.8)
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
               SoundSource.NEUTRAL,
               5.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.6, 1.8),
               false
            );
         }
      }

      if (world instanceof Level _levelx) {
         if (!_levelx.isClientSide()) {
            _levelx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:medium_small_explosion_distant")),
               SoundSource.NEUTRAL,
               20.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
            );
         } else {
            _levelx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:medium_small_explosion_distant")),
               SoundSource.NEUTRAL,
               20.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
               false
            );
         }
      }

      for (int index0 = 0; index0 < 60; index0++) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new PhosphorusParticleEntity((EntityType<? extends PhosphorusParticleEntity>)CrustyChunksModEntities.PHOSPHORUS_PARTICLE.get(), level) {
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
            _entityToSpawn.shoot(0.0, 1.0, 0.0, (float)Mth.nextDouble(RandomSource.create(), 0.5, 0.7), 120.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }
   }
}
