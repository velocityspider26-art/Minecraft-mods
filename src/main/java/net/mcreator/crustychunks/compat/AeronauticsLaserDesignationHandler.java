package net.mcreator.crustychunks.compat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.mcreator.crustychunks.WariumConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Server-authoritative laser designation for laser-guided munitions. A player
 * (or, in principle, a mounted block) designates a world point or an entity via
 * a server-side raycast; the designation is validated for range and line of
 * sight. Designations are transient runtime state keyed by the designator's UUID
 * and expire automatically, so nothing invalid is ever saved or reloaded.
 */
public final class AeronauticsLaserDesignationHandler {
	/** A validated designation: a world point, and optionally a designated entity, plus the tick it expires. */
	public record Designation(Vec3 point, UUID entityId, long expiresAtGameTime) {
	}

	private static final Map<UUID, Designation> DESIGNATIONS = new ConcurrentHashMap<>();

	private AeronauticsLaserDesignationHandler() {
	}

	/**
	 * Performs a server-side raycast from the designator's eye and records a
	 * validated designation. Returns the designation, or null if nothing valid was hit.
	 */
	public static Designation designate(ServerPlayer designator) {
		if (designator == null || !designator.isAlive())
			return null;
		Level level = designator.level();
		double range = WariumConfig.LASER_RANGE.get();
		Vec3 eye = designator.getEyePosition(1.0F);
		Vec3 look = designator.getLookAngle();
		Vec3 end = eye.add(look.scale(range));

		Designation result = null;
		// Entity designation takes priority when enabled.
		if (WariumConfig.LASER_CAN_DESIGNATE_ENTITIES.get()) {
			Entity best = null;
			double bestDist = range * range;
			for (Entity entity : level.getEntities(designator, designator.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0),
					e -> e != designator && e.isAlive() && e.isPickable())) {
				var clip = entity.getBoundingBox().inflate(0.3).clip(eye, end);
				if (clip.isPresent()) {
					double d = eye.distanceToSqr(clip.get());
					if (d < bestDist) {
						bestDist = d;
						best = entity;
					}
				}
			}
			if (best != null)
				result = new Designation(best.getBoundingBox().getCenter(), best.getUUID(), expiry(level));
		}
		if (result == null && WariumConfig.LASER_CAN_DESIGNATE_BLOCKS.get()) {
			HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
			if (hit instanceof BlockHitResult block && hit.getType() != HitResult.Type.MISS)
				result = new Designation(block.getLocation(), null, expiry(level));
		}
		if (result != null)
			DESIGNATIONS.put(designator.getUUID(), result);
		else
			DESIGNATIONS.remove(designator.getUUID());
		return result;
	}

	/** Current designation point for a designator, refreshing the live entity position if it moved. Null when expired/invalid. */
	public static Vec3 activePoint(Level level, UUID designatorId) {
		Designation d = current(level, designatorId);
		if (d == null)
			return null;
		if (d.entityId != null && level instanceof ServerLevel server) {
			Entity target = server.getEntity(d.entityId);
			if (target != null && target.isAlive())
				return target.getBoundingBox().getCenter();
		}
		return d.point;
	}

	/** Any active designation in the level (used by munitions that pick up the nearest active laser). Null if none. */
	public static Designation nearestActive(Level level, Vec3 near) {
		if (near == null)
			return null;
		long now = level.getGameTime();
		Designation best = null;
		double bestDist = Double.MAX_VALUE;
		for (Map.Entry<UUID, Designation> e : DESIGNATIONS.entrySet()) {
			Designation d = e.getValue();
			if (d.expiresAtGameTime < now || d.point == null)
				continue;
			double dist = d.point.distanceToSqr(near);
			if (dist < bestDist) {
				bestDist = dist;
				best = d;
			}
		}
		return best;
	}

	public static Designation current(Level level, UUID designatorId) {
		if (designatorId == null)
			return null;
		Designation d = DESIGNATIONS.get(designatorId);
		if (d == null)
			return null;
		if (d.expiresAtGameTime < level.getGameTime()) {
			DESIGNATIONS.remove(designatorId);
			return null;
		}
		return d;
	}

	public static void clear(UUID designatorId) {
		if (designatorId != null)
			DESIGNATIONS.remove(designatorId);
	}

	private static long expiry(Level level) {
		return level.getGameTime() + Math.max(2, WariumConfig.LASER_UPDATE_RATE.get() * 3L);
	}
}
