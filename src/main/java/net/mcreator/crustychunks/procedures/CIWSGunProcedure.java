package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.CIWSEntity;
import net.mcreator.crustychunks.entity.HugeAIBulletEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class CIWSGunProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         double Barrels = 0.0;
         double Xvector = 0.0;
         double DrumPosition = 0.0;
         double Zvector = 0.0;
         double Pitch = 0.0;
         double locationy = 0.0;
         double distancewithvector = 0.0;
         double locationz = 0.0;
         double distance = 0.0;
         double locationx = 0.0;
         double mvmultiplier = 0.0;
         mvmultiplier = ProjectileLibsSmallArmsProcedure.execute();
         Level projectileLevel = entity.level();
         if (!projectileLevel.isClientSide()) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                     AbstractArrow entityToSpawn = new HugeAIBulletEntity((EntityType<? extends HugeAIBulletEntity>)CrustyChunksModEntities.HUGE_AI_BULLET.get(), level) {
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
                     entityToSpawn.setOwner(shooter);
                     entityToSpawn.setBaseDamage((double)damage);
                     entityToSpawn.setSilent(true);
                     return entityToSpawn;
                  }
               })
               .getArrow(projectileLevel, entity, 0.1F, 1, (byte)3);
            _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
            _entityToSpawn.shoot(entity.getLookAngle().x, entity.getLookAngle().y, entity.getLookAngle().z, (float)(8.0 * mvmultiplier), 2.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:brtttfar")),
                  SoundSource.BLOCKS,
                  60.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4)
               );
            } else {
               _level.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:brtttfar")),
                  SoundSource.BLOCKS,
                  60.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4),
                  false
               );
            }
         }

         if (world instanceof Level _levelx) {
            if (!_levelx.isClientSide()) {
               _levelx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rac")),
                  SoundSource.BLOCKS,
                  10.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9)
               );
            } else {
               _levelx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rac")),
                  SoundSource.BLOCKS,
                  10.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9),
                  false
               );
            }
         }

         if (world instanceof Level _levelxx) {
            if (!_levelxx.isClientSide()) {
               _levelxx.playSound(
                  null,
                  BlockPos.containing(x, y, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:autocannonshot")),
                  SoundSource.BLOCKS,
                  8.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.5, 1.6)
               );
            } else {
               _levelxx.playLocalSound(
                  x,
                  y,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:autocannonshot")),
                  SoundSource.BLOCKS,
                  8.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 1.5, 1.6),
                  false
               );
            }
         }

         if (entity instanceof CIWSEntity) {
            ((CIWSEntity)entity).setAnimation("Fire");
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("CIWSGunProcedure.execute", _wtSafe);
      }
   }
}
