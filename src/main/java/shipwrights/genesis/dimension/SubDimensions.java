package shipwrights.genesis.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import shipwrights.genesis.GenesisMod;

import java.util.Set;

/**
 * The dimensions that are rooms in one house rather than separate houses.
 *
 * <p>Vanilla treats every dimension as unrelated, and pays for it on arrival:
 * changing dimension throws away the client's whole render section grid and
 * builds a new one, because the destination could be any shape. Genesis rooms
 * deliberately share one section geometry, so the standing grid can be reused
 * after the destination is verified to have the same floorplan.</p>
 */
public final class SubDimensions {
    /**
     * The single source of truth for built-in rooms.
     *
     * <p>Keep every room at {@code min_y: -64, height: 384}. New built-in
     * Genesis dimensions must be added here; render-grid reuse and transition
     * screen registration both read this set.</p>
     */
    private static final Set<ResourceKey<Level>> ROOMS = Set.of(
            Level.OVERWORLD,
            genesisRoom("great_unknown"),
            genesisRoom("subspace"),
            genesisRoom("mercury"),
            genesisRoom("moon"),
            genesisRoom("malachite"));

    private SubDimensions() {
    }

    private static ResourceKey<Level> genesisRoom(String path) {
        return ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, path));
    }

    /**
     * Returns the immutable built-in room set used by both transition logic and
     * client screen registration.
     */
    public static Set<ResourceKey<Level>> builtInRooms() {
        return ROOMS;
    }

    /** Whether this world is one of the rooms. Earth is the overworld. */
    public static boolean isRoom(ResourceKey<Level> dimension) {
        return dimension != null && ROOMS.contains(dimension);
    }

    /**
     * Whether two worlds are rooms in the same house and currently share the
     * section geometry a render grid is built around.
     *
     * <p>Intent is not trusted on its own. A datapack or future built-in body
     * with different height falls back to vanilla's rebuild rather than
     * reusing a grid that does not fit.</p>
     */
    public static boolean sameFloorplan(Level from, Level to) {
        if (from == null || to == null) {
            return false;
        }
        if (!isRoom(from.dimension()) || !isRoom(to.dimension())) {
            return false;
        }
        return from.getMinBuildHeight() == to.getMinBuildHeight()
                && from.getSectionsCount() == to.getSectionsCount();
    }
}
