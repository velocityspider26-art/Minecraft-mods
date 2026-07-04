package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import java.util.Comparator;
import net.mcreator.crustychunks.entity.RadioactiveCloudDetectorEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LightMeltdownProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      if (world instanceof ServerLevel _level) {
         _level.sendParticles(ParticleTypes.ASH, x + 0.5, y + 3.0, z + 0.5, 90, 5.0, 3.0, 5.0, 0.0);
      }

      Vec3 _center = new Vec3(x + 0.5, y + 0.5, z + 0.5);

      for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(5.0), e -> true)
         .stream()
         .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
         .toList()) {
         entityiterator.getPersistentData().putDouble("Radiation", entityiterator.getPersistentData().getDouble("Radiation") + 20.0);
         if (entityiterator instanceof LivingEntity) {
            LivingEntity _entity = (LivingEntity)entityiterator;
            if (!_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(CrustyChunksModMobEffects.RADIATION, 60, 1, false, false));
            }
         }
      }

      if (world instanceof ServerLevel projectileLevel) {
         Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback) {
                  AbstractArrow entityToSpawn = new RadioactiveCloudDetectorEntity((EntityType<? extends RadioactiveCloudDetectorEntity>)CrustyChunksModEntities.RADIOACTIVE_CLOUD_DETECTOR.get(), level) {
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
         _entityToSpawn.setPos(x + 0.5, y + 0.5, z + 0.5);
         _entityToSpawn.shoot(0.0, 0.0, 0.0, 0.0F, 0.0F);
         projectileLevel.addFreshEntity(_entityToSpawn);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("LightMeltdownProcedure.execute", _wtSafe);
      }
   }
}
