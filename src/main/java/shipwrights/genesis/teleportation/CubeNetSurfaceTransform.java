package shipwrights.genesis.teleportation;

import java.util.Objects;

/**
 * The single invertible transform between a celestial's rotating local frame
 * and the six surface regions in its planet dimension.
 *
 * <p>The surface world is a 3x2 cube net using the exact same face order as the
 * planet texture atlas:</p>
 *
 * <pre>
 * north | west | south
 * east  | down | up
 * </pre>
 *
 * <p>The up-face region is centred on world {@code (0, 0)}. This deliberately
 * preserves Genesis' old top-face mapping: a point entering through +Y lands at
 * the same X/Z it did before this transform existed. The other five regions are
 * offset by whole face spans into the rest of the net.</p>
 *
 * <p>Coordinates named {@code u}/{@code v} follow texture orientation, not an
 * arbitrary mathematical convention. Increasing {@code u} moves right in the
 * face texture and increasing {@code v} moves down. That is what makes a world
 * column, its atlas texel, and the point visible on the rendered cube all name
 * the same physical place.</p>
 */
public final class CubeNetSurfaceTransform {
    /** One space block represents sixteen planet blocks. */
    public static final double PLANET_BLOCKS_PER_SPACE_BLOCK = 16.0;

    private static final double ZERO_EPSILON = 1.0E-12;
    private static final double EDGE_EPSILON = 1.0E-9;

    /**
     * Atlas order is intentional. Face ordinal is also the face-array order used
     * by planet surface packets and textures.
     */
    public enum Face {
        NORTH(0, 0),
        WEST(1, 0),
        SOUTH(2, 0),
        EAST(0, 1),
        DOWN(1, 1),
        UP(2, 1);

        private final int atlasColumn;
        private final int atlasRow;

        Face(int atlasColumn, int atlasRow) {
            this.atlasColumn = atlasColumn;
            this.atlasRow = atlasRow;
        }

        public int atlasColumn() {
            return atlasColumn;
        }

        public int atlasRow() {
            return atlasRow;
        }

        /** Whole-face offset from the up-face centre. */
        public int netColumnOffset() {
            return atlasColumn - UP.atlasColumn;
        }

        /** Whole-face offset from the up-face centre. */
        public int netRowOffset() {
            return atlasRow - UP.atlasRow;
        }

        private static Face atAtlasCell(int column, int row) {
            for (Face face : values()) {
                if (face.atlasColumn == column && face.atlasRow == row) {
                    return face;
                }
            }
            throw new IllegalArgumentException("No cube face at atlas cell " + column + "," + row);
        }
    }

    /** A point in the planet world and the face-local coordinates it came from. */
    public record SurfacePoint(Face face, double worldX, double worldZ, double u, double v) {
        public SurfacePoint {
            Objects.requireNonNull(face, "face");
        }
    }

    /**
     * A point on the padded entry cube in the celestial's rotating local frame.
     * {@code outsideNetFallback} is true only for legacy/command positions that
     * are not inside any of the six cube-net regions.
     */
    public record LocalPoint(Face face, double x, double y, double z,
                             double u, double v, boolean outsideNetFallback) {
        public LocalPoint {
            Objects.requireNonNull(face, "face");
        }
    }

    private final double celestialSize;
    private final double halfExtent;
    private final double entryPadding;
    private final double entryRadius;
    private final double surfaceScale;
    private final double faceSpan;
    private final double faceHalfSpan;

    public CubeNetSurfaceTransform(double celestialSize, double entryPadding) {
        if (!Double.isFinite(celestialSize) || celestialSize <= 0.0) {
            throw new IllegalArgumentException("celestialSize must be finite and positive");
        }
        if (!Double.isFinite(entryPadding) || entryPadding < 0.0) {
            throw new IllegalArgumentException("entryPadding must be finite and non-negative");
        }
        this.celestialSize = celestialSize;
        this.halfExtent = celestialSize * 0.5;
        this.entryPadding = entryPadding;
        this.entryRadius = halfExtent + entryPadding;
        this.surfaceScale = halfExtent / entryRadius * PLANET_BLOCKS_PER_SPACE_BLOCK;
        this.faceSpan = celestialSize * PLANET_BLOCKS_PER_SPACE_BLOCK;
        this.faceHalfSpan = faceSpan * 0.5;
    }

    public double celestialSize() {
        return celestialSize;
    }

    public double halfExtent() {
        return halfExtent;
    }

    public double entryPadding() {
        return entryPadding;
    }

    public double entryRadius() {
        return entryRadius;
    }

    public double surfaceScale() {
        return surfaceScale;
    }

    public double faceSpan() {
        return faceSpan;
    }

    public double faceHalfSpan() {
        return faceHalfSpan;
    }

    public double faceCenterX(Face face) {
        return face.netColumnOffset() * faceSpan;
    }

    public double faceCenterZ(Face face) {
        return face.netRowOffset() * faceSpan;
    }

    /**
     * Projects any non-zero local-space point radially onto the padded entry
     * cube, selects the face it actually crosses, and maps it into the matching
     * world region. The origin has no direction and therefore maps to the centre
     * of the up face as the safe legacy default.
     */
    public SurfacePoint localToWorld(double localX, double localY, double localZ) {
        requireFinite(localX, "localX");
        requireFinite(localY, "localY");
        requireFinite(localZ, "localZ");

        double reach = Math.max(Math.abs(localX), Math.max(Math.abs(localY), Math.abs(localZ)));
        if (reach < ZERO_EPSILON) {
            return faceToWorld(Face.UP, 0.0, 0.0);
        }

        double shellScale = entryRadius / reach;
        double x = localX * shellScale;
        double y = localY * shellScale;
        double z = localZ * shellScale;
        Face face = dominantFace(x, y, z);
        double[] uv = faceCoordinates(face, x, y, z);
        return faceToWorld(face, uv[0], uv[1]);
    }

