package shipwrights.genesis.space.voxel;

import java.util.Arrays;

/** Primitive palette builder without per-voxel boxing. */
public final class PlanetVoxelBrickBuilder {
    private static final float LOAD_FACTOR = 0.60f;

    private final PlanetVoxelBrickKey key;
    private final long revision;
    private final short[] indices = new short[PlanetVoxelBrick.CELL_COUNT];
    private final byte[] coverage = new byte[PlanetVoxelBrick.CELL_COUNT];
    private final byte[] authority = new byte[PlanetVoxelBrick.CELL_COUNT];

    private long[] palette = new long[64];
    private int paletteSize = 1;
    private long[] hashKeys = new long[128];
    private int[] hashValues = new int[128];
    private int hashMask = hashKeys.length - 1;

    public PlanetVoxelBrickBuilder(PlanetVoxelBrickKey key, long revision) {
        this.key = key;
        this.revision = revision;
        palette[0] = PlanetVoxelMaterial.AIR;
        putHash(PlanetVoxelMaterial.AIR, 0);
    }

    public PlanetVoxelBrickBuilder set(int x, int y, int z, long material) {
        return set(x, y, z, material,
                material == PlanetVoxelMaterial.AIR ? 0 : 255,
                PlanetVoxelAuthority.GENERATED_EXACT);
    }

    public PlanetVoxelBrickBuilder set(int x, int y, int z, long material,
                                       int voxelCoverage) {
        return set(x, y, z, material, voxelCoverage,
                PlanetVoxelAuthority.GENERATED_EXACT);
    }

    public PlanetVoxelBrickBuilder set(int x, int y, int z, long material,
                                       int voxelCoverage, PlanetVoxelAuthority source) {
        int cell = PlanetVoxelBrick.index(x, y, z);
        authority[cell] = source.code();
        if (material == PlanetVoxelMaterial.AIR || voxelCoverage <= 0) {
            indices[cell] = 0;
            coverage[cell] = 0;
            return this;
        }
        int paletteIndex = paletteIndex(material);
        indices[cell] = (short) paletteIndex;
        coverage[cell] = (byte) Math.max(1, Math.min(255, voxelCoverage));
        return this;
    }

    public PlanetVoxelBrickBuilder setAir(int x, int y, int z,
                                          PlanetVoxelAuthority source) {
        return set(x, y, z, PlanetVoxelMaterial.AIR, 0, source);
    }

    public PlanetVoxelBrickBuilder fillAuthority(PlanetVoxelAuthority source) {
        Arrays.fill(authority, source.code());
        return this;
    }

    public PlanetVoxelBrick build() {
        return new PlanetVoxelBrick(key, revision,
                Arrays.copyOf(palette, paletteSize), indices, coverage, authority);
    }

    private int paletteIndex(long material) {
        int slot = locate(material);
        int existing = hashValues[slot];
        if (existing != 0 || hashKeys[slot] == material) return existing;
        if (paletteSize >= PlanetVoxelBrick.MAX_PALETTE) {
            throw new IllegalStateException("voxel palette exceeded 65535 entries");
        }
        if (paletteSize == palette.length) palette = Arrays.copyOf(palette, palette.length << 1);
        int index = paletteSize++;
        palette[index] = material;
        hashKeys[slot] = material;
        hashValues[slot] = index;
        if (paletteSize > hashKeys.length * LOAD_FACTOR) rehash();
        return index;
    }

    private int locate(long key) {
        int slot = mix(key) & hashMask;
        while (hashValues[slot] != 0 || hashKeys[slot] != 0L) {
            if (hashKeys[slot] == key) return slot;
            slot = (slot + 1) & hashMask;
        }
        return slot;
    }

    private void putHash(long key, int value) {
        int slot = locate(key);
        hashKeys[slot] = key;
        hashValues[slot] = value;
    }

    private void rehash() {
        long[] oldKeys = hashKeys;
        int[] oldValues = hashValues;
        hashKeys = new long[oldKeys.length << 1];
        hashValues = new int[hashKeys.length];
        hashMask = hashKeys.length - 1;
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldValues[i] != 0 || oldKeys[i] == PlanetVoxelMaterial.AIR) {
                putHash(oldKeys[i], oldValues[i]);
            }
        }
    }

    private static int mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        value ^= value >>> 33;
        return (int) value;
    }
}
