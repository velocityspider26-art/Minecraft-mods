package shipwrights.genesis.space.physics;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.item.GenesisItems;
import shipwrights.genesis.teleportation.SpaceTravelManager;

/**
 * Shows the wearer's live orbital state while the orbit goggles are on — the
 * numbers that actually matter for flying an orbit: how fast you are going,
 * the speed that would hold a circular orbit here, the speed needed to escape,
 * and where your current path tops out and bottoms out.
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class OrbitalReadoutTicker {
    /** Refresh rate — fast enough to fly by, slow enough not to spam. */
    private static final int READOUT_INTERVAL_TICKS = 10;

    private OrbitalReadoutTicker() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !GenesisMod.isSpaceDimension(level)) {
            return;
        }
        if (level.getGameTime() % READOUT_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (!wearingGoggles(player)) {
                continue;
            }

            // Read the craft's motion when aboard one; otherwise the player's own.
            Vector3dc position;
            Vector3d velocity;
            ServerSubLevel ship = SpaceTravelManager.shipCarrying(level, player);
            if (ship != null) {
                RigidBodyHandle handle = RigidBodyHandle.of(ship);
                if (handle == null || !handle.isValid()) {
                    continue;
                }
                position = ship.logicalPose().position();
                velocity = handle.getLinearVelocity(new Vector3d());
            } else {
                position = new Vector3d(player.getX(), player.getY(), player.getZ());
                // Entity motion is blocks/tick; orbital maths works in m/s.
                velocity = new Vector3d(
                        player.getDeltaMovement().x * 20.0,
                        player.getDeltaMovement().y * 20.0,
                        player.getDeltaMovement().z * 20.0);
            }

            CelestialGravityWells.OrbitalState state =
                    CelestialGravityWells.orbitalState(level, position, velocity);
            if (state == null) {
                player.displayClientMessage(Component.literal("Deep space — no dominant body")
                        .withStyle(ChatFormatting.DARK_GRAY), true);
                continue;
            }

            player.displayClientMessage(format(state), true);
        }
    }

    private static Component format(CelestialGravityWells.OrbitalState state) {
        ChatFormatting colour = switch (state.trajectory()) {
            case CIRCULAR -> ChatFormatting.AQUA;
            case ELLIPTICAL -> ChatFormatting.GREEN;
            case SUBORBITAL -> ChatFormatting.RED;
            case ESCAPE -> ChatFormatting.LIGHT_PURPLE;
        };

        String detail = state.trajectory() == CelestialGravityWells.TrajectoryClass.ESCAPE
                ? String.format("v %.0f  esc %.0f  LEAVING", state.speed(), state.escapeSpeed())
                : String.format("v %.0f  circ %.0f  esc %.0f  ap %.0f  pe %.0f  e %.2f",
                        state.speed(), state.circularSpeed(), state.escapeSpeed(),
                        state.apoapsis(), state.periapsis(), state.eccentricity());

        return Component.literal(state.trajectory().name() + "  ").withStyle(colour)
                .append(Component.literal(detail).withStyle(ChatFormatting.GRAY));
    }

    private static boolean wearingGoggles(ServerPlayer player) {
        return isGoggles(player.getItemBySlot(EquipmentSlot.HEAD))
                || isGoggles(player.getMainHandItem())
                || isGoggles(player.getOffhandItem());
    }

    private static boolean isGoggles(ItemStack stack) {
        return !stack.isEmpty() && stack.is(GenesisItems.ORBIT_GOGGLES.get());
    }
}
