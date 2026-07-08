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

    private static final double MIN_BURN_SPEED = 45.0;   // blocks/s where heating begins (hypersonic only)
    private static final double MAX_BURN_SPEED = 140.0;  // blocks/s of maximum plasma
    private static final double HEAT_LERP = 0.12;        // smoothing toward target heat

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
            // Re-entry heating only lives in the upper-atmosphere band: nothing down at ground level
            // (dense but slow — no plasma, and it must never light up while driving around), full through
            // the entry→exit band, tapering to nothing in the vacuum above. This is what keeps it from
            // triggering on the ground.
            double band;
            if (pos.y < entryHeight) {
                band = 0.0;
            } else if (pos.y <= exitHeight) {
                band = 1.0;
            } else {
                band = Mth.clamp(1.0 - (pos.y - exitHeight) / 1000.0, 0.0, 1.0); // thin exosphere, then vacuum
            }
            double target = Mth.clamp((speed - MIN_BURN_SPEED) / (MAX_BURN_SPEED - MIN_BURN_SPEED), 0.0, 1.0)
                    * band * atmosphereDensity;
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

        poseStack.pushPose();
        poseStack.translate(center.x - cam.x, center.y - cam.y, center.z - cam.z);
        Matrix4f mat = poseStack.last().pose();

        // The sheath is an ellipsoid matching the construct's own proportions, so it wraps the actual
        // hull (a long ship gets a long sheath, a flat one a flat sheath) rather than a fixed teardrop.
        // Three additive passes — a broad outer halo, the main plasma layer and a searing thin core —
        // stack up into a bright, hot sheath; brightness is concentrated on the windward side with a
        // tail streaming off the back.
        drawSheath(mat, dir, hx, hy, hz, heat, 0.8 + 1.6 * heat, 0.30f, 4.5 + 9.0 * heat);
        drawSheath(mat, dir, hx, hy, hz, heat, 0.4 + 0.8 * heat, 0.80f, 3.0 + 6.0 * heat);
        drawSheath(mat, dir, hx, hy, hz, heat, 0.15 + 0.4 * heat, 1.0f, 1.8 + 3.5 * heat);

        poseStack.popPose();
    }

    /**
     * Emits one additive ellipsoidal plasma layer conforming to (hx,hy,hz) + {@code margin}, oriented so
     * the bright plasma piles up on the windward side (into {@code dir}) and streams into a wake of
     * length {@code wakeLen} behind. {@code alphaScale} sets the layer's punch.
     */
    private static void drawSheath(Matrix4f mat, Vector3d dir, double hx, double hy, double hz,
                                   double heat, double margin, float alphaScale, double wakeLen) {
        final int rings = 16, seg = 24;
        double ax = hx + margin, ay = hy + margin, az = hz + margin;
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < rings; i++) {
            for (int s = 0; s < seg; s++) {
                sheathVertex(buf, mat, dir, ax, ay, az, i, s, rings, seg, heat, alphaScale, wakeLen);
                sheathVertex(buf, mat, dir, ax, ay, az, i + 1, s, rings, seg, heat, alphaScale, wakeLen);
                sheathVertex(buf, mat, dir, ax, ay, az, i + 1, s + 1, rings, seg, heat, alphaScale, wakeLen);
                sheathVertex(buf, mat, dir, ax, ay, az, i, s, rings, seg, heat, alphaScale, wakeLen);
                sheathVertex(buf, mat, dir, ax, ay, az, i + 1, s + 1, rings, seg, heat, alphaScale, wakeLen);
                sheathVertex(buf, mat, dir, ax, ay, az, i, s + 1, rings, seg, heat, alphaScale, wakeLen);
            }
        }
        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    private static void sheathVertex(BufferBuilder buf, Matrix4f mat, Vector3d dir,
                                     double ax, double ay, double az, int i, int s, int rings, int seg,
                                     double heat, float alphaScale, double wakeLen) {
        double phi = Math.PI * i / rings;          // 0..π latitude
        double theta = 2.0 * Math.PI * s / seg;    // longitude
        double ux = Math.sin(phi) * Math.cos(theta);
        double uy = Math.cos(phi);
        double uz = Math.sin(phi) * Math.sin(theta);

        // Push the sphere direction toward the unit *cube* so the sheath is a rounded box that hugs the
        // actual hull shape — a single block gets a rounded-cube sheath, a long ship a rounded slab —
        // instead of a generic ellipsoid. ROUND=0 is a hard box, 1 a sphere.
        final double ROUND = 0.30;
        double m = Math.max(Math.abs(ux), Math.max(Math.abs(uy), Math.abs(uz)));
        double bx = ux / m, by = uy / m, bz = uz / m;           // on the unit cube face
        double sx = bx + (ux - bx) * ROUND;                     // rounded-box direction
        double sy = by + (uy - by) * ROUND;
        double sz = bz + (uz - bz) * ROUND;

        double px = sx * ax, py = sy * ay, pz = sz * az;
        // Outward normal (rounded-box direction) -> how much this point faces into the airflow.
        double nlen = Math.sqrt(sx * sx + sy * sy + sz * sz);
        double windward = (sx * dir.x + sy * dir.y + sz * dir.z) / Math.max(1.0e-6, nlen); // -1..1
        double front = Math.max(0.0, windward);
        double back = Math.max(0.0, -windward);

        // Leeward side stretches downstream into a tapering plasma wake.
        double stretch = wakeLen * back * back;
        px -= dir.x * stretch; py -= dir.y * stretch; pz -= dir.z * stretch;

        // Colour: white-gold where it slams the air, through orange, to deep red on the flanks/wake.
        float c = (float) Math.pow(front, 0.7);
        float r = 1.0f;
        float g = lerp(0.22f, 0.97f, c);
        float b = lerp(0.04f, 0.80f, (float) Math.pow(front, 1.5));
        float white = (float) (0.35 * heat * front);
        r += (1f - r) * white; g += (1f - g) * white; b += (1f - b) * white;

        // Bright windward cap, plus a dimmer glow all around and down the wake so the whole hull reads hot.
        float a = (float) (heat * alphaScale * (0.14 + 0.95 * Math.pow(front, 1.25) + 0.30 * Math.pow(back, 0.6)));
        buf.addVertex(mat, (float) px, (float) py, (float) pz).setColor(r, g, b, Math.min(1.0f, a));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
