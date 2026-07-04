package shipwrights.genesis.teleportation.impl;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsContraptionLookup;
import shipwrights.genesis.teleportation.TeleportData;
import shipwrights.genesis.teleportation.TravelDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Collects the construct being teleported plus any nearby constructs so a whole vehicle cluster
 * travels together. Ported from the Valkyrien Skies constraint-graph collector; Create Aeronautics
 * exposes no joint graph, so connectivity is approximated by world-bounds proximity (which also
 * catches super-glued / bearing-linked sub-contraptions that overlap).
 */
@ApiStatus.Internal
public class ShipCollector {

    private static final double SHIP_COLLECT_RANGE = 10;

    private final TravelDirection direction;
    private final ServerLevel level;

    private final Map<UUID, TeleportData> constructs = new LinkedHashMap<>();
    private final Map<UUID, AeronauticsConstruct> byId = new HashMap<>();
    private final List<AeronauticsConstruct> collected = new ArrayList<>();
    private double greatestOffset;

    public ShipCollector(TravelDirection direction, ServerLevel level) {
        this.direction = direction;
        this.level = level;
    }

    public Map<UUID, TeleportData> collectConnected(AeronauticsConstruct root, Vector3dc origin, Vector3dc newPos, Quaterniondc rotation) {
        this.greatestOffset = 0;
        collectSingle(root, origin, newPos, rotation);
        collectNearby(origin, newPos, rotation);
        finish(rotation);
        return this.constructs;
    }

    public List<AeronauticsConstruct> getCollectedConstructs() {
        return this.collected;
    }

    public AeronauticsConstruct getById(UUID id) {
        return this.byId.get(id);
    }

    private void collectSingle(AeronauticsConstruct construct, Vector3dc origin, Vector3dc newPos, Quaterniondc rotation) {
        UUID id = construct.id();
        if (this.constructs.containsKey(id)) return;

        Vector3dc pos = construct.positionInWorld();
        Vector3d velocity = new Vector3d(construct.linearVelocity());
        Vector3d omega = new Vector3d(construct.angularVelocity());
        this.collected.add(construct);
        this.byId.put(id, construct);

        Vector3d relPos = pos.sub(origin, new Vector3d());
        Quaterniond newRotation = new Quaterniond(construct.rotation());

        if (this.direction == TravelDirection.PLANET_TO_SPACE) {
            double offset = relPos.y;
            if (offset < this.greatestOffset) this.greatestOffset = offset;
        }

        rotation.transform(relPos);
        rotation.transform(velocity);
        newRotation.premul(rotation).normalize();

        if (this.direction == TravelDirection.SPACE_TO_PLANET) {
            double offset = relPos.y;
            if (offset > this.greatestOffset) this.greatestOffset = offset;
        }

        relPos.add(newPos);

        Vector3d velocity0 = new Vector3d(velocity);
        Vector3d omega0 = new Vector3d(omega);
        if (this.direction == TravelDirection.PLANET_TO_SPACE) {
            velocity0.mul(0.0625);
        } else {
            velocity0.mul(2);
        }

        this.constructs.put(id, new TeleportData(relPos, newRotation, velocity0, omega0));
    }

    private void collectNearby(Vector3dc origin, Vector3dc newPos, Quaterniondc rotation) {
        for (int i = 0; i < this.collected.size(); i++) {
            var box = this.collected.get(i).worldBounds();
            for (AeronauticsConstruct other : AeronauticsContraptionLookup.getConstructsIntersecting(level,
                    box.minX() - SHIP_COLLECT_RANGE, box.minY() - SHIP_COLLECT_RANGE, box.minZ() - SHIP_COLLECT_RANGE,
                    box.maxX() + SHIP_COLLECT_RANGE, box.maxY() + SHIP_COLLECT_RANGE, box.maxZ() + SHIP_COLLECT_RANGE)) {
                collectSingle(other, origin, newPos, rotation);
            }
        }
    }

    private void finish(Quaterniondc rotation) {
        Vector3d offset = new Vector3d(0, -this.greatestOffset, 0);
        if (this.direction == TravelDirection.PLANET_TO_SPACE) {
            rotation.transform(offset);
        }
        for (AeronauticsConstruct construct : this.collected) {
            TeleportData data = this.constructs.get(construct.id());
            if (data != null) data.newPos().add(offset);
        }
    }
}
