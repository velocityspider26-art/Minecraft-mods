package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import shipwrights.genesis.GenesisMod;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypes;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;
import team.lodestar.lodestone.systems.rendering.StateShards;
import team.lodestar.lodestone.systems.rendering.rendeertype.RenderTypeToken;
import team.lodestar.lodestone.systems.rendering.shader.ShaderHolder;
import shipwrights.genesis.mixin.RenderStateShardAccessor;

import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ShaderRegistry {

    public static final ShaderHolder SUN_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "sun"), DefaultVertexFormat.POSITION_COLOR);
    public static final ShaderHolder BLACKHOLE_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "blackhole"), DefaultVertexFormat.POSITION_COLOR);
    public static final ShaderHolder PLANET_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet"), DefaultVertexFormat.POSITION_TEX_COLOR);
    public static final ShaderHolder PLANET_ATMOSPHERE_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet_atmosphere"), DefaultVertexFormat.POSITION_COLOR);
    public static final ShaderHolder PLANET_TEXTURED_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet_textured"), DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
    public static final ShaderHolder PLANET_MASK_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet_mask"), DefaultVertexFormat.POSITION_COLOR);
    public static final ShaderHolder PLANET_SHADOW_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet_shadow"), DefaultVertexFormat.POSITION_COLOR);
    public static final ShaderHolder WORMHOLE_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "wormhole"), DefaultVertexFormat.POSITION_TEX_COLOR);
    public static final ShaderHolder HYPERSPACE_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "hyperspace"), DefaultVertexFormat.POSITION);
    public static final ShaderHolder STAR_GLOW_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "star_glow"), DefaultVertexFormat.POSITION_COLOR);

    @SubscribeEvent
    public static void shaderRegistry(RegisterShadersEvent event) {
        SUN_SHADER.register(event);
        BLACKHOLE_SHADER.register(event);
        PLANET_SHADER.register(event);
        PLANET_ATMOSPHERE_SHADER.register(event);
        PLANET_TEXTURED_SHADER.register(event);
        PLANET_MASK_SHADER.register(event);
        PLANET_SHADOW_SHADER.register(event);
        WORMHOLE_SHADER.register(event);
        HYPERSPACE_SHADER.register(event);
        STAR_GLOW_SHADER.register(event);
    }

    private static LodestoneRenderType SUN_RENDER_TYPE;
    private static LodestoneRenderType SUN_OCCLUDING_RENDER_TYPE;
    private static LodestoneRenderType BLACKHOLE_RENDER_TYPE;
    private static LodestoneRenderType PLANET_RENDER_TYPE;
    private static LodestoneRenderType PLANET_ATMOSPHERE_RENDER_TYPE;
    private static LodestoneRenderType PLANET_MASK_RENDER_TYPE;
    private static final ConcurrentHashMap<ResourceLocation, LodestoneRenderType> TEXTURED_PLANET_RENDER_TYPES = new ConcurrentHashMap<>();

    public static LodestoneRenderType getSunRenderType() {
        if (SUN_RENDER_TYPE == null) {
            SUN_RENDER_TYPE = LodestoneRenderTypes.createGenericRenderType(null, "sun_render_type", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, LodestoneRenderTypes.builder()
                    .setShaderState(SUN_SHADER)
                    .setTransparencyState(StateShards.NORMAL_TRANSPARENCY)
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                    .setCullState(LodestoneRenderTypes.CULL)
            );
        }
        return SUN_RENDER_TYPE;
    }

    /**
     * The star, but writing depth as well as colour.
     *
     * <p>Celestials are all clamped to the same render distance, so which body
     * ends up in front is decided purely by draw order — and draw order is
     * sorted by <em>true</em> celestial distance, which the clamping throws
     * away. Letting the star occupy the depth buffer makes the hardware settle
     * it instead: the planet render type already writes depth, so whichever of
     * the two is genuinely nearer wins no matter which was drawn first. That is
     * what stops the sun showing through the Earth.</p>
     *
     * <p>Only used in the space dimension. On a planet surface the star sits in
     * the sky at the clamped distance, and depth-writing there would punch a
     * hole through any terrain drawn beyond it.</p>
     */
    public static LodestoneRenderType getSunOccludingRenderType() {
        if (SUN_OCCLUDING_RENDER_TYPE == null) {
            SUN_OCCLUDING_RENDER_TYPE = LodestoneRenderTypes.createGenericRenderType(null, "sun_occluding_render_type", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, LodestoneRenderTypes.builder()
                    .setShaderState(SUN_SHADER)
                    .setTransparencyState(StateShards.NORMAL_TRANSPARENCY)
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypes.CULL)
            );
        }
        return SUN_OCCLUDING_RENDER_TYPE;
    }

    public static LodestoneRenderType getBlackholeRenderType() {
        if (BLACKHOLE_RENDER_TYPE == null) {
            BLACKHOLE_RENDER_TYPE = LodestoneRenderTypes.createGenericRenderType(null, "blackhole_render_type", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, LodestoneRenderTypes.builder()
                    .setShaderState(BLACKHOLE_SHADER)
                    .setTransparencyState(StateShards.NORMAL_TRANSPARENCY)
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                    .setCullState(LodestoneRenderTypes.CULL)
            );
        }
        return BLACKHOLE_RENDER_TYPE;
    }

    public static LodestoneRenderType getPlanetRenderType() {
        if (PLANET_RENDER_TYPE == null) {
            PLANET_RENDER_TYPE = LodestoneRenderTypes.createGenericRenderType(null, "planet_render_type", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, LodestoneRenderTypes.builder()
                    .setShaderState(PLANET_SHADER)
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("no_transparency", RenderSystem::disableBlend, () -> {}))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypes.CULL)
            );
        }
        return PLANET_RENDER_TYPE;
    }

    public static LodestoneRenderType getPlanetAtmosphereRenderType() {
        if (PLANET_ATMOSPHERE_RENDER_TYPE == null) {
            PLANET_ATMOSPHERE_RENDER_TYPE = LodestoneRenderTypes.createGenericRenderType(null, "planet_atmosphere_render_type", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, LodestoneRenderTypes.builder()
                    .setShaderState(PLANET_ATMOSPHERE_SHADER)
                    .setTransparencyState(StateShards.NORMAL_TRANSPARENCY)
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                    .setCullState(LodestoneRenderTypes.CULL)
            );
        }
        return PLANET_ATMOSPHERE_RENDER_TYPE;
    }

    public static LodestoneRenderType getPlanetMaskRenderType() {
        if (PLANET_MASK_RENDER_TYPE == null) {
            PLANET_MASK_RENDER_TYPE = LodestoneRenderTypes.createGenericRenderType(null, "planet_mask_render_type", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, LodestoneRenderTypes.builder()
                    .setShaderState(PLANET_MASK_SHADER)
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("no_transparency", RenderSystem::disableBlend, () -> {}))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypes.CULL)
            );
        }
        return PLANET_MASK_RENDER_TYPE;
    }

    private static LodestoneRenderType PLANET_SHADOW_RENDER_TYPE;

    public static LodestoneRenderType getPlanetShadowRenderType() {
        if (PLANET_SHADOW_RENDER_TYPE == null) {
            PLANET_SHADOW_RENDER_TYPE = LodestoneRenderTypes.createGenericRenderType(
                null,
                "planet_shadow_render_type",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES,
                LodestoneRenderTypes.builder()
                    .setShaderState(PLANET_SHADOW_SHADER)
                    .setTransparencyState(StateShards.NORMAL_TRANSPARENCY)
                        .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                        .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                    .setCullState(LodestoneRenderTypes.CULL)
            );
        }
        return PLANET_SHADOW_RENDER_TYPE;
    }

    /**
     * Gets a render type for a textured planet.
     *
     * @param textureLocation The texture location (without textures/ prefix or .png extension)
     * @return A render type that uses the textured planet shader with the specified texture
     */
    public static LodestoneRenderType getTexturedPlanetRenderType(ResourceLocation textureLocation) {
        return TEXTURED_PLANET_RENDER_TYPES.computeIfAbsent(textureLocation, loc -> {
            // Build the full texture path
            ResourceLocation fullTexturePath = ResourceLocation.fromNamespaceAndPath(
                loc.getNamespace(),
                "textures/" + loc.getPath() + ".png"
            );

            return LodestoneRenderTypes.createGenericRenderType(
                RenderTypeToken.createToken(fullTexturePath),
                "planet_textured_" + loc.getNamespace() + "_" + loc.getPath().replace("/", "_"),
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
                VertexFormat.Mode.QUADS,
                LodestoneRenderTypes.builder()
                    .setShaderState(PLANET_TEXTURED_SHADER)
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("no_transparency", RenderSystem::disableBlend, () -> {}))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    // Planets DO write depth. In space the shader writes real
                    // depth (FlatDepth = 0), so a planet in front genuinely
                    // occludes the star's glow behind it. On a planet surface the
                    // shader pins depth to the far plane instead, so writing it
                    // there is harmless and distant terrain still draws over.
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypes.CULL)
                    // Bind the planet texture to texture unit 0 (Sampler0). Without this the
                    // shader samples whatever was last bound (the block atlas). The RenderTypeToken
                    // above only names/caches the render type; it does not bind the texture.
                    .setTextureState(new RenderStateShard.TextureStateShard(fullTexturePath, false, false))
            );
        });
    }

    /**
     * Clears cached textured planet render types. Call when resources are reloaded.
     */
    public static void clearTexturedPlanetRenderTypes() {
        TEXTURED_PLANET_RENDER_TYPES.clear();
    }
}
