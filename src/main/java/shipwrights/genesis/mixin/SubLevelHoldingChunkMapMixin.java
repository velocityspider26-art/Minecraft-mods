package shipwrights.genesis.mixin;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.teleportation.CraftKeepAlive;

/**
 * Stops a craft being unloaded out from under a player who just travelled with
 * it.
 *
 * <p>Sable removes a sub-level whose world-space footprint is not block-ticking,
 * serialising it into a holding store and deleting it from the container. That
 * is reasonable for craft nobody is near, but it also fires on a craft that has
 * just been transferred into another dimension: the destination is not ticking
 * yet, and the craft is gone within seconds. Every attempt to satisfy the check
 * from outside — region tickets, wider claims, forced chunks — bought time
 * without ever winning the race, because the check runs every physics tick and
 * only has to fail once.</p>
 *
 * <p>So the unload is refused outright for craft Genesis is protecting. The
 * protection is narrow and time-limited (see {@link CraftKeepAlive}), so
 * ordinary unloading still works for everything else.</p>
 */
@Mixin(SubLevelHoldingChunkMap.class)
public class SubLevelHoldingChunkMapMixin {
    @Inject(method = "moveToUnloaded", at = @At("HEAD"), cancellable = true, remap = false)
    private void genesis$keepProtectedCraftLoaded(ServerSubLevel subLevel, ChunkPos pos, CallbackInfo ci) {
        if (CraftKeepAlive.isProtected(subLevel)) {
            ci.cancel();
        }
    }
}
