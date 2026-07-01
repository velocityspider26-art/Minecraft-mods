package com.velocityspider.radarballistics.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocityspider.radarballistics.Config;
import com.velocityspider.radarballistics.FireControl;
import com.velocityspider.radarballistics.ballistics.FireSolution;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The {@code /radarballistics} command. It exists so the fire-control engine can be exercised
 * and eyeballed live in-game without needing a full cannon rig wired up:
 *
 * <ul>
 *   <li>{@code /radarballistics solve} &mdash; look at any entity and get the exact yaw,
 *       elevation, lead and time-of-flight a cannon at your eye would need to hit it,
 *       accounting for the target's current velocity.</li>
 *   <li>{@code /radarballistics profile} &mdash; print the projectile model the solver uses.</li>
 * </ul>
 */
public final class RadarBallisticsCommand {

    private static final double LOOK_REACH = 300.0;

    private RadarBallisticsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("radarballistics")
                .then(Commands.literal("solve")
                        .executes(ctx -> solveLookedAt(ctx.getSource())))
                .then(Commands.literal("profile")
                        .executes(ctx -> printProfile(ctx.getSource()))));
    }

    private static int printProfile(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Projectile model: ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format(
                        "muzzle=%.2f b/t, gravity=%.3f b/t², drag=%.3f/t | lead=%s",
                        Config.MUZZLE_SPEED.get(), Config.GRAVITY.get(), Config.DRAG.get(),
                        Config.LEAD_MOVING_TARGETS.get() ? "on" : "off"))
                        .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int solveLookedAt(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = eye.add(view.scale(LOOK_REACH));
        AABB searchBox = player.getBoundingBox().expandTowards(view.scale(LOOK_REACH)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eye, end, searchBox,
                e -> e != player && e.isAlive() && e.isPickable(),
                LOOK_REACH * LOOK_REACH);

        if (hit == null || hit.getEntity() == null) {
            source.sendFailure(Component.literal(
                    "No target in your crosshair (within " + (int) LOOK_REACH + " blocks)."));
            return 0;
        }

        Entity target = hit.getEntity();
        Vec3 aimAt = target.getBoundingBox().getCenter();
        FireSolution sol = FireControl.solveForEntity(eye, target);

        if (!sol.converged()) {
            source.sendFailure(Component.literal(String.format(
                    "%s is out of range for a %.2f b/t shell (dist %.1f blocks).",
                    target.getName().getString(), Config.MUZZLE_SPEED.get(), eye.distanceTo(aimAt))));
            return 0;
        }

        Vec3 tv = target.getDeltaMovement();
        double targetSpeed = tv.length();
        double leadDist = sol.aimPoint().distance(FireControl.toEngine(target.position()));

        source.sendSuccess(() -> Component.literal("Firing solution for ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.YELLOW)), false);
        source.sendSuccess(() -> line("  yaw",
                String.format("%.2f°", sol.yawDegrees())), false);
        source.sendSuccess(() -> line("  pitch",
                String.format("%.2f°  (elevation %.2f°)", sol.pitchDegrees(), sol.elevationDegrees())), false);
        source.sendSuccess(() -> line("  time of flight",
                String.format("%.1f ticks (%.2fs)", sol.timeToImpactTicks(), sol.timeToImpactTicks() / 20.0)), false);
        source.sendSuccess(() -> line("  target speed",
                String.format("%.3f b/t → led %.2f blocks ahead", targetSpeed, leadDist)), false);
        source.sendSuccess(() -> line("  impact angle",
                String.format("%.1f° below horizontal", sol.impactAngleDegrees())), false);
        source.sendSuccess(() -> line("  aim point",
                String.format("%.1f, %.1f, %.1f", sol.aimPoint().x(), sol.aimPoint().y(), sol.aimPoint().z())), false);
        return 1;
    }

    private static Component line(String label, String value) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }
}
