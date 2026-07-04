package net.mcreator.crustychunks.procedures;

import java.text.DecimalFormat;
import net.mcreator.crustychunks.init.CrustyChunksModGameRules;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class ApocalypseStatsDeclareProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      try {
      if (entity != null) {
         double Riflers = 0.0;
         double Commanders = 0.0;
         double Decimators = 0.0;
         double spawnx = 0.0;
         double spawnz = 0.0;
         double Mortarers = 0.0;
         double Flamers = 0.0;
         double Hunters = 0.0;
         if (world.getLevelData().getGameRules().getBoolean(CrustyChunksModGameRules.APOCALYPSE_MODE)) {
            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal("Strikers:" + new DecimalFormat("####").format(CrustyChunksModVariables.MapVariables.get(world).ApocalypseStrikers)),
                  false
               );
            }

            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal("Riflers:" + new DecimalFormat("####").format(CrustyChunksModVariables.MapVariables.get(world).ApocalypseRiflers)), false
               );
            }

            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal("Commanders:" + new DecimalFormat("####").format(CrustyChunksModVariables.MapVariables.get(world).ApocalypseCommanders)),
                  false
               );
            }

            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal("Hunters:" + new DecimalFormat("####").format(CrustyChunksModVariables.MapVariables.get(world).ApocalypseHunters)), false
               );
            }

            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal("Workers:" + new DecimalFormat("####").format(CrustyChunksModVariables.MapVariables.get(world).ApocalypseWorkers)), false
               );
            }

            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal("Decimators:" + new DecimalFormat("####").format(CrustyChunksModVariables.MapVariables.get(world).ApocalypseDecimators)),
                  false
               );
            }

            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal("Eradicators:" + new DecimalFormat("####").format(CrustyChunksModVariables.MapVariables.get(world).ApocalypseEradicators)),
                  false
               );
            }

            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal("Reapers:" + new DecimalFormat("####").format(CrustyChunksModVariables.MapVariables.get(world).ApocalypseReapers)), false
               );
            }

            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal("Motivation" + new DecimalFormat("####").format(CrustyChunksModVariables.MapVariables.get(world).motivation)), false
               );
            }

            if (entity instanceof Player _player && !_player.level().isClientSide()) {
               _player.displayClientMessage(
                  Component.literal("Production:" + new DecimalFormat("####").format(CrustyChunksModVariables.MapVariables.get(world).Production)), false
               );
            }
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("ApocalypseStatsDeclareProcedure.execute", _wtSafe);
      }
   }
}
