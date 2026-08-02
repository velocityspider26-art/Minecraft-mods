package shipwrights.genesis.space.voxel;

/**
 * Provenance of one voxel sample. Higher codes always supersede lower codes.
 * Authority is tracked per cell so exact air can carve predicted terrain while
 * untouched neighboring cells retain their coarse fallback.
 */
public enum PlanetVoxelAuthority {
    EMPTY(0),
    TERRAIN_PREDICTED(1),
    STRUCTURE_PREDICTED(2),
    GENERATED_EXACT(3),
    PLAYER_MODIFIED(4);

    private static final PlanetVoxelAuthority[] BY_CODE = values();
    private final byte code;

    PlanetVoxelAuthority(int code) {
        this.code = (byte) code;
    }

    public byte code() {
        return code;
    }

    public boolean outranks(PlanetVoxelAuthority other) {
        return Byte.toUnsignedInt(code) > Byte.toUnsignedInt(other.code);
    }

    public static PlanetVoxelAuthority fromCode(byte code) {
        int index = Byte.toUnsignedInt(code);
        if (index >= BY_CODE.length) {
            throw new IllegalArgumentException("invalid voxel authority " + index);
        }
        return BY_CODE[index];
    }
}
