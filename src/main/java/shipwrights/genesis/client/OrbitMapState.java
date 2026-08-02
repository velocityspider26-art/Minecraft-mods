package shipwrights.genesis.client;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The body the player has picked as a target in the orbit map. Kept apart from
 * the map screen itself so the in-world orbit renderer can highlight the same
 * selection while the map is closed.
 */
public final class OrbitMapState {
    @Nullable
    private static volatile ResourceLocation selectedTarget;

    private OrbitMapState() {
    }

    @Nullable
    public static ResourceLocation getSelectedTarget() {
        return selectedTarget;
    }

    public static void setSelectedTarget(@Nullable ResourceLocation target) {
        selectedTarget = target;
    }
}
