package shipwrights.genesis.space;

import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Coordinate mapping for a cubed planet (the Earth).
 *
 * <p>The planet is a cube in space, but its surface lives in a flat Minecraft world. This class
 * gives each of the six cube faces its own distinct overworld landing region (laid out as a cross
 * "net" around the origin), and maps <em>where on a face</em> you approach to <em>where in that
 * region</em> you land — so taking off from the top and landing on the left really does put you in
 * a different place, and taking off from a region again exits toward the matching face.</p>
 *
 * <p>Pure math, no Minecraft types — unit tested in {@code CubePlanetMappingTest}.</p>
 */
public final class CubePlanetMapping {

    private CubePlanetMapping() {}

    /** The six faces of the cubed planet, with their outward local-space normals. */
    public enum Face {
        UP(0, 1, 0), DOWN(0, -1, 0), NORTH(0, 0, -1), SOUTH(0, 0, 1), EAST(1, 0, 0), WEST(-1, 0, 0);

        public final double nx, ny, nz;

        Face(double nx, double ny, double nz) {
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }

        public Vector3d normal() {
            return new Vector3d(nx, ny, nz);
        }
    }

    /**
     * The centre (overworld x, z) of the landing region for a face, in a cross-net layout so the
     * six faces never collapse onto the same coordinates.
     */
    public static double[] regionCenter(Face face, int spacing) {
        return switch (face) {
            case UP -> new double[]{0, 0};
            case NORTH -> new double[]{0, -spacing};
            case SOUTH -> new double[]{0, spacing};
            case EAST -> new double[]{spacing, 0};
            case WEST -> new double[]{-spacing, 0};
            case DOWN -> new double[]{0, 2L * spacing}; // opposite UP, beyond SOUTH in the net
        };
    }

    /** The face a viewer is over, given the direction from the planet's centre to the viewer (local frame). */
    public static Face faceFromApproach(Vector3dc dirFromCenter) {
        double ax = Math.abs(dirFromCenter.x());
        double ay = Math.abs(dirFromCenter.y());
        double az = Math.abs(dirFromCenter.z());
        if (ay >= ax && ay >= az) return dirFromCenter.y() >= 0 ? Face.UP : Face.DOWN;
        if (ax >= az) return dirFromCenter.x() >= 0 ? Face.EAST : Face.WEST;
        return dirFromCenter.z() >= 0 ? Face.SOUTH : Face.NORTH;
    }

    /**
     * The (u, v) position on a face where the ray from the planet centre along {@code dirFromCenter}
     * pierces it, each component in roughly [-1, 1] (centre of the face is (0,0)).
     */
    public static double[] faceUV(Face face, Vector3dc dir) {
        double x = dir.x(), y = dir.y(), z = dir.z();
        return switch (face) {
            case UP -> new double[]{clamp(x / safe(y)), clamp(z / safe(y))};
            case DOWN -> new double[]{clamp(x / safe(y)), clamp(-z / safe(y))};
            case EAST -> new double[]{clamp(-z / safe(x)), clamp(y / safe(x))};
            case WEST -> new double[]{clamp(z / safe(x)), clamp(y / safe(x))};
            case SOUTH -> new double[]{clamp(x / safe(z)), clamp(y / safe(z))};
            case NORTH -> new double[]{clamp(-x / safe(z)), clamp(y / safe(z))};
        };
    }

    /**
     * The overworld landing coordinate for an approach: which face, then where in that face's region.
     *
     * @param dirFromCenter direction from the planet's centre to the approaching craft, in the
     *                      planet's local frame (need not be normalised)
     * @param spacing       {@link #regionCenter} spacing
     * @param intraSpread   how far (blocks) the edge of a face is from its region centre
     */
    public static double[] landingXZ(Vector3dc dirFromCenter, int spacing, double intraSpread) {
        Face face = faceFromApproach(dirFromCenter);
        double[] c = regionCenter(face, spacing);
        double[] uv = faceUV(face, dirFromCenter);
        return new double[]{c[0] + uv[0] * intraSpread, c[1] + uv[1] * intraSpread};
    }

    /** Result of mapping an overworld launch position back onto the cube: the face and the local exit direction. */
    public record LaunchExit(Face face, Vector3d localExitDir) {}

    /**
     * The reverse map: from an overworld launch position, the face you should exit through and the
     * local-frame direction to leave the planet along — so leaving the region you landed in sends
     * you back out the same side of the cube.
     */
    public static LaunchExit exitFor(double overworldX, double overworldZ, int spacing, double intraSpread) {
        Face best = Face.UP;
        double bestDistSq = Double.MAX_VALUE;
        for (Face f : Face.values()) {
            double[] c = regionCenter(f, spacing);
            double dx = overworldX - c[0];
            double dz = overworldZ - c[1];
            double d = dx * dx + dz * dz;
            if (d < bestDistSq) {
                bestDistSq = d;
                best = f;
            }
        }
        double[] c = regionCenter(best, spacing);
        double u = clamp((overworldX - c[0]) / intraSpread);
        double v = clamp((overworldZ - c[1]) / intraSpread);

        // Rebuild a local direction: mostly the face normal, nudged by the intra-face offset.
        Vector3d dir = best.normal();
        switch (best) {
            case UP -> dir.add(u, 0, v);
            case DOWN -> dir.add(u, 0, -v);
            case EAST -> dir.add(0, v, -u);
            case WEST -> dir.add(0, v, u);
            case SOUTH -> dir.add(u, v, 0);
            case NORTH -> dir.add(-u, v, 0);
        }
        return new LaunchExit(best, dir.normalize());
    }

    private static double safe(double v) {
        double a = Math.abs(v);
        return a < 1.0e-6 ? (v < 0 ? -1.0e-6 : 1.0e-6) : v;
    }

    private static double clamp(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }
}
