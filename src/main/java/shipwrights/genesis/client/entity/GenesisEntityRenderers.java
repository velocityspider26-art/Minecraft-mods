package shipwrights.genesis.client.entity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.entity.GenesisEntities;

/** Client-side wiring for Genesis entity models and renderers. */
@EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GenesisEntityRenderers {
    private GenesisEntityRenderers() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GenesisModelLayers.SPACE_CRITTER, SpaceCritterModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GenesisEntities.MOON_LURKER.get(), SpaceCritterRenderer::new);
        event.registerEntityRenderer(GenesisEntities.CINDER_CRAWLER.get(), SpaceCritterRenderer::new);
    }
}
