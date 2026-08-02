package shipwrights.genesis.space.voxel;

import java.util.Arrays;

/** Builds one parent brick from an aligned 2x2x2 group of child bricks. */
public final class PlanetVoxelReducer {
    private PlanetVoxelReducer() {
    }

    public static PlanetVoxelBrick reduce(PlanetVoxelBrick[] children, long revision) {
        return reduce(children, null, revision);
    }

    /**
     * Reduces children while retaining the existing coarser cell wherever a
     * child octant is not resident. This is what lets exact data progressively
     * replace predicted terrain without opening holes between them.
     */
    public static PlanetVoxelBrick reduce(PlanetVoxelBrick[] children,
                                          PlanetVoxelBrick fallback,
                                          long revision) {
        if (children == null || children.length != 8) {
            throw new IllegalArgumentException("exactly eight child slots are required");
        }
        PlanetVoxelBrick first = Arrays.stream(children).filter(value -> value != null)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("at least one child is required"));
        PlanetVoxelBrickKey parentKey = first.key().parent();
        int childLod = first.key().lod();
        if (fallback != null && !fallback.key().equals(parentKey)) {
            throw new IllegalArgumentException("fallback " + fallback.key() + " is not " + parentKey);
        }
        for (int index = 0; index < children.length; index++) {
            PlanetVoxelBrick child = children[index];
            if (child == null) continue;
            if (child.key().lod() != childLod || !child.key().parent().equals(parentKey)) {
                throw new IllegalArgumentException("misaligned child " + child.key() + " for " + parentKey);
            }
            if (child.key().childIndexWithinParent() != index) {
                throw new IllegalArgumentException("child is in slot " + index + " but key says "
                        + child.key().childIndexWithinParent());
            }
        }

        PlanetVoxelBrickBuilder output = new PlanetVoxelBrickBuilder(parentKey, revision);
        long[] materials = new long[8];
        int[] coverage = new int[8];
        PlanetVoxelAuthority[] authorities = new PlanetVoxelAuthority[8];
        for (int y = 0; y < PlanetVoxelBrick.EDGE; y++) {
            for (int z = 0; z < PlanetVoxelBrick.EDGE; z++) {
                for (int x = 0; x < PlanetVoxelBrick.EDGE; x++) {
                    int totalCoverage = 0;
                    int cursor = 0;
                    PlanetVoxelAuthority strongest = PlanetVoxelAuthority.EMPTY;
                    for (int dz = 0; dz < 2; dz++) {
                        for (int dy = 0; dy < 2; dy++) {
                            for (int dx = 0; dx < 2; dx++) {
                                Sample sample = sample(children, fallback, x, y, z,
                                        x * 2 + dx, y * 2 + dy, z * 2 + dz);
                                materials[cursor] = sample.material;
                                coverage[cursor] = sample.coverage;
                                authorities[cursor] = sample.authority;
                                totalCoverage += sample.coverage;
                                if (sample.authority.outranks(strongest)) strongest = sample.authority;
                                cursor++;
                            }
                        }
                    }
                    Selected selected = selectRepresentative(materials, coverage, authorities);
                    int parentCoverage = (totalCoverage + 4) / 8;
                    PlanetVoxelAuthority outputAuthority = selected.material == PlanetVoxelMaterial.AIR
                            ? strongest : selected.authority;
                    output.set(x, y, z, selected.material, parentCoverage, outputAuthority);
                }
            }
        }
        return output.build();
    }

    private static Selected selectRepresentative(long[] materials, int[] coverage,
                                                  PlanetVoxelAuthority[] authorities) {
        long best = PlanetVoxelMaterial.AIR;
        PlanetVoxelAuthority bestAuthority = PlanetVoxelAuthority.EMPTY;
        long bestScore = Long.MIN_VALUE;
        for (int i = 0; i < materials.length; i++) {
            long candidate = materials[i];
            if (candidate == PlanetVoxelMaterial.AIR || coverage[i] == 0) continue;
            int matchingCoverage = 0;
            int matchingCount = 0;
            for (int j = 0; j < materials.length; j++) {
                if (sameVisualMaterial(candidate, materials[j])) {
                    matchingCoverage += coverage[j];
                    matchingCount++;
                }
            }
            long sourcePriority = Byte.toUnsignedLong(authorities[i].code());
            long priority = PlanetVoxelMaterial.reductionPriority(candidate);
            long score = sourcePriority * 1_000_000_000_000_000L
                    + priority * 1_000_000L
                    + (long) matchingCoverage * 1_000L
                    + (long) matchingCount * 10L
                    + i;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
                bestAuthority = authorities[i];
            }
        }
        return new Selected(best, bestAuthority);
    }

    private static boolean sameVisualMaterial(long a, long b) {
        return a == b || (a != PlanetVoxelMaterial.AIR && b != PlanetVoxelMaterial.AIR
                && PlanetVoxelMaterial.blockStateId(a) == PlanetVoxelMaterial.blockStateId(b)
                && PlanetVoxelMaterial.flags(a) == PlanetVoxelMaterial.flags(b));
    }

    private static Sample sample(PlanetVoxelBrick[] children, PlanetVoxelBrick fallback,
                                 int parentX, int parentY, int parentZ,
                                 int x, int y, int z) {
        int childX = x >>> 4;
        int childY = y >>> 4;
        int childZ = z >>> 4;
        int childIndex = childX | (childY << 1) | (childZ << 2);
        PlanetVoxelBrick child = children[childIndex];
        if (child == null) {
            return fallback == null ? Sample.EMPTY : new Sample(
                    fallback.material(parentX, parentY, parentZ),
                    fallback.coverage(parentX, parentY, parentZ),
                    fallback.authority(parentX, parentY, parentZ));
        }
        int localX = x & 15;
        int localY = y & 15;
        int localZ = z & 15;
        return new Sample(child.material(localX, localY, localZ),
                child.coverage(localX, localY, localZ),
                child.authority(localX, localY, localZ));
    }

    private record Selected(long material, PlanetVoxelAuthority authority) {
    }

    private record Sample(long material, int coverage, PlanetVoxelAuthority authority) {
        private static final Sample EMPTY = new Sample(PlanetVoxelMaterial.AIR, 0,
                PlanetVoxelAuthority.EMPTY);
    }
}
