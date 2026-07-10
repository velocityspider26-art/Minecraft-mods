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

    /**
     * The construct's exposed surface faces — one per air-facing side of every non-air block — as
     * {@code [ox,oy,oz, nx,ny,nz]}: the face-centre offset (in blocks) from the local bounds centre and
     * its outward unit normal. This is the actual voxel silhouette the re-entry plasma hugs. Client-safe
     * (the plot's chunks are present for rendering); capped and fully guarded (empty list on any failure).
     */
    public java.util.List<float[]> exposedFaces(int cap) {
        java.util.List<float[]> out = new java.util.ArrayList<>();
        try {
            dev.ryanhcode.sable.sublevel.plot.LevelPlot plot = subLevel.getPlot();
            if (plot == null) return out;
            BoundingBox3ic b = plot.getBoundingBox();
            float cx = (b.minX() + b.maxX()) / 2f, cy = (b.minY() + b.maxY()) / 2f, cz = (b.minZ() + b.maxZ()) / 2f;
            net.minecraft.world.level.Level lvl = subLevel.getLevel();
            int minY = lvl.getMinBuildHeight(), maxY = lvl.getMaxBuildHeight();

            java.util.HashSet<Long> occ = new java.util.HashSet<>();
            java.util.List<int[]> cells = new java.util.ArrayList<>();
            for (dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder holder : plot.getLoadedChunks()) {
                net.minecraft.world.level.chunk.LevelChunk chunk = holder.getChunk();
                if (chunk == null) continue;
                int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = minY; y < maxY; y++) {
                    net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(minX + x, y, minZ + z);
                    if (chunk.getBlockState(p).isAir()) continue;
                    occ.add(key(p.getX(), p.getY(), p.getZ()));
                    cells.add(new int[]{p.getX(), p.getY(), p.getZ()});
                    if (cells.size() >= 8192) break;
                }
            }
            int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
            for (int[] c : cells) {
                for (int[] d : dirs) {
                    if (occ.contains(key(c[0]+d[0], c[1]+d[1], c[2]+d[2]))) continue; // interior face
                    out.add(new float[]{
                            c[0] + 0.5f + d[0] * 0.5f - cx,
                            c[1] + 0.5f + d[1] * 0.5f - cy,
                            c[2] + 0.5f + d[2] * 0.5f - cz,
                            d[0], d[1], d[2]});
                    if (out.size() >= cap) return out;
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static long key(int x, int y, int z) {
        return (((long) x & 0x1FFFFF) << 42) | (((long) y & 0x1FFFFF) << 21) | ((long) z & 0x1FFFFF);
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

    /** Construct-local to world transform matrix (translation + rotation), the CA analog of {@code ship.getShipToWorld()}. */
    public org.joml.Matrix4d toWorldMatrix() {
        Vector3dc p = positionInWorld();
        return new org.joml.Matrix4d().translate(p.x(), p.y(), p.z()).rotate(rotation());
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
