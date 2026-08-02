package shipwrights.genesis.space.voxel;

import java.util.ArrayList;
import java.util.List;

/** CPU greedy mesher for one palette-compressed brick. */
public final class PlanetVoxelGreedyMesher {
    public enum FaceDirection {
        DOWN(0, -1, 0), UP(0, 1, 0), NORTH(0, 0, -1), SOUTH(0, 0, 1), WEST(-1, 0, 0), EAST(1, 0, 0);
        private final int stepX, stepY, stepZ;
        FaceDirection(int stepX, int stepY, int stepZ) { this.stepX = stepX; this.stepY = stepY; this.stepZ = stepZ; }
        public int stepX() { return stepX; } public int stepY() { return stepY; } public int stepZ() { return stepZ; }
    }
    public interface NeighbourLookup {
        long material(int x, int y, int z);

        int coverage(int x, int y, int z);
    }

    /**
     * a/b axes depend on direction: UP/DOWN=(x,z), NORTH/SOUTH=(x,y),
     * WEST/EAST=(z,y). plane is the fixed x/y/z coordinate in [0,16].
     */
    public record Quad(FaceDirection direction, int plane,
                       int a0, int b0, int a1, int b1,
                       long material, int coverage) {
        public int area() {
            return (a1 - a0) * (b1 - b0);
        }
    }

    public record Mesh(PlanetVoxelBrickKey key, long revision,
                       Quad[] solid, Quad[] translucent) {
        public int quadCount() {
            return solid.length + translucent.length;
        }
    }

    private PlanetVoxelGreedyMesher() {
    }

    public static Mesh mesh(PlanetVoxelBrick brick, NeighbourLookup neighbours) {
        List<Quad> solid = new ArrayList<>();
        List<Quad> translucent = new ArrayList<>();
        for (FaceDirection direction : FaceDirection.values()) {
            meshDirection(brick, neighbours, direction, solid, translucent);
        }
        return new Mesh(brick.key(), brick.revision(),
                solid.toArray(Quad[]::new), translucent.toArray(Quad[]::new));
    }

    private static void meshDirection(PlanetVoxelBrick brick, NeighbourLookup neighbours,
                                      FaceDirection direction, List<Quad> solid,
                                      List<Quad> translucent) {
        Cell[] mask = new Cell[PlanetVoxelBrick.EDGE * PlanetVoxelBrick.EDGE];
        for (int slice = 0; slice < PlanetVoxelBrick.EDGE; slice++) {
            for (int b = 0; b < PlanetVoxelBrick.EDGE; b++) {
                for (int a = 0; a < PlanetVoxelBrick.EDGE; a++) {
                    int x, y, z;
                    switch (direction) {
                        case UP, DOWN -> { x = a; y = slice; z = b; }
                        case NORTH, SOUTH -> { x = a; y = b; z = slice; }
                        case WEST, EAST -> { x = slice; y = b; z = a; }
                        default -> throw new IllegalStateException();
                    }
                    long material = brick.material(x, y, z);
                    int coverage = brick.coverage(x, y, z);
                    if (material == PlanetVoxelMaterial.AIR || coverage == 0) {
                        mask[b * PlanetVoxelBrick.EDGE + a] = null;
                        continue;
                    }
                    int nx = x + direction.stepX();
                    int ny = y + direction.stepY();
                    int nz = z + direction.stepZ();
                    long neighbour = sampleMaterial(brick, neighbours, nx, ny, nz);
                    int neighbourCoverage = sampleCoverage(brick, neighbours, nx, ny, nz);
                    if (!faceVisible(material, coverage, neighbour, neighbourCoverage)) {
                        mask[b * PlanetVoxelBrick.EDGE + a] = null;
                        continue;
                    }
                    mask[b * PlanetVoxelBrick.EDGE + a] = new Cell(material, coverage);
                }
            }
            greedy(mask, direction, facePlane(direction, slice), solid, translucent);
        }
    }

    private static void greedy(Cell[] mask, FaceDirection direction, int plane,
                               List<Quad> solid, List<Quad> translucent) {
        boolean[] used = new boolean[mask.length];
        for (int b = 0; b < PlanetVoxelBrick.EDGE; b++) {
            for (int a = 0; a < PlanetVoxelBrick.EDGE; a++) {
                int index = b * PlanetVoxelBrick.EDGE + a;
                Cell cell = mask[index];
                if (cell == null || used[index]) continue;
                int width = 1;
                while (a + width < PlanetVoxelBrick.EDGE) {
                    int next = index + width;
                    if (used[next] || !cell.equals(mask[next])) break;
                    width++;
                }
                int height = 1;
                outer:
                while (b + height < PlanetVoxelBrick.EDGE) {
                    int row = (b + height) * PlanetVoxelBrick.EDGE + a;
                    for (int x = 0; x < width; x++) {
                        if (used[row + x] || !cell.equals(mask[row + x])) break outer;
                    }
                    height++;
                }
                for (int dy = 0; dy < height; dy++) {
                    int row = (b + dy) * PlanetVoxelBrick.EDGE + a;
                    for (int dx = 0; dx < width; dx++) used[row + dx] = true;
                }
                Quad quad = new Quad(direction, plane, a, b, a + width, b + height,
                        cell.material, cell.coverage);
                int flags = PlanetVoxelMaterial.flags(cell.material);
                if ((flags & (PlanetVoxelMaterial.FLAG_TRANSLUCENT
                        | PlanetVoxelMaterial.FLAG_LIQUID)) != 0) translucent.add(quad);
                else solid.add(quad);
            }
        }
    }

    private static boolean faceVisible(long material, int coverage,
                                       long neighbour, int neighbourCoverage) {
        if (neighbour == PlanetVoxelMaterial.AIR || neighbourCoverage == 0) return true;
        int currentFlags = PlanetVoxelMaterial.flags(material);
        int neighbourFlags = PlanetVoxelMaterial.flags(neighbour);
        boolean currentTranslucent = (currentFlags & (PlanetVoxelMaterial.FLAG_TRANSLUCENT
                | PlanetVoxelMaterial.FLAG_LIQUID)) != 0;
        boolean neighbourTranslucent = (neighbourFlags & (PlanetVoxelMaterial.FLAG_TRANSLUCENT
                | PlanetVoxelMaterial.FLAG_LIQUID)) != 0;
        if (currentTranslucent) return material != neighbour || coverage > neighbourCoverage;
        return neighbourTranslucent || neighbourCoverage < coverage;
    }

    private static long sampleMaterial(PlanetVoxelBrick brick, NeighbourLookup neighbours,
                                       int x, int y, int z) {
        if (inside(x, y, z)) return brick.material(x, y, z);
        return neighbours == null ? PlanetVoxelMaterial.AIR : neighbours.material(x, y, z);
    }

    private static int sampleCoverage(PlanetVoxelBrick brick, NeighbourLookup neighbours,
                                      int x, int y, int z) {
        if (inside(x, y, z)) return brick.coverage(x, y, z);
        return neighbours == null ? 0 : neighbours.coverage(x, y, z);
    }

    private static boolean inside(int x, int y, int z) {
        return x >= 0 && x < PlanetVoxelBrick.EDGE
                && y >= 0 && y < PlanetVoxelBrick.EDGE
                && z >= 0 && z < PlanetVoxelBrick.EDGE;
    }

    private static int facePlane(FaceDirection direction, int slice) {
        return switch (direction) {
            case UP, SOUTH, EAST -> slice + 1;
            case DOWN, NORTH, WEST -> slice;
        };
    }

    private record Cell(long material, int coverage) {
    }
}
