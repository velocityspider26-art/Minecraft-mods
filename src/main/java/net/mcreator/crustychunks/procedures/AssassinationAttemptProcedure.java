package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.entity.AssassinpodEntity;
import net.mcreator.crustychunks.entity.RiflerPodEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModGameRules;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class AssassinationAttemptProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      double Riflers = 0.0;
      double spawnx = 0.0;
      double spawnz = 0.0;
      double strikers = 0.0;
      double attempts = 0.0;
      if (world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.APOCALYPSE_MODE)) {
         if (CrustyChunksModVariables.MapVariables.get(world).ApocalypseWorkers > 3.0 && 1 == Mth.nextInt(RandomSource.create(), 1, 2)) {
            if (1 == Mth.nextInt(RandomSource.create(), 1, 2)) {
               spawnx = (double)Mth.nextInt(RandomSource.create(), -100, -75);
            } else {
               spawnx = (double)Mth.nextInt(RandomSource.create(), 75, 100);
            }

            if (1 == Mth.nextInt(RandomSource.create(), 1, 2)) {
               spawnz = (double)Mth.nextInt(RandomSource.create(), -100, -75);
            } else {
               spawnz = (double)Mth.nextInt(RandomSource.create(), 75, 100);
            }

            SmallAttackProcedure.execute(world, x + spawnx, z + spawnz);
         } else {
            label113: {
               if (world instanceof Level _lvl8 && _lvl8.isDay()) {
                  Riflers = Math.min(3.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers);
                  CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers -= Riflers;
                  CrustyChunksModVariables.MapVariables.get(world).syncData(world);
                  strikers = Math.min(3.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseStrikers);
                  CrustyChunksModVariables.MapVariables.get(world).ApocalypseStrikers -= strikers;
                  CrustyChunksModVariables.MapVariables.get(world).syncData(world);
                  break label113;
               }

               Riflers = 0.0;
               strikers = Math.min(6.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseStrikers);
               CrustyChunksModVariables.MapVariables.get(world).ApocalypseStrikers -= strikers;
               CrustyChunksModVariables.MapVariables.get(world).syncData(world);
            }
         }
      } else {
         label93: {
            if (world instanceof Level _lvl9 && _lvl9.isDay()) {
               Riflers = 3.0;
               strikers = 3.0;
               break label93;
            }

            Riflers = 0.0;
            strikers = 6.0;
         }
      }

      if (1 == Mth.nextInt(RandomSource.create(), 1, 3) && world.canSeeSkyFromBelowWater(BlockPos.containing(x, y, z))) {
         if (1 == Mth.nextInt(RandomSource.create(), 1, 2)) {
            spawnx = (double)Mth.nextInt(RandomSource.create(), -100, -75);
         } else {
            spawnx = (double)Mth.nextInt(RandomSource.create(), 75, 100);
         }

         if (1 == Mth.nextInt(RandomSource.create(), 1, 2)) {
            spawnz = (double)Mth.nextInt(RandomSource.create(), -100, -75);
         } else {
            spawnz = (double)Mth.nextInt(RandomSource.create(), 75, 100);
         }

         for (;
            world.getBlockState(BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING, Mth.floor(spawnx), Mth.floor(spawnz)) - 2), spawnz))
                  .getBlock() instanceof LiquidBlock
               || 10.0 < attempts;
            attempts++
         ) {
            if (1 == Mth.nextInt(RandomSource.create(), 1, 2)) {
               spawnx = (double)Mth.nextInt(RandomSource.create(), -100, -75);
            } else {
               spawnx = (double)Mth.nextInt(RandomSource.create(), 75, 100);
            }

            if (1 == Mth.nextInt(RandomSource.create(), 1, 2)) {
               spawnz = (double)Mth.nextInt(RandomSource.create(), -100, -75);
            } else {
               spawnz = (double)Mth.nextInt(RandomSource.create(), 75, 100);
            }
         }

         CrustyChunksModVariables.MapVariables.get(world).motivation = 0.0;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         SmallAttackProcedure.execute(world, x + spawnx, z + spawnz);
      } else {
         if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
               _level.playSound(
                  null,
                  BlockPos.containing(x, y + 300.0, z),
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sonicboom")),
                  SoundSource.NEUTRAL,
                  100.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1)
               );
            } else {
               _level.playLocalSound(
                  x,
                  y + 300.0,
                  z,
                  (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crusty_chunks:sonicboom")),
                  SoundSource.NEUTRAL,
                  100.0F,
                  (float)Mth.nextDouble(RandomSource.create(), 0.9, 1.1),
                  false
               );
            }
         }

         for (int index1 = 0; index1 < (int)Riflers; index1++) {
            CrustyChunksMod.queueServerWork(
               20,
               () -> {
                  if (world instanceof ServerLevel projectileLevel) {
                     Projectile _entityToSpawn = (new Object() {
                           public Projectile getArrow(Level level, float damage, int knockback) {
                              AbstractArrow entityToSpawn = new RiflerPodEntity((EntityType<? extends RiflerPodEntity>)CrustyChunksModEntities.RIFLER_POD.get(), level) {
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
                     _entityToSpawn.setPos(
                        x + (double)Mth.nextInt(RandomSource.create(), -30, 30), y + 350.0, z + (double)Mth.nextInt(RandomSource.create(), -30, 30)
                     );
                     _entityToSpawn.shoot(
                        Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                        Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                        Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                        1.0F,
                        25.0F
                     );
                     projectileLevel.addFreshEntity(_entityToSpawn);
                  }
               }
            );
         }

         for (int index2 = 0; index2 < (int)strikers; index2++) {
            CrustyChunksMod.queueServerWork(
               20,
               () -> {
                  if (world instanceof ServerLevel projectileLevel) {
                     Projectile _entityToSpawn = (new Object() {
                           public Projectile getArrow(Level level, float damage, int knockback) {
                              AbstractArrow entityToSpawn = new AssassinpodEntity((EntityType<? extends AssassinpodEntity>)CrustyChunksModEntities.ASSASSINPOD.get(), level) {
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
                     _entityToSpawn.setPos(
                        x + (double)Mth.nextInt(RandomSource.create(), -30, 30), y + 350.0, z + (double)Mth.nextInt(RandomSource.create(), -30, 30)
                     );
                     _entityToSpawn.shoot(
                        Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                        Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                        Mth.nextDouble(RandomSource.create(), -1.0, 1.0),
                        1.0F,
                        25.0F
                     );
                     projectileLevel.addFreshEntity(_entityToSpawn);
                  }
               }
            );
         }
      }
   }
}
