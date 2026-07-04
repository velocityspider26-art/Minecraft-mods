package shipwrights.genesis.compat.aeronautics;

import dev.ryanhcode.sable.Sable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Velocity-inheritance and entity/construct movement helpers.
 *
 * <p>Compatibility replacement for VS entity dragging and the {@code getVelocityAtPoint}
 * inheritance used when players stand on or leave a moving vehicle.</p>
 */
public final class AeronauticsMovementHelper {

    private AeronauticsMovementHelper() {}

    /**
     * @return the world velocity of a point that is rigidly attached to the construct
     *         (v = linear + omega &times; r).
     */
    public static Vector3d velocityAtPoint(AeronauticsConstruct construct, Vector3dc worldPoint, Vector3d dest) {
        Vector3dc linear = construct.linearVelocity();
        Vector3dc omega = construct.angularVelocity();
        Vector3d r = new Vector3d(worldPoint).sub(construct.positionInWorld());
        omega.cross(r, dest);
        dest.add(linear);
        return dest;
    }

    public static Vec3 velocityAtPoint(AeronauticsConstruct construct, Vec3 worldPoint) {
        Vector3d v = velocityAtPoint(construct, new Vector3d(worldPoint.x, worldPoint.y, worldPoint.z), new Vector3d());
        return new Vec3(v.x, v.y, v.z);
    }

    /**
     * @return the global velocity (relative to the world) of a world position, correctly accounting
     *         for any construct/sub-level it sits on. Delegates to Sable's helper which already
     *         understands sub-level motion.
     */
    public static Vector3d worldVelocityOfPoint(Level level, Vector3dc worldPos, Vector3d dest) {
        if (!AeronauticsCompat.isLoaded()) {
            return dest.set(0, 0, 0);
        }
        return Sable.HELPER.getVelocity(level, worldPos, dest);
    }

    /** @return the sub-level an entity is currently riding as a construct, or {@code null}. */
    public static AeronauticsConstruct constructRiddenBy(Entity entity) {
        return AeronauticsContraptionLookup.getConstructForEntity(entity);
    }
}
