package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map.Entry;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.ParticleProjectileEntity;
import net.mcreator.crustychunks.entity.SmallClientEffectEntity;
import net.mcreator.crustychunks.entity.SplashEffectClientBypassEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class TinyExplosionProcedure {
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
               if (Math.sqrt(Math.pow(sx, 2.0) + Math.pow(sz, 2.0)) < 3.0
                  && world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz)).getBlock() == Blocks.GRASS_BLOCK) {
                  BlockPos _bp = BlockPos.containing(x + sx, y + sy, z + sz);
                  BlockState _bs = Blocks.DIRT.defaultBlockState();
                  BlockState _bso = world.getBlockState(_bp);
                  java.util.Iterator<Property<?>> var20 = _bso.getProperties().iterator();

                  while (var20.hasNext()) {

                     Property<?> entry_prop = var20.next();

                     Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
                     if (_property != null && _bs.getValue(_property) != null) {
                        try {
                           _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                        } catch (Exception var24) {
                        }
                     }
                  }

                  world.setBlock(_bp, _bs, 3);
               }

               sz++;
            }

            sy++;
         }

         sx++;
      }

      if (world.getFluidState(BlockPos.containing(x, y + 1.0, z)).createLegacyBlock().getBlock() instanceof LiquidBlock) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new SplashEffectClientBypassEntity((EntityType<? extends SplashEffectClientBypassEntity>)CrustyChunksModEntities.SPLASH_EFFECT_CLIENT_BYPASS.get(), level) {
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
               .getArrow(projectileLevel, 0.0F, 0);
            _entityToSpawn.setPos(x, y + 1.5, z);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.0F, 180.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (world instanceof Level _level && !_level.isClientSide()) {
            net.mcreator.crustychunks.compat.WariumExplosions.explode(_level, null, x + 0.0, y + 0.0, z + 0.0, 2.0F, ExplosionInteraction.TNT);
         }
      } else if (world instanceof ServerLevel projectileLevel) {
         Projectile _entityToSpawn = (new Object() {
               public Projectile getArrow(Level level, float damage, int knockback) {
                  AbstractArrow entityToSpawn = new SmallClientEffectEntity((EntityType<? extends SmallClientEffectEntity>)CrustyChunksModEntities.SMALL_CLIENT_EFFECT.get(), level) {
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
            .getArrow(projectileLevel, 0.0F, 0);
         _entityToSpawn.setPos(x, y + 1.5, z);
         _entityToSpawn.shoot(0.0, 1.0, 0.0, 0.0F, 180.0F);
         projectileLevel.addFreshEntity(_entityToSpawn);
      }

      if (world instanceof Level _level) {
         if (!_level.isClientSide()) {
            _level.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:medium_small_explosion_distant")),
               SoundSource.MASTER,
               60.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9)
            );
         } else {
            _level.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:medium_small_explosion_distant")),
               SoundSource.MASTER,
               60.0F,
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 0.9),
               false
            );
         }
      }

      if (world instanceof Level _levelx) {
         if (!_levelx.isClientSide()) {
            _levelx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
               SoundSource.MASTER,
               15.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.2)
            );
         } else {
            _levelx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
               SoundSource.MASTER,
               15.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.1, 1.2),
               false
            );
         }
      }

      if (world instanceof Level _levelxx) {
         if (!_levelxx.isClientSide()) {
            _levelxx.playSound(
               null,
               BlockPos.containing(x, y, z),
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
               SoundSource.MASTER,
               200.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.3, 1.4)
            );
         } else {
            _levelxx.playLocalSound(
               x,
               y,
               z,
               (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:farblast")),
               SoundSource.MASTER,
               200.0F,
               (float)Mth.nextDouble(RandomSource.create(), 1.3, 1.4),
               false
            );
         }
      }

      for (int index3 = 0; index3 < 100; index3++) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback, byte piercing) {
                     AbstractArrow entityToSpawn = new ParticleProjectileEntity((EntityType<? extends ParticleProjectileEntity>)CrustyChunksModEntities.PARTICLE_PROJECTILE.get(), level) {
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
               .getArrow(projectileLevel, 0.0F, 3, (byte)2);
            _entityToSpawn.setPos(x, y + 0.5, z);
            _entityToSpawn.shoot(
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
               0.2,
               Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
               (float)Mth.nextDouble(RandomSource.create(), 0.8, 1.2),
               45.0F
            );
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }

      CrustyChunksMod.queueServerWork(2, () -> {
         if (world instanceof Level _levelxxx && !_levelxxx.isClientSide()) {
            net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxx, null, x, y, z, 2.0F, ExplosionInteraction.TNT);
         }

         if (world instanceof Level _levelxxx && !_levelxxx.isClientSide()) {
            net.mcreator.crustychunks.compat.WariumExplosions.explode(_levelxxx, null, x + 0.0, y + 0.0, z + 0.0, 4.0F, ExplosionInteraction.NONE);
         }
      });
      if (world instanceof ServerLevel _levelxxx) {
         _levelxxx.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.5, 0.5, 0.5, 0.6);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("TinyExplosionProcedure.execute", _wtSafe);
      }
   }
}
