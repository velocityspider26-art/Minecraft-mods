package net.mcreator.crustychunks.procedures;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.mcreator.crustychunks.network.CrustyChunksModVariables;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class ApocalypseSetMultiplierProcedure {
   public static void execute(LevelAccessor world, CommandContext<CommandSourceStack> arguments, Entity entity) {
      if (entity != null) {
         double Riflers = 0.0;
         double Commanders = 0.0;
         double Decimators = 0.0;
         double spawnx = 0.0;
         double spawnz = 0.0;
         double Mortarers = 0.0;
         double Flamers = 0.0;
         double Hunters = 0.0;
         CrustyChunksModVariables.MapVariables.get(world).ApocalypseMultiplier = DoubleArgumentType.getDouble(arguments, "multiplier");
         CrustyChunksModVariables.MapVariables.get(world).syncData(world);
         if (entity instanceof Player _player && !_player.level().isClientSide()) {
            _player.displayClientMessage(Component.literal("Multiplier Set: " + DoubleArgumentType.getDouble(arguments, "multiplier")), false);
         }
      }
   }
}
