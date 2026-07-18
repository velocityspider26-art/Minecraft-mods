package net.mcreator.crustychunks.compat;

import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * The only class that references Sable types directly (Sable is the physics
 * engine used by Create: Aeronautics). Never classloaded unless the "sable" mod
 * is present, and every method is defensively guarded: a missing container,
 * removed sublevel, null pose, or non-finite value yields a safe fallback rather
 * than an exception. No Sable runtime object is ever stored beyond the call.
 */
final class SableAdapter {
	private SableAdapter() {
	}

	private static SubLevelContainer container(Level level) {
		if (level == null)
			return null;
		return SubLevelContainer.getContainer(level);
	}

	private static boolean finite(Vec3 v) {
		return v != null && Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
	}

	private static boolean valid(SubLevel sub) {
		return sub != null && !sub.isRemoved() && sub.logicalPose() != null;
	}

	static boolean isPhysicsSpace(Level level, double x, double y, double z) {
		SubLevelContainer container = container(level);
		return container != null && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
				&& container.inBounds(new Vector3d(x, y, z));
	}

	private static SubLevel constructAt(Level level, Vec3 worldPos) {
		SubLevelContainer container = container(level);
		if (container == null || !finite(worldPos))
			return null;
		BoundingBox3d query = new BoundingBox3d(worldPos.x - 0.5, worldPos.y - 0.5, worldPos.z - 0.5,
				worldPos.x + 0.5, worldPos.y + 0.5, worldPos.z + 0.5);
		Iterable<SubLevel> hits = container.queryIntersecting(query);
		if (hits == null)
			return null;
		for (SubLevel sub : hits) {
			if (valid(sub))
				return sub;
		}
		return null;
	}

	static Vec3 velocityAt(Level level, Vec3 worldPos) {
		SubLevel sub = constructAt(level, worldPos);
		if (!(sub instanceof ServerSubLevel server))
			return Vec3.ZERO;
		Vector3d linear = server.latestLinearVelocity;
		Vector3d angular = server.latestAngularVelocity;
		Pose3dc pose = server.logicalPose();
		if (linear == null || angular == null || pose == null || pose.rotationPoint() == null)
			return Vec3.ZERO;
		Vector3dc pivot = pose.rotationPoint();
		Vector3d r = new Vector3d(worldPos.x - pivot.x(), worldPos.y - pivot.y(), worldPos.z - pivot.z());
		Vector3d total = new Vector3d(linear).add(new Vector3d(angular).cross(r));
		total.mul(1.0 / 20.0); // Sable velocities are per-second; convert to blocks/tick.
		if (!total.isFinite())
			return Vec3.ZERO;
		return new Vec3(total.x, total.y, total.z);
	}

	static Vec3 localToWorld(Level level, Vec3 pos) {
		SubLevelContainer container = container(level);
		if (container == null || !finite(pos) || !container.inBounds(new Vector3d(pos.x, pos.y, pos.z)))
			return pos;
		var plot = container.getPlot(new ChunkPos(BlockPos.containing(pos.x, pos.y, pos.z)));
		SubLevel sub = plot != null ? plot.getSubLevel() : null;
		if (!valid(sub))
			return pos;
		Vector3d out = sub.logicalPose().transformPosition(new Vector3d(pos.x, pos.y, pos.z));
		return out.isFinite() ? new Vec3(out.x, out.y, out.z) : pos;
	}

	/** Rotates a direction/velocity vector from sub-level local space into world space. */
	static Vec3 localDirToWorld(Level level, Vec3 localPos, Vec3 localDir) {
		SubLevelContainer container = container(level);
		if (container == null || !finite(localPos) || !finite(localDir)
				|| !container.inBounds(new Vector3d(localPos.x, localPos.y, localPos.z)))
			return localDir;
		var plot = container.getPlot(new ChunkPos(BlockPos.containing(localPos.x, localPos.y, localPos.z)));
		SubLevel sub = plot != null ? plot.getSubLevel() : null;
		if (!valid(sub))
			return localDir;
		Vector3d a = sub.logicalPose().transformPosition(new Vector3d(localPos.x, localPos.y, localPos.z));
		Vector3d b = sub.logicalPose().transformPosition(
				new Vector3d(localPos.x + localDir.x, localPos.y + localDir.y, localPos.z + localDir.z));
		if (!a.isFinite() || !b.isFinite())
			return localDir;
		return new Vec3(b.x - a.x, b.y - a.y, b.z - a.z);
	}

	static Vec3 worldToLocal(Level level, Vec3 pos) {
		SubLevel sub = constructAt(level, pos);
		if (!valid(sub))
			return pos;
		Vector3d out = sub.logicalPose().transformPositionInverse(new Vector3d(pos.x, pos.y, pos.z));
		return out.isFinite() ? new Vec3(out.x, out.y, out.z) : pos;
	}

	static void applyForce(Level level, Vec3 worldPos, Vec3 force) {
		SubLevel sub = constructAt(level, worldPos);
		if (!(sub instanceof ServerSubLevel server) || !finite(force))
			return;
		var group = server.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());
		if (group != null)
			group.applyAndRecordPointForce(new Vector3d(worldPos.x, worldPos.y, worldPos.z), new Vector3d(force.x, force.y, force.z));
	}

	static void applyExplosionImpulse(Level level, Vec3 center, double radius, double strength) {
		SubLevelContainer container = container(level);
		if (container == null || !finite(center) || !Double.isFinite(radius) || !Double.isFinite(strength))
			return;
		BoundingBox3d query = new BoundingBox3d(center.x - radius, center.y - radius, center.z - radius,
				center.x + radius, center.y + radius, center.z + radius);
		Iterable<SubLevel> hits = container.queryIntersecting(query);
		if (hits == null)
			return;
		for (SubLevel sub : hits) {
			if (!(sub instanceof ServerSubLevel server) || !valid(sub))
				continue;
			Pose3dc pose = server.logicalPose();
			if (pose == null || pose.rotationPoint() == null)
				continue;
			Vector3dc pivot = pose.rotationPoint();
			Vector3d away = new Vector3d(pivot.x() - center.x, pivot.y() - center.y, pivot.z() - center.z);
			double dist = Math.max(1.0, away.length());
			if (dist > radius)
				continue;
			away.normalize().mul(strength * (1.0 - dist / (radius + 1.0)));
			if (!away.isFinite())
				continue;
			var group = server.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());
			if (group != null)
				group.applyAndRecordPointForce(new Vector3d(center.x, center.y, center.z), away);
		}
	}
}
