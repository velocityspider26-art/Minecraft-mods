package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.entity.ImpactGrenadeProjectileEntity;
import net.mcreator.crustychunks.entity.SmokeImpactGrenadeEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.mcreator.crustychunks.item.GrenadeLauncherItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class GrenadeLauncherFireProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      try {
      if (entity != null) {
         double Movementinnacuracy = 0.0;
         if (entity.isSprinting()) {
            Movementinnacuracy = 9.0;
         } else {
            Movementinnacuracy = 5.0;
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == ItemStack.EMPTY.getItem()) {
            if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Loaded")) {
               if (itemstack.getItem() instanceof GrenadeLauncherItem) {
                  CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("geckoAnim", "fire"));
               }

               if (0.0 == itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoType")) {
                  Level projectileLevel = entity.level();
                  if (!projectileLevel.isClientSide()) {
                     Projectile _entityToSpawn = (new Object() {
                           public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                              AbstractArrow entityToSpawn = new ImpactGrenadeProjectileEntity((EntityType<? extends ImpactGrenadeProjectileEntity>)CrustyChunksModEntities.IMPACT_GRENADE_PROJECTILE.get(), level) {
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
                        .getArrow(projectileLevel, entity, 3.0F, 1, (byte)5);
                     _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                     _entityToSpawn.shoot(entity.getLookAngle().x, entity.getLookAngle().y, entity.getLookAngle().z, 3.5F, (float)Movementinnacuracy);
                     projectileLevel.addFreshEntity(_entityToSpawn);
                  }
               } else if (1.0 == itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("AmmoType")) {
                  Level projectileLevel = entity.level();
                  if (!projectileLevel.isClientSide()) {
                     Projectile _entityToSpawn = (new Object() {
                           public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                              AbstractArrow entityToSpawn = new SmokeImpactGrenadeEntity((EntityType<? extends SmokeImpactGrenadeEntity>)CrustyChunksModEntities.SMOKE_IMPACT_GRENADE.get(), level) {
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
                        .getArrow(projectileLevel, entity, 3.0F, 1, (byte)5);
                     _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                     _entityToSpawn.shoot(entity.getLookAngle().x, entity.getLookAngle().y, entity.getLookAngle().z, 3.5F, (float)Movementinnacuracy);
                     projectileLevel.addFreshEntity(_entityToSpawn);
                  }
               }

               MortarFireSoundProcedure.execute(world, x, y, z);

               for (int index0 = 0; index0 < 25; index0++) {
                  world.addParticle(
                     (SimpleParticleType)CrustyChunksModParticleTypes.GUN_SMOKE.get(),
                     x + entity.getLookAngle().x * 1.0,
                     y + entity.getLookAngle().y + 1.5,
                     z + entity.getLookAngle().z * 1.0,
                     entity.getLookAngle().x * 0.3 + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                     entity.getLookAngle().y * 0.3 + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                     entity.getLookAngle().z * 0.3 + Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
                  );
               }

               entity.setYRot((float)((double)entity.getYRot() + Mth.nextDouble(RandomSource.create(), -8.0, 8.0)));
               entity.setXRot((float)((double)entity.getXRot() - Mth.nextDouble(RandomSource.create(), 12.0, 14.0)));
               entity.setYBodyRot(entity.getYRot());
               entity.setYHeadRot(entity.getYRot());
               entity.yRotO = entity.getYRot();
               entity.xRotO = entity.getXRot();
               if (entity instanceof LivingEntity _entity) {
                  _entity.yBodyRotO = _entity.getYRot();
                  _entity.yHeadRotO = _entity.getYRot();
               }

               CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putBoolean("Loaded", false));
            } else if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                     false
                  );
               }
            }
         } else if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(Component.literal("§4Weapon requires 2 hands to fire."), true);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("GrenadeLauncherFireProcedure.execute", _wtSafe);
      }
   }
}
