package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsContraptionLookup;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.properties.PlanetProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The re-entry burn-up as a <b>real rendered plasma envelope</b> — not particles — for Sable physics
 * objects only.
 *
 * <p>When a construct tears through atmosphere fast enough, an additive glowing shock shell is drawn
 * hugging its leading face: a white-gold stagnation cap bleeding through orange to deep-red at the
 * shoulders and streaming into a tapered plasma wake behind the hull (the look in the reference image).
 * Because the client never sees a construct's velocity, speed is derived from its own motion each tick;
 * heat then ramps with speed and air density and builds/decays smoothly, exactly like the server model.
 * Players, mobs and loose entities are never considered — only Sable sub-levels.</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID)
public final class ReentryPlasmaRenderer {

    private static final double MIN_BURN_SPEED = 15.0;   // blocks/s where heating begins
    private static final double MAX_BURN_SPEED = 65.0;   // blocks/s of maximum plasma
    private static final double HEAT_LERP = 0.10;        // smoothing toward target heat

    private static final class Track {
        Vector3d lastPos;
        Vector3d velocity = new Vector3d();
        double heat = 0.0;
        int missingTicks = 0;
    }

    private static final Map<UUID, Track> TRACKS = new HashMap<>();

    private ReentryPlasmaRenderer() {}

