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
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import java.util.List;
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

    /** Cached voxel silhouette (exposed faces) per construct, so we don't re-scan blocks every frame. */
    private static final Map<UUID, List<float[]>> SHAPES = new HashMap<>();
    private static final Map<UUID, Long> SHAPE_TIME = new HashMap<>();
    private static final int MAX_FACES = 700;

    private ReentryPlasmaRenderer() {}

    private static List<float[]> facesFor(AeronauticsConstruct construct, ClientLevel level) {
        long now = level.getGameTime();
        Long t = SHAPE_TIME.get(construct.id());
        List<float[]> cached = SHAPES.get(construct.id());
        if (cached != null && t != null && now - t < 40) return cached; // refresh every 2s
        List<float[]> faces = construct.exposedFaces(MAX_FACES);
        SHAPES.put(construct.id(), faces);
        SHAPE_TIME.put(construct.id(), now);
        return faces;
    }

    /** Derive each construct's speed from its motion and update its (smoothly ramping) heat. */
    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ClientLevel level)) return;
        try {
            tickHeat(level);
        } catch (Throwable ignored) {
            // Never let re-entry bookkeeping crash the client (e.g. registries mid-dimension-change).
        }
    }

    private static void tickHeat(ClientLevel level) {
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
        SHAPES.keySet().removeIf(id -> !TRACKS.containsKey(id));
        SHAPE_TIME.keySet().removeIf(id -> !TRACKS.containsKey(id));
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

        try {
            for (AeronauticsConstruct construct : AeronauticsContraptionLookup.getSortedConstructs(level)) {
                Track track = TRACKS.get(construct.id());
                if (track == null || track.heat < 0.04) continue;
                double speed = track.velocity.length();
                if (speed < 1.0e-3) continue;
                drawShell(pose, cam, construct, level, track.velocity, speed, track.heat);
            }
        } catch (Throwable ignored) {
            // A single bad frame must not crash the game.
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void drawShell(PoseStack poseStack, Vec3 cam, AeronauticsConstruct construct,
                                  ClientLevel level, Vector3d velocity, double speed, double heat) {
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

        List<float[]> faces = facesFor(construct, level);
        if (faces != null && !faces.isEmpty()) {
            // Plasma glued to the construct's ACTUAL voxel silhouette: a glowing skin on every block
            // face that meets the airflow, streaming into the wake — so it takes the real shape of the
            // craft (a wing burns along its wing, a cube along its cube), not a generic hull.
            var r = construct.rotation();
            Quaternionf rot = new Quaternionf((float) r.x(), (float) r.y(), (float) r.z(), (float) r.w());
            drawVoxelPlasma(mat, faces, rot, dir, heat);
        } else {
            // Fallback (shape not readable): the proportional rounded-box sheath.
            drawSheath(mat, dir, hx, hy, hz, heat, 0.8 + 1.6 * heat, 0.30f, 9.0 + 20.0 * heat);
            drawSheath(mat, dir, hx, hy, hz, heat, 0.4 + 0.8 * heat, 0.80f, 6.0 + 13.0 * heat);
            drawSheath(mat, dir, hx, hy, hz, heat, 0.15 + 0.4 * heat, 1.0f, 3.0 + 7.0 * heat);
        }

        poseStack.popPose();
    }

    /** Draws additive plasma on the windward faces of the construct's real voxel shape, plus wake streaks. */
    private static void drawVoxelPlasma(Matrix4f mat, List<float[]> faces, Quaternionf rot, Vector3d dir, double heat) {
        float dx = (float) dir.x, dy = (float) dir.y, dz = (float) dir.z;
        float standoff = (float) (0.15 + 0.5 * heat);
        float wakeLen = (float) (4.0 + 12.0 * heat);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Vector3f o = new Vector3f(), n = new Vector3f(), t1 = new Vector3f(), t2 = new Vector3f();
        for (float[] f : faces) {
            rot.transform(f[0], f[1], f[2], o);
            rot.transform(f[3], f[4], f[5], n);
            float wind = n.x * dx + n.y * dy + n.z * dz;   // -1..1, how much the face meets the airflow
            if (wind <= 0.05f) continue;                   // only leading faces glow

            Vector3f up = Math.abs(n.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            n.cross(up, t1); t1.normalize();
            n.cross(t1, t2); t2.normalize();

            float cx = o.x + n.x * standoff, cy = o.y + n.y * standoff, cz = o.z + n.z * standoff;

            float wl = Math.min(1f, wind);
            // white-gold where it slams the air, pink-magenta on the shallower faces
            float r = 1f, g = lerp(0.45f, 0.95f, wl), b = lerp(0.85f, 0.62f, wl);
            float white = (float) Math.min(1.0, heat * 0.55 * wl);
            r += (1f - r) * white; g += (1f - g) * white; b += (1f - b) * white;
            float a = (float) Math.min(1.0, heat * (0.25 + 0.85 * wl));

            float s = 0.75f; // overlap neighbouring faces so the skin reads smooth
            quad(buf, mat, cx, cy, cz, t1, t2, s, r, g, b, a);

            if (wind > 0.35f) {   // strong leading faces trail a streak
                streak(buf, mat, cx, cy, cz, dx, dy, dz, t1, wakeLen * wind, heat, wl);
            }
        }
        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    private static void quad(BufferBuilder buf, Matrix4f mat, float cx, float cy, float cz,
                             Vector3f t1, Vector3f t2, float s, float r, float g, float b, float a) {
        v(buf, mat, cx - t1.x * s - t2.x * s, cy - t1.y * s - t2.y * s, cz - t1.z * s - t2.z * s, r, g, b, a);
        v(buf, mat, cx + t1.x * s - t2.x * s, cy + t1.y * s - t2.y * s, cz + t1.z * s - t2.z * s, r, g, b, a);
        v(buf, mat, cx + t1.x * s + t2.x * s, cy + t1.y * s + t2.y * s, cz + t1.z * s + t2.z * s, r, g, b, a);
        v(buf, mat, cx - t1.x * s + t2.x * s, cy - t1.y * s + t2.y * s, cz - t1.z * s + t2.z * s, r, g, b, a);
    }

    /** A tapering plasma streak trailing a leading face down the wake, cooling white-gold → magenta → out. */
    private static void streak(BufferBuilder buf, Matrix4f mat, float cx, float cy, float cz,
                               float dx, float dy, float dz, Vector3f t1, float len, double heat, float wl) {
        float w = 0.6f;
        float ex = cx - dx * len, ey = cy - dy * len, ez = cz - dz * len;
        float r0 = 1f, g0 = lerp(0.55f, 0.95f, wl), b0 = lerp(0.85f, 0.7f, wl);
        float aNear = (float) Math.min(1.0, heat * 0.7 * wl);
        // near (bright white-pink) -> far (transparent magenta)
        v(buf, mat, cx - t1.x * w, cy - t1.y * w, cz - t1.z * w, r0, g0, b0, aNear);
        v(buf, mat, cx + t1.x * w, cy + t1.y * w, cz + t1.z * w, r0, g0, b0, aNear);
        v(buf, mat, ex + t1.x * w * 0.3f, ey + t1.y * w * 0.3f, ez + t1.z * w * 0.3f, 1f, 0.4f, 0.9f, 0f);
        v(buf, mat, ex - t1.x * w * 0.3f, ey - t1.y * w * 0.3f, ez - t1.z * w * 0.3f, 1f, 0.4f, 0.9f, 0f);
    }

    private static void v(BufferBuilder buf, Matrix4f mat, float x, float y, float z, float r, float g, float b, float a) {
        buf.addVertex(mat, x, y, z).setColor(r, g, b, Math.min(1f, a));
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

        // Colour to match real hypersonic plasma (the reference photo): a searing white-gold cap where
        // the hull slams the air, bleeding into pink → magenta → violet plasma that streams back down
        // the wake, with brighter white streak highlights.
        float fr = (float) front;
        float bk = (float) back;
        float r = 1.0f;
        float g = lerp(0.45f, 0.93f, (float) Math.pow(fr, 0.7));   // magenta-pink flanks -> gold cap
        float b = lerp(0.90f, 0.62f, (float) Math.pow(fr, 1.3));   // violet/magenta flanks -> warm cap
        // White-hot at the stagnation cap, and a bright white-pink core streaming down the wake (the
        // trails read white-pink, not pure magenta, in the reference).
        float white = (float) Math.min(1.0, heat * (0.60 * fr + 0.55 * bk));
        r += (1f - r) * white; g += (1f - g) * white; b += (1f - b) * white;

        // Bright windward cap, plus streaking plasma down the wake so the craft is engulfed and trailing.
        float a = (float) (heat * alphaScale * (0.12 + 0.95 * Math.pow(fr, 1.25) + 0.45 * Math.pow(bk, 0.5)));
        buf.addVertex(mat, (float) px, (float) py, (float) pz).setColor(r, g, b, Math.min(1.0f, a));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
