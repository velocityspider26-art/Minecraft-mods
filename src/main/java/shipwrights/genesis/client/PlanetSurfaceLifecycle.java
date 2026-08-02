package shipwrights.genesis.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.lod.DhCubeLodBridge;
import shipwrights.genesis.client.lod.PlanetLodVolumeClientCache;
import shipwrights.genesis.client.lod.PlanetVolumeLodRenderer;
import shipwrights.genesis.client.lod.PlanetVolumeTextureCache;
import shipwrights.genesis.client.lod.PlanetVoxelClientCache;
import shipwrights.genesis.client.lod.PlanetVoxelRenderer;
import shipwrights.genesis.client.lod.SparsePlanetLodClientCache;

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
        // Reset them before DH closes its level so no delayed render callback can
        // send a packet or draw stale terrain into the title-screen shutdown.
        SeamlessTransitionScreen.resetTransitionNotification();
        OrbitalSurveyController.reset();
        DhCubeLodBridge.clear();
        PlanetSurfaceTextures.clear();
        SparsePlanetLodClientCache.clear();
        PlanetLodVolumeClientCache.clear();
        PlanetVoxelClientCache.clear();
        PlanetVoxelRenderer.reset();
        PlanetVolumeTextureCache.clear();
        PlanetVolumeLodRenderer.resetLogging();
    }
}
