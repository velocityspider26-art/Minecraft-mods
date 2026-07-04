package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.entity.LargeAPBulletEntity;
import net.mcreator.crustychunks.entity.LargeBulletFireProjectileEntity;
import net.mcreator.crustychunks.entity.LargeStealthBulletEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.mcreator.crustychunks.item.BoltActionRifleAnimatedItem;
import net.mcreator.crustychunks.item.ScopedBoltActionRifleAnimatedItem;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
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

public class BoltFireScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      try {
      if (entity != null) {
         double Movementinnacuracy = 0.0;
         double mvmultiplier = 0.0;
         mvmultiplier = ProjectileLibsSmallArmsProcedure.execute();
         if (entity.isSprinting()) {
            Movementinnacuracy = 5.0;
         } else {
            Movementinnacuracy = 2.0;
         }

         if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
            .AimDownSights) {
            Movementinnacuracy = 0.0;
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == ItemStack.EMPTY.getItem()) {
            if (0.0 >= itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("C") && !itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("action")) {
               CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putBoolean("action", true));
               if (1.0 <= itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("AP")) {
                     Level projectileLevel = entity.level();
                     if (!projectileLevel.isClientSide()) {
                        Projectile _entityToSpawn = (new Object() {
                              public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                                 AbstractArrow entityToSpawn = new LargeAPBulletEntity((EntityType<? extends LargeAPBulletEntity>)CrustyChunksModEntities.LARGE_AP_BULLET.get(), level) {
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
                           .getArrow(projectileLevel, entity, 0.1F, 0, (byte)5);
                        _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                        _entityToSpawn.shoot(
                           entity.getLookAngle().x,
                           entity.getLookAngle().y,
                           entity.getLookAngle().z,
                           (float)(8.5 * mvmultiplier),
                           (float)Movementinnacuracy
                        );
                        projectileLevel.addFreshEntity(_entityToSpawn);
                     }
                  } else if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("MB")) {
                     Level projectileLevel = entity.level();
                     if (!projectileLevel.isClientSide()) {
                        Projectile _entityToSpawn = (new Object() {
                              public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                                 AbstractArrow entityToSpawn = new LargeBulletFireProjectileEntity((EntityType<? extends LargeBulletFireProjectileEntity>)CrustyChunksModEntities.LARGE_BULLET_FIRE_PROJECTILE.get(), level) {
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
                           .getArrow(projectileLevel, entity, 0.1F, 0, (byte)2);
                        _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                        _entityToSpawn.shoot(
                           entity.getLookAngle().x,
                           entity.getLookAngle().y,
                           entity.getLookAngle().z,
                           (float)(8.5 * mvmultiplier),
                           (float)Movementinnacuracy
                        );
                        projectileLevel.addFreshEntity(_entityToSpawn);
                     }
                  } else if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("ST")) {
                     Level projectileLevel = entity.level();
                     if (!projectileLevel.isClientSide()) {
                        Projectile _entityToSpawn = (new Object() {
                              public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                                 AbstractArrow entityToSpawn = new LargeStealthBulletEntity((EntityType<? extends LargeStealthBulletEntity>)CrustyChunksModEntities.LARGE_STEALTH_BULLET.get(), level) {
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
                           .getArrow(projectileLevel, entity, 0.1F, 0, (byte)2);
                        _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                        _entityToSpawn.shoot(
                           entity.getLookAngle().x,
                           entity.getLookAngle().y,
                           entity.getLookAngle().z,
                           (float)(8.5 * mvmultiplier),
                           (float)Movementinnacuracy
                        );
                        projectileLevel.addFreshEntity(_entityToSpawn);
                     }
                  }

                  { final var _fvcc1 = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") - 1.0; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                  CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putBoolean("Casing", true));
                  LargeFireSoundProcedure.execute(world, x, y, z);

                  for (int index0 = 0; index0 < 3; index0++) {
                     world.addParticle(
                        (SimpleParticleType)CrustyChunksModParticleTypes.GUN_SMOKE.get(),
                        x + entity.getLookAngle().x * 0.8,
                        y + (double)entity.getEyeHeight() - 0.1,
                        z + entity.getLookAngle().z * 0.8,
                        entity.getLookAngle().x * 0.2 + Mth.nextDouble(RandomSource.create(), -0.05, 0.05),
                        entity.getLookAngle().y * 0.2 + Mth.nextDouble(RandomSource.create(), -0.05, 0.05),
                        entity.getLookAngle().z * 0.2 + Mth.nextDouble(RandomSource.create(), -0.05, 0.05)
                     );
                  }

                  entity.setYRot((float)((double)entity.getYRot() + Mth.nextDouble(RandomSource.create(), -2.0, 2.0)));
                  entity.setXRot((float)((double)entity.getXRot() - Mth.nextDouble(RandomSource.create(), 3.0, 4.0)));
                  entity.setYBodyRot(entity.getYRot());
                  entity.setYHeadRot(entity.getYRot());
                  entity.yRotO = entity.getYRot();
                  entity.xRotO = entity.getXRot();
                  if (entity instanceof LivingEntity _entity) {
                     _entity.yBodyRotO = _entity.getYRot();
                     _entity.yHeadRotO = _entity.getYRot();
                  }

                  if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                     .AimDownSights) {
                     if (itemstack.getItem() instanceof BoltActionRifleAnimatedItem) {
                        CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("geckoAnim", "sightfire"));
                     }
                  } else if (itemstack.getItem() instanceof BoltActionRifleAnimatedItem) {
                     CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("geckoAnim", "fire"));
                  }

                  if (itemstack.getItem() instanceof ScopedBoltActionRifleAnimatedItem) {
                     CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("geckoAnim", "fire"));
                  }
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
            }
         } else if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(Component.literal("§4Weapon requires 2 hands to fire."), true);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("BoltFireScriptProcedure.execute", _wtSafe);
      }
   }
}
