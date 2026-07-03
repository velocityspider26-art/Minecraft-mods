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

public class AirRaidProcedure {
   public static void execute(LevelAccessor world, double x, double z) {
      double Riflers = 0.0;
      double spawnx = 0.0;
      double spawnz = 0.0;
      double strikers = 0.0;
      double workers = 0.0;
      double reapers = 0.0;
      double hunters = 0.0;
      if (world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.APOCALYPSE_MODE)) {
         reapers = Math.min(1.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseReapers);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseReapers -= reapers;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         hunters = Math.min(1.0, CrustyChunksModVariables.MapVariables.get(world).ApocalypseHunters);
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseHunters -= hunters;
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
      } else {
         reapers = 1.0;
         hunters = 1.0;
      }

      for (int index0 = 0; index0 < (int)reapers; index0++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.REAPER.get())
               .spawn(
                  _level,
                  BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 20), spawnz),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }

      for (int index1 = 0; index1 < (int)hunters; index1++) {
         spawnx = x + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         spawnz = z + (double)Mth.nextInt(RandomSource.create(), -25, 25);
         if (world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            Entity entityToSpawn = ((EntityType)CrustyChunksModEntities.HUNTER.get())
               .spawn(
                  _level,
                  BlockPos.containing(spawnx, (double)(world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnx, (int)spawnz) + 10), spawnz),
                  MobSpawnType.MOB_SUMMONED
               );
            if (entityToSpawn != null) {
               entityToSpawn.setDeltaMovement(0.0, 0.0, 0.0);
            }
         }
      }
   }
}
