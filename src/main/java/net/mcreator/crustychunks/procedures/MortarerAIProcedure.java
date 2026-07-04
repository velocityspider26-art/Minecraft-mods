package net.mcreator.crustychunks.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.entity.ArtilleryWarningMarkerEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class MortarerAIProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         double rev = 0.0;
         double targetrange = 0.0;
         if (entity.getPersistentData().getDouble("T") > -1.0) {
            entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") - 1.0);
         }

         if ((entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null) != null) {
            AlivecheckProcedure.execute(entity);
            targetrange = Math.sqrt(
               Math.pow(Math.abs(y - (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getY()), 2.0)
                  + Math.pow(Math.abs(z - (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getZ()), 2.0)
                  + Math.pow(Math.abs(x - (entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null).getX()), 2.0)
            );
            entity.lookAt(
               Anchor.EYES,
               new Vec3(
                  x - (x - (entity instanceof Mob _mobEntxxxxx ? _mobEntxxxxx.getTarget() : null).getX()) / 2.0,
                  y + 186.0 - targetrange / 2.0,
                  z - (z - (entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.getTarget() : null).getZ()) / 2.0
               )
            );
            if (entity.getPersistentData().getDouble("T") <= -1.0 && 200.0 > targetrange && 25.0 < targetrange) {
               MortarerCannonProcedure.execute(world, x, y, z, entity);
               if (world instanceof ServerLevel projectileLevel) {
                  Projectile _entityToSpawn = (new Object() {
                        public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                           AbstractArrow entityToSpawn = new ArtilleryWarningMarkerEntity((EntityType<? extends ArtilleryWarningMarkerEntity>)CrustyChunksModEntities.ARTILLERY_WARNING_MARKER.get(), level) {
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
                     .getArrow(projectileLevel, entity, 0.0F, 0);
                  _entityToSpawn.setPos(
                     (entity instanceof Mob _mobEntxxxxxxxx ? _mobEntxxxxxxxx.getTarget() : null).getX(),
                     (entity instanceof Mob _mobEntxxxxxxx ? _mobEntxxxxxxx.getTarget() : null).getY() + 2.0,
                     (entity instanceof Mob _mobEntxxxxxx ? _mobEntxxxxxx.getTarget() : null).getZ()
                  );
                  _entityToSpawn.shoot(0.0, 0.0, 0.0, 0.0F, 0.0F);
                  projectileLevel.addFreshEntity(_entityToSpawn);
               }
            }

            if ((entity instanceof Mob _mobEntxxxxxx ? _mobEntxxxxxx.getTarget() : null)
                  .getType()
                  .is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))
               && entity instanceof Mob) {
               try {
                  ((Mob)entity).setTarget(null);
               } catch (Exception var16) {
                  var16.printStackTrace();
               }
            }
         }

         if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
               < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F) / 2.0F
            && 10 == Mth.nextInt(RandomSource.create(), 1, 15)) {
            world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x, y + 4.0, z, 0.0, 1.0, 0.0);
         }

         if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
            < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F) / 4.0F) {
            if (world instanceof ServerLevel _level) {
               _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SPARKS.get(), x, y + 2.0, z, 2, 1.0, 1.0, 1.0, 1.0);
            }

            if (10 == Mth.nextInt(RandomSource.create(), 1, 80) && world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                     SoundSource.NEUTRAL,
                     4.0F,
                     0.7F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                     SoundSource.NEUTRAL,
                     4.0F,
                     0.7F,
                     false
                  );
               }
            }
         }

         if (entity.getPersistentData().getDouble("T") > 0.0) {
            entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") - 1.0);
         }

         if (entity.getPersistentData().getDouble("T2") > 0.0) {
            entity.getPersistentData().putDouble("T2", entity.getPersistentData().getDouble("T2") - 1.0);
         }

         if (!entity.isInWater()
            && !entity.isNoGravity()
            && !world.canSeeSkyFromBelowWater(BlockPos.containing(x, y - 200.0, z))
            && world.canSeeSkyFromBelowWater(BlockPos.containing(x, y - 10.0, z))) {
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               entity.getDeltaMovement().x() * 2.0 + 1.0,
               entity.getDeltaMovement().y() * 3.0,
               entity.getDeltaMovement().z() * 2.0 + 1.0
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               entity.getDeltaMovement().x() * 2.0 - 1.0,
               entity.getDeltaMovement().y() * 3.0,
               entity.getDeltaMovement().z() * 2.0 + 1.0
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               entity.getDeltaMovement().x() * 2.0 + 1.0,
               entity.getDeltaMovement().y() * 3.0,
               entity.getDeltaMovement().z() * 2.0 - 1.0
            );
            world.addParticle(
               (SimpleParticleType)CrustyChunksModParticleTypes.JET_FLAME.get(),
               x,
               y,
               z,
               entity.getDeltaMovement().x() * 2.0 - 1.0,
               entity.getDeltaMovement().y() * 3.0,
               entity.getDeltaMovement().z() * 2.0 - 1.0
            );
            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                     SoundSource.NEUTRAL,
                     7.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4)
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                     SoundSource.NEUTRAL,
                     7.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4),
                     false
                  );
               }
            }

            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:jetfar")),
                     SoundSource.NEUTRAL,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4)
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:jetfar")),
                     SoundSource.NEUTRAL,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4),
                     false
                  );
               }
            }

            if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
               _entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0));
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("MortarerAIProcedure.execute", _wtSafe);
      }
   }
}
