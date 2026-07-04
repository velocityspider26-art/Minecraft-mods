package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.entity.APMediumBulletEntity;
import net.mcreator.crustychunks.entity.BulletfireProjectileEntity;
import net.mcreator.crustychunks.entity.StealthMediumBulletEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.mcreator.crustychunks.item.LeverRifleItem;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class LeverGunFireScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      try {
      if (entity != null) {
         double Movementinnacuracy = 0.0;
         double recoil = 0.0;
         double mvmultiplier = 0.0;
         mvmultiplier = ProjectileLibsSmallArmsProcedure.execute();
         if (entity.isSprinting()) {
            Movementinnacuracy = 3.0;
         } else {
            Movementinnacuracy = 0.7;
         }

         if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
            .AimDownSights) {
            Movementinnacuracy = 0.2;
            recoil = Mth.nextDouble(RandomSource.create(), 0.5, 1.0);
         } else {
            recoil = Mth.nextDouble(RandomSource.create(), 1.0, 2.0);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == ItemStack.EMPTY.getItem()) {
            if (!(entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("action")) {
               CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("action", true));
               if (1.0 <= (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                  if (0.0 >= (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Cooldown")) {
                     if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                        .AimDownSights) {
                        if ((entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof LeverRifleItem) {
                           CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "sightfire"));
                        }
                     } else if ((entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof LeverRifleItem
                        )
                      {
                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putString("geckoAnim", "fire"));
                     }

                     if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("AP")) {
                        Level projectileLevel = entity.level();
                        if (!projectileLevel.isClientSide()) {
                           Projectile _entityToSpawn = (new Object() {
                                 public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                                    AbstractArrow entityToSpawn = new APMediumBulletEntity((EntityType<? extends APMediumBulletEntity>)CrustyChunksModEntities.AP_MEDIUM_BULLET.get(), level) {
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
                              .getArrow(projectileLevel, entity, 0.1F, 0, (byte)4);
                           _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                           _entityToSpawn.shoot(
                              entity.getLookAngle().x,
                              entity.getLookAngle().y,
                              entity.getLookAngle().z,
                              (float)(8.0 * mvmultiplier),
                              (float)Movementinnacuracy
                           );
                           projectileLevel.addFreshEntity(_entityToSpawn);
                        }
                     } else if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("MB")) {
                        Level projectileLevel = entity.level();
                        if (!projectileLevel.isClientSide()) {
                           Projectile _entityToSpawn = (new Object() {
                                 public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                                    AbstractArrow entityToSpawn = new BulletfireProjectileEntity((EntityType<? extends BulletfireProjectileEntity>)CrustyChunksModEntities.BULLETFIRE_PROJECTILE.get(), level) {
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
                              .getArrow(projectileLevel, entity, 0.1F, 0, (byte)1);
                           _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                           _entityToSpawn.shoot(
                              entity.getLookAngle().x,
                              entity.getLookAngle().y,
                              entity.getLookAngle().z,
                              (float)(8.0 * mvmultiplier),
                              (float)Movementinnacuracy
                           );
                           projectileLevel.addFreshEntity(_entityToSpawn);
                        }
                     } else if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("ST")) {
                        Level projectileLevel = entity.level();
                        if (!projectileLevel.isClientSide()) {
                           Projectile _entityToSpawn = (new Object() {
                                 public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                                    AbstractArrow entityToSpawn = new StealthMediumBulletEntity((EntityType<? extends StealthMediumBulletEntity>)CrustyChunksModEntities.STEALTH_MEDIUM_BULLET.get(), level) {
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
                              .getArrow(projectileLevel, entity, 0.1F, 0, (byte)1);
                           _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                           _entityToSpawn.shoot(
                              entity.getLookAngle().x,
                              entity.getLookAngle().y,
                              entity.getLookAngle().z,
                              (float)(8.0 * mvmultiplier),
                              (float)Movementinnacuracy
                           );
                           projectileLevel.addFreshEntity(_entityToSpawn);
                        }
                     }

                     { final var _fvcc1 = (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo") - 1.0; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                     if (world instanceof ServerLevel _level) {
                        ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.MEDIUM_CASING.get()));
                        entityToSpawn.setPickUpDelay(10);
                        _level.addFreshEntity(entityToSpawn);
                     }

                     CasingDropProcedure.execute(world, x, y, z);
                     world.addParticle(
                        (SimpleParticleType)CrustyChunksModParticleTypes.GUN_SMOKE.get(),
                        x + entity.getLookAngle().x * 0.8,
                        y + (double)entity.getEyeHeight() - 0.1,
                        z + entity.getLookAngle().z * 0.8,
                        entity.getLookAngle().x * 0.2 + Mth.nextDouble(RandomSource.create(), -0.05, 0.05),
                        entity.getLookAngle().y * 0.2 + Mth.nextDouble(RandomSource.create(), -0.05, 0.05),
                        entity.getLookAngle().z * 0.2 + Mth.nextDouble(RandomSource.create(), -0.05, 0.05)
                     );
                     MediumFireSoundProcedure.execute(world, x, y, z);
                     entity.setYRot((float)((double)entity.getYRot() + Mth.nextDouble(RandomSource.create(), -1.0, 1.0)));
                     entity.setXRot((float)((double)entity.getXRot() - recoil));
                     entity.setYBodyRot(entity.getYRot());
                     entity.setYHeadRot(entity.getYRot());
                     entity.yRotO = entity.getYRot();
                     entity.xRotO = entity.getXRot();
                     if (entity instanceof LivingEntity _entity) {
                        _entity.yBodyRotO = _entity.getYRot();
                        _entity.yHeadRotO = _entity.getYRot();
                     }
                  }
               } else if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                        SoundSource.NEUTRAL,
                        1.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                        SoundSource.NEUTRAL,
                        1.0F,
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
         net.mcreator.crustychunks.compat.WariumSafety.report("LeverGunFireScriptProcedure.execute", _wtSafe);
      }
   }
}
