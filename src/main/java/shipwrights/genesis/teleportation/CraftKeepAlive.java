package shipwrights.genesis.teleportation;

import dev.ryanhcode.sable.sublevel.SubLevel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Craft that must not be unloaded for a while.
 *
 * <p>A craft that has just crossed into another dimension sits in a destination
 * that is not ticking yet, and Sable's unload check runs every physics tick and
 * only has to fail once. Rather than keep racing that check from outside, a
 * transferred craft is marked here and the unload is refused for it (see
 * {@code SubLevelHoldingChunkMapMixin}).</p>
 *
 * <p>The protection expires on its own so it cannot pin craft in memory
 * forever: by the time it lapses the destination is loaded and ticking
 * normally, and ordinary unloading resumes.</p>
 */
public final class CraftKeepAlive {
    /** How long a transferred craft is protected, in milliseconds. */
    private static final long PROTECTION_MS = 60_000;

    private static final Map<UUID, Long> PROTECTED_UNTIL = new ConcurrentHashMap<>();

    private CraftKeepAlive() {
    }

    /** Protects a craft (and its dependency chain) from being unloaded. */
    public static void protect(UUID subLevelId) {
        if (subLevelId != null) {
            PROTECTED_UNTIL.put(subLevelId, System.currentTimeMillis() + PROTECTION_MS);
        }
    }

    /**
     * Whether a craft must stay loaded because somebody is using it.
     *
     * <p>A craft with a player aboard or right beside it is in use, and
     * unloading it takes the ground out from under them. One with nobody near
     * is free to unload the way Sable intends. Recently transferred craft are
     * also held briefly, covering the moment after arrival when the crew's
     * client is still catching up and they are not yet counted as nearby.</p>
     */
    public static boolean shouldStayLoaded(Object container, SubLevel subLevel) {
        if (subLevel == null) {
            return false;
        }
        if (isProtected(subLevel)) {
            return true;
        }
        try {
            if (!(container instanceof dev.ryanhcode.sable.api.sublevel.SubLevelContainer levels)
                    || !(levels.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
                return false;
            }
            org.joml.Vector3dc pose = subLevel.logicalPose().position();
            for (net.minecraft.server.level.ServerPlayer player : level.players()) {
                if (player.position().distanceToSqr(pose.x(), pose.y(), pose.z())
                        < OCCUPIED_RADIUS * OCCUPIED_RADIUS) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Never let a housekeeping guard break the unload path itself.
        }
        return false;
    }

    /** How close a player must be for a craft to count as in use. */
    private static final double OCCUPIED_RADIUS = 256.0;

    public static boolean isProtected(SubLevel subLevel) {
        if (subLevel == null) {
            return false;
        }
        Long until = PROTECTED_UNTIL.get(subLevel.getUniqueId());
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            PROTECTED_UNTIL.remove(subLevel.getUniqueId());
            return false;
        }
        return true;
    }
}
