package net.mcreator.crustychunks.compat;

import java.util.UUID;
import net.mcreator.crustychunks.WariumConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side guidance for Warium missiles. Consumes radar locks and laser
 * designations produced by the other handlers and steers a missile toward the
 * resolved point, with a clamped turn rate, a self-collision grace period, and a
 * lost-target grace window. Every method is null-safe: if the target or
 * designation disappears, guidance quietly stops instead of crashing.
 *
 * <p>Only stable data is persisted on the missile (a target UUID and a last-known
 * point in its persistent NBT); no runtime construct/entity/level references are
 * stored, so save/reload is always safe.</p>
 */
public final class AeronauticsMissileGuidanceHandler {
	public enum Guidance {
		UNGUIDED, RADAR, LASER, HEAT
	}

	private AeronauticsMissileGuidanceHandler() {
	}

	/**
	 * Steers the missile one tick toward its guidance solution. Returns true if a
	 * valid solution was applied, false if the missile should coast (unguided).
	 */
	public static boolean guide(Entity missile, Guidance guidance) {
		if (missile == null || guidance == Guidance.UNGUIDED)
			return false;
		Level level = missile.level();
		if (level == null || level.isClientSide())
			return false;
		// Self-collision grace: do not steer (and let the firing procedure ignore the shooter) right after launch.
		if (missile.tickCount < WariumConfig.SELF_COLLISION_GRACE_TICKS.get())
			return false;

		Vec3 targetPoint = resolveTargetPoint(missile, guidance);
		CompoundTag data = missile.getPersistentData();
		if (targetPoint == null) {
			// Lost-target grace: keep flying toward the last known point briefly, then coast.
			long lostAt = data.getLong("WariumGuidanceLostAt");
			if (data.getBoolean("WariumHasLastPoint")) {
				long grace = guidance == Guidance.LASER ? WariumConfig.LASER_GUIDANCE_GRACE_TICKS.get() : WariumConfig.RADAR_LOST_GRACE.get();
				if (lostAt == 0L) {
					data.putLong("WariumGuidanceLostAt", level.getGameTime());
					lostAt = level.getGameTime();
				}
				if (level.getGameTime() - lostAt <= grace)
					targetPoint = new Vec3(data.getDouble("WariumLastX"), data.getDouble("WariumLastY"), data.getDouble("WariumLastZ"));
			}
			if (targetPoint == null)
				return false;
		} else {
			data.putBoolean("WariumHasLastPoint", true);
			data.putDouble("WariumLastX", targetPoint.x);
			data.putDouble("WariumLastY", targetPoint.y);
			data.putDouble("WariumLastZ", targetPoint.z);
			data.putLong("WariumGuidanceLostAt", 0L);
		}

		Vec3 velocity = missile.getDeltaMovement();
		double speed = velocity.length();
		if (speed < 1.0E-4)
			return false;
		Vec3 desired = targetPoint.subtract(missile.position());
		if (desired.lengthSqr() < 1.0E-6)
			return false;
		desired = desired.normalize();
		Vec3 current = velocity.scale(1.0 / speed);

		double maxTurn = WariumConfig.MISSILE_MAX_TURN_RATE.get();
		double dot = Math.max(-1.0, Math.min(1.0, current.dot(desired)));
		double angle = Math.acos(dot);
		Vec3 newDir;
		if (angle <= maxTurn || angle < 1.0E-4) {
			newDir = desired;
		} else {
			double t = maxTurn / angle;
			newDir = current.scale(1.0 - t).add(desired.scale(t));
			if (newDir.lengthSqr() < 1.0E-6)
				return false;
			newDir = newDir.normalize();
		}
		Vec3 steered = newDir.scale(speed);
		if (!Double.isFinite(steered.x) || !Double.isFinite(steered.y) || !Double.isFinite(steered.z))
			return false;
		missile.setDeltaMovement(steered);
		missile.hasImpulse = true;
		return true;
	}

	private static Vec3 resolveTargetPoint(Entity missile, Guidance guidance) {
		Level level = missile.level();
		CompoundTag data = missile.getPersistentData();
		switch (guidance) {
			case LASER -> {
				var d = AeronauticsLaserDesignationHandler.nearestActive(level, missile.position());
				if (d == null)
					return null;
				if (d.entityId() != null) {
					Vec3 p = AeronauticsLaserDesignationHandler.activePoint(level, ownerId(missile));
					if (p != null)
						return p;
				}
				return d.point();
			}
			case RADAR -> {
				UUID locked = data.hasUUID("WariumRadarTarget") ? data.getUUID("WariumRadarTarget") : null;
				if (locked == null) {
					AeronauticsRadarHandler.RadarTrack best = AeronauticsRadarHandler.bestTarget(level, missile.position(), missile);
					if (best == null)
						return null;
					data.putUUID("WariumRadarTarget", best.targetId());
					return best.position();
				}
				Entity target = AeronauticsRadarHandler.resolve(level, missile.position(), locked);
				if (target == null) {
					data.remove("WariumRadarTarget");
					return null;
				}
				return target.getBoundingBox().getCenter();
			}
			case HEAT -> {
				AeronauticsRadarHandler.RadarTrack best = AeronauticsRadarHandler.bestTarget(level, missile.position(), missile);
				return best == null ? null : best.position();
			}
			default -> {
				return null;
			}
		}
	}

	private static UUID ownerId(Entity missile) {
		if (missile instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
			Entity owner = projectile.getOwner();
			if (owner != null)
				return owner.getUUID();
		}
		return null;
	}
}
