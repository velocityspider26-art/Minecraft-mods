package shipwrights.genesis.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;
import team.lodestar.lodestone.systems.rendering.shader.ExtendedShaderInstance;
import team.lodestar.lodestone.systems.rendering.shader.ShaderHolder;

/**
 * Registers Genesis' bespoke celestial GLSL shaders (sun / planet / atmosphere / black hole …)
 * through Lodestone's shader system — the same shaders VS Genesis shipped on 1.20, now loaded on
 * Lodestone 1.8.2 for Minecraft 1.21.1. Renderers pull the live {@link ExtendedShaderInstance}
 * via {@link ShaderHolder#getShaderInstance()} and fall back to the vanilla-pipeline billboard
 * renderer if a shader failed to load (e.g. unsupported hardware).
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ShaderRegistry {

    public static final ShaderHolder SUN = new ShaderHolder(id("sun"), DefaultVertexFormat.POSITION_COLOR);
    public static final ShaderHolder BLACKHOLE = new ShaderHolder(id("blackhole"), DefaultVertexFormat.POSITION_COLOR);
    public static final ShaderHolder PLANET_ATMOSPHERE = new ShaderHolder(id("planet_atmosphere"), DefaultVertexFormat.POSITION_COLOR);
    public static final ShaderHolder PLANET_TEXTURED = new ShaderHolder(id("planet_textured"), DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
    public static final ShaderHolder STAR_GLOW = new ShaderHolder(id("star_glow"), DefaultVertexFormat.POSITION_COLOR);

    private ShaderRegistry() {}

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        SUN.register(event);
        BLACKHOLE.register(event);
        PLANET_ATMOSPHERE.register(event);
        PLANET_TEXTURED.register(event);
        STAR_GLOW.register(event);
        GenesisMod.LOGGER.info("Registered Genesis celestial shaders (sun, planet_textured, planet_atmosphere, blackhole, star_glow)");
    }

    /** The live shader instance, or null if it failed to load — callers must fall back. */
    public static ExtendedShaderInstance instance(ShaderHolder holder) {
        return holder.getShaderInstance();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, path);
    }

    /** Legacy hook retained for callers; no cached render types in the direct-draw path. */
    public static void clearTexturedPlanetRenderTypes() {
        // no-op
    }
}
