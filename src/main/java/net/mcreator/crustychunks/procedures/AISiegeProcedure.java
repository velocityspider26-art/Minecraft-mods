package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.crustychunks.entity.CommanderPodEntity;
import net.mcreator.crustychunks.entity.RiflerPodEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModGameRules;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class AISiegeProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double Modules = 0.0;
      double spawnx = 0.0;
      double spawnz = 0.0;
      double Strikers = 0.0;
      double Riflers = 0.0;
      double Commanders = 0.0;
      double Hunters = 0.0;
      double Mortarers = 0.0;
      double Decimators = 0.0;
      double Flamers = 0.0;
      double Reapers = 0.0;
      if (world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.APOCALYPSE_MODE)) {
         Riflers = Math.min((double)Mth.nextInt(RandomSource.create(), 3, 4), CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers -= Riflers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         Hunters = Math.min((double)Mth.nextInt(RandomSource.create(), 1, 2), CrustyChunksModVariables.MapVariables.get(world).ApocalypseHunters);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseHunters -= Hunters;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         Commanders = Math.min(3.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseCommanders);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseCommanders -= Commanders;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         Mortarers = Math.min((double)Mth.nextInt(RandomSource.create(), 2, 4), CrustyChunksModVariables.MapVariables.get(world).ApocalypseArtillery);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseArtillery -= Mortarers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         Decimators = Math.min(1.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseDecimators);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseDecimators -= Decimators;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         Flamers = Math.min((double)Mth.nextInt(RandomSource.create(), 0, 1), CrustyChunksModVariables.MapVariables.get(world).ApocalypseDecimators);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseDecimators -= Flamers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         Reapers = Math.min(1.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseReapers);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseReapers -= Reapers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
      } else {
         Reapers = 1.0;
         Riflers = (double)Mth.nextInt(RandomSource.create(), 3, 4);
         Hunters = (double)Mth.nextInt(RandomSource.create(), 1, 2);
         Commanders = 3.0;
         Mortarers = (double)Mth.nextInt(RandomSource.create(), 2, 4);
         Decimators = 1.0;
         Flamers = (double)Mth.nextInt(RandomSource.create(), 0, 1);
      }

      for (int index0 = 0; index0 < (int)Hunters; index0++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.HUNTER.get())
               .spawn(
                  _level,
                  BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 45), spawnz),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }

      for (int index1 = 0; index1 < (int)Riflers; index1++) {
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
            }).getArrow(projectileLevel, 5.0F, 1);
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

      for (int index2 = 0; index2 < (int)Commanders; index2++) {
         if (world instanceof ServerLevel projectileLevel) {
            Projectile _entityToSpawn = (new Object() {
                  public Projectile getArrow(Level level, float damage, int knockback) {
                     AbstractArrow entityToSpawn = new CommanderPodEntity((EntityType<? extends CommanderPodEntity>)CrustyChunksModEntities.COMMANDER_POD.get(), level) {
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

      spawnx = x + (double)Mth.nextInt(RandomSource.create(), -100, 100);
      spawnz = z + (double)Mth.nextInt(RandomSource.create(), -100, 100);

      for (int index3 = 0; index3 < (int)Decimators; index3++) {
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.DECIMATOR.get())
               .spawn(
                  _level,
                  BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 450), spawnz),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }

      for (int index4 = 0; index4 < (int)Reapers; index4++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), 75, 100);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), 75, 100);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.REAPER.get())
               .spawn(
                  _level,
                  BlockPos.containing(
                     spawnx,
                     (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + Mth.nextInt(RandomSource.create(), 35, 45)),
                     spawnz
                  ),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }

      for (int index5 = 0; index5 < (int)Flamers; index5++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.FLAMER.get())
               .spawn(
                  _level,
                  BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 450), spawnz),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }

      for (int index6 = 0; index6 < (int)Mortarers; index6++) {
         if (Mth.nextInt(RandomSource.create(), 0, 1) == 1) {
            spawnx = x + (double)Mth.nextInt(RandomSource.create(), 100, 150);
         } else {
            spawnx = x + (double)Mth.nextInt(RandomSource.create(), -150, -100);
         }

         if (Mth.nextInt(RandomSource.create(), 0, 1) == 1) {
            spawnz = z + (double)Mth.nextInt(RandomSource.create(), 100, 150);
         } else {
            spawnz = z + (double)Mth.nextInt(RandomSource.create(), -150, -100);
         }

         if (world instanceof ServerLevel _level) {
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.MORTARER.get())
               .spawn(
                  _level,
                  BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 450), spawnz),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("AISiegeProcedure.execute", _wtSafe);
      }
   }
}
