package shipwrights.genesis.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.dimension.SubDimensions;

/**
 * Keeps the render section grid alive across a crossing between rooms of the
 * same house.
 *
 * <p>Vanilla releases every section buffer and allocates a new {@link ViewArea}
 * whenever the client level changes. Genesis rooms have matching section
 * geometry, so the existing grid can be re-pointed at the destination instead.
 * Every other part of {@code LevelRenderer.allChanged} remains vanilla.</p>
 *
 * <p>The redirects use {@code require = 0}. If mappings move, the game falls
 * back to vanilla's slower rebuild instead of failing during class transform.</p>
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererViewAreaMixin {
    @Shadow
    private ViewArea viewArea;

    @Shadow
    private ClientLevel level;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private long genesis$reuseStartedNanos;

    @Inject(method = "allChanged", at = @At("HEAD"), require = 0)
    private void genesis$beginReuseTiming(CallbackInfo callback) {
        genesis$reuseStartedNanos = genesis$canReuse(this.viewArea, this.level)
                ? System.nanoTime()
                : 0L;
    }

    @Inject(method = "allChanged", at = @At("RETURN"), require = 0)
    private void genesis$finishReuseTiming(CallbackInfo callback) {
        if (genesis$reuseStartedNanos == 0L) {
            return;
        }
        long elapsedMillis = (System.nanoTime() - genesis$reuseStartedNanos) / 1_000_000L;
        GenesisMod.LOGGER.info(
                "[VIEWAREA] room-grid handoff completed in {} ms",
                elapsedMillis);
        genesis$reuseStartedNanos = 0L;
    }

    /**
     * Skips releasing the buffers we are about to keep. Runs before the grid is
     * rebuilt, when {@code this.level} is already the destination.
     */
    @Redirect(
            method = "allChanged",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewArea;releaseAllBuffers()V"),
            require = 0)
    private void genesis$keepBuffersBetweenRooms(ViewArea instance) {
        if (!genesis$canReuse(instance, this.level)) {
            instance.releaseAllBuffers();
        }
    }

    /** Hands back the standing grid instead of allocating a new one. */
    @Redirect(
            method = "allChanged",
            at = @At(value = "NEW", target = "(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;"
                    + "Lnet/minecraft/world/level/Level;ILnet/minecraft/client/renderer/LevelRenderer;)"
                    + "Lnet/minecraft/client/renderer/ViewArea;"),
            require = 0)
    private ViewArea genesis$reuseGridBetweenRooms(SectionRenderDispatcher dispatcher,
                                                   Level destination,
                                                   int viewDistance,
                                                   LevelRenderer renderer) {
        ViewArea existing = this.viewArea;
        if (!genesis$canReuse(existing, destination)) {
            return new ViewArea(dispatcher, destination, viewDistance, renderer);
        }

        ((ViewAreaAccessor) existing).genesis$setLevel(destination);

        // The buffers themselves are reusable, but their geometry belongs to
        // the world we just left. Mark every section dirty so vanilla rebuilds
        // them from destination chunks in its normal distance-prioritised queue.
        for (SectionRenderDispatcher.RenderSection section : existing.sections) {
            section.setDirty(false);
        }

        GenesisMod.LOGGER.info(
                "[VIEWAREA] reused {} sections crossing into {}",
                existing.sections.length,
                destination.dimension().location());
        return existing;
    }

    /**
     * Whether the standing grid actually fits the destination: same room set,
     * same vertical geometry, and the same current render-distance width.
     */
    @Unique
    private boolean genesis$canReuse(ViewArea existing, Level destination) {
        if (existing == null || destination == null) {
            return false;
        }
        Level previous = ((ViewAreaAccessor) existing).genesis$getLevel();
        if (!SubDimensions.sameFloorplan(previous, destination)) {
            return false;
        }

        int expectedWidth = this.minecraft.options.getEffectiveRenderDistance() * 2 + 1;
        return ((ViewAreaAccessor) existing).genesis$getSectionGridSizeX() == expectedWidth
                && ((ViewAreaAccessor) existing).genesis$getSectionGridSizeY()
                == destination.getSectionsCount();
    }
}
