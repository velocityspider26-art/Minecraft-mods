package shipwrights.genesis.space.planet;

import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.teleportation.CubeFaceFrame;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.Objects;

/**
 * The one canonical mapping between the flat Minecraft world, the six cube
 * faces, and rendered cube space.
 *
 * <p>Genesis previously spread approximate versions of this arithmetic across
 * the ascent renderer, the orbital renderer and the LOD selector, which is how
 * face orientations drifted apart. Every consumer now goes through this class,
 * so a world column, its voxel brick and the point visible on the rendered cube
 * always name the same physical place.</p>
 *
 * <h2>Frames</h2>
 * <ul>
 *   <li><b>World</b> — ordinary Minecraft {@code (x, y, z)} in the planet
 *       dimension. The six faces occupy the 3x2 cube-net regions defined by
 *       {@link CubeNetSurfaceTransform}.</li>
 *   <li><b>Face-local</b> — {@code (u, v)} offsets from a face's own centre,
 *       plus a world {@code y}. This is what voxel brick keys address.</li>
 *   <li><b>Cube</b> — the celestial's rotating local frame, in which the planet
 *       is a cube of half-extent {@link #faceHalfSpan()} centred on the origin.
 *       Terrain sits {@code elevation} blocks outside its face plane.</li>
 * </ul>
 *
 * <h2>Why the fold is affine</h2>
 * <p>Both the flat and the cube placement of a face are affine functions of the
 * same local vector {@code L = (u, halfSpan + elevation, v)}. A per-vertex
 * {@code mix(flat, cube, fold)} is therefore exactly reproduced by
 * interpolating the two 4x4 matrices, which is what lets the renderer keep one
 * static GPU buffer per brick and still morph continuously. See
 * {@link #foldMatrix}.</p>
 *
 * <h2>The camera's own face never moves</h2>
 * <p>For {@code face == currentFace} the cube matrix reduces to the flat matrix
 * identically — not approximately. The ground under the player is bit-for-bit
 * where vanilla puts it at every fold value, which is what makes the flat zone
 * around the craft exact rather than tuned.</p>
 */
public final class CubeSurfaceProjection {

    /** A place on the planet surface, addressed by face rather than by world column. */
    public record PlanetSurfacePosition(CubeNetSurfaceTransform.Face face,
                                        double surfaceX,
                                        double surfaceZ,
                                        double height) {
        public PlanetSurfacePosition {
            Objects.requireNonNull(face, "face");
        }
    }

    /** Where a ray met the cube, in face-local coordinates. */
    public record CubeSurfaceHit(CubeNetSurfaceTransform.Face face,
                                 double surfaceX,
                                 double surfaceZ,
                                 double distance) {
        public CubeSurfaceHit {
            Objects.requireNonNull(face, "face");
        }
    }

    private final CubeNetSurfaceTransform transform;
    private final int seaLevel;

    public CubeSurfaceProjection(CubeNetSurfaceTransform transform, int seaLevel) {
        this.transform = Objects.requireNonNull(transform, "transform");
        this.seaLevel = seaLevel;
    }

    public CubeNetSurfaceTransform transform() {
        return transform;
    }

    public int seaLevel() {
        return seaLevel;
    }

    /** Half the width of one cube face, in Minecraft blocks. */
    public double faceHalfSpan() {
        return transform.faceHalfSpan();
    }

    // ------------------------------------------------------------------
    // World <-> face
    // ------------------------------------------------------------------

    /**
     * Which face owns this world column, and where on it.
     *
     * <p>Columns outside the 3x2 net cannot have come from cube travel; they
     * are reported against {@link CubeNetSurfaceTransform.Face#UP} with
     * unclamped coordinates so callers can detect and reject them rather than
     * silently landing somewhere else.</p>
     */
    public PlanetSurfacePosition worldToFace(double worldX, double worldY, double worldZ) {
        CubeNetSurfaceTransform.Face face = transform.faceContaining(worldX, worldZ);
        if (face == null) face = CubeNetSurfaceTransform.Face.UP;
        return new PlanetSurfacePosition(face,
                worldX - transform.faceCenterX(face),
                worldZ - transform.faceCenterZ(face),
                worldY);
    }

