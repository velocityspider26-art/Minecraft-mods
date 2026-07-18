package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.LargeStealthBulletEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.mcreator.crustychunks.item.MusketItem;
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

public class MusketFireScriptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
      if (entity != null) {
         double Movementinnacuracy = 0.0;
         double mvmultiplier = 0.0;
         double misfire = 0.0;
         mvmultiplier = ProjectileLibsSmallArmsProcedure.execute();
         if (entity.isSprinting()) {
            Movementinnacuracy = 7.0;
         } else {
            Movementinnacuracy = 5.0;
         }

         if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
            .AimDownSights) {
            Movementinnacuracy = 3.0;
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == ItemStack.EMPTY.getItem()) {
            if (!itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("action")) {
               CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putBoolean("action", true));
               if (itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Loaded") && itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Charge") && 1.0 != itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("misfire")) {
                  if (world instanceof Level _level) {
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

                  if (world instanceof Level _levelx) {
                     if (!_levelx.isClientSide()) {
                        _levelx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                           SoundSource.NEUTRAL,
                           2.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                        );
                     } else {
                        _levelx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.firecharge.use")),
                           SoundSource.NEUTRAL,
                           2.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                           false
                        );
                     }
                  }

                  if (entity instanceof Player _player) {
                     _player.getCooldowns().addCooldown(itemstack.getItem(), 10);
                  }

                  if (world instanceof net.minecraft.server.level.ServerLevel _srvlvl) {
                     itemstack.hurtAndBreak(Mth.nextInt(RandomSource.create(), 1, 2), _srvlvl, null, _itmcns -> {});
                  }

                  if (entity.getData(CrustyChunksModVariables.PLAYER_VARIABLES)
                     .AimDownSights) {
                     if (itemstack.getItem() instanceof MusketItem) {
                        CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("geckoAnim", "sightfire"));
                     }
                  } else if (itemstack.getItem() instanceof MusketItem) {
                     CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putString("geckoAnim", "fire"));
                  }

                  { final var _fvccMi = Movementinnacuracy; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Innacuracy", _fvccMi)); }
                  { final var _fvccMv = mvmultiplier; CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putDouble("Velocity", 6.0 * _fvccMv)); }
                  CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putBoolean("Loaded", false));
                  CustomData.update(DataComponents.CUSTOM_DATA, itemstack, _tagupd -> _tagupd.putBoolean("Charge", false));
                  if (!itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Charge")) {
                     for (int index0 = 0; index0 < 20; index0++) {
                        world.addParticle(
                           (SimpleParticleType)CrustyChunksModParticleTypes.CAMP_SMOKE.get(),
                           x + entity.getLookAngle().x * 1.0,
                           y + entity.getLookAngle().y + 1.5,
                           z + entity.getLookAngle().z * 1.0,
                           entity.getLookAngle().x * 0.2 + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                           entity.getLookAngle().y * 0.2 + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                           entity.getLookAngle().z * 0.2 + Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
                        );
                        world.addParticle(
                           (SimpleParticleType)CrustyChunksModParticleTypes.BLACK_POWDER_SMOKE.get(),
                           x + entity.getLookAngle().x * 1.0,
                           y + entity.getLookAngle().y + 1.5,
                           z + entity.getLookAngle().z * 1.0,
                           entity.getLookAngle().x * 0.2 + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                           entity.getLookAngle().y * 0.2 + Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                           entity.getLookAngle().z * 0.2 + Mth.nextDouble(RandomSource.create(), -0.1, 0.1)
                        );
                     }
                  }

                  CrustyChunksMod.queueServerWork(
                     2,
                     () -> {
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
                              .getArrow(projectileLevel, entity, 1.0F, 1, (byte)1);
                           _entityToSpawn.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
                           _entityToSpawn.shoot(
                              entity.getLookAngle().x,
                              entity.getLookAngle().y,
                              entity.getLookAngle().z,
                              (float)itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Velocity"),
                              (float)itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("Innacuracy")
                           );
                           projectileLevel.addFreshEntity(_entityToSpawn);
                        }

                        ExtraLargeFireSoundProcedure.execute(world, x, y, z);
                        ShotgunFireSoundProcedure.execute(world, x, y, z);
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
                     }
                  );
               } else {
                  if (world instanceof Level _levelxx) {
                     if (!_levelxx.isClientSide()) {
                        _levelxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.flintandsteel.use")),
                           SoundSource.NEUTRAL,
                           2.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                        );
                     } else {
                        _levelxx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.flintandsteel.use")),
                           SoundSource.NEUTRAL,
                           2.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                           false
                        );
                     }
                  }

                  if (world instanceof Level _levelxxx) {
                     if (!_levelxxx.isClientSide()) {
                        _levelxxx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:dryfire")),
                           SoundSource.NEUTRAL,
                           2.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
                        );
                     } else {
                        _levelxxx.playLocalSound(
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

                  if (1.0 == itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("misfire")) {
                     if (entity instanceof Player _player && !_player.level().isClientSide()) {
                        _player.displayClientMessage(Component.literal("§4Misfired."), true);
                     }

                     if (world instanceof ServerLevel _srvlvl) itemstack.hurtAndBreak(2, _srvlvl, null, _itmcns -> {});
                  }
               }
            }
         } else if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(Component.literal("§4Weapon requires 2 hands to fire."), true);
         }
      }
   }
}
