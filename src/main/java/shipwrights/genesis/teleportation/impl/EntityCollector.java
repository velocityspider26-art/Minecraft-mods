package shipwrights.genesis.teleportation.impl;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsContraptionLookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects the entities that should travel with a construct cluster and computes their destination
 * positions. Ported from Valkyrien Skies onto the Aeronautics compat layer.
 */
public class EntityCollector {

    private final ServerLevel oldLevel;
    private final Map<Entity, Vec3> entityStorage = new HashMap<>();

    public EntityCollector(ServerLevel oldLevel) {
        this.oldLevel = oldLevel;
    }

    public Map<Entity, Vec3> collect(List<AeronauticsConstruct> constructs, Vector3dc origin, Vector3dc newPos, Quaterniondc rotation) {
        constructs.forEach(construct -> addEntitiesForConstruct(construct, origin, newPos, rotation));
        return entityStorage;
    }

    private void addEntitiesForConstruct(AeronauticsConstruct construct, Vector3dc origin, Vector3dc newPos, Quaterniondc rotation) {
        var box = construct.worldBounds();
        double inflate = 8 * GenesisMod.getDimensionScale(oldLevel);
        AABB worldAABB = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()).inflate(inflate);

        oldLevel.getEntities((Entity) null, worldAABB, entity -> !entityStorage.containsKey(entity))
                .forEach(entity -> addEntity(entity, origin, newPos, rotation));
    }

    private void addEntity(Entity entity, Vector3dc origin, Vector3dc newPos, Quaterniondc rotation) {
        Entity root = entity.getRootVehicle();
        if (entityStorage.containsKey(root)) return;

        Vec3 pos = root.position();
        // Entities standing inside a construct's plot region keep their local offset (they ride
        // along); free-world entities are rotated/translated with the construct frame.
        if (!AeronauticsContraptionLookup.isConstructBlock(oldLevel, pos)) {
            Vector3d relPos = new Vector3d(pos.x, pos.y, pos.z).sub(origin);
            rotation.transform(relPos);
            relPos.add(newPos);
            pos = new Vec3(relPos.x, relPos.y, relPos.z);
        }
        entityStorage.put(root, pos);
    }
}