    /** True when the column really is inside one of the six cube-net regions. */
    public boolean isInsideNet(double worldX, double worldZ) {
        return transform.faceContaining(worldX, worldZ) != null;
    }

    public double faceToWorldX(CubeNetSurfaceTransform.Face face, double surfaceX) {
        return transform.faceCenterX(Objects.requireNonNull(face, "face")) + surfaceX;
    }

    public double faceToWorldZ(CubeNetSurfaceTransform.Face face, double surfaceZ) {
        return transform.faceCenterZ(Objects.requireNonNull(face, "face")) + surfaceZ;
    }

    public Vector3d faceToWorld(PlanetSurfacePosition position) {
        Objects.requireNonNull(position, "position");
        return new Vector3d(faceToWorldX(position.face(), position.surfaceX()),
                position.height(),
                faceToWorldZ(position.face(), position.surfaceZ()));
    }

    // ------------------------------------------------------------------
    // Face <-> cube
    // ------------------------------------------------------------------

    /** Outward unit normal of a face in cube space. */
    public Vector3d faceNormal(CubeNetSurfaceTransform.Face face) {
        return new Vector3d(CubeFaceFrame.axes(face).up());
    }

    /** Rotation taking the ordinary Minecraft surface basis onto this face. */
    public Quaterniond faceOrientation(CubeNetSurfaceTransform.Face face) {
        return CubeFaceFrame.surfaceToCelestial(face);
    }

    /**
     * Places a face-local terrain point into cube space.
     *
     * @param elevation blocks above sea level, measured outward from the face
     */
    public Vector3d faceLocalToCube(CubeNetSurfaceTransform.Face face,
                                    double surfaceX, double surfaceZ, double elevation) {
        return CubeFaceFrame.cubePoint(face, surfaceX, surfaceZ, faceHalfSpan(), elevation);
    }

    /** Convenience overload taking a world Y instead of an elevation. */
    public Vector3d faceLocalToCubeAtHeight(CubeNetSurfaceTransform.Face face,
                                            double surfaceX, double surfaceZ, double worldY) {
        return faceLocalToCube(face, surfaceX, surfaceZ, worldY - seaLevel);
    }

    /**
     * Inverse of {@link #faceLocalToCube}: radially projects any non-zero cube
     * point back onto the face it actually looks out of.
     */
    public CubeSurfaceHit cubeToFaceLocal(Vector3dc cube) {
        Objects.requireNonNull(cube, "cube");
        double ax = Math.abs(cube.x());
        double ay = Math.abs(cube.y());
        double az = Math.abs(cube.z());
        double reach = Math.max(ax, Math.max(ay, az));
        if (reach < 1.0E-12) {
            return new CubeSurfaceHit(CubeNetSurfaceTransform.Face.UP, 0.0, 0.0, 0.0);
        }
        // Y owns exact ties, then Z over X. Identical to the travel transform's
        // rule, so a round trip cannot jitter between two faces on a seam.
        CubeNetSurfaceTransform.Face face;
        if (ay >= ax && ay >= az) {
            face = cube.y() >= 0.0 ? CubeNetSurfaceTransform.Face.UP
                    : CubeNetSurfaceTransform.Face.DOWN;
        } else if (az >= ax) {
            face = cube.z() >= 0.0 ? CubeNetSurfaceTransform.Face.SOUTH
                    : CubeNetSurfaceTransform.Face.NORTH;
        } else {
            face = cube.x() >= 0.0 ? CubeNetSurfaceTransform.Face.EAST
                    : CubeNetSurfaceTransform.Face.WEST;
        }
        CubeFaceFrame.Axes axes = CubeFaceFrame.axes(face);
        double height = cube.dot(axes.up());
        double scale = height != 0.0 ? faceHalfSpan() / height : 1.0;
        double u = cube.dot(axes.right()) * scale;
        double v = cube.dot(axes.south()) * scale;
        return new CubeSurfaceHit(face, u, v, height - faceHalfSpan());
    }

