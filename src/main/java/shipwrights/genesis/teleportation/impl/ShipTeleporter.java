package shipwrights.genesis.teleportation.impl;

import net.minecraft.server.level.ServerLevel;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsTeleportHelper;
import shipwrights.genesis.teleportation.TeleportData;

/**
 * Teleports a single construct using the Aeronautics compat layer. Replaces the VS
 * {@code shipWorld.teleportShip}/transform-provider velocity plumbing.
 */
public class ShipTeleporter {

    public static void teleportConstruct(AeronauticsConstruct construct, TeleportData data, ServerLevel newLevel) {
        AeronauticsTeleportHelper.teleportToLevel(
                construct, newLevel, data.newPos(), data.rotation(), data.velocity(), data.omega());
    }
}
