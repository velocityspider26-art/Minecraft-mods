package shipwrights.genesis.teleportation;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Keeps a seated player seated across a dimension change.
 *
 * <p>A seat aboard a craft is an entity, and the warp rebuilds the craft and its
 * entities in the destination. The player survives the move, and so does a seat
 * — but they arrive as strangers: whatever the player was riding no longer
 * exists under that identity, so they land standing next to the chair they were
 * sitting in.</p>
 *
 * <p>Riding is therefore recorded before the move and re-established after it.
 * The seat cannot be matched by identity for the same reason, so it is matched
 * by where it ended up: the craft carries its seats with it, so the seat nearest
 * the arrived craft is the one the player was in.</p>
 */
public final class RidingPreservation {
    /** How far from the craft to look for the seat a player was riding. */
    private static final double SEAT_SEARCH_RADIUS = 48.0;

    private RidingPreservation() {
    }

    /** A player who was riding something when their craft departed. */
    public record SeatedRider(UUID playerId, Vec3 offsetFromCraft) {
    }

    /** Records which crew were seated, and where their seat sat on the craft. */
    public static List<SeatedRider> snapshot(ServerLevel originLevel, ServerSubLevel craft,
                                             List<ServerPlayer> crew) {
        List<SeatedRider> seated = new ArrayList<>();
        try {
            Vector3dc craftPos = craft.logicalPose().position();
            for (ServerPlayer player : crew) {
                Entity vehicle = player.getVehicle();
                if (vehicle == null) {
                    continue;
                }
                seated.add(new SeatedRider(player.getUUID(), vehicle.position().subtract(
                        craftPos.x(), craftPos.y(), craftPos.z())));
            }
        } catch (Throwable t) {
            GenesisMod.LOGGER.warn("[SEAT] could not record riding state", t);
        }
        return seated;
    }

    /**
     * Puts recorded riders back in their seats aboard the arrived craft.
     *
     * <p>Deferred a tick: the craft and its seats have to exist in the
     * destination before anyone can be mounted on them.</p>
     */
    public static void restore(ServerLevel targetLevel, Vec3 arrival, List<SeatedRider> seated,
                               List<UUID> arrivedShipIds) {
        if (seated.isEmpty()) {
            return;
        }
        targetLevel.getServer().execute(() -> {
            try {
                ServerSubLevel arrived =
                        SpaceTravelManager.nearestShip(targetLevel, arrival, 128.0, arrivedShipIds);
                if (arrived == null) {
                    return;
                }
                Vector3dc craftPos = arrived.logicalPose().position();

                for (SeatedRider rider : seated) {
                    ServerPlayer player = targetLevel.getServer().getPlayerList().getPlayer(rider.playerId());
                    if (player == null || player.level() != targetLevel || player.isPassenger()) {
                        continue;
                    }

                    // Where the seat should now be, given the craft moved.
                    Vec3 expected = new Vec3(
                            craftPos.x() + rider.offsetFromCraft().x,
                            craftPos.y() + rider.offsetFromCraft().y,
                            craftPos.z() + rider.offsetFromCraft().z);

                    Entity seat = nearestRideable(targetLevel, expected);
                    if (seat == null) {
                        continue;
                    }
                    player.teleportTo(targetLevel, seat.getX(), seat.getY(), seat.getZ(),
                            player.getYRot(), player.getXRot());
                    player.startRiding(seat, true);
                    GenesisMod.LOGGER.info("[SEAT] re-seated {} aboard craft {}",
                            player.getGameProfile().getName(), arrived.getUniqueId());
                }
            } catch (Throwable t) {
                GenesisMod.LOGGER.warn("[SEAT] could not restore riding state", t);
            }
        });
    }

    /** The closest thing to {@code near} that a player can sit on. */
    private static Entity nearestRideable(ServerLevel level, Vec3 near) {
        AABB box = new AABB(near, near).inflate(SEAT_SEARCH_RADIUS);
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : level.getEntities((Entity) null, box, candidate -> !(candidate instanceof ServerPlayer))) {
            // A seat is whatever will take a passenger; matching on the Create
            // seat type by name would break for any other rideable a craft has.
            if (!entity.canBeCollidedWith() && entity.getPassengers().isEmpty()) {
                double distance = entity.position().distanceToSqr(near);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = entity;
                }
            }
        }
        return best;
    }
}
