package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.HVParticleProjectileEntity;
import net.mcreator.crustychunks.entity.HugeFragmentEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
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
import net.minecraft.world.level.block.entity.BlockEntity;

public class LargeSolidHitProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      if (immediatesourceentity != null) {
         double Power = 0.0;
         CrustyChunksMod.queueServerWork(3, () -> MicroExplosionProcedure.execute(world, x + 0.5, y + 0.5, z + 0.5));
         CrustyChunksMod.queueServerWork(
            1,
            () -> {
               if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.BATTLE_CANNON_BREECH.get() && (new Object() {
                  public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Loaded")) {
                  SmallExplosionProcedure.execute(world, x + 0.5, y + 0.5, z + 0.5);
                  world.destroyBlock(BlockPos.containing(x, y, z), false);
               }

               if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() == CrustyChunksModBlocks.ARTILLERYBREECH.get() && (new Object() {
                  public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
                     BlockEntity blockEntity = world.getBlockEntity(pos);
                     return blockEntity != null ? blockEntity.getPersistentData().getBoolean(tag) : false;
                  }
               }).getValue(world, BlockPos.containing(x, y, z), "Loaded")) {
                  MediumExplosionProcedure.execute(world, x + 0.5, y + 0.5, z + 0.5);
                  world.destroyBlock(BlockPos.containing(x, y, z), false);
               }

               DamagesProcedure.execute(world, x, y, z);
               if (world instanceof ServerLevel _level) {
                  _level.sendParticles(ParticleTypes.FLASH, x + 0.5, y + 0.5, z + 0.5, 5, 0.5, 0.5, 0.5, 0.05);
               }

               if (world instanceof ServerLevel _level) {
                  _level.sendParticles((SimpleParticleType)CrustyChunksModParticleTypes.HUGE_SPARKS.get(), x + 0.5, y + 0.5, z + 0.5, 40, 0.6, 0.6, 0.6, 1.2);
               }

               HugeFragmentHitProcedure.execute(world, x, y, z, immediatesourceentity);
               if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:pennable")))
                  || world.getBlockState(BlockPos.containing(x, y, z)).getDestroySpeed(world, BlockPos.containing(x, y, z)) < 5.0F
                     && world.getBlockState(BlockPos.containing(x, y, z)).getDestroySpeed(world, BlockPos.containing(x, y, z)) >= 0.0F) {
                  if (world instanceof Level _level) {
                     if (!_level.isClientSide()) {
                        _level.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:medium_small_explosion_distant")),
                           SoundSource.NEUTRAL,
                           20.0F,
                           0.8F
                        );
                     } else {
                        _level.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:medium_small_explosion_distant")),
                           SoundSource.NEUTRAL,
                           20.0F,
                           0.8F,
                           false
                        );
                     }
                  }

                  if (world.getBlockState(BlockPos.containing(x, y, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:metal")))
                     && world instanceof Level _levelx) {
                     if (!_levelx.isClientSide()) {
                        _levelx.playSound(
                           null,
                           BlockPos.containing(x, y, z),
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                           SoundSource.NEUTRAL,
                           6.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 0.5, 0.7)
                        );
                     } else {
                        _levelx.playLocalSound(
                           x,
                           y,
                           z,
                           (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.anvil.place")),
                           SoundSource.NEUTRAL,
                           6.0F,
                           (float)Mth.nextDouble(RandomSource.create(), 0.5, 0.7),
                           false
                        );
                     }
                  }

                  world.destroyBlock(BlockPos.containing(x, y, z), false);

                  for (int index0 = 0; index0 < 10; index0++) {
                     if (world instanceof ServerLevel projectileLevel) {
                        Projectile _entityToSpawn = (new Object() {
                              public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                                 AbstractArrow entityToSpawn = new HVParticleProjectileEntity((EntityType<? extends HVParticleProjectileEntity>)CrustyChunksModEntities.HV_PARTICLE_PROJECTILE.get(), level) {
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
                           .getArrow(projectileLevel, immediatesourceentity, 0.5F, 1);
                        _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
                        _entityToSpawn.shoot(
                           immediatesourceentity.getDeltaMovement().x(),
                           immediatesourceentity.getDeltaMovement().y(),
                           immediatesourceentity.getDeltaMovement().z(),
                           3.0F,
                           15.0F
                        );
                        projectileLevel.addFreshEntity(_entityToSpawn);
                     }
                  }

                  for (int index1 = 0; index1 < 10; index1++) {
                     if (world instanceof ServerLevel projectileLevel) {
                        Projectile _entityToSpawn = (new Object() {
                              public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
                                 AbstractArrow entityToSpawn = new HugeFragmentEntity((EntityType<? extends HugeFragmentEntity>)CrustyChunksModEntities.HUGE_FRAGMENT.get(), level) {
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
                           .getArrow(projectileLevel, immediatesourceentity, 3.0F, 1);
                        _entityToSpawn.setPos(immediatesourceentity.getX(), immediatesourceentity.getY(), immediatesourceentity.getZ());
                        _entityToSpawn.shoot(
                           immediatesourceentity.getDeltaMovement().x(),
                           immediatesourceentity.getDeltaMovement().y(),
                           immediatesourceentity.getDeltaMovement().z(),
                           3.0F,
                           10.0F
                        );
                        projectileLevel.addFreshEntity(_entityToSpawn);
                     }
                  }
               }

               if (!immediatesourceentity.level().isClientSide()) {
                  immediatesourceentity.discard();
               }
            }
         );
      }
   }
}
