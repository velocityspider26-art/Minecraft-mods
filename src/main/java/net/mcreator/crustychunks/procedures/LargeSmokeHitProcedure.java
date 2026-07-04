package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.PhosphorusParticleEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class LargeSmokeHitProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         for (int index0 = 0; index0 < 45; index0++) {
            if (world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new PhosphorusParticleEntity((EntityType<? extends PhosphorusParticleEntity>)CrustyChunksModEntities.PHOSPHORUS_PARTICLE.get(), level) {
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
                  .getArrow(projectileLevel, 0.0F, 0, (byte)1);
               _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
               _entityToSpawn.shoot(
                  immediatesourceentity.getDeltaMovement().x(),
                  immediatesourceentity.getDeltaMovement().y(),
                  immediatesourceentity.getDeltaMovement().z(),
                  3.0F,
                  20.0F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         DamagesProcedure.execute(world, x, y, z);
         HeavyCrackProcedureProcedure.execute(world, x, y, z);
         CrustyChunksMod.queueServerWork(
            1,
            () -> LargeSmokeExplosionProcedure.execute(
                  world, immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ()
               )
         );
         if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("LargeSmokeHitProcedure.execute", _wtSafe);
      }
   }
}
