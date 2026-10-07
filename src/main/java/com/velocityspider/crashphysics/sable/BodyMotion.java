package com.velocityspider.crashphysics.sable;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * The motion of one vehicle, sampled before every physics sub-step.
 * <p>
 * Rapier can't be queried while it is stepping (that re-enters its locks), so contact callbacks work from this
 * snapshot. Comparing consecutive snapshots also gives the accelerations a crash puts on the structure and the crew.
 */
public final class BodyMotion {
    final ServerSubLevel subLevel;

    /**
     * World-space velocities at the start of the current sub-step [m/s], [rad/s]
     */
    final Vector3d linear = new Vector3d();
    final Vector3d angular = new Vector3d();

    /**
     * Pose at the start of the current sub-step
     */
    final Pose3d pose = new Pose3d();

    private final Vector3d previousLinear = new Vector3d();
    private final Vector3d previousAngular = new Vector3d();
    private boolean hasPrevious;

    /**
     * Velocity at the start of the game tick, used to throw loose riders
     */
    final Vector3d tickStartLinear = new Vector3d();
    private boolean hasTickStart;

    /**
     * If this vehicle had a contact during the sub-step that is currently running (set from inside the step)
     */
    boolean contactThisSubstep;

    /**
     * Worst load seen this tick
     */
    double peakG;
    final Vector3d peakLinearAcceleration = new Vector3d();
    final Vector3d peakAngularAcceleration = new Vector3d();
    final Vector3d peakAngularVelocity = new Vector3d();

    /**
     * Plot positions of blocks that took hard hits this tick, where crash loads enter the structure
     */
    final LongOpenHashSet impactBlocks = new LongOpenHashSet();

    long lastSeenTick;

    BodyMotion(final ServerSubLevel subLevel) {
        this.subLevel = subLevel;
    }

    /**
     * Reads the current velocities from the physics engine and measures the acceleration since the previous sample.
     *
     * @param handle   rigid body handle
     * @param dt       time since the previous sample [s]
     * @param gravity  gravitational acceleration of the dimension [m/s²]
     * @param tickStart if this is the first sub-step of a game tick
     */
    void sample(final RigidBodyHandle handle, final double dt, final Vector3dc gravity, final boolean tickStart) {
        // The change since the previous sample happened during the step that just ran
        final boolean contactDuringStep = this.contactThisSubstep;
        this.contactThisSubstep = false;

        this.previousLinear.set(this.linear);
        this.previousAngular.set(this.angular);

        handle.getLinearVelocity(this.linear);
        handle.getAngularVelocity(this.angular);
        this.pose.set(this.subLevel.logicalPose());

        if (tickStart || !this.hasTickStart) {
            this.tickStartLinear.set(this.linear);
            this.hasTickStart = true;
        }

        if (this.hasPrevious && dt > 0.0 && contactDuringStep) {
            // Proper acceleration: free fall feels like nothing, so take gravity out of the measured change
            final double ax = (this.linear.x - this.previousLinear.x) / dt - gravity.x();
            final double ay = (this.linear.y - this.previousLinear.y) / dt - gravity.y();
            final double az = (this.linear.z - this.previousLinear.z) / dt - gravity.z();
            final double g = Math.sqrt(ax * ax + ay * ay + az * az) / 9.80665;

            if (g > this.peakG) {
                this.peakG = g;
                this.peakLinearAcceleration.set(ax, ay, az);
                this.peakAngularAcceleration.set(this.angular).sub(this.previousAngular).div(dt);
                this.peakAngularVelocity.set(this.angular);
            }
        }

        this.hasPrevious = true;
    }

    public ServerSubLevel subLevel() {
        return this.subLevel;
    }

    /**
     * @return world-space velocity of the centre of mass at the start of the current sub-step [m/s]
     */
    public Vector3dc linear() {
        return this.linear;
    }

    /**
     * @return world-space angular velocity at the start of the current sub-step [rad/s]
     */
    public Vector3dc angular() {
        return this.angular;
    }

    /**
     * Velocity of a world-space point fixed to this body [m/s].
     */
    public Vector3d pointVelocity(final double x, final double y, final double z, final Vector3d dest) {
        final Vector3dc com = this.pose.position();
        final double rx = x - com.x();
        final double ry = y - com.y();
        final double rz = z - com.z();
        return dest.set(
                this.linear.x + this.angular.y * rz - this.angular.z * ry,
                this.linear.y + this.angular.z * rx - this.angular.x * rz,
                this.linear.z + this.angular.x * ry - this.angular.y * rx
        );
    }

    void resetTick() {
        this.peakG = 0.0;
        this.impactBlocks.clear();
    }
}
