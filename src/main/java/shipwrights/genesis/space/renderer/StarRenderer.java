package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetDimensionEffects;
import shipwrights.genesis.client.ShaderRegistry;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.properties.StarProperties;
import team.lodestar.lodestone.systems.rendering.shader.ExtendedShaderInstance;

/**
 * Renders a star (the Sun) with VS Genesis' original {@code sun} shader: a volumetric rounded
 * cube ray-marched in the fragment shader, tinted between the star's two colours. Ported from the
 * 1.20 Lodestone renderer — the cube corners carry their normalised local position in the vertex
 * colour (the shader decodes it), and the camera position is supplied in the cube's local frame.
 *
 * <p>Falls back to the vanilla-pipeline billboard glow if the shader isn't available.</p>
 */
public class StarRenderer implements CelestialRenderer {

    private static final double SKY_RADIUS = 90.0;
    private final SimpleBillboardCelestialRenderer fallback =
            new SimpleBillboardCelestialRenderer(1.0f, 0.95f, 0.7f, 1.0f);

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @NotNull VantagePoint vantagePoint) {
        ExtendedShaderInstance shader = GenesisClientConfig.useProceduralShaders()
                ? ShaderRegistry.instance(ShaderRegistry.SUN) : null;
        if (shader == null) {
            fallback.invoke(event, toRender, vantagePoint);
            return;
        }

        ClientLevel level = ((LevelRendererAccessor) event.getLevelRenderer()).getLevel();
        if (level == null) return;
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        var registry = GenesisMod.getCelestialRegistry(level);

        Vector3dc bodyPos = toRender.getPosition(ticks, partialTick, registry);
        Vector3dc vantagePos = vantagePoint.getPosition();
        Vector3d dir = new Vector3d(bodyPos).sub(vantagePos);
        double dist = dir.length();
        if (dist < 1.0e-6) return;
        dir.div(dist);

        // Into the observer's sky frame.
        Quaterniond viewRot = new Quaterniond(vantagePoint.getRotation()).conjugate();
        dir.rotate(viewRot);

        double bodyRadius = toRender.getActualSize() * 0.5;
        double angularRadius = Math.min(Math.atan2(bodyRadius, dist), 1.2);
        float half = (float) Math.max(0.75, Math.tan(angularRadius) * SKY_RADIUS);
        Vector3d center = new Vector3d(dir).mul(SKY_RADIUS);

        float opacity = 1f;
        if (vantagePoint instanceof VantagePoint.OnCelestial) {
            double d2 = vantagePos.distanceSquared(bodyPos);
            opacity = (float) Math.min(1.0, PlanetDimensionEffects.cachedStarBrightness
                    + 20_000.0 * 20_000.0 / Math.max(d2, 1.0e-6));
            if (opacity < 0.01f) return;
        }

        StarProperties sp = toRender.properties() instanceof StarProperties s ? s : null;

        // Cube orientation in the view frame = celestial spin folded into the view rotation.
        Quaternionf cubeRot = new Quaternionf(new Quaterniond(viewRot).mul(new Quaterniond(toRender.getRotation(ticks, partialTick, registry))));
        Quaternionf invCubeRot = new Quaternionf(cubeRot).conjugate();

        // Camera (view-space origin) expressed in the cube's local frame.
        Vector3f camLocal = invCubeRot.transform(new Vector3f((float) -center.x, (float) -center.y, (float) -center.z));

        setF(shader, "HalfSize", half);
        setF(shader, "Opacity", opacity);
        set3(shader, "CameraPosition", camLocal.x, camLocal.y, camLocal.z);
        if (sp != null) {
            set3(shader, "Color0", sp.r0() / 255f, sp.g0() / 255f, sp.b0() / 255f);
            set3(shader, "Color1", sp.r1() / 255f, sp.g1() / 255f, sp.b1() / 255f);
        } else {
            set3(shader, "Color0", 1f, 0f, 0f);
            set3(shader, "Color1", 1f, 0.8f, 0.15f);
        }

        Matrix4f pose = new Matrix4f(event.getModelViewMatrix());

        RenderSystem.setShader(() -> shader);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        CelestialCube.emit(buf, pose, center, cubeRot, half, (b, p, px, py, pz, cr, cg, cb) ->
                b.addVertex(p, px, py, pz).setColor(cr, cg, cb, 1f));
        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void setF(ExtendedShaderInstance s, String name, float v) {
        Uniform u = s.getUniform(name);
        if (u != null) u.set(v);
    }

    private static void set3(ExtendedShaderInstance s, String name, float x, float y, float z) {
        Uniform u = s.getUniform(name);
        if (u != null) u.set(x, y, z);
    }
}
