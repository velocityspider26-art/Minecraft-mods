package shipwrights.genesis.compat.aeronautics;

import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.UUID;

/**
 * Genesis-facing handle for a Create&nbsp;Aeronautics moving construct.
 *
 * <p>Under the hood a construct is a Sable {@code SubLevel} (the "plot"/sub-world that
 * Create Aeronautics assembles a vehicle into). This class is the compatibility replacement
 * for what used to be a Valkyrien Skies {@code Ship}/{@code ServerShip}: it exposes a stable
 * id, a world transform, velocities and bounds without leaking Sable types into the rest of
 * the mod. All coordinate conversions delegate to the sub-level's logical pose.</p>
 */
public final class AeronauticsConstruct {

    private final SubLevel subLevel;

    AeronauticsConstruct(SubLevel subLevel) {
        this.subLevel = subLevel;
    }

    /** @return the wrapped Sable sub-level. Kept package-private-ish for the rest of the compat layer. */
    public SubLevel subLevel() {
        return subLevel;
    }

    /** Stable, save-safe identifier for this construct (equivalent to the old VS ship id). */
    public UUID id() {
        return subLevel.getUniqueId();
    }

    /** Human-readable name/slug of the construct, may be {@code null}. */
    @Nullable
    public String name() {
        return subLevel.getName();
    }

    public boolean isServer() {
        return subLevel instanceof ServerSubLevel;
    }

    /** The construct's current world-space pose (position + orientation + scale). */
    public Pose3dc pose() {
        return subLevel.logicalPose();
    }

    /** Center-of-construct position in world space. */
    public Vector3dc positionInWorld() {
        return subLevel.logicalPose().position();
    }

    /** Construct-to-world rotation. */
    public Quaterniondc rotation() {
        return subLevel.logicalPose().orientation();
    }

    /** Transform a construct-local position into world space. */
    public Vector3d toWorld(Vector3dc local, Vector3d dest) {
        return subLevel.logicalPose().transformPosition(local, dest);
    }

    /** Transform a world position into construct-local space. */
    public Vector3d toLocal(Vector3dc world, Vector3d dest) {
        return subLevel.logicalPose().transformPositionInverse(world, dest);
    }

    /** Transform a construct-local direction into world space. */
    public Vector3d dirToWorld(Vector3dc localDir, Vector3d dest) {
        return subLevel.logicalPose().transformNormal(localDir, dest);
    }

    /** World-space linear velocity of the construct (m/s). Zero on the client. */
    public Vector3dc linearVelocity() {
        if (subLevel instanceof ServerSubLevel server) {
            return server.latestLinearVelocity;
        }
        return new Vector3d();
    }

    /** World-space angular velocity of the construct (rad/s). Zero on the client. */
    public Vector3dc angularVelocity() {
        if (subLevel instanceof ServerSubLevel server) {
            return server.latestAngularVelocity;
        }
        return new Vector3d();
    }

    /** World-space bounding box of the construct. */
    public BoundingBox3dc worldBounds() {
        return subLevel.boundingBox();
    }

    /** Local (plot-space) block bounds of the construct, or {@code null} if unavailable. */
    @Nullable
    public BoundingBox3ic localBounds() {
        if (subLevel.getPlot() == null) return null;
        return subLevel.getPlot().getBoundingBox();
    }

    /** Total mass of the construct in kg, or {@code 0} if the mass tracker is not built yet. */
    public double mass() {
        if (subLevel instanceof ServerSubLevel server && server.getMassTracker() != null) {
            return server.getMassTracker().getMass();
        }
        return 0.0;
    }

    /** Center of mass in construct-local coordinates. */
    public Vector3dc centerOfMass() {
        if (subLevel instanceof ServerSubLevel server && server.getMassTracker() != null) {
            return server.getMassTracker().getCenterOfMass();
        }
        return new Vector3d();
    }

    public boolean isRemoved() {
        return subLevel.isRemoved();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AeronauticsConstruct other && other.subLevel == this.subLevel;
    }

    @Override
    public int hashCode() {
        return subLevel.hashCode();
    }

    @Override
    public String toString() {
        return "AeronauticsConstruct[" + id() + "]";
    }

    static Quaterniond copyRot(Quaterniondc q) {
        return new Quaterniond(q);
    }
}
