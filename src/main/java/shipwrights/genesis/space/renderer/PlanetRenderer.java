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
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
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
import shipwrights.genesis.mixin.FogRendererAccessor;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.properties.PlanetProperties;
import team.lodestar.lodestone.systems.rendering.shader.ExtendedShaderInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Renders a planet (Earth, Moon, …) with VS Genesis' original {@code planet_textured} shader: a
 * six-faced cube whose faces sample a 3×2 surface net, directionally lit from the nearest star, with
 * an additive atmosphere rim. Ported from the 1.20 Lodestone renderer; the per-vertex normal carries
 * the cube-local corner so the shader lights and shades it as a rounded body.
 *
 * <p>Falls back to the vanilla-pipeline textured billboard if the shader isn't available.</p>
 */
public class PlanetRenderer implements CelestialRenderer {

    private static final double SKY_RADIUS = 90.0;
    private static final Map<ResourceLocation, Optional<ResourceLocation>> TEXTURE_CACHE = new HashMap<>();

    private final SimpleBillboardCelestialRenderer fallback;

    public PlanetRenderer(float fr, float fg, float fb, float fa) {
        this.fallback = new SimpleBillboardCelestialRenderer(fr, fg, fb, fa);
    }

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @NotNull VantagePoint vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor) event.getLevelRenderer()).getLevel();
        if (level == null) return;
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        ResourceLocation id = registry.getKey(toRender);

        ExtendedShaderInstance shader = GenesisClientConfig.useProceduralShaders()
                ? ShaderRegistry.instance(ShaderRegistry.PLANET_TEXTURED) : null;
        Optional<ResourceLocation> texture = id != null ? textureFor(id) : Optional.empty();
        if (shader == null || texture.isEmpty()) {
            fallback.invoke(event, toRender, vantagePoint);
            return;
        }

        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);

        Vector3dc bodyPos = toRender.getPosition(ticks, partialTick, registry);
        Vector3dc vantagePos = vantagePoint.getPosition();
        Vector3d dir = new Vector3d(bodyPos).sub(vantagePos);
        double dist = dir.length();
        double bodyRadius = toRender.getActualSize() * 0.5;
        if (dist < Math.max(1.0e-3, bodyRadius * 0.5)) return;
        dir.div(dist);

        Quaterniond viewRot = new Quaterniond(vantagePoint.getRotation()).conjugate();
        dir.rotate(viewRot);

        double angularRadius = Math.min(Math.atan2(bodyRadius, dist), 1.2);
        float half = (float) Math.max(0.75, Math.tan(angularRadius) * SKY_RADIUS);
        Vector3d center = new Vector3d(dir).mul(SKY_RADIUS);

        Quaternionf cubeRot = new Quaternionf(new Quaterniond(viewRot).mul(new Quaterniond(toRender.getRotation(ticks, partialTick, registry))));

        // Light direction: from the nearest star toward this body, in the view frame.
        Celestial star = toRender.getNearestStar(ticks, partialTick, registry);
        Vector3d lightDir = new Vector3d(bodyPos).sub(star.getPosition(ticks, partialTick, registry));
        if (lightDir.lengthSquared() < 1.0e-9) lightDir.set(0, -1, 0);
        lightDir.normalize().rotate(viewRot);

        Vector3d skyColor = vantagePoint instanceof VantagePoint.OnCelestial
                ? new Vector3d(PlanetDimensionEffects.cachedSkyColor.x, PlanetDimensionEffects.cachedSkyColor.y, PlanetDimensionEffects.cachedSkyColor.z)
                : new Vector3d(0, 0, 0);

        setF(shader, "HalfSize", half);
        set3(shader, "LightDirection", (float) lightDir.x, (float) lightDir.y, (float) lightDir.z);
        set3(shader, "SkyColor", (float) skyColor.x, (float) skyColor.y, (float) skyColor.z);

        Matrix4f pose = new Matrix4f(event.getModelViewMatrix());

        // --- atmosphere rim behind the disc (additive) ---
        if (toRender.properties() instanceof PlanetProperties pp && pp.atmosphere() != null && pp.atmosphere().density() > 0) {
            fallback.drawAtmosphereRim(pose, center, cubeRot, half, pp);
        }

        // --- the planet cube (opaque, textured, lit) ---
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, texture.get());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        float fr = FogRendererAccessor.getFogRed(), fg = FogRendererAccessor.getFogGreen(), fb = FogRendererAccessor.getFogBlue();
        float alpha = vantagePoint instanceof VantagePoint.OnCelestial ? planetAlpha(level) : 1f;

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
        float t = 1f / 3f, tt = 2f / 3f;
        // face corners (unit cube) + net UV rect (u1,v1,u2,v2)
        face(buf, pose, center, cubeRot, half, fr, fg, fb, alpha, lightDir, // +Z south
                -1,-1, 1,  1,-1, 1,  1, 1, 1, -1, 1, 1,  tt,0f, 1f,.5f);
        face(buf, pose, center, cubeRot, half, fr, fg, fb, alpha, lightDir, //  1,-1,-1 north -Z
                 1,-1,-1, -1,-1,-1, -1, 1,-1,  1, 1,-1,  0f,0f, t,.5f);
        face(buf, pose, center, cubeRot, half, fr, fg, fb, alpha, lightDir, // -X west
                -1,-1,-1, -1,-1, 1, -1, 1, 1, -1, 1,-1,  t,0f, tt,.5f);
        face(buf, pose, center, cubeRot, half, fr, fg, fb, alpha, lightDir, // +X east
                 1,-1, 1,  1,-1,-1,  1, 1,-1,  1, 1, 1,  0f,.5f, t,1f);
        face(buf, pose, center, cubeRot, half, fr, fg, fb, alpha, lightDir, // -Y down
                -1,-1,-1,  1,-1,-1,  1,-1, 1, -1,-1, 1,  t,.5f, tt,1f);
        face(buf, pose, center, cubeRot, half, fr, fg, fb, alpha, lightDir, // +Y up
                -1, 1, 1,  1, 1, 1,  1, 1,-1, -1, 1,-1,  tt,.5f, 1f,1f);
        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static float planetAlpha(ClientLevel level) {
        // When standing on the planet, fade the body in as the camera climbs (matches sky fade).
        double camY = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y;
        return (float) Math.max(0.0, Math.min(1.0, (camY - 320.0) / 512.0));
    }

    /** Emits one textured cube face (4 verts) with net UVs; per-vertex normal = rotated local corner. */
    private static void face(BufferBuilder buf, Matrix4f pose, Vector3d center, Quaternionf rot, float half,
                             float fr, float fg, float fb, float alpha, Vector3d ignored,
                             int ax, int ay, int az, int bx, int by, int bz,
                             int cx, int cy, int cz, int dx, int dy, int dz,
                             float u1, float v1, float u2, float v2) {
        vertex(buf, pose, center, rot, half, ax, ay, az, u1, v2, fr, fg, fb, alpha);
        vertex(buf, pose, center, rot, half, bx, by, bz, u2, v2, fr, fg, fb, alpha);
        vertex(buf, pose, center, rot, half, cx, cy, cz, u2, v1, fr, fg, fb, alpha);
        vertex(buf, pose, center, rot, half, dx, dy, dz, u1, v1, fr, fg, fb, alpha);
    }

    private static void vertex(BufferBuilder buf, Matrix4f pose, Vector3d center, Quaternionf rot, float half,
                               int lx, int ly, int lz, float u, float v,
                               float fr, float fg, float fb, float alpha) {
        Vector3f local = new Vector3f(lx * half, ly * half, lz * half);
        Vector3f placed = rot.transform(new Vector3f(local));
        Vector3f n = rot.transform(new Vector3f((float) lx, (float) ly, (float) lz).normalize());
        buf.addVertex(pose, (float) center.x + placed.x, (float) center.y + placed.y, (float) center.z + placed.z)
                .setUv(u, v)
                .setColor(fr, fg, fb, alpha)
                .setNormal(n.x, n.y, n.z);
    }

    private static Optional<ResourceLocation> textureFor(ResourceLocation id) {
        return TEXTURE_CACHE.computeIfAbsent(id, key -> {
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID,
                    "textures/planets/" + key.getNamespace() + "/" + key.getPath() + ".png");
            return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(tex).isPresent()
                    ? Optional.of(tex) : Optional.empty();
        });
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
