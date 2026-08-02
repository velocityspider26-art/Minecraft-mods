package shipwrights.genesis.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.teleportation.impl.EntityTeleporter;

/**
 * Convenience debug command for jumping between the genesis dimensions:
 *   /genesis goto overworld | space | subspace | moon | malachite
 *
 * Unlike vanilla /tp it needs no coordinates, and unlike the celestial /tp
 * branch it drops you into the actual destination dimension (e.g. onto the moon
 * surface), which makes it easy to inspect each planet's rendering.
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class GenesisCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> gotoNode = Commands.literal("goto")
                .then(destination("overworld", Level.OVERWORLD, 120))
                .then(destination("space", ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM), 96))
                .then(destination("subspace", ResourceKey.create(Registries.DIMENSION, GenesisMod.WORMHOLE_DIM), 96))
                .then(destination("moon", dim("moon"), 160))
                .then(destination("mercury", dim("mercury"), 160))
                .then(destination("malachite", dim("malachite"), 160));

        // /genesis hyperdrive class <0.1..6> — sets the class of the hyperdrive
        // block you are looking at (6 slowest, 0.5 fastest, 0.1 Falken-only).
        LiteralArgumentBuilder<CommandSourceStack> hyperdriveNode = Commands.literal("hyperdrive")
                .then(Commands.literal("class")
                        .then(Commands.argument("value",
                                        com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(
                                                shipwrights.genesis.hyperspace.HyperspaceData.MIN_DRIVE_CLASS,
                                                shipwrights.genesis.hyperspace.HyperspaceData.MAX_DRIVE_CLASS))
                                .executes(ctx -> setDriveClass(ctx.getSource(),
                                        com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "value")))));

        dispatcher.register(Commands.literal("genesis")
                .requires(src -> src.hasPermission(2))
                .then(gotoNode)
                .then(hyperdriveNode));
    }

    private static int setDriveClass(CommandSourceStack source, double value) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var hit = player.pick(8.0, 0.0f, false);
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)
                || !(player.level().getBlockEntity(blockHit.getBlockPos())
                        instanceof shipwrights.genesis.hyperspace.HyperdriveBlockEntity hyperdrive)) {
            source.sendFailure(Component.literal("Look at a hyperdrive block (within 8 blocks)"));
            return 0;
        }

        hyperdrive.setDriveClass(value);
        source.sendSuccess(() -> Component.literal(
                String.format("Hyperdrive class set to %.1f", hyperdrive.driveClass())), true);
        return 1;
    }

    private static ResourceKey<Level> dim(String path) {
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, path));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> destination(String name, ResourceKey<Level> dimension, int y) {
        return Commands.literal(name).executes(ctx -> teleport(ctx.getSource(), dimension, y));
    }

    private static int teleport(CommandSourceStack source, ResourceKey<Level> dimension, int y) throws CommandSyntaxException {
        Entity entity = source.getEntityOrException();
        ServerLevel target = source.getServer().getLevel(dimension);
        if (target == null) {
            source.sendFailure(Component.literal("Dimension not loaded: " + dimension.location()));
            return 0;
        }

        if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
            shipwrights.genesis.networking.GenesisNetworking.sendToPlayer(player,
                    shipwrights.genesis.networking.HyperspaceStatePacket.loadingHint());
        }

        Vec3 pos = new Vec3(entity.getX(), y, entity.getZ());
        EntityTeleporter.teleportEntityAndPassengers(entity, target, pos);

        source.sendSuccess(() -> Component.literal(
                "Teleported to " + dimension.location() + " (fly down to reach the surface)"), true);
        return 1;
    }
}
