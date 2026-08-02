package shipwrights.genesis.space.voxel;

import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.Objects;

/** Address of one 16^3 brick in a cube-face voxel pyramid. */
public record PlanetVoxelBrickKey(
        CubeNetSurfaceTransform.Face face,
        int lod,
        int brickU,
        int brickY,
        int brickV) {

    public static final int MAX_LOD = 12;

    public PlanetVoxelBrickKey {
        Objects.requireNonNull(face, "face");
        if (lod < 0 || lod > MAX_LOD) {
            throw new IllegalArgumentException("lod must be in [0," + MAX_LOD + "]: " + lod);
        }
    }

    public int cellSize() {
        return 1 << lod;
    }

    public int brickSpan() {
        return PlanetVoxelBrick.EDGE << lod;
    }

    public long minU() {
        return (long) brickU * brickSpan();
    }

    public long minY() {
        return (long) brickY * brickSpan();
    }

    public long minV() {
        return (long) brickV * brickSpan();
    }

    public PlanetVoxelBrickKey parent() {
        if (lod == MAX_LOD) return this;
        return new PlanetVoxelBrickKey(face, lod + 1,
                Math.floorDiv(brickU, 2),
                Math.floorDiv(brickY, 2),
                Math.floorDiv(brickV, 2));
    }

    public int childIndexWithinParent() {
        int x = Math.floorMod(brickU, 2);
        int y = Math.floorMod(brickY, 2);
        int z = Math.floorMod(brickV, 2);
        return x | (y << 1) | (z << 2);
    }
}
