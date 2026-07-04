package net.mcreator.crustychunks.compat;

import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * The only class that references Sable types directly. Never classload this
 * without checking {@code AeronauticsCompat.isPhysicsLoaded()} first.
 */
final class SableAdapter {
	private SableAdapter() {
	}

	static boolean isPhysicsSpace(Level level, double x, double y, double z) {
		SubLevelContainer container = SubLevelContainer.getContainer(level);
		return container != null && container.inBounds(new Vector3d(x, y, z));
	}

	private static SubLevel constructAt(Level level, Vec3 worldPos) {
		SubLevelContainer container = SubLevelContainer.getContainer(level);
		if (container == null)
			return null;
		BoundingBox3d query = new BoundingBox3d(worldPos.x - 0.5, worldPos.y - 0.5, worldPos.z - 0.5, worldPos.x + 0.5, worldPos.y + 0.5, worldPos.z + 0.5);
		for (SubLevel sub : container.queryIntersecting(query)) {
			if (sub != null && !sub.isRemoved())
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
		Vector3dc pivot = server.logicalPose().rotationPoint();
		Vector3d r = new Vector3d(worldPos.x - pivot.x(), worldPos.y - pivot.y(), worldPos.z - pivot.z());
		Vector3d spin = new Vector3d(angular).cross(r);
		Vector3d total = new Vector3d(linear).add(spin);
		// Sable velocities are per second; convert to blocks per tick.
		total.mul(1.0 / 20.0);
		if (!total.isFinite())
			return Vec3.ZERO;
		return new Vec3(total.x, total.y, total.z);
	}

	static Vec3 localToWorld(Level level, Vec3 pos) {
		SubLevelContainer container = SubLevelContainer.getContainer(level);
		if (container == null || !container.inBounds(new Vector3d(pos.x, pos.y, pos.z)))
			return pos;
		SubLevel sub = container.getPlot(new net.minecraft.world.level.ChunkPos(net.minecraft.core.BlockPos.containing(pos.x, pos.y, pos.z))) != null
				? container.getPlot(new net.minecraft.world.level.ChunkPos(net.minecraft.core.BlockPos.containing(pos.x, pos.y, pos.z))).getSubLevel()
				: null;
		if (sub == null)
			return pos;
		Vector3d out = sub.logicalPose().transformPosition(new Vector3d(pos.x, pos.y, pos.z));
		return new Vec3(out.x, out.y, out.z);
	}

	static Vec3 worldToLocal(Level level, Vec3 pos) {
		SubLevel sub = constructAt(level, pos);
		if (sub == null)
			return pos;
		Vector3d out = sub.logicalPose().transformPositionInverse(new Vector3d(pos.x, pos.y, pos.z));
		return new Vec3(out.x, out.y, out.z);
	}

	static void applyForce(Level level, Vec3 worldPos, Vec3 force) {
		SubLevel sub = constructAt(level, worldPos);
		if (!(sub instanceof ServerSubLevel server))
			return;
		server.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get())
				.applyAndRecordPointForce(new Vector3d(worldPos.x, worldPos.y, worldPos.z), new Vector3d(force.x, force.y, force.z));
	}

	static void applyExplosionImpulse(Level level, Vec3 center, double radius, double strength) {
		SubLevelContainer container = SubLevelContainer.getContainer(level);
		if (container == null)
			return;
		BoundingBox3d query = new BoundingBox3d(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
		for (SubLevel sub : container.queryIntersecting(query)) {
			if (!(sub instanceof ServerSubLevel server) || sub.isRemoved())
				continue;
			Vector3dc pivot = server.logicalPose().rotationPoint();
			Vector3d away = new Vector3d(pivot.x() - center.x, pivot.y() - center.y, pivot.z() - center.z);
			double dist = Math.max(1.0, away.length());
			if (dist > radius)
				continue;
			away.normalize();
			double falloff = 1.0 - (dist / (radius + 1.0));
			away.mul(strength * falloff);
			server.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get())
					.applyAndRecordPointForce(new Vector3d(center.x, center.y, center.z), away);
		}
	}
}
