package shipwrights.genesis.compat.aeronautics;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;

/**
 * Applies forces, impulses and torques to a Create Aeronautics construct.
 *
 * <p>Compatibility replacement for the VS force-inducer / attachment force application. Sable's
 * physics pipeline is impulse based, so continuous "forces" applied once per tick are converted to
 * per-tick impulses (force &times; dt) by the caller, or applied directly through the impulse API.</p>
 */
public final class AeronauticsForceHandler {

    private AeronauticsForceHandler() {}

    /** Apply an instantaneous impulse (kg&middot;m/s) at a world-space point. */
    public static void applyImpulseAtPoint(AeronauticsConstruct construct, Vector3dc worldPoint, Vector3dc impulse) {
        RigidBodyHandle body = AeronauticsPhysicsAdapter.getRigidBody(construct);
        if (body != null && body.isValid()) {
            body.applyImpulseAtPoint(worldPoint, impulse);
        }
    }

    public static void applyImpulseAtPoint(AeronauticsConstruct construct, Vec3 worldPoint, Vec3 impulse) {
        RigidBodyHandle body = AeronauticsPhysicsAdapter.getRigidBody(construct);
        if (body != null && body.isValid()) {
            body.applyImpulseAtPoint(worldPoint, impulse);
        }
    }

    /** Apply a linear impulse through the center of mass. */
    public static void applyLinearImpulse(AeronauticsConstruct construct, Vector3dc impulse) {
        RigidBodyHandle body = AeronauticsPhysicsAdapter.getRigidBody(construct);
        if (body != null && body.isValid()) {
            body.applyLinearImpulse(impulse);
        }
    }

    /** Apply a torque impulse (kg&middot;m&sup2;/s). */
    public static void applyTorqueImpulse(AeronauticsConstruct construct, Vector3dc torque) {
        RigidBodyHandle body = AeronauticsPhysicsAdapter.getRigidBody(construct);
        if (body != null && body.isValid()) {
            body.applyTorqueImpulse(torque);
        }
    }

    /**
     * Apply a continuous force (N) at a world point for a single tick. Converts to an impulse using
     * the fixed 1/20s server tick.
     */
    public static void applyForceAtPoint(AeronauticsConstruct construct, Vector3dc worldPoint, Vector3dc force) {
        applyImpulseAtPoint(construct, worldPoint,
                new org.joml.Vector3d(force).mul(1.0 / 20.0));
    }

    /** Directly add a linear + angular velocity delta to the construct. */
    public static void addVelocity(AeronauticsConstruct construct, Vector3dc linear, Vector3dc angular) {
        RigidBodyHandle body = AeronauticsPhysicsAdapter.getRigidBody(construct);
        if (body != null && body.isValid()) {
            body.addLinearAndAngularVelocity(linear, angular);
        }
    }
}
