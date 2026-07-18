package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map.Entry;
import net.mcreator.crustychunks.entity.HVParticleProjectileEntity;
import net.mcreator.crustychunks.entity.ParticleProjectileEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class HVParticleProjectileHitsBlockProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         BlockState impactblock = Blocks.AIR.defaultBlockState();
         impactblock = world.getBlockState(BlockPos.containing(x, y, z));
         DamagesProcedure.execute(world, x, y, z);
         if (impactblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:dirts"))) && world instanceof ServerLevel _level) {
            _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.DUST.get(), x + 0.5, y + 1.0, z + 0.5, 5, 0.0, 2.0, 0.0, 1.0);
         }

         if (impactblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:shatterable")))) {
            world.destroyBlock(BlockPos.containing(x, y, z), false);
            if (world instanceof Level _level) {
               if (!_level.isClientSide()) {
                  _level.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.glass.break")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     1.0F
                  );
               } else {
                  _level.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.glass.break")),
                     SoundSource.NEUTRAL,
                     3.0F,
                     1.0F,
                     false
                  );
               }
            }

            if ((immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) > 2.0
               && world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new HVParticleProjectileEntity((EntityType<? extends HVParticleProjectileEntity>)CrustyChunksModEntities.HV_PARTICLE_PROJECTILE.get(), level) {
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
                        entityToSpawn.setCritArrow(true);
                        return entityToSpawn;
                     }
                  })
                  .getArrow(projectileLevel, immediatesourceentity, 0.1F, 1, (byte)50);
               _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
               _entityToSpawn.shoot(
                  immediatesourceentity.getDeltaMovement().x(),
                  immediatesourceentity.getDeltaMovement().y(),
                  immediatesourceentity.getDeltaMovement().z(),
                  (float)((immediatesourceentity instanceof Projectile _projEntx ? _projEntx.getDeltaMovement().length() : 0.0) - 0.1),
                  0.8F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (impactblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:chippable")))) {
            world.destroyBlock(BlockPos.containing(x, y, z), false);
            if (world instanceof Level _levelx) {
               if (!_levelx.isClientSide()) {
                  _levelx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.zombie.break_wooden_door")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            if ((immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) > 2.0
               && world instanceof ServerLevel projectileLevel) {
               Projectile _entityToSpawn = (new Object() {
                     public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                        AbstractArrow entityToSpawn = new HVParticleProjectileEntity((EntityType<? extends HVParticleProjectileEntity>)CrustyChunksModEntities.HV_PARTICLE_PROJECTILE.get(), level) {
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
                        entityToSpawn.setCritArrow(true);
                        return entityToSpawn;
                     }
                  })
                  .getArrow(projectileLevel, immediatesourceentity, 0.1F, 1, (byte)50);
               _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
               _entityToSpawn.shoot(
                  immediatesourceentity.getDeltaMovement().x(),
                  immediatesourceentity.getDeltaMovement().y(),
                  immediatesourceentity.getDeltaMovement().z(),
                  (float)((immediatesourceentity instanceof Projectile _projEntx ? _projEntx.getDeltaMovement().length() : 0.0) - 0.1),
                  0.8F
               );
               projectileLevel.addFreshEntity(_entityToSpawn);
            }
         }

         if (impactblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:breakable_metal")))) {
            world.destroyBlock(BlockPos.containing(x, y, z), false);
            if (world instanceof Level _levelxx) {
               if (!_levelxx.isClientSide()) {
                  _levelxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                     SoundSource.NEUTRAL,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            if ((immediatesourceentity instanceof Projectile _projEnt ? _projEnt.getDeltaMovement().length() : 0.0) > 2.0) {
               for (int index0 = 0; index0 < Mth.nextInt(RandomSource.create(), 1, 3); index0++) {
                  if (world instanceof ServerLevel projectileLevel) {
                     Projectile _entityToSpawn = (new Object() {
                           public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
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
                              entityToSpawn.setOwner(shooter);
                              entityToSpawn.setBaseDamage((double)damage);
                              entityToSpawn.setSilent(true);
                              entityToSpawn.setCritArrow(true);
                              return entityToSpawn;
                           }
                        })
                        .getArrow(projectileLevel, immediatesourceentity, 0.1F, 1, (byte)50);
                     _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
                     _entityToSpawn.shoot(
                        immediatesourceentity.getDeltaMovement().x(),
                        immediatesourceentity.getDeltaMovement().y(),
                        immediatesourceentity.getDeltaMovement().z(),
                        (float)((immediatesourceentity instanceof Projectile _projEntx ? _projEntx.getDeltaMovement().length() : 0.0) - 0.1),
                        2.0F
                     );
                     projectileLevel.addFreshEntity(_entityToSpawn);
                  }
               }
            }
         }

         if (impactblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:splinterable")))) {
            world.destroyBlock(BlockPos.containing(x, y, z), false);
         }

         if (impactblock.is(BlockTags.create(ResourceLocation.parse("crusty_chunks:crushable")))) {
            if (world instanceof Level _levelxxx) {
               if (!_levelxxx.isClientSide()) {
                  _levelxxx.playSound(
                     null,
                     BlockPos.containing(x, y, z),
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.iron_golem.damage")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)(1.0 + Mth.nextDouble(RandomSource.create(), -0.2, 0.4))
                  );
               } else {
                  _levelxxx.playLocalSound(
                     x,
                     y,
                     z,
                     (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.iron_golem.damage")),
                     SoundSource.NEUTRAL,
                     2.0F,
                     (float)(1.0 + Mth.nextDouble(RandomSource.create(), -0.2, 0.4)),
                     false
                  );
               }
            }

            BlockPos _bp = BlockPos.containing(x, y, z);
            BlockState _bs = Blocks.GRAVEL.defaultBlockState();
            BlockState _bso = world.getBlockState(_bp);
            java.util.Iterator<Property<?>> var29 = _bso.getProperties().iterator();

            while (var29.hasNext()) {

               Property<?> entry_prop = var29.next();

               Property _property = _bs.getBlock().getStateDefinition().getProperty(entry_prop.getName());
               if (_property != null && _bs.getValue(_property) != null) {
                  try {
                     _bs = (BlockState)_bs.setValue(_property, _bso.getValue(entry_prop));
                  } catch (Exception var15) {
                  }
               }
            }

            world.setBlock(_bp, _bs, 3);
            if (world instanceof ServerLevel _levelxxxx) {
               _levelxxxx.sendParticles(ParticleTypes.POOF, x + 0.5, y + 0.5, z + 0.5, 3, 0.4, 0.4, 0.4, 0.2);
            }
         }

         CrackProcedureProcedure.execute(world, x, y, z);
         SmallBulletHitProcedure.execute(world, x, y, z);
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("HVParticleProjectileHitsBlockProcedure.execute", _wtSafe);
      }
   }
}
