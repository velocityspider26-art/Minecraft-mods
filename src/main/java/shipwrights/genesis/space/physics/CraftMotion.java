package shipwrights.genesis.space.physics;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.space.SpaceLevel;

/**
 * Where a player's craft is and how fast it is moving, in celestial-space
 * coordinates and metres per second — the two things every orbit calculation
 * needs. Shared by the in-world orbit lines and the orbit map so both read the
 * same state.
 *
 * <p>When the player is aboard a ship the ship's motion is used, taken from the
 * pose delta because the rigid body itself lives server-side; otherwise the
 * player's own motion is used.</p>
 */
public record CraftMotion(Vector3d position, Vector3d velocity) {
    /** Aboard-ship search radius, matching the travel manager's own check. */
    private static final double ABOARD_RADIUS = 32.0;
    private static final double TICKS_PER_SECOND = 20.0;

    public static CraftMotion of(Level level, Player player, float partialTick) {
        SubLevel ship = shipCarrying(level, player);
        if (ship != null) {
            Vector3dc now = ship.logicalPose().position();
            Vector3dc last = ship.lastPose().position();
            return new CraftMotion(
                    SpaceLevel.toCelestialSpace(level, now),
                    // Pose delta is blocks/tick; orbital maths works in m/s.
                    new Vector3d(now.x() - last.x(), now.y() - last.y(), now.z() - last.z())
                            .mul(TICKS_PER_SECOND));
        }

        Vec3 motion = player.getDeltaMovement();
        return new CraftMotion(
                SpaceLevel.toCelestialSpace(level, player.getPosition(partialTick)),
                new Vector3d(motion.x * TICKS_PER_SECOND, motion.y * TICKS_PER_SECOND,
                        motion.z * TICKS_PER_SECOND));
    }

    private static SubLevel shipCarrying(Level level, Player player) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }
        Vec3 position = player.position();
        for (SubLevel subLevel : container.getAllSubLevels()) {
            Vector3dc pose = subLevel.logicalPose().position();
            if (position.distanceToSqr(pose.x(), pose.y(), pose.z()) < ABOARD_RADIUS * ABOARD_RADIUS) {
                return subLevel;
            }
        }
        return null;
    }
}