    /**
     * First intersection of a ray with the terrain shell of the cube.
     *
     * <p>Used by the approach controller to resolve exactly which face and
     * which surface coordinates a craft is heading for, long before any
     * dimension handoff is considered. Returns null when the ray misses.</p>
     *
     * @param elevation shell offset outside the face planes, normally the
     *                  target's terrain height above sea level
     */
    public CubeSurfaceHit cubeRayIntersection(Vector3dc origin, Vector3dc direction,
                                              double elevation) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(direction, "direction");
        double half = faceHalfSpan() + elevation;
        if (half <= 0.0) return null;
        double length = direction.length();
        if (!(length > 1.0E-12)) return null;
        Vector3d unit = new Vector3d(direction).div(length);

        CubeSurfaceHit best = null;
        for (CubeNetSurfaceTransform.Face face : CubeNetSurfaceTransform.Face.values()) {
            CubeFaceFrame.Axes axes = CubeFaceFrame.axes(face);
            double denominator = unit.dot(axes.up());
            // Only faces we can actually see the outside of.
            if (denominator >= -1.0E-12) continue;
            double distance = (half - origin.dot(axes.up())) / denominator;
            if (distance < 0.0) continue;
            Vector3d point = new Vector3d(unit).mul(distance).add(origin);
            double u = point.dot(axes.right());
            double v = point.dot(axes.south());
            if (Math.abs(u) > half || Math.abs(v) > half) continue;
            if (best == null || distance < best.distance()) {
                best = new CubeSurfaceHit(face, u, v, distance);
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Velocity / orientation frames
    // ------------------------------------------------------------------

    /**
     * Re-expresses a cube-space velocity in the flat surface basis of a face,
     * so a craft crossing into the planet dimension keeps its motion.
     */
    public Vector3d velocityToSurfaceFrame(CubeNetSurfaceTransform.Face face, Vector3dc cubeVelocity) {
        return CubeFaceFrame.celestialToSurface(face, cubeVelocity);
    }

    /** Inverse of {@link #velocityToSurfaceFrame}. */
    public Vector3d velocityToSpaceFrame(CubeNetSurfaceTransform.Face face, Vector3dc surfaceVelocity) {
        CubeFaceFrame.Axes axes = CubeFaceFrame.axes(face);
        return new Vector3d(axes.right()).mul(surfaceVelocity.x())
                .fma(surfaceVelocity.y(), axes.up())
                .fma(surfaceVelocity.z(), axes.south());
    }

    /**
     * Local up used for gravity while approaching a face. Rotating this toward
     * {@link #faceNormal} over the descent is what removes the orientation snap
     * at the dimension boundary.
     */
    public Vector3d approachGravityUp(CubeNetSurfaceTransform.Face face, double alignment) {
        double t = Math.max(0.0, Math.min(1.0, alignment));
        Vector3d spaceUp = new Vector3d(0.0, 1.0, 0.0);
        Vector3d target = faceNormal(face);
        Vector3d result = spaceUp.mul(1.0 - t).fma(t, target);
        if (result.lengthSquared() < 1.0E-12) return target;
        return result.normalize();
    }

    // ------------------------------------------------------------------
    // Render matrices
    // ------------------------------------------------------------------

    /**
     * The local vector a renderer should feed the matrices below, for terrain
     * at face-local {@code (u, v)} and world height {@code y}.
     *
     * <p>{@code outwardNudge} lifts geometry a fraction of a block off the face
     * plane so LOD terrain never z-fights the vanilla chunks it overlaps.</p>
     */
    public Vector3d localVector(double surfaceX, double worldY, double surfaceZ,
                                double outwardNudge) {
        return new Vector3d(surfaceX,
                faceHalfSpan() + (worldY - seaLevel) + outwardNudge,
                surfaceZ);
    }

    /**
     * Places a face's local vectors at their true flat world coordinates.
     * Rotation is identity: the flat world is the identity projection.
     */
    public Matrix4d flatMatrix(CubeNetSurfaceTransform.Face face) {
        Objects.requireNonNull(face, "face");
        return new Matrix4d().translation(
                transform.faceCenterX(face),
                seaLevel - faceHalfSpan(),
                transform.faceCenterZ(face));
    }

    /**
     * Places a face's local vectors where they belong once the world has folded
     * into a cube seen from {@code currentFace}.
     *
     * <p>The result is expressed in the same world coordinates as
     * {@link #flatMatrix} so the two can be mixed directly.</p>
     */
    public Matrix4d cubeMatrix(CubeNetSurfaceTransform.Face face,
                               CubeNetSurfaceTransform.Face currentFace) {
        Objects.requireNonNull(face, "face");
        Objects.requireNonNull(currentFace, "currentFace");
        Matrix3d basis = faceBasis(face);
        Matrix3d viewer = faceBasis(currentFace);
        // viewer^T * face maps the face's local axes into the viewer's flat
        // surface basis, which is exactly CubeFaceFrame.celestialToSurface.
        Matrix3d combined = viewer.transpose(new Matrix3d()).mul(basis);
        return new Matrix4d()
                .translation(transform.faceCenterX(currentFace),
                        seaLevel - faceHalfSpan(),
                        transform.faceCenterZ(currentFace))
                .mul(new Matrix4d().set(combined));
    }

    /**
     * The continuous flat-to-cube morph, as a single affine transform.
     *
     * <p>Component-wise interpolation of the two matrices is identical to
     * interpolating every transformed vertex, because both are affine in the
     * same local vector. One matrix per face per frame therefore replaces a
     * per-vertex CPU morph, and the geometry can live in an immutable GPU
     * buffer.</p>
     *
     * @param fold 0 = entirely flat world, 1 = entirely cube-projected
     */
    public Matrix4d foldMatrix(CubeNetSurfaceTransform.Face face,
                               CubeNetSurfaceTransform.Face currentFace,
                               double fold) {
        double t = Math.max(0.0, Math.min(1.0, fold));
        if (face == currentFace || t <= 0.0) {
            // Identical results, but skipping the mix keeps the camera's own
            // face bit-for-bit stable no matter what the fold schedule does.
            return flatMatrix(face);
        }
        Matrix4d flat = flatMatrix(face);
        Matrix4d cube = cubeMatrix(face, currentFace);
        return flat.lerp(cube, t);
    }

    /**
     * Places a face's local vectors into the orbital render frame: cube space
     * scaled to the rendered planet, rotated by the celestial's rotation and
     * translated to where the planet is drawn.
     */
    public Matrix4d orbitMatrix(CubeNetSurfaceTransform.Face face,
                                Vector3dc planetPosition,
                                Quaterniond rotation,
                                double renderScale) {
        Objects.requireNonNull(face, "face");
        Objects.requireNonNull(planetPosition, "planetPosition");
        return new Matrix4d()
                .translation(planetPosition.x(), planetPosition.y(), planetPosition.z())
                .rotate(rotation)
                .scale(renderScale)
                .mul(new Matrix4d().set(faceBasis(face)));
    }

    private static Matrix3d faceBasis(CubeNetSurfaceTransform.Face face) {
        CubeFaceFrame.Axes axes = CubeFaceFrame.axes(face);
        return new Matrix3d()
                .setColumn(0, axes.right())
                .setColumn(1, axes.up())
                .setColumn(2, axes.south());
    }
}
