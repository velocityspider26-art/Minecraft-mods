package com.velocityspider.crashphysics.sable;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * The motion of one vehicle, sampled around every physics sub-step.
 * <p>
 * Rapier can't be queried while it is stepping (that re-enters its locks), so contact callbacks work from the snapshot
 * taken before the step. Comparing it with the velocities after the step (and after the crash impulses applied around
 * it) gives the accelerations a crash puts on the structure and the crew.
 */
public final class BodyMotion {
    final ServerSubLevel subLevel;

    /**
     * World-space velocities the current sub-step starts from [m/s], [rad/s]. After the sub-step, the velocities it ended with.
     */
    final Vector3d linear = new Vector3d();
    final Vector3d angular = new Vector3d();

    /**
     * Pose at the start of the current sub-step
     */
    final Pose3d pose = new Pose3d();

    /**
     * Velocities before anything acted on the vehicle in the current sub-step
     */
    private final Vector3d stepStartLinear = new Vector3d();
    private final Vector3d stepStartAngular = new Vector3d();
    private boolean stepStarted;

    /**
     * Velocity at the start of the game tick, used to throw loose riders
     */
    final Vector3d tickStartLinear = new Vector3d();
    private boolean hasTickStart;

    /**
     * If this vehicle hit something hard during the current sub-step
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
    final long firstSeenTick;

    /**
     * Velocities at the ends of recent sub-steps with contact, so accelerations can be averaged over the time a
     * structure (or a body) takes to respond: a rigid contact stops a vehicle within a single physics step, but a frame
     * flexes and spreads that shock out, so it feels the change in velocity over its response time instead
     */
    private static final int HISTORY = 32;
    private final double[] historyTime = new double[HISTORY];
    private final double[] historyLinear = new double[HISTORY * 3];
    private final double[] historyAngular = new double[HISTORY * 3];
    private int historyCount;
    private int historyHead;
    private double clock;
    private double lastContactTime = Double.NEGATIVE_INFINITY;

    private final Vector3d scratchLinear = new Vector3d();
    private final Vector3d scratchAngular = new Vector3d();

    BodyMotion(final ServerSubLevel subLevel, final long tick) {
        this.subLevel = subLevel;
        this.firstSeenTick = tick;
    }

    /**
     * Reads the velocities and pose a sub-step starts from.
     *
     * @param tickStart if this is the first sub-step of a game tick
     */
    void beginSubstep(final RigidBodyHandle handle, final boolean tickStart) {
        handle.getLinearVelocity(this.linear);
        handle.getAngularVelocity(this.angular);
        this.pose.set(this.subLevel.logicalPose());

        this.stepStartLinear.set(this.linear);
        this.stepStartAngular.set(this.angular);
        this.stepStarted = true;
        this.contactThisSubstep = false;

        if (tickStart || !this.hasTickStart) {
            this.tickStartLinear.set(this.linear);
            this.hasTickStart = true;
        }
    }

    /**
     * Re-reads the velocities after impulses were applied before the step, so contact callbacks see them.
     */
    void refreshVelocity(final RigidBodyHandle handle) {
        handle.getLinearVelocity(this.linear);
        handle.getAngularVelocity(this.angular);
    }

    /**
     * Reads the velocities a sub-step ended with, and if the vehicle hit something, how hard it was decelerated: the
     * change in velocity over the last response time, so a short sharp jolt counts as its impulse spread over that
     * time and a long crash as its average deceleration.
     *
     * @param dt           sub-step length [s]
     * @param gravity      gravitational acceleration of the dimension [m/s²]
     * @param responseTime how long the structure takes to respond to a jolt [s]
     */
    void endSubstep(final RigidBodyHandle handle, final double dt, final Vector3dc gravity, final double responseTime) {
        this.clock += dt;
        if (!this.stepStarted) {
            return;
        }
        this.stepStarted = false;
        if (!this.contactThisSubstep) {
            // Nothing hit it, so there is no deceleration to measure; the next sub-step reads its velocity anyway
            return;
        }
        this.contactThisSubstep = false;

        final Vector3d endLinear = handle.getLinearVelocity(this.scratchLinear);
        final Vector3d endAngular = handle.getAngularVelocity(this.scratchAngular);

        // A new crash after a quiet spell: start from the velocity before this step
        if (this.clock - dt - this.lastContactTime > responseTime + dt) {
            this.historyCount = 0;
            this.record(this.clock - dt, this.stepStartLinear, this.stepStartAngular);
        }
        this.lastContactTime = this.clock;
        this.record(this.clock, endLinear, endAngular);

        // Compare with the velocity one response time ago, or as far back as this crash goes
        int reference = -1;
        for (int i = 1; i < this.historyCount; i++) {
            final int index = Math.floorMod(this.historyHead - 1 - i, HISTORY);
            reference = index;
            if (this.clock - this.historyTime[index] >= responseTime) {
                break;
            }
        }
        if (reference >= 0) {
            final double elapsed = this.clock - this.historyTime[reference];
            final double window = Math.max(elapsed, Math.max(responseTime, dt));
            // Proper acceleration: free fall feels like nothing, so take gravity's share of the change out
            final double ax = (endLinear.x - this.historyLinear[reference * 3] - gravity.x() * elapsed) / window;
            final double ay = (endLinear.y - this.historyLinear[reference * 3 + 1] - gravity.y() * elapsed) / window;
            final double az = (endLinear.z - this.historyLinear[reference * 3 + 2] - gravity.z() * elapsed) / window;
            final double g = Math.sqrt(ax * ax + ay * ay + az * az) / 9.80665;

            if (g > this.peakG) {
                this.peakG = g;
                this.peakLinearAcceleration.set(ax, ay, az);
                this.peakAngularAcceleration.set(
                        (endAngular.x - this.historyAngular[reference * 3]) / window,
                        (endAngular.y - this.historyAngular[reference * 3 + 1]) / window,
                        (endAngular.z - this.historyAngular[reference * 3 + 2]) / window);
                this.peakAngularVelocity.set(endAngular);
            }
        }

        this.linear.set(endLinear);
        this.angular.set(endAngular);
    }

    private void record(final double time, final Vector3dc linear, final Vector3dc angular) {
        final int index = this.historyHead;
        this.historyTime[index] = time;
        this.historyLinear[index * 3] = linear.x();
        this.historyLinear[index * 3 + 1] = linear.y();
        this.historyLinear[index * 3 + 2] = linear.z();
        this.historyAngular[index * 3] = angular.x();
        this.historyAngular[index * 3 + 1] = angular.y();
        this.historyAngular[index * 3 + 2] = angular.z();
        this.historyHead = (index + 1) % HISTORY;
        this.historyCount = Math.min(this.historyCount + 1, HISTORY);
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
