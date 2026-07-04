package shipwrights.genesis.compat.aeronautics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Block/behaviour attachment to a Create Aeronautics construct.
 *
 * <p>In Valkyrien Skies, Genesis machines registered "attachments" and "force inducers" on the ship
 * object. On Create Aeronautics the block simply lives inside the construct's sub-level, so
 * "attachment" reduces to: is this block part of a construct, and which one. Behaviour that needs to
 * push the construct does so through {@link AeronauticsForceHandler} while it ticks.</p>
 *
 * <p>Compatibility replacement for VS {@code ship.saveAttachment}/{@code getAttachment} and the
 * force-inducer registration.</p>
 */
public final class AeronauticsBlockAttachmentHandler {

    private AeronauticsBlockAttachmentHandler() {}

    /** @return true if the block at {@code pos} is currently part of a construct. */
    public static boolean isAttached(Level level, BlockPos pos) {
        return AeronauticsContraptionLookup.getConstructManaging(level, pos) != null;
    }

    /** @return the construct a block is attached to, or {@code null} if it is a world block. */
    @Nullable
    public static AeronauticsConstruct getAttachedConstruct(Level level, BlockPos pos) {
        return AeronauticsContraptionLookup.getConstructManaging(level, pos);
    }

    @Nullable
    public static AeronauticsConstruct getAttachedConstruct(BlockEntity be) {
        return AeronauticsContraptionLookup.getConstructForBlockEntity(be);
    }

    /**
     * @return true if the block entity is being ticked while mounted on a moving construct.
     *         Genesis machines use this to decide whether to transform their outputs into world
     *         space before acting.
     */
    public static boolean isMounted(BlockEntity be) {
        return getAttachedConstruct(be) != null;
    }
}
