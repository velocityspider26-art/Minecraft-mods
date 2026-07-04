package shipwrights.genesis.compat.aeronautics;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Construct-related network sync helpers.
 *
 * <p>Sable already networks the existence, transform and motion of every construct/sub-level to
 * tracking clients, so Genesis never needs to sync raw physics state itself. What Genesis <i>does</i>
 * need is to reference a construct across the network safely: it may only ever send a construct's
 * stable {@link UUID} (never a live object), and resolve it back on the receiving side.</p>
 *
 * <p>Compatibility replacement for the VS ship-id networking Genesis used for its warp packets.</p>
 */
public final class AeronauticsNetworkingHelper {

    private AeronauticsNetworkingHelper() {}

    /** Save-/network-safe id for a construct. */
    public static UUID idOf(AeronauticsConstruct construct) {
        return construct.id();
    }

    /** Resolve a construct id received over the network back to a live construct in the given level. */
    @Nullable
    public static AeronauticsConstruct resolve(Level level, UUID id) {
        return AeronauticsContraptionLookup.getConstructById(level, id);
    }
}
