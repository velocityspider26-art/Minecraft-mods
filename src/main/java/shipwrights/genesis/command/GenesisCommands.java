package shipwrights.genesis.command;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.teleportation.integration.PlanetToSpaceTeleporter;
import shipwrights.genesis.teleportation.integration.SpaceToPlanetTeleporter;

/**
 * /genesis space — jump straight into the Great Unknown from anywhere.
 * /genesis land  — drop from space into the nearest planet's atmosphere.
 * Available to all players (permission level 0) so space is always reachable.
 */
public final class GenesisCommands {

    private GenesisCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("genesis")
                .then(Commands.literal("space").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (GenesisMod.isSpaceDimension(player.level())) {
                        ctx.getSource().sendFailure(Component.literal("You are already in the Great Unknown."));
                        return 0;
                    }
                    if (!PlanetToSpaceTeleporter.sendToSpace(player)) {
                        ctx.getSource().sendFailure(Component.literal("The Great Unknown is not available in this world."));
                        return 0;
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("land").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!GenesisMod.isSpaceDimension(player.level())) {
                        ctx.getSource().sendFailure(Component.literal("You are not in space."));
                        return 0;
                    }
                    if (!SpaceToPlanetTeleporter.sendToNearestPlanet(player)) {
                        ctx.getSource().sendFailure(Component.literal("No planet found to land on."));
                        return 0;
                    }
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
