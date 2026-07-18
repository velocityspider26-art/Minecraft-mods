package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.EradicatorTurretEntity;
import net.mcreator.crustychunks.entity.FireClientEffectEntity;
import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class AncientEradicatorDeathProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      try {
      if (entity != null) {
         ExplosionExampleProcedure.execute(world, x, y, z, 6.0);
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new FireClientEffectEntity((EntityType<? extends FireClientEffectEntity>)CrustyChunksModEntities.FIRE_CLIENT_EFFECT.get(), level) {
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
            _entityToSpawn.setPos(x, y, z);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, 1.0F, 0.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new EradicatorTurretEntity((EntityType<? extends EradicatorTurretEntity>)CrustyChunksModEntities.ERADICATOR_TURRET.get(), level) {
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
            _entityToSpawn.setPos(x, y + 2.0, z);
            _entityToSpawn.shoot(0.0, 1.0, 0.0, 2.0F, 25.0F);
            projectileLevel.addFreshEntity(_entityToSpawn);
         }

         if (!entity.level().isClientSide()) {
            entity.discard();
         }

         CrustyChunksMod.queueServerWork(
            60,
            () -> {
               for (int index0 = 0; index0 < 3; index0++) {
                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModBlocks.STEEL_BLOCK.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.HEAT_SHELL.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.HEAT_SHELL.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModBlocks.BATTLE_CANNON_BARREL.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.ADVANCED_COMPONENT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.ADVANCED_COMPONENT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.UNFABRICATED_TECH_COMPONENT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.UNFABRICATED_TECH_COMPONENT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.UNFABRICATED_TECH_COMPONENT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_GEAR.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.STEEL_COMPONENT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.CAST_COMPONENT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.ELECTRIC_MOTOR.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }

                  if (world instanceof ServerLevel _level) {
                     ItemEntity entityToSpawn = new ItemEntity(
                        _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.URANIUM_DEPLETED_INGOT.get())
                     );
                     entityToSpawn.setPickUpDelay(10);
                     _level.addFreshEntity(entityToSpawn);
                  }
               }

               if (world instanceof ServerLevel _level) {
                  ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModItems.MG_RECEIVER.get()));
                  entityToSpawn.setPickUpDelay(10);
                  entityToSpawn.setUnlimitedLifetime();
                  _level.addFreshEntity(entityToSpawn);
               }

               if (world instanceof ServerLevel _level) {
                  ItemEntity entityToSpawn = new ItemEntity(
                     _level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModBlocks.BATTLE_CANNON_BREECH.get())
                  );
                  entityToSpawn.setPickUpDelay(10);
                  _level.addFreshEntity(entityToSpawn);
               }

               if (world instanceof ServerLevel _level) {
                  ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModBlocks.STEEL_BLOCK.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _level.addFreshEntity(entityToSpawn);
               }

               if (world instanceof ServerLevel _level) {
                  ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModBlocks.STEEL_BLOCK.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _level.addFreshEntity(entityToSpawn);
               }

               if (world instanceof ServerLevel _level) {
                  ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModBlocks.STEEL_BLOCK.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _level.addFreshEntity(entityToSpawn);
               }

               if (world instanceof ServerLevel _level) {
                  ItemEntity entityToSpawn = new ItemEntity(_level, x + 0.5, y + 1.0, z + 0.5, new ItemStack((ItemLike)CrustyChunksModBlocks.STEEL_BLOCK.get()));
                  entityToSpawn.setPickUpDelay(10);
                  _level.addFreshEntity(entityToSpawn);
               }
            }
         );
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("AncientEradicatorDeathProcedure.execute", _wtSafe);
      }
   }
}
