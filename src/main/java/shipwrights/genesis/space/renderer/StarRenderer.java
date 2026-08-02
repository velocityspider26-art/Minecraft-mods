package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Registry;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetDimensionEffects;
import shipwrights.genesis.client.ShaderRegistry;
import shipwrights.genesis.client.VeilBloomBridge;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.properties.StarProperties;

import static shipwrights.genesis.client.ShaderRegistry.getSunRenderType;

public class StarRenderer implements CelestialRenderer {
    private static final double[] CORONA_SCALES = {1.008, 1.025, 1.055, 1.10};
    private static final int[][] CORONA_COLORS = {
            {255, 253, 232},
            {255, 229, 148},
            {255, 183, 72},
            {255, 104, 28}
    };

    private record FaceBasis(Vector3d normal, Vector3d tangent, Vector3d bitangent) {
    }

    private enum GlowPass {
        MAIN,
        BLOOM
    }

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender,
                       @NotNull VantagePoint vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor) event.getLevelRenderer()).getLevel();
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        Vector3dc absolutePosition = toRender.getPosition(ticks, partialTick, registry);
        Vector3dc observerPosition = CelestialRenderCoordinates.getObserverCelestialPosition(
                vantagePoint, event.getCamera().getPosition());

        float opacity = 1.0f;
        double distanceSquared = observerPosition.distanceSquared(absolutePosition);
        if (vantagePoint instanceof VantagePoint.OnCelestial) {
            opacity = (float) Math.min(1.0,
                    PlanetDimensionEffects.cachedStarBrightness
                            + 20_000.0 * 20_000.0 / Math.max(distanceSquared, 1.0E-8));
        }
        if (opacity < 0.01f) {
            return;
        }

        CelestialRenderCoordinates.RenderTransform renderTransform = CelestialRenderCoordinates.getRenderTransform(
                toRender, vantagePoint, event.getCamera().getPosition(), ticks, partialTick, registry);
        Vector3dc center = renderTransform.position();
        Quaterniondc rotation = renderTransform.rotation();
        float timeSeconds = (ticks + partialTick) / 20.0f;
        float halfSize = (float) toRender.getActualSize() * 0.5f;
        boolean spaceView = GenesisMod.isSpaceDimension(level);

        // Keep the star inside a finite far plane while preserving its angular
        // size. Sending a 15,000-block absolute vertex position through an
        // infinite projection broke depth reconstruction and made world
        // geometry turn black at altitude, especially with Sable/Flywheel.
        double renderDistanceScale = CelestialRenderCoordinates.getPlanetRenderDistanceScale(center);
        if (renderDistanceScale != 1.0) {
            center = CelestialRenderCoordinates.scaleRenderPosition(center, renderDistanceScale);
            halfSize *= (float) renderDistanceScale;
        }

        Vector3f cameraLocal = new Vector3f((float) -center.x(), (float) -center.y(), (float) -center.z());
        new Quaternionf(rotation).conjugate().transform(cameraLocal);
        Matrix4f localMatrix = new Matrix4f(event.getPoseStack().last().pose())
                .translate((float) center.x(), (float) center.y(), (float) center.z())
                .rotate(new Quaternionf(rotation));

        // Draw the square corona behind the opaque star so only its expanded
        // silhouette remains visible around the cube.
        float coronaIntensity = spaceView ? 1.0f : 1.28f;
        renderCorona(localMatrix, halfSize, new Vector3d(cameraLocal),
                opacity * coronaIntensity, GlowPass.MAIN);

        StarProperties properties = toRender.properties() instanceof StarProperties star ? star : null;
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        // The star never writes depth (see the planet render type): far-plane
        // depth precision is too poor to occlude with, so a nearer planet drawn
        // later in the far-to-near loop simply paints over the star instead.
        VertexConsumer sunBuffer = bufferSource.getBuffer(getSunRenderType());
        renderSun(sunBuffer, localMatrix, halfSize, cameraLocal, properties,
                timeSeconds, opacity, spaceView);
        bufferSource.endBatch(getSunRenderType());

        // Draw both glow layers INLINE, right after the star, in the main
        // framebuffer. Because celestials draw far-to-near, a planet nearer than
        // the star is drawn later in the loop and — being opaque — paints over
        // the additive glow; terrain and the player's ship, drawn after the sky
        // pass, do the same. That painter's ordering is what hides the sun
        // behind things. Deferring the glow past the whole scene broke it: two
        // bodies clamped to the same render distance then z-fight, and the sun
        // bled through a planet it was sitting behind.
        if (spaceView) {
            SolarFlareParticleEmitter.renderAttachedMain(
                    localMatrix, new Vector3d(cameraLocal), halfSize, timeSeconds, opacity);
        }
        renderCorona(localMatrix, halfSize, new Vector3d(cameraLocal),
                opacity * (spaceView ? 1.0f : 1.38f), GlowPass.BLOOM);
        if (spaceView) {
            SolarFlareParticleEmitter.renderAttachedBloom(
                    localMatrix, new Vector3d(cameraLocal), halfSize, timeSeconds, opacity);
        }
    }

    private static void renderSun(VertexConsumer buffer, Matrix4f matrix, float halfSize,
                                  Vector3f cameraLocal, StarProperties properties,
                                  float timeSeconds, float opacity, boolean spaceView) {
        ShaderInstance shader = ShaderRegistry.SUN_SHADER.getShaderInstance();
        if (shader != null) {
            set(shader, "CameraPosition", cameraLocal.x, cameraLocal.y, cameraLocal.z);
            set(shader, "HalfSize", halfSize);
            set(shader, "Time", timeSeconds);
            set(shader, "Opacity", opacity);
            set(shader, "SpaceView", spaceView ? 1.0f : 0.0f);

            if (properties != null) {
                set(shader, "Color0", properties.r0() / 255.0f, properties.g0() / 255.0f,
                        properties.b0() / 255.0f);
                set(shader, "Color1", properties.r1() / 255.0f, properties.g1() / 255.0f,
                        properties.b1() / 255.0f);
            } else {
                set(shader, "Color0", 1.0f, 0.58f, 0.08f);
                set(shader, "Color1", 1.0f, 0.96f, 0.80f);
            }
        }

        addSunCube(matrix, buffer, halfSize);
    }

    private static void set(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void set(ShaderInstance shader, String name, float x, float y, float z) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z);
        }
    }

    private static void renderCorona(Matrix4f matrix, double halfSize, Vector3d cameraLocal,
                                     float opacity, GlowPass pass) {
        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        emitCorona(builder, matrix, halfSize, cameraLocal, opacity, pass);
        drawGlow(builder, pass == GlowPass.BLOOM);
    }

    private static void emitCorona(BufferBuilder builder, Matrix4f matrix, double halfSize,
                                   Vector3d cameraLocal, float opacity, GlowPass pass) {
        int[] mainAlpha = {48, 28, 14, 6};
        int[] bloomAlpha = {138, 88, 44, 18};
        int[] alphas = pass == GlowPass.BLOOM ? bloomAlpha : mainAlpha;

        for (int shell = CORONA_SCALES.length - 1; shell >= 0; shell--) {
            double radius = halfSize * CORONA_SCALES[shell];
            int[] color = rgba(CORONA_COLORS[shell][0], CORONA_COLORS[shell][1],
                    CORONA_COLORS[shell][2], alphas[shell] * opacity);
            for (int face = 0; face < 6; face++) {
                FaceBasis basis = faceBasis(face);
                Vector3d faceCenter = new Vector3d(basis.normal()).mul(radius);
                if (basis.normal().dot(new Vector3d(cameraLocal).sub(faceCenter)) <= 0.0) {
                    continue;
                }
                Vector3d p00 = new Vector3d(faceCenter).fma(-radius, basis.tangent())
                        .fma(-radius, basis.bitangent());
                Vector3d p10 = new Vector3d(faceCenter).fma(radius, basis.tangent())
                        .fma(-radius, basis.bitangent());
                Vector3d p11 = new Vector3d(faceCenter).fma(radius, basis.tangent())
                        .fma(radius, basis.bitangent());
                Vector3d p01 = new Vector3d(faceCenter).fma(-radius, basis.tangent())
                        .fma(radius, basis.bitangent());
                quad(builder, matrix, p00, p10, p11, p01, color);
            }
        }
    }

    private static void drawGlow(BufferBuilder builder, boolean bloom) {
        MeshData mesh = builder.build();
        if (mesh == null) {
            return;
        }

        // Draw into the MAIN framebuffer, not Veil's bloom target. That target
        // shares no depth buffer, so it always composited the glow over the
        // finished frame — the sun bleeding through blocks and terrain. Here the
        // depth test occludes it against whatever is actually in front.
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            BufferUploader.drawWithShader(mesh);
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static FaceBasis faceBasis(int face) {
        return switch (face) {
            case 0 -> new FaceBasis(new Vector3d(1, 0, 0), new Vector3d(0, 1, 0), new Vector3d(0, 0, 1));
            case 1 -> new FaceBasis(new Vector3d(-1, 0, 0), new Vector3d(0, 1, 0), new Vector3d(0, 0, -1));
            case 2 -> new FaceBasis(new Vector3d(0, 1, 0), new Vector3d(1, 0, 0), new Vector3d(0, 0, -1));
            case 3 -> new FaceBasis(new Vector3d(0, -1, 0), new Vector3d(1, 0, 0), new Vector3d(0, 0, 1));
            case 4 -> new FaceBasis(new Vector3d(0, 0, 1), new Vector3d(1, 0, 0), new Vector3d(0, 1, 0));
            default -> new FaceBasis(new Vector3d(0, 0, -1), new Vector3d(-1, 0, 0), new Vector3d(0, 1, 0));
        };
    }

    private static void addSunCube(Matrix4f matrix, VertexConsumer buffer, float h) {
        addSunFace(matrix, buffer, h,
                -h, -h, h, h, -h, h, h, h, h, -h, h, h);
        addSunFace(matrix, buffer, h,
                -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -h);
        addSunFace(matrix, buffer, h,
                -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h);
        addSunFace(matrix, buffer, h,
                h, -h, -h, h, h, -h, h, h, h, h, -h, h);
        addSunFace(matrix, buffer, h,
                -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h);
        addSunFace(matrix, buffer, h,
                -h, h, -h, -h, h, h, h, h, h, h, h, -h);
    }

    private static void addSunFace(Matrix4f matrix, VertexConsumer buffer, float h,
                                   float x1, float y1, float z1, float x2, float y2, float z2,
                                   float x3, float y3, float z3, float x4, float y4, float z4) {
        sunVertex(matrix, buffer, h, x1, y1, z1);
        sunVertex(matrix, buffer, h, x2, y2, z2);
        sunVertex(matrix, buffer, h, x3, y3, z3);
        sunVertex(matrix, buffer, h, x4, y4, z4);
    }

    private static void sunVertex(Matrix4f matrix, VertexConsumer buffer, float h,
                                  float x, float y, float z) {
        float size = h * 2.0f;
        buffer.addVertex(matrix, x, y, z).setColor(
                (int) (255.0f * (x + h) / size),
                (int) (255.0f * (y + h) / size),
                (int) (255.0f * (z + h) / size), 255);
    }

    private static void quad(BufferBuilder builder, Matrix4f matrix,
                             Vector3d a, Vector3d b, Vector3d c, Vector3d d, int[] color) {
        quad(builder, matrix, a, b, c, d, color, color, color, color);
    }

    private static void quad(BufferBuilder builder, Matrix4f matrix,
                             Vector3d a, Vector3d b, Vector3d c, Vector3d d,
                             int[] ca, int[] cb, int[] cc, int[] cd) {
        vertex(builder, matrix, a, ca);
        vertex(builder, matrix, b, cb);
        vertex(builder, matrix, c, cc);
        vertex(builder, matrix, d, cd);
    }

    private static void vertex(BufferBuilder builder, Matrix4f matrix, Vector3d point, int[] color) {
        builder.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setColor(color[0], color[1], color[2], color[3]);
    }

    private static int[] rgba(int r, int g, int b, double alpha) {
        return new int[]{r, g, b, (int) Mth.clamp(alpha, 0.0, 255.0)};
    }

}
