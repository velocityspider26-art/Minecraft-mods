package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.crustychunks.entity.FlameThrowerEmberEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class GasolineExplosionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      boolean found = false;
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      sx = -4.0;
      found = false;

      for (int index0 = 0; index0 < 8; index0++) {
         sy = -3.0;

         for (int index1 = 0; index1 < 4; index1++) {
            sz = -4.0;

            for (int index2 = 0; index2 < 8; index2++) {
               if (Math.sqrt(Math.pow(sx, 2.0) + Math.pow(sz, 2.0)) < 3.0) {
                  BurnBlockProcedure.execute(world, x + sx, y + sy, z + sz);
               }

               sz++;
            }

            sy++;
         }

         sx++;
      }

      world.destroyBlock(BlockPos.containing(x, y, z), false);
      if (world instanceof ServerLevel _level) {
         _level.sendParticles(ParticleTypes.FLAME, x, y + 1.0, z, 15, 0.5, 0.5, 0.5, 0.6);
      }

      for (int index3 = 0; index3 < 50; index3++) {
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
                     entityToSpawn.igniteForSeconds(100);
                     entityToSpawn.setCritArrow(true);
                     return entityToSpawn;
                  }
               })
               .getArrow(projectileLevel, 1.0F, 0, (byte)2);
            _entityToSpawn.setPos(x + 0.0, y + 0.0, z + 0.0);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.2F, 180.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }

      if (world instanceof ServerLevel _level) {
         _level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.5, 0.5, 0.5, 0.6);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("GasolineExplosionProcedure.execute", _wtSafe);
      }
   }
}
