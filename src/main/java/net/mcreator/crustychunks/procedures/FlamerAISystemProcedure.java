package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class FlamerAISystemProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         double rev = 0.0;
         RandomVoicelinesProcedure.execute(world, x, y, z, entity);
         if (entity.getPersistentData().getDouble("T") > 0.0) {
            entity.getPersistentData().putDouble("T", entity.getPersistentData().getDouble("T") - 1.0);
         }

         if (entity.getPersistentData().getDouble("T2") > 0.0) {
            entity.getPersistentData().putDouble("T2", entity.getPersistentData().getDouble("T2") - 1.0);
         }

         if ((entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null) != null) {
            if (Mth.nextInt(RandomSource.create(), 1, 5) == 1) {
               AlivecheckProcedure.execute(entity);
               entity.lookAt(
                  Anchor.EYES,
                  new Vec3(
                     (entity instanceof Mob _mobEntxxx ? _mobEntxxx.getTarget() : null).getX(),
                     (entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null).getY() + 3.0,
                     (entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null).getZ()
                  )
               );
            }

            if (entity.getPersistentData().getDouble("T") <= 0.0) {
               DecimatorFlameThrowerProcedure.execute(world, x, y, z, entity);
            }

            if (Mth.nextInt(RandomSource.create(), 1, 100) == 1) {
               DecimatorMeleeProcedure.execute(world, x, y, z, entity);
            }

            if ((entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null)
                  .getType()
                  .is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))
               && entity instanceof Mob) {
               try {
                  ((Mob)entity).setTarget(null);
               } catch (Exception var15) {
                  var15.printStackTrace();
               }
            }
         } else if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
               < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F)
            && 1 == Mth.nextInt(RandomSource.create(), 1, 15)) {
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sparks")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     1.0F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sparks")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     1.0F,
                     false
                  );
               }
            }

            if (entity instanceof LivingEntity _entity) {
               _entity.setHealth((entity instanceof LivingEntity _livEntxx ? _livEntxx.getHealth() : -1.0F) + 1.0F);
            }
         }

         if (entity.getPersistentData().getDouble("Cycle") > 0.0) {
            entity.getPersistentData().putDouble("Cycle", entity.getPersistentData().getDouble("Cycle") - 1.0);
         } else {
            if (entity.getDeltaMovement().x() != 0.0 && entity.getDeltaMovement().z() != 0.0) {
               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:mechstep")),
                        SoundSource.NEUTRAL,
                        2.5F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.1)
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:mechstep")),
                        SoundSource.NEUTRAL,
                        2.5F,
                        (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.1),
                        false
                     );
                  }
               }

               if (world instanceof ServerLevel _levelxx) {
                  _levelxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.DUST.get(), x, y + 0.5, z, 7, 3.0, 0.0, 3.0, 0.1);
               }

               rev = 2.0;
            } else {
               rev = 0.0;
            }

            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:engine")),
                     SoundSource.NEUTRAL,
                     (float)(2.0 + rev),
                     (float)(Mth.nextDouble(RandomSource.create(), 0.5, 0.6) + rev)
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:engine")),
                     SoundSource.NEUTRAL,
                     (float)(2.0 + rev),
                     (float)(Mth.nextDouble(RandomSource.create(), 0.5, 0.6) + rev),
                     false
                  );
               }
            }

            entity.getPersistentData().putDouble("Cycle", 10.0);
         }

         if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
               < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F) / 2.0F
            && 10 == Mth.nextInt(RandomSource.create(), 1, 15)) {
            world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SMOKE.get(), x, y + 4.0, z, 0.0, 1.0, 0.0);
         }

         if ((entity instanceof LivingEntity _livEntx ? _livEntx.getHealth() : -1.0F)
            < (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1.0F) / 4.0F) {
            if (world instanceof ServerLevel _levelxxx) {
               _levelxxx.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SPARKS.get(), x, y + 2.0, z, 2, 1.0, 1.0, 1.0, 1.0);
            }

            if (10 == Mth.nextInt(RandomSource.create(), 1, 80) && world instanceof Level _levelxxx) {
               if (!_levelxxx.isClientSide()) {
                  _levelxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.item.break")),
                     SoundSource.NEUTRAL,
                     4.0F,
                     0.7F
                  );
               } else {
                  _levelxxx.playLocalSound(
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

         if (entity.isInWater()) {
            if (world instanceof Level _levelxxxx) {
               if (!_levelxxxx.isClientSide()) {
                  _levelxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.6, 0.8)
                  );
               } else {
                  _levelxxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.bucket.fill")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 0.6, 0.8),
                     false
                  );
               }
            }

            world.addParticle((SimpleParticleType)CrustyChunksModParticleTypes.SPLASH_PUFF.get(), x, y, z, 0.0, -1.0, 0.0);
            entity.push(entity.getLookAngle().x / 20.0, 0.0, entity.getLookAngle().z / 20.0);
         } else if (!entity.isNoGravity()
            && !entity.onGround()
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
            if (world instanceof Level _levelxxxxx) {
               if (!_levelxxxxx.isClientSide()) {
                  _levelxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:rocketflight")),
                     SoundSource.NEUTRAL,
                     7.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4)
                  );
               } else {
                  _levelxxxxx.playLocalSound(
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

            if (world instanceof Level _levelxxxxxx) {
               if (!_levelxxxxxx.isClientSide()) {
                  _levelxxxxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:jetfar")),
                     SoundSource.NEUTRAL,
                     20.0F,
                     (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.4)
                  );
               } else {
                  _levelxxxxxx.playLocalSound(
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
         net.mcreator.crustychunks.compat.WariumSafety.report("FlamerAISystemProcedure.execute", _wtSafe);
      }
   }
}
