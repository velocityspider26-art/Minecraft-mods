package net.mcreator.crustychunks.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.mcreator.crustychunks.WariumConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative radar. Rebuilt for the Create: Aeronautics port; does not
 * reference Valkyrien Skies. Detects nearby valid targets (players, mobs,
 * aircraft/physics constructs, optionally projectiles), scored by distance, with
 * an optional line-of-sight requirement. State is transient runtime data only -
 * nothing is written to disk, so radar locks always clear cleanly on reload.
 */
public final class AeronauticsRadarHandler {
	/** A validated radar contact. Stores only a stable entity UUID plus a snapshot of derived data. */
	public record RadarTrack(UUID targetId, Vec3 position, Vec3 velocity, double distance, boolean aircraft) {
	}

	private static final TagKey<EntityType<?>> HOSTILE_TARGET =
			TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("crusty_chunks", "robotarget"));

	private AeronauticsRadarHandler() {
	}

	/** Scans around an origin and returns validated tracks, nearest first, capped by config. */
	public static List<RadarTrack> scan(Level level, Vec3 origin, Entity self) {
		List<RadarTrack> tracks = new ArrayList<>();
		if (!(level instanceof ServerLevel serverLevel) || origin == null
				|| !Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z))
			return tracks;
		double range = WariumConfig.RADAR_RANGE.get();
		AABB box = new AABB(origin, origin).inflate(range);
		for (Entity entity : serverLevel.getEntities(self, box, e -> isValidTarget(e, self))) {
			double dist = entity.position().distanceTo(origin);
			if (dist > range)
				continue;
			if (WariumConfig.RADAR_REQUIRES_LINE_OF_SIGHT.get() && !hasLineOfSight(serverLevel, origin, entity))
				continue;
			boolean aircraft = AeronauticsCompat.isPhysicsSpace(level, entity.getX(), entity.getY(), entity.getZ())
					|| entity.getVehicle() != null;
			tracks.add(new RadarTrack(entity.getUUID(), entity.position(), entity.getDeltaMovement(), dist, aircraft));
		}
		tracks.sort((a, c) -> Double.compare(a.distance, c.distance));
		int max = WariumConfig.RADAR_MAX_TARGETS.get();
		return tracks.size() > max ? tracks.subList(0, max) : tracks;
	}

	/** The best (nearest valid) target from an origin, or null. Used by radar-guided missiles at launch. */
	public static RadarTrack bestTarget(Level level, Vec3 origin, Entity self) {
		List<RadarTrack> tracks = scan(level, origin, self);
		return tracks.isEmpty() ? null : tracks.get(0);
	}

	/** Re-resolves a tracked target to a live entity, validating it is still in range and (optionally) visible. */
	public static Entity resolve(Level level, Vec3 origin, UUID targetId) {
		if (!(level instanceof ServerLevel serverLevel) || targetId == null)
			return null;
		Entity entity = serverLevel.getEntity(targetId);
		if (entity == null || !entity.isAlive())
			return null;
		if (origin != null && entity.position().distanceTo(origin) > WariumConfig.RADAR_RANGE.get())
			return null;
		if (origin != null && WariumConfig.RADAR_REQUIRES_LINE_OF_SIGHT.get() && !hasLineOfSight(serverLevel, origin, entity))
			return null;
		return entity;
	}

	private static boolean isValidTarget(Entity entity, Entity self) {
		if (entity == self || entity == null || !entity.isAlive())
			return false;
		if (self != null && entity.getUUID().equals(self.getUUID()))
			return false;
		if (entity.getType().is(HOSTILE_TARGET))
			return true;
		if (entity instanceof Player player)
			return WariumConfig.RADAR_CAN_TRACK_PLAYERS.get() && !player.isSpectator() && !player.isCreative();
		if (WariumConfig.RADAR_CAN_TRACK_AIRCRAFT.get()
				&& AeronauticsCompat.isPhysicsSpace(entity.level(), entity.getX(), entity.getY(), entity.getZ()))
			return true;
		if (WariumConfig.RADAR_CAN_TRACK_PROJECTILES.get())
			return entity instanceof net.minecraft.world.entity.projectile.Projectile;
		return WariumConfig.RADAR_CAN_TRACK_AIRCRAFT.get() && entity instanceof LivingEntity && entity.getVehicle() != null;
	}

	private static boolean hasLineOfSight(Level level, Vec3 from, Entity target) {
		Vec3 to = target.getBoundingBox().getCenter();
		HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
				net.minecraft.world.phys.shapes.CollisionContext.empty()));
		return !(hit instanceof BlockHitResult) || hit.getType() == HitResult.Type.MISS
				|| hit.getLocation().distanceTo(from) >= to.distanceTo(from) - 1.0;
	}
}
