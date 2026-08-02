package shipwrights.genesis.space.surface;

import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

/**
 * One planet's six cube-net LOD surfaces sampled from its own Minecraft world.
 *
 * <p>Arrays are face-major in the exact atlas order declared by
 * {@link CubeNetSurfaceTransform.Face}: north, west, south, east, down, up.
 * Each value is one coarse world column, not a painted biome pixel. The colour
 * comes from the generated column's visible block material and the height is
 * the actual generated surface Y used to displace the orbital cube.</p>
 *
 * @param colours six face grids of ARGB block-derived colours, face-major
 * @param heights the same six grids, as world Y of each LOD column's surface
 * @param minHeight lowest sampled column across all six regions
 * @param maxHeight highest sampled column across all six regions
 * @param seaLevel generator sea level; the cube's base half-extent is anchored here
 */
public record PlanetSurfaceData(int[] colours, short[] heights, int minHeight,
                                int maxHeight, int seaLevel) {
    public static int faceLength() {
        return PlanetSurfaceSampler.RESOLUTION * PlanetSurfaceSampler.RESOLUTION;
    }

    public static int expectedLength() {
        return CubeNetSurfaceTransform.Face.values().length * faceLength();
    }

    public static int faceOffset(CubeNetSurfaceTransform.Face face) {
        return face.ordinal() * faceLength();
    }

    public static int index(CubeNetSurfaceTransform.Face face, int x, int y) {
        int resolution = PlanetSurfaceSampler.RESOLUTION;
        if (x < 0 || x >= resolution || y < 0 || y >= resolution) {
            throw new IndexOutOfBoundsException("surface texel " + x + "," + y);
        }
        return faceOffset(face) + y * resolution + x;
    }

    /** Whether this is a complete, self-consistent six-face LOD atlas. */
    public boolean isValid() {
        return colours != null && heights != null
                && colours.length == expectedLength()
                && heights.length == expectedLength();
    }

    /** Terrain displacement relative to sea level, in planet blocks. */
    public double elevation(int index) {
        return heights[index] - (double) seaLevel;
    }
}
