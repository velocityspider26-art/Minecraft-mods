package shipwrights.genesis.teleportation;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import shipwrights.genesis.GenesisMod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stops a player being dragged into the Sable plot "shadow region" when they log
 * in seated on a ship — the crash behind "connection lost" on loading a space
 * save.
 *
 * <p>A {@code create:seat} aboard a Sable ship is retained inside its sub-level,
 * so it physically lives at plot coordinates (~20 million blocks out), not at
 * the ship's visible position. When you log out riding one, the game also stores
 * it as your {@code RootVehicle} and, at the very end of {@code placeNewPlayer},
 * re-mounts you onto it — teleporting you into the plot region. Vanilla then
 * streams the client the chunks around you there, and the moment any of those
 * plot chunks needs dropping, Sable's client mixin throws rather than drop it
 * ({@code UnsupportedOperationException: Cannot drop chunks in plot}) and the
 * connection dies.</p>
 *
 * <p>The one clean seam is that {@code placeNewPlayer} adds the player to the
 * level (firing {@link EntityJoinLevelEvent}) <em>before</em> it does the vehicle
 * re-mount. So we flag the player as "just joined" there, and when the re-mount
 * fires {@link EntityMountEvent} into a plot-region seat during that window, we
 * cancel it. The player simply stays standing where they logged out, at the
 * ship's visible position — never entering the plot region, so no plot chunks
 * are ever sent. A normal in-world sit-down carries no join flag and is left
 * alone.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class PlotRegionRescue {
    /** Players currently inside the login placement window, by UUID. */
    private static final Set<UUID> JOINING = ConcurrentHashMap.newKeySet();

    private PlotRegionRescue() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // Fired from placeNewPlayer's addNewPlayer, before the RootVehicle
        // re-mount. Mark the window so the immediately-following mount can be
        // recognised as login re-attachment rather than a real sit-down.
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            JOINING.add(player.getUUID());
            // Safety valve: the flag must not outlive the placement. If no mount
            // follows, clear it on the next tick's worth of scheduling.
            player.server.execute(() -> JOINING.remove(player.getUUID()));
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.getLevel().isClientSide()
                || !event.isMounting()
                || !(event.getEntityMounting() instanceof ServerPlayer player)
                || !JOINING.contains(player.getUUID())) {
            return;
        }

        // Only cancel the re-mount when the seat is actually in the plot region;
        // a ship parked in normal space would re-seat harmlessly.
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || event.getEntityBeingMounted() == null
                || !container.inBounds(event.getEntityBeingMounted().blockPosition())) {
            return;
        }

        // Cancel the mount: the player keeps the visible position they logged out
        // at instead of being dragged to the seat's plot coordinates. They lose
        // the seat across the relog — a small price beside a hard crash.
        JOINING.remove(player.getUUID());
        event.setCanceled(true);
        GenesisMod.LOGGER.info("Kept {} out of the plot region on login (seat re-mount cancelled)",
                player.getGameProfile().getName());
    }
}
