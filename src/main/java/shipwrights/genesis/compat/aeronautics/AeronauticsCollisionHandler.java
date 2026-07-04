package shipwrights.genesis.compat.aeronautics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Moving-construct collision helpers.
 *
 * <p>Sable already handles the heavy lifting of block/entity collision against moving sub-levels
 * through its own mixins, so this class only exposes the higher-level queries Genesis needs:
 * "does this world box overlap a construct" and "which constructs is this entity touching".</p>
 *
 * <p>Compatibility replacement for the VS ship AABB overlap checks and the mounted-block collision
 * bookkeeping.</p>
 */
public final class AeronauticsCollisionHandler {

    private AeronauticsCollisionHandler() {}

    /** @return every construct whose world bounds overlap the given world-space box. */
    public static List<AeronauticsConstruct> constructsOverlapping(Level level, AABB box) {
        return AeronauticsContraptionLookup.getConstructsIntersecting(
                level, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /** @return true if any construct's world bounds contain the point. */
    public static boolean isInsideConstructBounds(Level level, Vec3 point) {
        for (AeronauticsConstruct c : AeronauticsContraptionLookup.getAllConstructs(level)) {
            var b = c.worldBounds();
            if (b.contains(point.x, point.y, point.z)) return true;
        }
        return false;
    }

    /** @return the constructs an entity's bounding box currently overlaps. */
    public static List<AeronauticsConstruct> constructsTouching(Entity entity) {
        List<AeronauticsConstruct> result = new ArrayList<>();
        if (!AeronauticsCompat.isLoaded()) return result;
        return constructsOverlapping(entity.level(), entity.getBoundingBox());
    }
}
