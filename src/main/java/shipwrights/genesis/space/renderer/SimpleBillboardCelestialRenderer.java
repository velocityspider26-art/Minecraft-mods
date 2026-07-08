package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.properties.PlanetProperties;
import shipwrights.genesis.space.properties.StarProperties;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Vanilla-pipeline celestial renderer.
 *
 * <p>Bodies with a texture at {@code genesis:textures/planets/<namespace>/<path>.png}
 * (e.g. the Earth and Moon) draw as textured billboards with an additive atmosphere
 * halo when the body has an atmosphere. Stars draw as a bright core disc inside a
 * large additive glow; black holes draw as a dark disc with a thin accretion rim.
 * All geometry is billboarded on a fixed-radius sky sphere around the observer and
 * transformed by the level renderer's real model-view matrix.</p>
 */
public class SimpleBillboardCelestialRenderer implements CelestialRenderer {

    private static final double SKY_RADIUS = 100.0;
    /** celestial registry id -> planet texture (empty when the texture doesn't exist). */
    private static final Map<ResourceLocation, Optional<ResourceLocation>> TEXTURE_CACHE = new HashMap<>();

    private final float r, g, b, a;

    public SimpleBillboardCelestialRenderer(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @NotNull VantagePoint vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor) event.getLevelRenderer()).getLevel();
        if (level == null) return;
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);

        Vector3dc bodyPos = toRender.getPosition(ticks, partialTick, registry);
        Vector3dc vantagePos = vantagePoint.getObserverPosition();

        Vector3d dir = new Vector3d(bodyPos).sub(vantagePos);
        double dist = dir.length();
        double bodyRadius = toRender.getActualSize() * 0.5;
        // Inside (or effectively at) the body — nothing sensible to draw.
        if (dist < Math.max(1.0e-3, bodyRadius * 0.75)) return;
        dir.div(dist);

        // If the observer is on a celestial, express the direction in the local sky frame.
        if (vantagePoint instanceof VantagePoint.OnCelestial oc) {
            dir.rotate(oc.getRotation().conjugate(new org.joml.Quaterniond()));
        }

        // Angular size -> billboard half-extent on the sky sphere (capped so a close
        // planet fills the view without the tangent blowing up).
        double angularRadius = Math.min(Math.atan2(bodyRadius, dist), 1.15);
        float half = (float) Math.max(0.5, Math.tan(angularRadius) * SKY_RADIUS);

        Vector3d up = Math.abs(dir.y) > 0.99 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(dir).cross(up).normalize();
        Vector3d realUp = new Vector3d(right).cross(dir).normalize();
        Vector3d center = new Vector3d(dir).mul(SKY_RADIUS);

        Matrix4f pose = new Matrix4f(event.getModelViewMatrix());

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();

        if (toRender.type() == BuiltinCelestialTypes.STAR) {
            renderStar(pose, toRender, center, right, realUp, half);
        } else if (toRender.type() == BuiltinCelestialTypes.BLACKHOLE) {
            renderBlackHole(pose, center, right, realUp, half);
        } else {
            renderBody(pose, toRender, registry, center, right, realUp, half);
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void renderBody(Matrix4f pose, Celestial body, Registry<Celestial> registry,
                            Vector3d center, Vector3d right, Vector3d up, float half) {
        // Additive atmosphere halo behind the body — the blue rim that makes a planet
        // read as a world seen from orbit rather than a flat sprite.
        if (body.properties() instanceof PlanetProperties pp && pp.atmosphere() != null && pp.atmosphere().density() > 0) {
            float density = (float) Math.min(1.0, pp.atmosphere().density());
            float thickness = (float) Math.max(0.15, Math.min(1.0, pp.atmosphere().thickness()));
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            drawRadialFan(pose, center, right, up,
                    half * 0.99f, half * (1.0f + 0.22f * thickness),
                    0.35f, 0.60f, 1.0f, 0.55f * density,
                    0.30f, 0.55f, 1.0f, 0.0f);
            RenderSystem.defaultBlendFunc();
        }

        Optional<ResourceLocation> texture = textureFor(body, registry);
        if (texture.isPresent()) {
            drawTexturedGlobe(pose, texture.get(), center, right, up, half);
        } else {
            // No art for this body — draw a shaded disc in the type's fallback colour.
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            drawRadialFan(pose, center, right, up, 0f, half,
                    r, g, b, a, r * 0.55f, g * 0.55f, b * 0.55f, a);
        }
    }

    private void renderStar(Matrix4f pose, Celestial star, Vector3d center, Vector3d right, Vector3d up, float half) {
        float coreR = 1.0f, coreG = 0.95f, coreB = 0.75f;
        float haloR = 1.0f, haloG = 0.8f, haloB = 0.15f;
        if (star.properties() instanceof StarProperties sp) {
            haloR = sp.r1() / 255f;
            haloG = sp.g1() / 255f;
            haloB = sp.b1() / 255f;
            // Core biased strongly toward white so the disc reads as blindingly hot.
            coreR = haloR + (1f - haloR) * 0.85f;
            coreG = haloG + (1f - haloG) * 0.85f;
            coreB = haloB + (1f - haloB) * 0.85f;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        // Wide soft corona.
        drawRadialFan(pose, center, right, up, 0f, half * 3.2f,
                haloR, haloG, haloB, 0.35f, haloR, haloG, haloB, 0.0f);
        // Tight bright glow hugging the disc.
        drawRadialFan(pose, center, right, up, half * 0.85f, half * 1.6f,
                coreR, coreG, coreB, 0.75f, haloR, haloG, haloB, 0.0f);
        // The photosphere itself.
        drawRadialFan(pose, center, right, up, 0f, half,
                coreR, coreG, coreB, 1.0f, haloR, haloG, haloB, 0.95f);

        RenderSystem.defaultBlendFunc();
    }

    /**
     * Camera-facing additive atmosphere rim around a body — used by the shader planet renderer to
     * give the cube planet the same blue limb the billboard planet has.
     */
    public void drawAtmosphereRim(Matrix4f pose, Vector3d center, org.joml.Quaternionf ignoredRot, float half,
                                  PlanetProperties pp) {
        if (pp.atmosphere() == null || pp.atmosphere().density() <= 0) return;
        Vector3d dir = new Vector3d(center);
        if (dir.lengthSquared() < 1.0e-9) return;
        dir.normalize();
        Vector3d up = Math.abs(dir.y) > 0.99 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(dir).cross(up).normalize();
        Vector3d realUp = new Vector3d(right).cross(dir).normalize();

        float density = (float) Math.min(1.0, pp.atmosphere().density());
        float thickness = (float) Math.max(0.15, Math.min(1.0, pp.atmosphere().thickness()));
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        drawRadialFan(pose, center, right, realUp,
                half * 1.02f, half * (1.0f + 0.28f * thickness),
                0.35f, 0.60f, 1.0f, 0.55f * density,
                0.30f, 0.55f, 1.0f, 0.0f);
        RenderSystem.defaultBlendFunc();
    }

    private void renderBlackHole(Matrix4f pose, Vector3d center, Vector3d right, Vector3d up, float half) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        // Thin hot accretion rim...
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        drawRadialFan(pose, center, right, up, half * 0.95f, half * 1.45f,
                0.85f, 0.55f, 1.0f, 0.6f, 0.3f, 0.05f, 0.6f, 0.0f);
        RenderSystem.defaultBlendFunc();
        // ...around a void-black event horizon.
        drawRadialFan(pose, center, right, up, 0f, half,
                0f, 0f, 0f, 1.0f, 0.02f, 0.0f, 0.05f, 1.0f);
    }

    /**
     * Draws the visible hemisphere of a planet as a disc whose UVs orthographically
     * sample the central hemisphere of an equirectangular surface map — the flat map
     * wraps convincingly into a globe seen from space.
     */
    private static void drawTexturedGlobe(Matrix4f pose, ResourceLocation texture,
                                          Vector3d center, Vector3d right, Vector3d up, float half) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, texture);

        int rings = 10;
        int segments = 36;
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int j = 0; j < rings; j++) {
            float r0 = (float) j / rings;
            float r1 = (float) (j + 1) / rings;
            for (int i = 0; i < segments; i++) {
                double a0 = i * (Math.PI * 2.0) / segments;
                double a1 = (i + 1) * (Math.PI * 2.0) / segments;
                globeVertex(buffer, pose, center, right, up, half, r0, a0);
                globeVertex(buffer, pose, center, right, up, half, r0, a1);
                globeVertex(buffer, pose, center, right, up, half, r1, a1);
                globeVertex(buffer, pose, center, right, up, half, r1, a0);
            }
        }
        draw(buffer);
    }

    private static void globeVertex(BufferBuilder buffer, Matrix4f pose,
                                    Vector3d center, Vector3d right, Vector3d up,
                                    float half, float radius, double angle) {
        double dx = Math.cos(angle) * radius;
        double dy = Math.sin(angle) * radius;
        double px = dx * half;
        double py = dy * half;
        // Orthographic point on the unit sphere -> longitude/latitude -> equirect UV.
        double dz = Math.sqrt(Math.max(0.0, 1.0 - dx * dx - dy * dy));
        double lon = Math.atan2(dx, dz);           // [-pi/2, pi/2] visible hemisphere
        double lat = Math.asin(Math.max(-1.0, Math.min(1.0, dy)));
        float u = (float) (0.5 + lon / (Math.PI * 2.0));
        float v = (float) (0.5 - lat / Math.PI);
        buffer.addVertex(pose,
                (float) (center.x + right.x * px + up.x * py),
                (float) (center.y + right.y * px + up.y * py),
                (float) (center.z + right.z * px + up.z * py)).setUv(u, v);
    }

    /** Draws an annular (or full) radial-gradient fan facing the observer. */
    private void drawRadialFan(Matrix4f pose, Vector3d center, Vector3d right, Vector3d up,
                               float innerRadius, float outerRadius,
                               float ir, float ig, float ib, float ia,
                               float or_, float og, float ob, float oa) {
        int segments = 40;
        BufferBuilder buffer;
        if (innerRadius <= 0f) {
            buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            buffer.addVertex(pose, (float) center.x, (float) center.y, (float) center.z).setColor(ir, ig, ib, ia);
            for (int i = 0; i <= segments; i++) {
                double angle = i * (Math.PI * 2.0) / segments;
                ring(buffer, pose, center, right, up, outerRadius, angle).setColor(or_, og, ob, oa);
            }
        } else {
            buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i <= segments; i++) {
                double angle = i * (Math.PI * 2.0) / segments;
                ring(buffer, pose, center, right, up, innerRadius, angle).setColor(ir, ig, ib, ia);
                ring(buffer, pose, center, right, up, outerRadius, angle).setColor(or_, og, ob, oa);
            }
        }
        draw(buffer);
    }

    private static com.mojang.blaze3d.vertex.VertexConsumer ring(BufferBuilder buffer, Matrix4f pose, Vector3d center, Vector3d right, Vector3d up, float radius, double angle) {
        double cos = Math.cos(angle) * radius;
        double sin = Math.sin(angle) * radius;
        return buffer.addVertex(pose,
                (float) (center.x + right.x * cos + up.x * sin),
                (float) (center.y + right.y * cos + up.y * sin),
                (float) (center.z + right.z * cos + up.z * sin));
    }

    private static com.mojang.blaze3d.vertex.VertexConsumer corner(BufferBuilder buffer, Matrix4f pose, Vector3d center, Vector3d right, Vector3d up, float sx, float sy) {
        return buffer.addVertex(pose,
                (float) (center.x + right.x * sx + up.x * sy),
                (float) (center.y + right.y * sx + up.y * sy),
                (float) (center.z + right.z * sx + up.z * sy));
    }

    private static void draw(BufferBuilder buffer) {
        MeshData mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }

    private static Optional<ResourceLocation> textureFor(Celestial body, Registry<Celestial> registry) {
        ResourceLocation id = registry.getKey(body);
        if (id == null) return Optional.empty();
        return TEXTURE_CACHE.computeIfAbsent(id, key -> {
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID,
                    "textures/planets/" + key.getNamespace() + "/" + key.getPath() + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(tex).isPresent()
                    ? Optional.of(tex) : Optional.empty();
        });
    }
}
