package com.velocityspider.deathstar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.velocityspider.deathstar.config.DeathStarConfig;
import com.velocityspider.deathstar.structure.DeathStarAssembler;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Registers {@code /deathstar summon [radius]}. Op-only, because assembling tens of thousands of
 * blocks is not something you want any player triggering at will.
 */
public final class DeathStarCommand {

    private DeathStarCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deathstar")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("summon")
                        .executes(ctx -> summon(ctx.getSource(), DeathStarConfig.RADIUS.get()))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(8, 96))
                                .executes(ctx -> summon(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"))))));
    }

    private static int summon(CommandSourceStack source, int radius) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("message.deathstar.needs_player"));
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos center = centerInFrontOf(player, radius);

        DeathStarAssembler.Result result = DeathStarAssembler.summon(level, center, radius, level.getRandom().nextLong());
        boolean ok = result instanceof DeathStarAssembler.Result.Success;
        Component message = DeathStarAssembler.describe(result);
        if (ok) {
            source.sendSuccess(() -> message, true);
        } else {
            source.sendFailure(message);
        }
        return ok ? 1 : 0;
    }

    /**
     * Picks a spawn centre out in front of the player and lifts it so the sphere rests on the
     * terrain there (plus the configured drop height), rather than spawning on top of the player.
     */
    private static BlockPos centerInFrontOf(ServerPlayer player, int radius) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0e-4) {
            flat = new Vec3(0.0, 0.0, 1.0); // looking straight up/down: default to +Z
        }
        flat = flat.normalize();

        double distance = 2.0 * radius + 6.0;
        Vec3 eye = player.getEyePosition();
        int x = (int) Math.floor(eye.x + flat.x * distance);
        int z = (int) Math.floor(eye.z + flat.z * distance);

        // Rest the sphere on the terrain in front of the player: centre one radius above the ground.
        int groundY = player.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int y = groundY + radius + 1;
        return new BlockPos(x, y, z);
    }
}
