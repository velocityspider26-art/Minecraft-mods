package net.mcreator.crustychunks.compat;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.WariumConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * Facade for the Sable physics engine used by Create: Aeronautics.
 *
 * <p>All methods are safe to call whether or not Sable / Create: Aeronautics are
 * installed; without them (or on error) they fall back to vanilla no-op behavior.
 * Sable classes are only touched inside {@link SableAdapter}, which is never
 * classloaded unless the "sable" mod is present.</p>
 */
public final class AeronauticsCompat {
	private static Boolean sableLoaded = null;

	private AeronauticsCompat() {
	}

	public static boolean isPhysicsLoaded() {
		if (sableLoaded == null)
			sableLoaded = ModList.get() != null && ModList.get().isLoaded("sable");
		return sableLoaded;
	}

	/** True when the given world position lies inside the physics plot storage space (i.e. "on a construct" in stored coordinates). */
	public static boolean isPhysicsSpace(Level level, double x, double y, double z) {
		if (!isPhysicsLoaded())
			return false;
		try {
			return SableAdapter.isPhysicsSpace(level, x, y, z);
		} catch (Throwable t) {
			warnOnce(t);
			return false;
		}
	}

	/** Velocity (blocks/tick) of the physics construct whose world-space bounds contain the position, or ZERO. */
	public static Vec3 getConstructVelocityAt(Level level, Vec3 worldPos) {
		if (!isPhysicsLoaded())
			return Vec3.ZERO;
		try {
			return SableAdapter.velocityAt(level, worldPos);
		} catch (Throwable t) {
			warnOnce(t);
			return Vec3.ZERO;
		}
	}

	/** Converts a position in construct plot space to its current world-space position. Returns the input when not applicable. */
	public static Vec3 localToWorld(Level level, Vec3 pos) {
		if (!isPhysicsLoaded())
			return pos;
		try {
			return SableAdapter.localToWorld(level, pos);
		} catch (Throwable t) {
			warnOnce(t);
			return pos;
		}
	}

	/** Converts a world-space position to plot space of the construct at that position. Returns the input when not applicable. */
	public static Vec3 worldToLocal(Level level, Vec3 pos) {
		if (!isPhysicsLoaded())
			return pos;
		try {
			return SableAdapter.worldToLocal(level, pos);
		} catch (Throwable t) {
			warnOnce(t);
			return pos;
		}
	}

	/** Applies a force to the construct at the given world position (server side only). */
	public static void applyForce(Level level, Vec3 worldPos, Vec3 force) {
		if (!isPhysicsLoaded() || level.isClientSide() || !WariumConfig.AFFECT_PHYSICS_CONSTRUCTS.get())
			return;
		try {
			SableAdapter.applyForce(level, worldPos, force.scale(WariumConfig.CONSTRUCT_IMPULSE_SCALE.get()));
		} catch (Throwable t) {
			warnOnce(t);
		}
	}

	/** Applies recoil opposite the firing direction at the muzzle position of a mounted weapon. */
	public static void applyRecoil(Level level, Vec3 muzzlePos, Vec3 fireDirection, double strength) {
		if (fireDirection.lengthSqr() < 1.0E-6)
			return;
		applyForce(level, muzzlePos, fireDirection.normalize().scale(-strength));
	}

	/** Pushes all constructs intersecting the explosion radius away from its center. */
	public static void applyExplosionImpulse(Level level, Vec3 center, double radius, double strength) {
		if (!isPhysicsLoaded() || level.isClientSide() || !WariumConfig.AFFECT_PHYSICS_CONSTRUCTS.get())
			return;
		try {
			SableAdapter.applyExplosionImpulse(level, center, radius, strength * WariumConfig.CONSTRUCT_IMPULSE_SCALE.get());
		} catch (Throwable t) {
			warnOnce(t);
		}
	}

	/**
	 * Velocity a projectile should inherit from its shooter: the vanilla root vehicle's motion
	 * plus, when standing on a moving Sable construct, that construct's velocity.
	 */
	public static Vec3 inheritedShooterVelocity(Entity shooter) {
		if (shooter == null || !WariumConfig.INHERIT_VEHICLE_VELOCITY.get())
			return Vec3.ZERO;
		Vec3 inherited = Vec3.ZERO;
		Entity root = shooter.getRootVehicle();
		if (root != null && root != shooter)
			inherited = root.getDeltaMovement();
		Vec3 construct = getConstructVelocityAt(shooter.level(), shooter.position());
		if (construct.lengthSqr() > inherited.lengthSqr())
			inherited = construct;
		return inherited;
	}

	private static boolean warned = false;

	private static void warnOnce(Throwable t) {
		if (!warned) {
			warned = true;
			CrustyChunksMod.LOGGER.warn("Warium: Sable/Create Aeronautics compat call failed; physics integration degraded", t);
		}
	}
}
