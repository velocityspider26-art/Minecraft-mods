package net.mcreator.crustychunks.procedures;

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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class AISuperSiegeProcedure {
   public static void execute(LevelAccessor world, double x, double z) {
      try {
      double Modules = 0.0;
      double spawnx = 0.0;
      double spawnz = 0.0;
      double Riflers = 0.0;
      double Commanders = 0.0;
      double Decimators = 0.0;
      double Mortarers = 0.0;
      double Flamers = 0.0;
      double Hunters = 0.0;
      double eradicators = 0.0;
      double reapers = 0.0;
      if (world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.APOCALYPSE_MODE)) {
         Riflers = Math.min((double)Mth.nextInt(RandomSource.create(), 3, 4), CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers -= Riflers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         Hunters = Math.min((double)Mth.nextInt(RandomSource.create(), 2, 3), CrustyChunksModVariables.MapVariables.get(world).ApocalypseHunters);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseHunters -= Hunters;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         Commanders = Math.min(3.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseCommanders);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseCommanders -= Commanders;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         Mortarers = Math.min((double)Mth.nextInt(RandomSource.create(), 2, 4), CrustyChunksModVariables.MapVariables.get(world).ApocalypseArtillery);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseArtillery -= Mortarers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         Decimators = Math.min((double)Mth.nextInt(RandomSource.create(), 1, 2), CrustyChunksModVariables.MapVariables.get(world).ApocalypseDecimators);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseDecimators -= Decimators;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         eradicators = Math.min(1.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseEradicators);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseEradicators -= eradicators;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         reapers = Math.min(2.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseReapers);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseReapers -= reapers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
      } else {
         reapers = 2.0;
         Riflers = (double)Mth.nextInt(RandomSource.create(), 3, 4);
         Hunters = (double)Mth.nextInt(RandomSource.create(), 2, 3);
         Commanders = 3.0;
         Mortarers = (double)Mth.nextInt(RandomSource.create(), 2, 4);
         Decimators = (double)Mth.nextInt(RandomSource.create(), 1, 2);
         Flamers = (double)Mth.nextInt(RandomSource.create(), 0, 1);
         eradicators = 1.0;
      }

      for (int index0 = 0; index0 < (int)Riflers; index0++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.RIFLER_POD.get())
               .spawn(
                  _level,
                  BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 350), spawnz),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }

      for (int index1 = 0; index1 < (int)Hunters; index1++) {
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

      for (int index2 = 0; index2 < (int)eradicators; index2++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         if (Mth.nextInt(RandomSource.create(), 1, 4) == 1) {
            if (world instanceof ServerLevel) {
               ServerLevel _level = (ServerLevel)world;
               Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.PROTOTYPE_ERADICATOR.get())
                  .spawn(
                     _level,
                     BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 2), spawnz),
                     MobSpawnType.MOB_SUMMONED
                  );
               if (entityToSpawn != null) {
                  entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
               }
            }
         } else if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.ERADICATOR.get())
               .spawn(
                  _level,
                  BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 2), spawnz),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }

      for (int index3 = 0; index3 < (int)Decimators; index3++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.DECIMATOR.get())
               .spawn(
                  _level,
                  BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 2), spawnz),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }

      for (int index4 = 0; index4 < (int)reapers; index4++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), 75, 100);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -100, -75);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.REAPER.get())
               .spawn(
                  _level,
                  BlockPos.containing(
                     spawnx,
                     (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + Mth.nextInt(RandomSource.create(), 35, 55)),
                     spawnz
                  ),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }

      for (int index5 = 0; index5 < (int)Mortarers; index5++) {
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
                  BlockPos.containing(spawnx, (double)world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 0.5, spawnz),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }

      for (int index6 = 0; index6 < Mth.nextInt(RandomSource.create(), 1, 2); index6++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -100, 100);
         if (world instanceof ServerLevel) {
            ServerLevel _levelx = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.SCOUT.get())
               .spawn(
                  _levelx,
                  BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 15), spawnz),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("AISuperSiegeProcedure.execute", _wtSafe);
      }
   }
}
