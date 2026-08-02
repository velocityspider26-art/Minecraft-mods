package shipwrights.genesis.teleportation;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;

import java.lang.reflect.Method;

/**
 * Runtime bridge to Dimensional Sable's cross-dimension warp.
 *
 * <p>Sable itself exposes no way to move a sub-level between dimensions, which
 * is why Genesis previously rebuilt one from the outside: serialise the craft,
 * claim a plot in the destination, load it there, delete the original. That
 * works right up until Sable's physics tick notices a sub-level standing in
 * chunks it does not consider ticking and deletes it — nothing in the engine
 * knew the craft was legitimately mid-transfer.</p>
 *
 * <p>Dimensional Sable adds the missing operation, and it moves the craft's
 * entities with it. Accessed reflectively, in the same spirit as the Veil
 * bridge: the mod may not be installed, and a missing optional dependency
 * should degrade rather than fail to load.</p>
 */
public final class DimensionalSableBridge {
    private static final Method WARP;

    static {
        Method warp = null;
        try {
            Class<?> warper = Class.forName("dev.egg.SubLevelWarper");
            warp = warper.getMethod("WarpSubLevel",
                    ServerSubLevel.class, net.minecraft.world.level.Level.class, Vector3d.class);
        } catch (ReflectiveOperationException | LinkageError error) {
            GenesisMod.LOGGER.warn(
                    "Dimensional Sable not present; craft will use the built-in transfer, "
                            + "which Sable may unload shortly after arrival", error);
        }
        WARP = warp;
    }

    private DimensionalSableBridge() {
    }

    /** Whether cross-dimension warping is available at all. */
    public static boolean isAvailable() {
        return WARP != null;
    }

    /**
     * Moves a craft (and its connected chain and entities) into another
     * dimension at the given position.
     *
     * @return the number of sub-levels moved, or zero if unavailable or the
     *         warp failed
     */
    public static int warp(ServerSubLevel subLevel, ServerLevel targetLevel, Vector3d position) {
        if (WARP == null || subLevel == null || targetLevel == null || position == null) {
            return 0;
        }
        try {
            Object result = WARP.invoke(null, subLevel, targetLevel, position);
            if (!(result instanceof Number movedResult)) {
                GenesisMod.LOGGER.error(
                        "[WARP] Dimensional Sable returned an unexpected result for craft {}: {}",
                        subLevel.getUniqueId(), result);
                return 0;
            }
            int moved = movedResult.intValue();
            if (moved <= 0) {
                GenesisMod.LOGGER.warn("[WARP] Dimensional Sable moved no sub-levels for craft {}",
                        subLevel.getUniqueId());
                return 0;
            }
            GenesisMod.LOGGER.info("[WARP] Dimensional Sable moved craft {} to {} (result {})",
                    subLevel.getUniqueId(), targetLevel.dimension().location(), moved);
            return moved;
        } catch (ReflectiveOperationException error) {
            GenesisMod.LOGGER.error("[WARP] Dimensional Sable warp failed for craft {}",
                    subLevel.getUniqueId(), error);
            return 0;
        }
    }
}
