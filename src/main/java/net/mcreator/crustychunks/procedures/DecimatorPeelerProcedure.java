package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.RocketEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class DecimatorPeelerProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 8, true, false));
         }

         if (entity.getPersistentData().getDouble("Rocket") < 4.0) {
            if (entity instanceof Mob _entity) {
               _entity.getNavigation().stop();
            }

            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new RocketEntity((EntityType<? extends RocketEntity>)CrustyChunksModEntities.ROCKET.get(), level) {
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
               }).getArrow(projectileLevel, entity, 1.0F, 1);
               _entityToSpawn.setPos(x + entity.getLookAngle().x * 2.0, y + 3.0, z + entity.getLookAngle().z * 2.0);
               _entityToSpawn.shoot(entity.getLookAngle().x, entity.getLookAngle().y + 0.08, entity.getLookAngle().z, 5.0F, 1.0F);
               projectileLevel.addFreshEntity(_entityToSpawn);
            }

            entity.getPersistentData().putDouble("T", (double)Mth.nextInt(RandomSource.create(), 15, 20));
            entity.getPersistentData().putDouble("Rocket", entity.getPersistentData().getDouble("Rocket") + 1.0);
            PeelerPodFireSoundProcedure.execute(world, x, y, z);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("DecimatorPeelerProcedure.execute", _wtSafe);
      }
   }
}
