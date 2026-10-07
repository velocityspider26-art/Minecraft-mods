package com.velocityspider.crashphysics.sable;

import com.velocityspider.crashphysics.CrashPhysics;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * The collision callback this mod gives every solid block state.
 * <p>
 * Sable bakes one callback per block state into Rapier and calls it from the native solver for both sides of every
 * contact point. Blocks that already had a callback (TNT, bells, beehives, fragile blocks, other mods) keep their
 * behaviour: it runs first, and if it already broke the block, crash physics leaves that contact alone.
 */
public final class CrashCollisionCallback implements BlockSubLevelCollisionCallback {

    // Rust only reads these arrays: [tangent x, tangent y, tangent z, remove (1) / keep (0)]
    private static final double[] KEEP = {0.0, 0.0, 0.0, 0.0};
    private static final double[] REMOVE = {0.0, 0.0, 0.0, 1.0};

    private static boolean loggedFailure;

    private final BlockState state;
    @Nullable
    private final BlockSubLevelCollisionCallback delegate;

    public CrashCollisionCallback(final BlockState state, @Nullable final BlockSubLevelCollisionCallback delegate) {
        this.state = state;
        this.delegate = delegate;
    }

    /**
     * Entry point from the native physics pipeline. Exceptions must never escape back into Rust, so everything is
     * caught here.
     */
    @Override
    public double[] onCollision(final int x, final int y, final int z, final int otherX, final int otherY, final int otherZ,
                                final double x1, final double y1, final double z1, final double impactVelocity, final boolean hasOtherBlock) {
        try {
            if (this.delegate != null) {
                final double[] delegateResult = this.delegate.onCollision(x, y, z, otherX, otherY, otherZ, x1, y1, z1, impactVelocity, hasOtherBlock);
                if (delegateResult.length >= 4 && delegateResult[3] > 0.0) {
                    return delegateResult;
                }

                if (CrashTickHandler.onContact(this.state, x, y, z, otherX, otherY, otherZ, x1, y1, z1, impactVelocity, hasOtherBlock)) {
                    final double[] removed = delegateResult.length >= 4 ? delegateResult.clone() : new double[4];
                    removed[3] = 1.0;
                    return removed;
                }
                return delegateResult;
            }

            return CrashTickHandler.onContact(this.state, x, y, z, otherX, otherY, otherZ, x1, y1, z1, impactVelocity, hasOtherBlock) ? REMOVE : KEEP;
        } catch (final Throwable t) {
            if (!loggedFailure) {
                loggedFailure = true;
                CrashPhysics.LOGGER.error("Crash physics failed inside a collision callback; that contact is ignored", t);
            }
            return KEEP;
        }
    }

    @Override
    public CollisionResult sable$onCollision(final BlockPos hitBlockPos, @Nullable final BlockPos otherHitBlockPos, final Vector3d impactPosition, final double impactVelocity) {
        final BlockPos other = otherHitBlockPos != null ? otherHitBlockPos : hitBlockPos;
        final double[] result = this.onCollision(hitBlockPos.getX(), hitBlockPos.getY(), hitBlockPos.getZ(), other.getX(), other.getY(), other.getZ(),
                impactPosition.x, impactPosition.y, impactPosition.z, impactVelocity, otherHitBlockPos != null);
        return result[3] > 0.0 ? new CollisionResult(JOMLConversion.ZERO, true) : CollisionResult.NONE;
    }
}
