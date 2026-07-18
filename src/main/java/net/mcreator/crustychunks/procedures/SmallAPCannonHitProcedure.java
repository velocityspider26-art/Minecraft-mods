package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.HVParticleProjectileEntity;
import net.mcreator.crustychunks.entity.HugeFragmentEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
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

public class SmallAPCannonHitProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         double Power = 0.0;
         CrustyChunksMod.queueServerWork(
            1,
            () -> {
               if (!world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:variablearmor")))
                  && world.getBlockState(BlockPos.containing(x, y, z)).getDestroySpeed(world, BlockPos.containing(x, y, z)) < 5.0F
                  && world.getBlockState(BlockPos.containing(x, y, z)).getDestroySpeed(world, BlockPos.containing(x, y, z)) >= 0.0F) {
                  world.destroyBlock(BlockPos.containing(x, y, z), false);
               } else {
                  for (int index0 = 0; index0 < 2; index0++) {
                     ArmorDegradeProcedure.execute(world, x, y, z);
                  }
               }

               for (int index1 = 0; index1 < 10; index1++) {
                  if (world instanceof ServerLevel projectileLevel) {
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
                        .getArrow(projectileLevel, immediatesourceentity, 5.0F, 1, (byte)2);
                     _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
                     _entityToSpawn.shoot(
                        immediatesourceentity.getDeltaMovement().x(),
                        immediatesourceentity.getDeltaMovement().y(),
                        immediatesourceentity.getDeltaMovement().z(),
                        5.0F,
                        9.0F
                     );
                     projectileLevel.addFreshEntity(_entityToSpawn);
                  }
               }

               DamagesProcedure.execute(world, x, y, z);
               HeavyCrackProcedureProcedure.execute(world, x, y, z);

               for (int index2 = 0; index2 < 3; index2++) {
                  if (world instanceof ServerLevel projectileLevel) {
                     Projectile _entityToSpawn = (new Object() {
                           public Projectile getArrow(Level level, Entity shooter, float damage, int knockback, byte piercing) {
                              AbstractArrow entityToSpawn = new HugeFragmentEntity((EntityType<? extends HugeFragmentEntity>)CrustyChunksModEntities.HUGE_FRAGMENT.get(), level) {
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
                        .getArrow(projectileLevel, immediatesourceentity, 5.0F, 1, (byte)2);
                     _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
                     _entityToSpawn.shoot(
                        immediatesourceentity.getDeltaMovement().x(),
                        immediatesourceentity.getDeltaMovement().y(),
                        immediatesourceentity.getDeltaMovement().z(),
                        5.0F,
                        5.0F
                     );
                     projectileLevel.addFreshEntity(_entityToSpawn);
                  }
               }

               if (world instanceof Level _level) {
                  if (!_level.isClientSide()) {
                     _level.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
                        SoundSource.BLOCKS,
                        5.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3)
                     );
                  } else {
                     _level.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:smallexplosion")),
                        SoundSource.BLOCKS,
                        5.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3),
                        false
                     );
                  }
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.isClientSide()) {
                     _levelx.playSound(
                        null,
                        BlockPos.containing(x, y, z),
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:tinyexplosionfar")),
                        SoundSource.BLOCKS,
                        20.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3)
                     );
                  } else {
                     _levelx.playLocalSound(
                        x,
                        y,
                        z,
                        (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:tinyexplosionfar")),
                        SoundSource.BLOCKS,
                        20.0F,
                        (float)Mth.nextDouble(RandomSource.create(), 1.2, 1.3),
                        false
                     );
                  }
               }

               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            }
         );
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SmallAPCannonHitProcedure.execute", _wtSafe);
      }
   }
}
