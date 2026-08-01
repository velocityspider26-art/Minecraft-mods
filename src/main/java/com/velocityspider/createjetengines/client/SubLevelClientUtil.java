package com.velocityspider.createjetengines.client;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import javax.annotation.Nullable;

/**
 * Local-to-world helpers for anything that has to appear at the right place in the world while
 * the block itself lives inside a moving Sable sublevel.
 *
 * <p>Two different transforms are needed and they are not interchangeable:
 * positions go through {@code projectOutOfSubLevel} / {@code transformPosition}, directions go
 * through {@code transformNormal}. Using the position transform on a direction is what makes
 * effects point sideways or appear mirrored.
 */
public final class SubLevelClientUtil {

    private SubLevelClientUtil() {
    }

    /** Sublevel-local position to world position. Returns the input unchanged outside a sublevel. */
    public static Vec3 toWorld(@Nullable Level level, Vec3 local) {
        if (level == null) {
            return local;
        }
        try {
            return SableCompanion.INSTANCE.projectOutOfSubLevel(level, local);
        } catch (Throwable t) {
            return local;
        }
    }

    /**
     * Sublevel-local direction to world direction. Unlike a position this must not pick up the
     * sublevel's translation, only its orientation.
     */
    public static Vec3 dirToWorld(@Nullable Level level, BlockPos at, Vec3 localDir) {
        SubLevelAccess sub = containing(level, at);
        if (sub == null) {
            return localDir;
        }
        try {
            Vector3d out = sub.logicalPose().transformNormal(
                    new Vector3d(localDir.x, localDir.y, localDir.z));
            return new Vec3(out.x, out.y, out.z);
        } catch (Throwable t) {
            return localDir;
        }
    }

    /** The world-space velocity of the sublevel at a local point, for trailing effects. */
    public static Vec3 velocityAt(@Nullable Level level, Vec3 local) {
        if (level == null) {
            return Vec3.ZERO;
        }
        try {
            Vector3d v = SableCompanion.INSTANCE.getVelocity(
                    level, JOMLConversion.toJOML(local), new Vector3d());
            return new Vec3(v.x, v.y, v.z);
        } catch (Throwable t) {
            return Vec3.ZERO;
        }
    }

    @Nullable
    public static SubLevelAccess containing(@Nullable Level level, BlockPos pos) {
        if (level == null) {
            return null;
        }
        try {
            return SableCompanion.INSTANCE.getContaining(level, pos);
        } catch (Throwable t) {
            return null;
        }
    }

    /** True when this block sits inside a Sable sublevel rather than the ordinary world. */
    public static boolean inSubLevel(@Nullable Level level, BlockPos pos) {
        return containing(level, pos) != null;
    }
}
