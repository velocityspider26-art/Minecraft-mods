package net.mcreator.crustychunks.compat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.mcreator.crustychunks.CrustyChunksMod;

/**
 * Fail-safe reporting for Warium gameplay logic. Any exception thrown by a
 * weapon / projectile / physics procedure is routed here instead of being
 * allowed to propagate into (and crash) the server tick loop or a world save.
 *
 * <p>Each distinct failure site is logged once with a full stack trace, then
 * suppressed, so a single malformed projectile or unloaded construct degrades
 * to a no-op rather than corrupting the world.</p>
 */
public final class WariumSafety {
	private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();

	private WariumSafety() {
	}

	public static void report(String where, Throwable t) {
		if (SEEN.add(where))
			CrustyChunksMod.LOGGER.error("Warium: recovered from an error in '{}'; this system is degraded but the world is safe.", where, t);
	}
}
