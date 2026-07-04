package shipwrights.genesis.teleportation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.teleportation.impl.EntityCollector;
import shipwrights.genesis.teleportation.impl.EntityTeleporter;
import shipwrights.genesis.teleportation.impl.ShipCollector;
import shipwrights.genesis.teleportation.impl.ShipTeleporter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Teleports a Create Aeronautics construct (and any constructs / entities riding along with it)
 * between dimensions, applying the frame rotation and velocity scaling that Genesis' space travel
 * needs. Ported from the Valkyrien Skies implementation onto the Aeronautics compat layer.
 */
public class DimensionTravelTeleporter {

    private static final ConcurrentHashMap<UUID, Long> constructCooldownExpiry = new ConcurrentHashMap<>();

    public static void teleportConstruct(
            AeronauticsConstruct construct,
            TravelDirection direction,
            ServerLevel oldLevel,
            ServerLevel newLevel,
            Vector3dc newPosition,
            Quaterniondc newRotation
    ) {
        long gameTime = oldLevel.getGameTime();
        if (shouldSkip(construct.id(), gameTime)) return;

        Vector3dc origin = construct.positionInWorld();

        ShipCollector collector = new ShipCollector(direction, oldLevel);
        Map<UUID, TeleportData> constructs = collector.collectConnected(construct, origin, newPosition, newRotation);

        for (UUID id : constructs.keySet()) {
            if (shouldSkip(id, gameTime)) return;
        }
        for (UUID id : constructs.keySet()) {
            mark(id, gameTime);
        }

        EntityCollector entityCollector = new EntityCollector(oldLevel);
        Map<Entity, Vec3> entities = entityCollector.collect(collector.getCollectedConstructs(), origin, newPosition, newRotation);

        for (var entry : constructs.entrySet()) {
            AeronauticsConstruct c = collector.getById(entry.getKey());
            if (c != null) {
                ShipTeleporter.teleportConstruct(c, entry.getValue(), newLevel);
            }
        }

        for (var entry : entities.entrySet()) {
            EntityTeleporter.teleportEntityAndPassengers(entry.getKey(), newLevel, entry.getValue(), newRotation);
        }
    }

    private static boolean shouldSkip(UUID id, long currentTick) {
        return currentTick < constructCooldownExpiry.getOrDefault(id, 0L);
    }

    private static void mark(UUID id, long currentTick) {
        constructCooldownExpiry.put(id, currentTick + 20);
    }
}