    /** Maps known face-local entry-cube coordinates into the planet world. */
    public SurfacePoint faceToWorld(Face face, double u, double v) {
        Objects.requireNonNull(face, "face");
        requireFinite(u, "u");
        requireFinite(v, "v");
        double worldX = faceCenterX(face) + u * surfaceScale;
        double worldZ = faceCenterZ(face) + v * surfaceScale;
        return new SurfacePoint(face, worldX, worldZ, u, v);
    }

    /**
     * Inverts a planet-world position back to the padded entry cube.
     *
     * <p>Positions inside the 3x2 net are exact. A position outside the net can
     * only have come from a command, an old save explored beyond the original
     * reachable square, or another mod. Those positions safely leave through
     * the up face, clamped to its edge, instead of producing an impossible local
     * point whose in-face component is larger than the face normal.</p>
     */
    public LocalPoint worldToLocal(double worldX, double worldZ) {
        requireFinite(worldX, "worldX");
        requireFinite(worldZ, "worldZ");

        Face face = faceContaining(worldX, worldZ);
        boolean fallback = face == null;
        if (fallback) {
            face = Face.UP;
        }

        double surfaceU = worldX - faceCenterX(face);
        double surfaceV = worldZ - faceCenterZ(face);
        if (fallback) {
            surfaceU = clamp(surfaceU, -faceHalfSpan, faceHalfSpan);
            surfaceV = clamp(surfaceV, -faceHalfSpan, faceHalfSpan);
        }

        double u = surfaceU / surfaceScale;
        double v = surfaceV / surfaceScale;
        // Shared edges are valid on two faces. Keep numerical noise from
        // making an in-face component exceed the entry cube's normal.
        u = clamp(u, -entryRadius, entryRadius);
        v = clamp(v, -entryRadius, entryRadius);
        return faceToLocal(face, u, v, fallback);
    }

    /** Builds the exact padded-cube point represented by a face and texture axes. */
    public LocalPoint faceToLocal(Face face, double u, double v) {
        return faceToLocal(face, u, v, false);
    }

    private LocalPoint faceToLocal(Face face, double u, double v, boolean fallback) {
        Objects.requireNonNull(face, "face");
        requireFinite(u, "u");
        requireFinite(v, "v");
        return switch (face) {
            // u right, v down, matching PlanetRenderer's UV orientation.
            case NORTH -> new LocalPoint(face, -u, -v, -entryRadius, u, v, fallback);
            case WEST -> new LocalPoint(face, -entryRadius, -v, u, u, v, fallback);
            case SOUTH -> new LocalPoint(face, u, -v, entryRadius, u, v, fallback);
            case EAST -> new LocalPoint(face, entryRadius, -v, -u, u, v, fallback);
            case DOWN -> new LocalPoint(face, u, -entryRadius, -v, u, v, fallback);
            case UP -> new LocalPoint(face, u, entryRadius, v, u, v, fallback);
        };
    }

    /** Which net region contains this world position, or null outside the 3x2 rectangle. */
    public Face faceContaining(double worldX, double worldZ) {
        double minimumX = -2.5 * faceSpan;
        double maximumX = 0.5 * faceSpan;
        double minimumZ = -1.5 * faceSpan;
        double maximumZ = 0.5 * faceSpan;

        if (worldX < minimumX - EDGE_EPSILON || worldX > maximumX + EDGE_EPSILON
                || worldZ < minimumZ - EDGE_EPSILON || worldZ > maximumZ + EDGE_EPSILON) {
            return null;
        }

        double boundedX = worldX >= maximumX ? Math.nextDown(maximumX) : Math.max(worldX, minimumX);
        double boundedZ = worldZ >= maximumZ ? Math.nextDown(maximumZ) : Math.max(worldZ, minimumZ);
        int column = (int) Math.floor((boundedX - minimumX) / faceSpan);
        int row = (int) Math.floor((boundedZ - minimumZ) / faceSpan);
        if (column < 0 || column >= 3 || row < 0 || row >= 2) {
            return null;
        }
        return Face.atAtlasCell(column, row);
    }

    private static Face dominantFace(double x, double y, double z) {
        double ax = Math.abs(x);
        double ay = Math.abs(y);
        double az = Math.abs(z);

        // Y owns exact ties to preserve the old up-face projection along the
        // top/bottom rim. Z then owns ties against X. The choice only matters on
        // mathematically zero-width seams; it is deterministic so round trips do
        // not jitter from floating-point noise.
        if (ay >= ax && ay >= az) {
            return y >= 0.0 ? Face.UP : Face.DOWN;
        }
        if (az >= ax) {
            return z >= 0.0 ? Face.SOUTH : Face.NORTH;
        }
        return x >= 0.0 ? Face.EAST : Face.WEST;
    }

    private static double[] faceCoordinates(Face face, double x, double y, double z) {
        return switch (face) {
            case NORTH -> new double[]{-x, -y};
            case WEST -> new double[]{z, -y};
            case SOUTH -> new double[]{x, -y};
            case EAST -> new double[]{-z, -y};
            case DOWN -> new double[]{x, -z};
            case UP -> new double[]{x, z};
        };
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