    /** Derive each construct's speed from its motion and update its (smoothly ramping) heat. */
    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ClientLevel level)) return;

        double atmosphereDensity = 0.0;
        Celestial body = GenesisMod.getCelestialForLevel(level);
        if (body != null && body.properties() instanceof PlanetProperties pp && pp.atmosphere() != null) {
            atmosphereDensity = Mth.clamp(pp.atmosphere().density(), 0.0, 1.0);
        }
        int entryHeight = GenesisCommonConfig.getAtmosphereEntryHeight();
        int exitHeight = GenesisCommonConfig.getAtmosphereExitHeight();

        for (Track t : TRACKS.values()) t.missingTicks++;

        for (AeronauticsConstruct construct : AeronauticsContraptionLookup.getSortedConstructs(level)) {
            if (construct.isRemoved()) continue;
            Vector3dc p = construct.positionInWorld();
            Vector3d pos = new Vector3d(p.x(), p.y(), p.z());
            Track track = TRACKS.computeIfAbsent(construct.id(), k -> new Track());
            track.missingTicks = 0;

            if (track.lastPos != null) {
                track.velocity = new Vector3d(pos).sub(track.lastPos).mul(20.0); // per-tick delta -> blocks/s
            }
            track.lastPos = pos;

            double speed = track.velocity.length();
            double airDensity = atmosphereDensity * Mth.clamp(
                    1.0 - (pos.y - entryHeight) / (double) (exitHeight - entryHeight), 0.0, 1.0);
            double target = Mth.clamp((speed - MIN_BURN_SPEED) / (MAX_BURN_SPEED - MIN_BURN_SPEED), 0.0, 1.0) * airDensity;
            track.heat += (target - track.heat) * HEAT_LERP;
        }

        TRACKS.entrySet().removeIf(e -> e.getValue().missingTicks > 40);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || TRACKS.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        if (pose == null) return;
        Vec3 cam = event.getCamera().getPosition();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE); // additive
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        for (AeronauticsConstruct construct : AeronauticsContraptionLookup.getSortedConstructs(level)) {
            Track track = TRACKS.get(construct.id());
            if (track == null || track.heat < 0.04) continue;
            double speed = track.velocity.length();
            if (speed < 1.0e-3) continue;
            drawShell(pose, cam, construct, track.velocity, speed, track.heat);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void drawShell(PoseStack poseStack, Vec3 cam, AeronauticsConstruct construct,
                                  Vector3d velocity, double speed, double heat) {
        var bounds = construct.worldBounds();
        Vector3d center = new Vector3d(
                (bounds.minX() + bounds.maxX()) * 0.5,
                (bounds.minY() + bounds.maxY()) * 0.5,
                (bounds.minZ() + bounds.maxZ()) * 0.5);
        double hx = (bounds.maxX() - bounds.minX()) * 0.5;
        double hy = (bounds.maxY() - bounds.minY()) * 0.5;
        double hz = (bounds.maxZ() - bounds.minZ()) * 0.5;

        Vector3d dir = new Vector3d(velocity).div(speed);
        double alongExtent = hx * Math.abs(dir.x) + hy * Math.abs(dir.y) + hz * Math.abs(dir.z);
        double faceR = Math.max(0.6, (hx + hy + hz - alongExtent) * 0.5) * (0.9 + 0.5 * heat);

        // Basis across the leading face.
        Vector3d up = Math.abs(dir.y) > 0.99 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(dir).cross(up).normalize();
        Vector3d across = new Vector3d(right).cross(dir).normalize();

        // Envelope runs from a rounded cap just ahead of the hull back into a tapered wake.
        double standoff = 0.4 + 0.7 * heat;
        Vector3d tip = new Vector3d(dir).mul(alongExtent + standoff).add(center);
        double capLen = 2.0 * alongExtent + faceR + (3.0 + 8.0 * heat); // wake grows with heat

        poseStack.pushPose();
        poseStack.translate(tip.x - cam.x, tip.y - cam.y, tip.z - cam.z);
        Matrix4f mat = poseStack.last().pose();

        // Two additive passes: a broad soft halo and a tighter bright core.
        drawEnvelope(mat, dir, right, across, faceR * 1.35, capLen * 1.15, heat, 0.35f);
        drawEnvelope(mat, dir, right, across, faceR, capLen, heat, 0.9f);

        poseStack.popPose();
    }

    private static void drawEnvelope(Matrix4f mat, Vector3d dir, Vector3d right, Vector3d across,
                                     double faceR, double capLen, double heat, float alphaScale) {
        final int rings = 12, seg = 18;
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < rings; i++) {
            double t0 = i / (double) rings;
            double t1 = (i + 1) / (double) rings;
            for (int s = 0; s <= seg; s++) {
                double a = s / (double) seg * Math.PI * 2.0;
                ringVertex(buf, mat, dir, right, across, faceR, capLen, t1, a, heat, alphaScale);
                ringVertex(buf, mat, dir, right, across, faceR, capLen, t0, a, heat, alphaScale);
            }
        }
        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    private static void ringVertex(BufferBuilder buf, Matrix4f mat, Vector3d dir, Vector3d right, Vector3d across,
                                   double faceR, double capLen, double t, double angle, double heat, float alphaScale) {
        // Teardrop profile: rounded bright front (t=0) swelling to faceR then tapering into the wake (t=1).
        double radius = faceR * Math.pow(Math.sin(Math.PI * Math.min(1.0, t * 0.92 + 0.08)), 0.7);
        double back = capLen * t;
        double cos = Math.cos(angle) * radius, sin = Math.sin(angle) * radius;
        double px = -dir.x * back + right.x * cos + across.x * sin;
        double py = -dir.y * back + right.y * cos + across.y * sin;
        double pz = -dir.z * back + right.z * cos + across.z * sin;

        // Colour: white-gold stagnation front -> orange -> deep red down the wake.
        float r, g, b;
        if (t < 0.35) {
            float k = (float) (t / 0.35);
            r = 1.0f; g = lerp(0.95f, 0.55f, k); b = lerp(0.80f, 0.18f, k);
        } else {
            float k = (float) ((t - 0.35) / 0.65);
            r = lerp(1.0f, 0.80f, k); g = lerp(0.55f, 0.12f, k); b = lerp(0.18f, 0.03f, k);
        }
        // Brighten toward white at peak heat; fade alpha along the wake.
        float white = (float) (0.25 * heat);
        r += (1f - r) * white; g += (1f - g) * white; b += (1f - b) * white;
        float alpha = (float) (heat * alphaScale * Math.pow(1.0 - t, 1.3));
        buf.addVertex(mat, (float) px, (float) py, (float) pz).setColor(r, g, b, alpha);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
