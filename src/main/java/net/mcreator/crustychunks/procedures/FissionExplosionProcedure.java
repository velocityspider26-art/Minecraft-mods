package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.NuclearBlastEntityEntity;
import net.mcreator.crustychunks.entity.NuclearSecondaryEffectEntity;
import net.mcreator.crustychunks.entity.NuclearThermalRadEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.neoforged.fml.ModList;

public class FissionExplosionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      double xRadius = 0.0;
      double loop = 0.0;
      double zRadius = 0.0;
      double particleAmount = 0.0;
      boolean found = false;
      boolean space = false;
      if (("D:"
               + (
                  world instanceof Level _lvlxxx
                     ? _lvlxxx.dimension()
                     : (world instanceof WorldGenLevel _wglxxx ? _wglxxx.getLevel().dimension() : Level.OVERWORLD)
               ))
            .contains("orbit")
         || ("D:"
               + (world instanceof Level _lvlxx ? _lvlxx.dimension() : (world instanceof WorldGenLevel _wglxx ? _wglxx.getLevel().dimension() : Level.OVERWORLD)))
            .contains("solar_system")
         || ("D:" + (world instanceof Level _lvlx ? _lvlx.dimension() : (world instanceof WorldGenLevel _wglx ? _wglx.getLevel().dimension() : Level.OVERWORLD)))
            .contains("orbit")
         || ("D:" + (world instanceof Level _lvl ? _lvl.dimension() : (world instanceof WorldGenLevel _wgl ? _wgl.getLevel().dimension() : Level.OVERWORLD)))
            .contains("alpha_system")
         || y - 1000.0 > (double)world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)x, (int)z)
         || y > 2048.0) {
         space = true;
      }

      if (space) {
         CrustyChunksMod.queueServerWork(1, () -> SpaceFissionExplosionProcedure.execute(world, x, y, z));
      } else {
         if (!world.getBlockState(BlockPos.containing(x, y + 15.0, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:immortal")))) {
            world.destroyBlock(BlockPos.containing(x, y + 15.0, z), false);
         } else if (world.getBlockState(BlockPos.containing(x, y + 7.0, z)).is(BlockTags.create(ResourceLocation.parse("crusty_chunks:immortal")))) {
            world.destroyBlock(BlockPos.containing(x, y + 7.0, z), false);
         }

         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new NuclearThermalRadEntity((EntityType<? extends NuclearThermalRadEntity>)CrustyChunksModEntities.NUCLEAR_THERMAL_RAD.get(), level) {
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
               .getArrow(projectileLevel, 5.0F, 1);
            _entityToSpawn.setPos(x, y + 7.0, z);
            _entityToSpawn.shoot(0.0, 0.0, 0.0, 0.0F, 0.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new NuclearBlastEntityEntity((EntityType<? extends NuclearBlastEntityEntity>)CrustyChunksModEntities.NUCLEAR_BLAST_ENTITY.get(), level) {
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
               .getArrow(projectileLevel, 5.0F, 1);
            _entityToSpawn.setPos(x, y + 15.0, z);
            _entityToSpawn.shoot(0.0, 0.0, 0.0, 0.0F, 0.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (ModList.get().isLoaded("explosionoverhaul")) {
            if (world instanceof Level _level && !_level.isClientSide()) {
               _level.explode(null, x, y + 4.0, z, 127.0F, ExplosionInteraction.BLOCK);
            }
         } else {
            if (world instanceof Level _level && !_level.isClientSide()) {
               _level.explode(null, x, y + 4.0, z, 30.0F, ExplosionInteraction.BLOCK);
            }

            if (world instanceof Level _level && !_level.isClientSide()) {
               _level.explode(null, x, y + 4.0, z, 40.0F, ExplosionInteraction.NONE);
            }
         }

         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new NuclearSecondaryEffectEntity((EntityType<? extends NuclearSecondaryEffectEntity>)CrustyChunksModEntities.NUCLEAR_SECONDARY_EFFECT.get(), level) {
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
               .getArrow(projectileLevel, 5.0F, 1);
            _entityToSpawn.setPos(x, y + 7.0, z);
            _entityToSpawn.shoot(0.0, 0.0, 0.0, 0.0F, 0.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }
      }
   }
}
