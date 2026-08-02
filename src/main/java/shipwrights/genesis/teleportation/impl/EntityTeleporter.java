package shipwrights.genesis.teleportation.impl;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for teleporting entities across server levels (dimensions),
 * preserving passengers and their relative positions.
 */
public class EntityTeleporter {

    /**
     * Teleports an entity and all of its passengers to the given level and position.
     * Passengers are detached before teleporting and reattached afterward with their
     * positions offset relative to the vehicle's movement.
     * The frame rotation is applied to each entity's look direction so it stays
     * consistent relative to the ship after the coordinate frame changes.
     *
     * @param <T>      the type of the entity being teleported
     * @param entity   the entity to teleport
     * @param newLevel the destination level
     * @param newPos   the destination position
     */
    public static <T extends Entity> void teleportEntityAndPassengers(T entity, ServerLevel newLevel, Vec3 newPos) {
        teleportEntityAndPassengers(entity, newLevel, newPos, new Quaterniond());
    }

    public static <T extends Entity> T teleportEntityAndPassengers(T entity, ServerLevel newLevel, Vec3 newPos, Quaterniondc rotation) {
        Vec3 oldPos = entity.position();
        // Read before the move: teleporting clears motion, and arriving at a
        // dead stop is what makes a transition read as a teleport instead of
        // as continuing to fall.
        Vec3 carried = carriedVelocity(entity, rotation);

        List<Entity> passengers = detachPassengers(entity);

        T newEntity = teleportEntity(entity, newLevel, newPos, rotation);

        if (newEntity != null) {
            applyCarriedVelocity(newEntity, carried);
            reattachPassengers(passengers, oldPos, newPos, newEntity, newLevel, rotation);
        }

        return newEntity;
    }

    /**
     * The fastest a traveller may arrive, in blocks per tick.
     *
     * <p>Space and planet dimensions are not at the same scale, so carrying a
     * speed across literally would multiply it and fling the arrival away from
     * where they were put. Direction is what sells the continuity; the
     * magnitude only has to be enough to still feel like falling.</p>
     */
    private static final double MAX_ARRIVAL_SPEED = 4.0;

    /** The traveller's motion, turned to match the destination's orientation. */
    private static Vec3 carriedVelocity(Entity entity, Quaterniondc rotation) {
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() < 1.0E-8) {
            return Vec3.ZERO;
        }
        Vector3d turned = new Vector3d(velocity.x, velocity.y, velocity.z);
        rotation.transform(turned);

        double speed = turned.length();
        if (speed > MAX_ARRIVAL_SPEED) {
            turned.mul(MAX_ARRIVAL_SPEED / speed);
        }
        return new Vec3(turned.x, turned.y, turned.z);
    }

    private static void applyCarriedVelocity(Entity entity, Vec3 velocity) {
        if (velocity.lengthSqr() < 1.0E-8) {
            return;
        }
        entity.setDeltaMovement(velocity);
        entity.hasImpulse = true;
        entity.resetFallDistance();
        // A player's motion lives on their client; without this the server value
        // is overwritten by the next movement packet and the carry is silently
        // lost. Guarded because a player mid dimension-change is exactly when a
        // connection can be absent, and losing the carry is not worth a crash.
        if (entity instanceof ServerPlayer player && player.connection != null) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> T teleportEntity(T entity, ServerLevel newLevel, Vec3 newPos, Quaterniondc rotation) {
        float[] newAngles = rotateLookAngles(entity.getYRot(), entity.getXRot(), rotation);
        float newYRot = newAngles[0];
        float newXRot = newAngles[1];

        if (entity instanceof ServerPlayer player) {
            teleportPlayer(player, newLevel, newPos, newYRot, newXRot);
            return entity;
        }

        if (entity.level() == newLevel) {
            entity.moveTo(newPos.x, newPos.y, newPos.z, newYRot, newXRot);
            return entity;
        }

        return (T) cloneAndTeleportEntity(entity, newLevel, newPos, newYRot, newXRot);
    }

    private static void teleportPlayer(ServerPlayer player, ServerLevel level, Vec3 pos, float yRot, float xRot) {
        player.teleportTo(level, pos.x, pos.y, pos.z, yRot, xRot);
    }

    private static Entity cloneAndTeleportEntity(Entity entity, ServerLevel newLevel, Vec3 newPos, float yRot, float xRot) {
        Entity newEntity = entity.getType().create(newLevel);

        if (!entity.getClass().isInstance(newEntity)) {
            return null;
        }

        newEntity.restoreFrom(entity);
        newEntity.moveTo(newPos.x, newPos.y, newPos.z, yRot, xRot);
        newEntity.setYHeadRot(yRot);
        newEntity.setYBodyRot(yRot);

        newLevel.addDuringTeleport(newEntity);

        entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);

        return newEntity;
    }

    private static List<Entity> detachPassengers(Entity entity) {
        List<Entity> passengers = new ArrayList<>(entity.getPassengers());
        entity.ejectPassengers();
        return passengers;
    }

    private static void reattachPassengers(
            List<Entity> passengers,
            Vec3 oldPos,
            Vec3 newPos,
            Entity newEntity,
            ServerLevel newLevel,
            Quaterniondc rotation
    ) {
        for (Entity passenger : passengers) {
            Vec3 passengerPos = passenger.position().subtract(oldPos).add(newPos);

            Entity newPassenger = teleportEntityAndPassengers(passenger, newLevel, passengerPos, rotation);

            if (newPassenger != null) {
                newPassenger.startRiding(newEntity, true);
            }
        }
    }

    /**
     * Rotates a Minecraft yaw/pitch look direction by the given quaternion rotation.
     * Minecraft uses: dx = -sin(yaw)*cos(pitch), dy = -sin(pitch), dz = cos(yaw)*cos(pitch).
     *
     * @return float[]{newYRot, newXRot} in degrees
     */
    private static float[] rotateLookAngles(float yRot, float xRot, Quaterniondc rotation) {
        double yawRad = Math.toRadians(yRot);
        double pitchRad = Math.toRadians(xRot);

        double dx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dy = -Math.sin(pitchRad);
        double dz =  Math.cos(yawRad) * Math.cos(pitchRad);

        Vector3d dir = rotation.transform(dx, dy, dz, new Vector3d());

        float newXRot = (float) Math.toDegrees(Math.asin(-dir.y));
        float newYRot = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        return new float[]{newYRot, newXRot};
    }
}
