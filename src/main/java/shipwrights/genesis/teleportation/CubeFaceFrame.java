package shipwrights.genesis.teleportation;

import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Objects;

/**
 * Orientation and geometry shared by cube-net travel, high-altitude world LOD,
 * and the orbital cube renderer.
 *
 * <p>The axes form the Minecraft surface basis for a face: {@code right} is
 * increasing world X inside that face's cube-net region, {@code up} is away
 * from the planet, and {@code south} is increasing world Z. Keeping this in one
 * place prevents the travel transform and the renderer from silently using
 * different face rotations.</p>
 */
public final class CubeFaceFrame {
    public record Axes(Vector3dc right, Vector3dc up, Vector3dc south) {
        public Axes {
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(up, "up");
            Objects.requireNonNull(south, "south");
        }
    }

    private CubeFaceFrame() {
    }

    public static Axes axes(CubeNetSurfaceTransform.Face face) {
        Objects.requireNonNull(face, "face");
        return switch (face) {
            case NORTH -> new Axes(
                    new Vector3d(-1.0, 0.0, 0.0),
                    new Vector3d(0.0, 0.0, -1.0),
                    new Vector3d(0.0, -1.0, 0.0));
            case WEST -> new Axes(
                    new Vector3d(0.0, 0.0, 1.0),
                    new Vector3d(-1.0, 0.0, 0.0),
                    new Vector3d(0.0, -1.0, 0.0));
            case SOUTH -> new Axes(
                    new Vector3d(1.0, 0.0, 0.0),
                    new Vector3d(0.0, 0.0, 1.0),
                    new Vector3d(0.0, -1.0, 0.0));
            case EAST -> new Axes(
                    new Vector3d(0.0, 0.0, -1.0),
                    new Vector3d(1.0, 0.0, 0.0),
                    new Vector3d(0.0, -1.0, 0.0));
            case DOWN -> new Axes(
                    new Vector3d(1.0, 0.0, 0.0),
                    new Vector3d(0.0, -1.0, 0.0),
                    new Vector3d(0.0, 0.0, -1.0));
            case UP -> new Axes(
                    new Vector3d(1.0, 0.0, 0.0),
                    new Vector3d(0.0, 1.0, 0.0),
                    new Vector3d(0.0, 0.0, 1.0));
        };
    }

    /** Rotates the ordinary Minecraft X/Y/Z surface basis into the face basis. */
    public static Quaterniond surfaceToCelestial(CubeNetSurfaceTransform.Face face) {
        Axes axes = axes(face);
        // Build the rotation from the declared basis directly. lookAlong uses
        // camera -Z semantics, which inverted the Y/Z basis on side faces and
        // made rendering and travel disagree about face orientation.
        Matrix3d basis = new Matrix3d()
                .setColumn(0, axes.right())
                .setColumn(1, axes.up())
                .setColumn(2, axes.south());
        return new Quaterniond().setFromNormalized(basis).normalize();
    }

    /**
     * A terrain point on a cube face in the planet's rotating local frame.
     * Elevation is measured outward from the face plane.
     */
    public static Vector3d cubePoint(CubeNetSurfaceTransform.Face face,
                                     double u, double v,
                                     double halfExtent, double elevation) {
        Axes axes = axes(face);
        return new Vector3d(axes.right()).mul(u)
                .fma(halfExtent + elevation, axes.up())
                .fma(v, axes.south());
    }

    /**
     * Converts a celestial-local vector into the flat world basis of the face
     * currently beneath the player.
     */
    public static Vector3d celestialToSurface(CubeNetSurfaceTransform.Face currentFace,
                                              Vector3dc celestialVector) {
        Axes current = axes(currentFace);
        return new Vector3d(
                celestialVector.dot(current.right()),
                celestialVector.dot(current.up()),
                celestialVector.dot(current.south()));
    }
}
