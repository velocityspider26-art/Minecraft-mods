package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.entity.SmallBulletHPEntity;
import net.mcreator.crustychunks.entity.SmallBulletStealthEntity;
import net.mcreator.crustychunks.entity.SmallbulletfireProjectileEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.item.MachineCarbineItem;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.core.BlockPos;
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

public class MCFireScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      if (entity != null) {
         double Movementinnacuracy = 0.0;
         double recoil = 0.0;
         double mvmultiplier = 0.0;
         mvmultiplier = ProjectileLibsSmallArmsProcedure.execute();
         if (entity.isSprinting()) {
            Movementinnacuracy = 5.0;
         } else {
            Movementinnacuracy = 3.0;
         }

         if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
            .AimDownSights) {
            Movementinnacuracy = 0.3;
            recoil = Mth.nextDouble(RandomSource.create(), 0.2, 0.3);
         } else {
            recoil = Mth.nextDouble(RandomSource.create(), 0.4, 0.5);
         }

         if (0.0 != (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Firemode")
            || entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
               .clickrelease) {
            if ((entity instanceof LivingEntity _livEntx ? _livEntx.getOffhandItem() : ItemStack.EMPTY).getItem() == ItemStack.EMPTY.getItem()) {
               if (!(entity instanceof LivingEntity _livEntxx ? _livEntxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("action")) {
                  CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxx ? _livEntxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("action", true));
                  if (1.0 <= (entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")) {
                     if ((entity instanceof LivingEntity _livEntxxxxxx ? _livEntxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Loaded")
                        && 0.0 >= (entity instanceof LivingEntity _livEntxxxxx ? _livEntxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Cooldown")
                        )
                      {
                        if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                           .AimDownSights) {
                           if (itemstack.getItem() instanceof MachineCarbineItem) {
                              CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("geckoAnim", "sightfire"));
                           }
                        } else if (itemstack.getItem() instanceof MachineCarbineItem) {
                           CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("geckoAnim", "fire"));
                        }

                        if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("AP")) {
                           Level projectileLevel = entity.level();
                           if (!projectileLevel.isClientSide()) {
                              Projectile _entityToSpawn = (new Object() {
                                    public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                                       AbstractArrow entityToSpawn = new SmallbulletfireProjectileEntity((EntityType<? extends SmallbulletfireProjectileEntity>)CrustyChunksModEntities.SMALLBULLETFIRE_PROJECTILE.get(),
                                          level) {
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
                                 (float)(6.0 * mvmultiplier),
                                 (float)Movementinnacuracy
                              );
                              projectileLevel.addFreshEntity(_entityToSpawn);
                           }
                        } else if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("HP")) {
                           Level projectileLevel = entity.level();
                           if (!projectileLevel.isClientSide()) {
                              Projectile _entityToSpawn = (new Object() {
                                    public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                                       AbstractArrow entityToSpawn = new SmallBulletHPEntity((EntityType<? extends SmallBulletHPEntity>)CrustyChunksModEntities.SMALL_BULLET_HP.get(), level) {
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
                                 .getArrow(projectileLevel, entity, 0.1F, 0);
                              _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                              _entityToSpawn.shoot(
                                 entity.getLookAngle().x,
                                 entity.getLookAngle().y,
                                 entity.getLookAngle().z,
                                 (float)(6.0 * mvmultiplier),
                                 (float)Movementinnacuracy
                              );
                              projectileLevel.addFreshEntity(_entityToSpawn);
                           }
                        } else if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("Type").equals("ST")) {
                           Level projectileLevel = entity.level();
                           if (!projectileLevel.isClientSide()) {
                              Projectile _entityToSpawn = (new Object() {
                                    public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                                       AbstractArrow entityToSpawn = new SmallBulletStealthEntity((EntityType<? extends SmallBulletStealthEntity>)CrustyChunksModEntities.SMALL_BULLET_STEALTH.get(), level) {
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
                                 .getArrow(projectileLevel, entity, 0.1F, 0);
                              _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                              _entityToSpawn.shoot(
                                 entity.getLookAngle().x,
                                 entity.getLookAngle().y,
                                 entity.getLookAngle().z,
                                 (float)(6.0 * mvmultiplier),
                                 (float)Movementinnacuracy
                              );
                              projectileLevel.addFreshEntity(_entityToSpawn);
                           }
                        }

                        { final var _fvcc1 = (entity instanceof LivingEntity _livEntxxxxxxx ? _livEntxxxxxxx.getMainHandItem() : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Ammo")
                                 - 1.0; CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxx ? _livEntxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putDouble("Ammo", _fvcc1)); }
                        if (world instanceof ServerLevel _level) {
                           ItemEntity entityToSpawn = new ItemEntity(_level, x, y, z, new ItemStack((ItemLike)CrustyChunksModItems.SMALL_CASING.get()));
                           entityToSpawn.setPickUpDelay(10);
                           _level.addFreshEntity(entityToSpawn);
                        }

                        SmallCasingDropProcedure.execute(world, x, y, z);
                        PistolFireSoundProcedure.execute(world, x, y, z);
                        GunMechanismSoundProcedure.execute(world, x, y, z);
                        entity.setYRot((float)((double)entity.getYRot() + Mth.nextDouble(RandomSource.create(), -0.3, 0.3)));
                        entity.setXRot((float)((double)entity.getXRot() - recoil));
                        entity.setYBodyRot(entity.getYRot());
                        entity.setYHeadRot(entity.getYRot());
                        entity.yRotO = entity.getYRot();
                        entity.xRotO = entity.getXRot();
                        if (entity instanceof LivingEntity _entity) {
                           _entity.yBodyRotO = _entity.getYRot();
                           _entity.yHeadRotO = _entity.getYRot();
                        }

                        CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntxxxxxxxxx ? _livEntxxxxxxxxx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("action", false));
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

            boolean _setval = false;
            {
            CrustyChunksModVariables.PlayerVariables _vars = entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES);
               _vars.clickrelease = _setval;
               _vars.syncPlayerVariables(entity);
            
         }
         }
      }
   }
}
