package dev.velocityspider.caw.client;

import dev.velocityspider.caw.CreateAerialWarfare;
import dev.velocityspider.caw.registry.ModBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CreateAerialWarfare.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ClientRegistration {
    private ClientRegistration() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.ENGINE_NOZZLE.get(),
                EngineNozzleRenderer::new
        );
    }
}
