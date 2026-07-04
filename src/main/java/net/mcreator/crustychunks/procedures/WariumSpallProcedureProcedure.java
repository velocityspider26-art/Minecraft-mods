package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.crustychunks.entity.HugeFragmentEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class WariumSpallProcedureProcedure {
   public static void execute(LevelAccessor world, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         double Power = 0.0;
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new HugeFragmentEntity((EntityType<? extends HugeFragmentEntity>)CrustyChunksModEntities.HUGE_FRAGMENT.get(), level) {
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
               .getArrow(projectileLevel, immediatesourceentity, 3.0F, 1);
            _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
            _entityToSpawn.shoot(
               immediatesourceentity.getDeltaMovement().x(), immediatesourceentity.getDeltaMovement().y(), immediatesourceentity.getDeltaMovement().z(), 3.0F, 8.0F
            );
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("WariumSpallProcedureProcedure.execute", _wtSafe);
      }
   }
}
