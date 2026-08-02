package shipwrights.genesis.mixin;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.teleportation.CraftKeepAlive;

/**
 * Keeps a craft loaded while somebody is standing on it.
 *
 * <p>Sable unloads a sub-level whose chunks it does not consider ticking,
 * moving it into a holding store. That is correct housekeeping for craft nobody
 * is near, but it also fires on a craft a player is currently flying: in the
 * space dimension almost nothing is loaded by default, so a craft that has just
 * crossed a dimension boundary — or simply drifted — gets swept up and vanishes
 * out from under its crew.</p>
 *
 * <p>Both routes to that removal, the physics tick and the world save, funnel
 * through this one method, so it is the single place worth guarding. Earlier
 * attempts blocked one path at a time and the other simply took over a few
 * seconds later.</p>
 *
 * <p>The condition is deliberately about players rather than a timer: a craft
 * with a player aboard is in use and must not be unloaded, and one with nobody
 * near is free to unload normally. Only UNLOADED is refused — a genuine removal
 * (broken up, deleted, world shutting down) still goes through.</p>
 */
@Mixin(SubLevelContainer.class)
public class SubLevelContainerMixin {
    @Inject(method = "removeSubLevel(Ldev/ryanhcode/sable/sublevel/SubLevel;Ldev/ryanhcode/sable/sublevel/storage/SubLevelRemovalReason;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void genesis$keepOccupiedCraftLoaded(SubLevel subLevel, SubLevelRemovalReason reason, CallbackInfo ci) {
        if (reason == SubLevelRemovalReason.UNLOADED
                && CraftKeepAlive.shouldStayLoaded((SubLevelContainer) (Object) this, subLevel)) {
            ci.cancel();
        }
    }
}
