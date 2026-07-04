package shipwrights.genesis.compat.aeronautics;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Local &lt;-&gt; world coordinate conversions for Create Aeronautics constructs.
 *
 * <p>Compatibility replacement for {@code ship.getShipToWorld().transformPosition(...)},
 * {@code getWorldToShip()} and {@code VectorConversionsMCKt}.</p>
 */
public final class AeronauticsTransformHelper {

    private AeronauticsTransformHelper() {}

    public static Vector3d constructToWorld(AeronauticsConstruct construct, Vector3dc local, Vector3d dest) {
        return construct.toWorld(local, dest);
    }

    public static Vector3d worldToConstruct(AeronauticsConstruct construct, Vector3dc world, Vector3d dest) {
        return construct.toLocal(world, dest);
    }

    public static Vector3d directionToWorld(AeronauticsConstruct construct, Vector3dc localDir, Vector3d dest) {
        return construct.dirToWorld(localDir, dest);
    }

    public static Quaterniondc orientation(AeronauticsConstruct construct) {
        return construct.rotation();
    }

    public static Vector3dc position(AeronauticsConstruct construct) {
        return construct.positionInWorld();
    }

    public static Vec3 toMinecraft(Vector3dc v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    public static Vector3d toJoml(Vec3 v) {
        return new Vector3d(v.x, v.y, v.z);
    }
}
