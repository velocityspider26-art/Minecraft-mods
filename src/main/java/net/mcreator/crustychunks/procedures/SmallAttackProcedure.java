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

public class SmallAttackProcedure {
   public static void execute(LevelAccessor world, double x, double z) {
      try {
      double Riflers = 0.0;
      double spawnx = 0.0;
      double spawnz = 0.0;
      double strikers = 0.0;
      double workers = 0.0;
      double breachers = 0.0;
      if (world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.APOCALYPSE_MODE)) {
         Riflers = Math.min(4.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers -= Riflers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         strikers = Math.min(4.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseStrikers);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseStrikers -= strikers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         workers = Math.min(2.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseWorkers);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseWorkers -= workers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         breachers = Math.min(2.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseBreachers);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseBreachers -= breachers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
      } else {
         strikers = 4.0;
         Riflers = 4.0;
         workers = 2.0;
         breachers = 2.0;
      }

      for (int index0 = 0; index0 < (int)Riflers; index0++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.RIFLER.get())
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

      for (int index1 = 0; index1 < (int)strikers; index1++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.STRIKER.get())
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

      for (int index2 = 0; index2 < (int)workers; index2++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.WORKER.get())
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

      for (int index3 = 0; index3 < (int)breachers; index3++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.BREACHER.get())
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

      if (world instanceof ServerLevel _level) {
         Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.SCOUT.get())
            .spawn(
               _level,
               BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 8), spawnz),
               MobSpawnType.MOB_SUMMONED
            );
         if (entityToSpawn != null) {
            entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("SmallAttackProcedure.execute", _wtSafe);
      }
   }
}
