package com.velocityspider.createjetengines.client;

import com.velocityspider.createjetengines.CreateJetEngines;
import com.velocityspider.createjetengines.client.render.CombustionCoreRenderer;
import com.velocityspider.createjetengines.client.render.JetModuleRenderer;
import com.velocityspider.createjetengines.client.particle.ExhaustParticle;
import com.velocityspider.createjetengines.client.particle.JetFlameParticle;
import com.velocityspider.createjetengines.client.render.JetPartials;
import com.velocityspider.createjetengines.client.render.PlumeManager;
import com.velocityspider.createjetengines.client.sound.JetSoundHandler;
import com.velocityspider.createjetengines.registry.JetBlockEntities;
import com.velocityspider.createjetengines.registry.JetParticles;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-only wiring.
 *
 * <p>Nothing in this package is referenced from common code, so a dedicated server never loads a
 * client class. The target bus is inferred from each event type.
 */
@EventBusSubscriber(modid = CreateJetEngines.MODID, value = Dist.CLIENT)
public final class JetClientSetup {

    private JetClientSetup() {
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(JetBlockEntities.MODULE.get(), JetModuleRenderer::new);
        event.registerBlockEntityRenderer(JetBlockEntities.COMBUSTION_CORE.get(), CombustionCoreRenderer::new);
    }

    /** The animated parts are standalone models, so they must be requested explicitly. */
    @SubscribeEvent
    static void registerModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : JetPartials.ALL) {
            event.register(model);
        }
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(JetParticles.EXHAUST_HAZE.get(), ExhaustParticle.HazeProvider::new);
        event.registerSpriteSet(JetParticles.EXHAUST_SOOT.get(), ExhaustParticle.SootProvider::new);
        event.registerSpriteSet(JetParticles.JET_FLAME.get(), JetFlameParticle.FlameProvider::new);
        event.registerSpriteSet(JetParticles.SHOCK_DIAMOND.get(), JetFlameParticle.ShockProvider::new);
    }

    /**
     * Draws the exhaust plumes in world space.
     *
     * <p>After translucent blocks but before particles, so the particles composite on top of the
     * plume rather than being swallowed by it.
     */
    @SubscribeEvent
    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            PlumeManager.render(event.getPoseStack(), event.getCamera(),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }

    /** Ages the plumes even when their nozzle is off screen. */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        PlumeManager.tick();
    }

    /** Drop all looping sounds and plumes when leaving a world so nothing carries over. */
    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        JetSoundHandler.reset();
        PlumeManager.reset();
    }
}
