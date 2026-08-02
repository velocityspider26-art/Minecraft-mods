package shipwrights.genesis.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.lod.PlanetVolumeTextureCache;
import shipwrights.genesis.client.lod.PlanetVoxelClientCache;
import shipwrights.genesis.client.lod.PlanetVoxelRenderer;

/**
 * Drops sampled planet surfaces when leaving a world.
 *
 * <p>Surfaces are keyed by dimension, and two different saves both have an
 * "overworld" — so without this, joining a second world would show the first
 * one's terrain hanging in orbit until something happened to overwrite it.</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID)
public final class PlanetSurfaceLifecycle {
    private PlanetSurfaceLifecycle() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // These client-only state machines survive as static event subscribers.
        // Reset them before the level closes so no delayed render callback can
        // send a packet or draw stale terrain into the title-screen shutdown.
        SeamlessTransitionScreen.resetTransitionNotification();
        OrbitalSurveyController.reset();
        PlanetSurfaceTextures.clear();
        PlanetVoxelClientCache.clear();
        PlanetVoxelRenderer.reset();
        PlanetVolumeTextureCache.clear();
    }
}
