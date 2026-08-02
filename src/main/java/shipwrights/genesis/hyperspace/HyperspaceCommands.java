package shipwrights.genesis.hyperspace;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import shipwrights.genesis.GenesisMod;

@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class HyperspaceCommands {
    private HyperspaceCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("hyperspace")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("enter")
                        .executes(context -> enter(context, HyperspaceData.DEFAULT_SPEED, HyperspaceData.DEFAULT_DURATION_SECONDS))
                        .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.05, 8.0))
                                .executes(context -> enter(context, DoubleArgumentType.getDouble(context, "speed"), HyperspaceData.DEFAULT_DURATION_SECONDS))
                                .then(Commands.argument("durationSeconds", IntegerArgumentType.integer(0, 600))
                                        .executes(context -> enter(
                                                context,
                                                DoubleArgumentType.getDouble(context, "speed"),
                                                IntegerArgumentType.getInteger(context, "durationSeconds")
                                        )))))
                .then(Commands.literal("exit")
                        .executes(HyperspaceCommands::exit))
                .then(Commands.literal("status")
                        .executes(HyperspaceCommands::status))
                .then(Commands.literal("jump")
                        .then(Commands.argument("destination", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (HyperspaceDestination destination : HyperspaceDestination.values()) {
                                        builder.suggest(destination.id());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(HyperspaceCommands::jump))));
    }

    private static int jump(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        HyperspaceDestination destination = HyperspaceDestination.byId(StringArgumentType.getString(context, "destination"));
        if (destination == null) {
            context.getSource().sendFailure(Component.literal("Unknown destination"));
            return 0;
        }
        if (HyperspaceData.isActive(player)) {
            context.getSource().sendFailure(Component.literal("Already in hyperspace"));
            return 0;
        }

        HyperspaceData.beginSpool(player, destination, player.blockPosition(), HyperspaceData.SPOOL_TICKS);
        shipwrights.genesis.networking.GenesisNetworking.sendToPlayer(player,
                shipwrights.genesis.networking.HyperspaceStatePacket.preJump(HyperspaceData.SPOOL_TICKS));
        context.getSource().sendSuccess(() -> Component.literal("Hyperspace jump to " + destination.displayName()), true);
        return 1;
    }

    private static int enter(CommandContext<CommandSourceStack> context, double speed, int durationSeconds) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        HyperspaceData.enterCruise(player, speed, durationSeconds);
        context.getSource().sendSuccess(() -> Component.literal(
                durationSeconds > 0
                        ? "Hyperspace engaged: speed " + speed + " for " + durationSeconds + "s"
                        : "Hyperspace engaged: speed " + speed
        ), true);
        return 1;
    }

    private static int exit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        // Mid-jump inside the hyperspace dimension: drop out properly (with the
        // ship) at the current position instead of just clearing the state.
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level
                && level.dimension() == HyperspaceTicker.SUBSPACE_KEY
                && HyperspaceData.phase(player) == HyperspaceData.PHASE_TRANSIT) {
            HyperspaceTicker.requestManualExit(level, player);
            context.getSource().sendSuccess(() -> Component.literal("Dropping out of hyperspace"), true);
            return 1;
        }

        HyperspaceData.exit(player);
        player.setDeltaMovement(player.getDeltaMovement().scale(0.25));
        context.getSource().sendSuccess(() -> Component.literal("Hyperspace disengaged"), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!HyperspaceData.isActive(player)) {
            context.getSource().sendSuccess(() -> Component.literal("Hyperspace inactive"), false);
            return 1;
        }

        int durationTicks = HyperspaceData.durationTicks(player);
        String duration = durationTicks > 0
                ? ", remaining " + Math.max(0, (durationTicks - HyperspaceData.ticks(player)) / 20) + "s"
                : ", indefinite";
        context.getSource().sendSuccess(() -> Component.literal(
                "Hyperspace active: speed " + HyperspaceData.speed(player) + duration
        ), false);
        return 1;
    }
}
