package shipwrights.genesis.compat.aeronautics;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

/**
 * Physics-state queries for a Create Aeronautics construct: velocity, orientation, mass and the
 * underlying rigid body handle used by the force/collision helpers.
 *
 * <p>Compatibility replacement for {@code ship.getVelocity()}, {@code getAngularVelocity()},
 * {@code getInertiaData()} and the VS physics pipeline access.</p>
 */
public final class AeronauticsPhysicsAdapter {

    private AeronauticsPhysicsAdapter() {}

    public static Vector3dc getVelocity(AeronauticsConstruct construct) {
        return construct.linearVelocity();
    }

    public static Vector3dc getAngularVelocity(AeronauticsConstruct construct) {
        return construct.angularVelocity();
    }

    public static Quaterniondc getOrientation(AeronauticsConstruct construct) {
        return construct.rotation();
    }

    public static double getMass(AeronauticsConstruct construct) {
        return construct.mass();
    }

    public static Vector3dc getCenterOfMass(AeronauticsConstruct construct) {
        return construct.centerOfMass();
    }

    /**
     * @return the server-side rigid body handle for this construct, or {@code null} on the client
     *         or before the physics body is created.
     */
    @Nullable
    public static RigidBodyHandle getRigidBody(AeronauticsConstruct construct) {
        SubLevel sub = construct.subLevel();
        if (sub instanceof ServerSubLevel server) {
            return RigidBodyHandle.of(server);
        }
        return null;
    }

    public static boolean hasPhysics(AeronauticsConstruct construct) {
        return getRigidBody(construct) != null;
    }
}
