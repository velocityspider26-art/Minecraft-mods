package com.vantage.client;

import com.vantage.Vantage;
import com.vantage.api.VantageApi;
import com.vantage.client.net.ClientTerrain;
import com.vantage.net.PlanetInfo;
import com.vantage.net.VantageNetwork;
import com.vantage.client.gen.DistantGenerator;
import com.vantage.client.ingest.RegionImporter;
import com.vantage.client.render.LodRenderer;
import com.vantage.client.render.Planner;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** Connects Vantage to the game: lifecycle, chunk events, rendering, fog and the F3 screen. */
public final class VantageClient {
    private static @Nullable LodSession session;
    private static boolean wasEnabled = true;

    private VantageClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(VantageClient::onRegisterReloadListeners);
        NeoForge.EVENT_BUS.addListener(VantageClient::onLevelLoad);
        NeoForge.EVENT_BUS.addListener(VantageClient::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(VantageClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(VantageClient::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(VantageClient::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(VantageClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(VantageClient::onRenderStage);
        NeoForge.EVENT_BUS.addListener(VantageClient::onRenderFog);
        NeoForge.EVENT_BUS.addListener(VantageClient::onFogColor);
        NeoForge.EVENT_BUS.addListener(VantageClient::onDebugText);
        VantageNetwork.setClientHandler(ClientTerrain.INSTANCE);
        AutoTest.init();
    }

    public static @Nullable LodSession session() {
        return session;
    }

    /** Called by the level renderer hook whenever vanilla marks a section dirty (blocks or light). */
    public static void onSectionDirty(int sectionX, int sectionZ) {
        LodSession s = session;
        if (s != null) {
            s.ingest.onChunkChanged(sectionX, sectionZ);
        }
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            LodSession s = session;
            if (s != null) {
                s.analyzer.reset();
                s.visuals.recomputeColors();
            }
        });
    }

    private static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel level)) {
            return;
        }
        closeSession();
        ClientTerrain.INSTANCE.reset(false);
        openSession(level, false);
    }

    /** @param catchUp also queue the chunks that are already loaded (Vantage was switched on mid-game) */
    private static void openSession(ClientLevel level, boolean catchUp) {
        // Always-foggy dimensions (the Nether) hide everything past ~100 blocks, so LODs would never show.
        if (!VantageConfig.ENABLED.get() || level.effects().isFoggyAt(0, 0)) {
            return;
        }
        try {
            LodSession s = LodSession.open(level);
            session = s;
            Player player = Minecraft.getInstance().player;
            if (catchUp && player != null) {
                int r = Minecraft.getInstance().options.getEffectiveRenderDistance() + 2;
                int cx = player.chunkPosition().x, cz = player.chunkPosition().z;
                for (int x = cx - r; x <= cx + r; x++) {
                    for (int z = cz - r; z <= cz + r; z++) {
                        if (level.getChunkSource().hasChunk(x, z)) {
                            s.ingest.onChunkLoaded(x, z);
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            Vantage.LOGGER.error("Vantage failed to start for {}", level.dimension().location(), e);
            session = null;
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level && session != null && session.dimension == level.dimension()) {
            closeSession();
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        closeSession();
        ClientTerrain.INSTANCE.reset(true);
    }

    static void closeSession() {
        LodSession s = session;
        session = null;
        if (s != null) {
            try {
                s.close();
            } catch (RuntimeException e) {
                Vantage.LOGGER.error("Error closing Vantage session", e);
            }
        }
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        LodSession s = session;
        if (s != null && event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk) {
            s.ingest.onChunkLoaded(chunk.getPos().x, chunk.getPos().z);
        }
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {
        LodSession s = session;
        if (s != null && event.getLevel() instanceof ClientLevel level && event.getChunk() instanceof LevelChunk chunk) {
            s.ingest.onChunkUnloaded(level, chunk);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientTerrain.INSTANCE.expire(15_000);
        ClientLevel level = Minecraft.getInstance().level;
        boolean enabled = VantageConfig.ENABLED.get();
        if (enabled && !wasEnabled && session == null && level != null) {
            openSession(level, true); // just switched on in the config screen
        }
        wasEnabled = enabled;
        LodSession s = session;
        if (s != null && level != null && level.dimension() == s.dimension) {
            s.tick(level);
        }
    }

    /** LODs are pointless (and would look wrong) when vanilla fog hides everything nearby. */
    private static boolean lodVisible(Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (camera.getFluidInCamera() != FogType.NONE || mc.level == null) {
            return false;
        }
        if (camera.getEntity() instanceof Player p && (p.hasEffect(MobEffects.BLINDNESS) || p.hasEffect(MobEffects.DARKNESS))) {
            return false;
        }
        Vec3 pos = camera.getPosition();
        return !mc.level.effects().isFoggyAt(Mth.floor(pos.x), Mth.floor(pos.y)) && !mc.gui.getBossOverlay().shouldCreateWorldFog();
    }

    private static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        LodSession s = session;
        Minecraft mc = Minecraft.getInstance();
        if (s == null || mc.level == null || mc.level.dimension() != s.dimension || !VantageConfig.ENABLED.get()) {
            return;
        }
        if (AutoTest.lodSuppressed) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 pos = camera.getPosition();
        s.setCamera(pos.x, pos.y, pos.z);
        LodRenderer renderer = s.renderer();
        if (renderer == null) {
            return;
        }
        float seaLevel = mc.level.getSeaLevel();
        Sky sky = sky(mc.level.dimension());
        float renderDistance = lodDistance(pos.y, seaLevel, sky.radius());
        float vanillaFar = mc.gameRenderer.getDepthFar();
        int height = Math.max(1, mc.getMainRenderTarget().height);
        double tanHalfFov = 1.0 / Math.max(1e-3, Math.abs(event.getProjectionMatrix().m11()));
        double radiansPerPixel = 2.0 * Math.atan(tanHalfFov) / height;
        // Vanilla skips chunk sections further above or below the camera than its render distance
        // (Sodium measures a little differently: keep a section's margin).
        float vanillaVertical = mc.options.getEffectiveRenderDistance() * 16f - 16f;
        s.planner.setView(new Planner.View(pos.x, pos.y, pos.z, renderDistance, radiansPerPixel,
                VantageConfig.DETAIL.get(), vanillaFar, vanillaVertical, s.coverage.current()));

        Planner.Plan plan = s.planner.plan();
        renderer.upload(s.meshes, (long) VantageConfig.UPLOAD_BUDGET_KB.get() << 10, plan.id);
        if (!lodVisible(camera)) {
            return;
        }
        int radius = sky.radius();
        float fogStart = VantageConfig.FOG_START.get().floatValue();
        if (radius > 0) {
            // The horizon hides the end of the data already; only fade the last mountain tops.
            fogStart = Math.max(fogStart, 0.95f);
        }
        LodRenderer.Frame frame = new LodRenderer.Frame(event.getModelViewMatrix(), event.getProjectionMatrix(),
                pos.x, pos.y, pos.z, renderDistance, fogStart, sky.hazeDensity(), sky.hazeHeight(), seaLevel,
                mc.options.getEffectiveRenderDistance() * 16f, radius > 0 ? 0.5f / radius : 0f, vanillaFar, vanillaVertical,
                // Seen from space the air still glows in daylight: haze keeps the colour of the sky down there.
                sky.hazeColor() >= 0 ? sky.hazeColor() : groundFogColor);
        renderer.render(frame, plan, s.world, s.visuals, s.coverage.current());
    }

    /** Set while vanilla works out the fog colour (see FogRendererMixin). Render thread. */
    public static boolean computingFogColor;
    /** The fog colour before the space sky darkened it ({@code 0xRRGGBB}), or -1. Render thread. */
    private static int groundFogColor = -1;

    private static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float d = spaceDarkness(event.getCamera().getPosition().y);
        if (d <= 0f) {
            groundFogColor = -1;
            return;
        }
        groundFogColor = rgb(event.getRed()) << 16 | rgb(event.getGreen()) << 8 | rgb(event.getBlue());
        event.setRed(event.getRed() * (1f - d));
        event.setGreen(event.getGreen() * (1f - d));
        event.setBlue(event.getBlue() * (1f - d));
    }

    private static int rgb(float c) {
        return Mth.clamp(Math.round(c * 255f), 0, 255);
    }

    /**
     * How far the sky has faded to black for a camera at height {@code y}: 0 inside the atmosphere,
     * then rising from three atmosphere scale heights above sea level, nearly 1 two scale heights
     * later. Air too thin to scatter sunlight leaves a black sky with stars, even by day.
     */
    public static float spaceDarkness(double y) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !VantageConfig.SPACE_SKY.get() || !VantageConfig.ENABLED.get()
                || level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL
                || mc.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
            return 0f;
        }
        Sky sky = sky(level.dimension());
        if (sky.hazeDensity() <= 0f) {
            // No air at all: whoever made this place draws its sky.
            return 0f;
        }
        double scaleHeights = (y - level.getSeaLevel()) / sky.hazeHeight();
        return (float) Mth.clamp(1.0 - Math.exp(3.0 - scaleHeights), 0.0, 1.0);
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        LodSession s = session;
        if (s == null || AutoTest.lodSuppressed || !VantageConfig.ENABLED.get() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN
                || event.getType() != FogType.NONE || !lodVisible(event.getCamera())) {
            return;
        }
        LodRenderer r = s.renderer();
        if (r == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float renderDistance = lodDistance(event.getCamera().getPosition().y, mc.level.getSeaLevel(), sky(mc.level.dimension()).radius());
        event.setNearPlaneDistance(renderDistance * VantageConfig.FOG_START.get().floatValue());
        event.setFarPlaneDistance(renderDistance);
        event.setCanceled(true);
    }

    /**
     * LOD distance in blocks for a camera at height {@code y}: the configured distance on the
     * ground, more when flying high so the ground reaches the horizon; with planet curvature, out
     * to the horizon (and the mountains peeking over it).
     */
    static float lodDistance(double y, float seaLevel, int radius) {
        double altitude = Math.max(0.0, y - seaLevel);
        // On a planet nothing past the horizon (plus the mountains peeking over it) can show.
        double reach = radius > 0 ? horizon(radius, altitude) + horizon(radius, 256)
                : VantageConfig.ALTITUDE_VIEW.get() * altitude;
        double base = VantageConfig.RENDER_DISTANCE.get() * 16.0;
        return (float) Math.min(Math.max(base, reach), VantageConfig.MAX_RENDER_DISTANCE * 16.0);
    }

    /** Planet look in effect: set by another mod through {@link VantageApi}, else from the config. */
    record Sky(int radius, float hazeDensity, float hazeHeight, int hazeColor) {
    }

    static Sky sky(ResourceKey<Level> dimension) {
        VantageApi.Planet p = VantageApi.planet(dimension);
        if (p != null) {
            return new Sky(p.radius(), p.hazeDistance() == 0 ? 0f : 1f / p.hazeDistance(), p.atmosphereHeight(), p.hazeColor());
        }
        // A Vantage Planet world: curve the distant terrain exactly like the world wraps.
        PlanetInfo info = ClientTerrain.INSTANCE.planet(dimension);
        int radius = info != null && info.radius() > 0 ? info.radius() : VantageConfig.PLANET_RADIUS.get();
        return new Sky(radius, 1f / VantageConfig.HAZE_DISTANCE.get(), VantageConfig.ATMOSPHERE_HEIGHT.get(), -1);
    }

    /** Distance to the horizon from {@code h} blocks above the surface of a planet of radius {@code r}. */
    private static double horizon(double r, double h) {
        return Math.sqrt(2.0 * r * h + h * h);
    }

    private static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        LodSession s = session;
        Minecraft mc = Minecraft.getInstance();
        if (s == null || !mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        List<String> right = event.getRight();
        right.add("");
        LodRenderer r = s.renderer();
        if (r == null) {
            right.add("[Vantage] unavailable on this GPU (needs OpenGL 4.3)");
            return;
        }
        Planner.Plan plan = s.planner.plan();
        right.add(String.format(Locale.ROOT, "[Vantage] %d sections, %d draws, %.1fk quads, cpu %.2f ms (+gl %.2f ms)",
                r.lastSections, r.lastDraws, r.lastQuads / 1000.0, r.lastBuildMillis, r.lastCpuMillis - r.lastBuildMillis));
        right.add(String.format(Locale.ROOT, "[Vantage] plan %d (%d visited, %.1f ms)",
                plan.entries.length, plan.visited, plan.nanos / 1e6));
        right.add(String.format(Locale.ROOT, "[Vantage] GPU %d/%d MiB, %d meshes, %.1fM quads",
                r.gpuBytesUsed() >> 20, r.gpuBytesReserved() >> 20, s.meshes.residentMeshes(), s.meshes.residentQuads() / 1e6));
        right.add(String.format(Locale.ROOT, "[Vantage] jobs %d, meshing %d, uploads %d, ingest %d",
                s.pool.queued(), s.meshes.inFlight(), s.meshes.pendingUploads(), s.ingest.pending()));
        right.add(String.format(Locale.ROOT, "[Vantage] cache %d sections (%d MiB), %d dirty",
                s.world.cachedSections(), s.world.cachedBytes() >> 20, s.world.dirtyCount()));
        RegionImporter imp = s.importer();
        if (imp != null && imp.total() > 0) {
            right.add(String.format(Locale.ROOT, "[Vantage] import %d/%d chunks%s", imp.done(), imp.total(), imp.scanning() ? "" : " (idle)"));
        }
        DistantGenerator gen = s.generator();
        if (gen != null) {
            right.add(String.format(Locale.ROOT, "[Vantage] generated %d sections (%.1f ms each), %d queued",
                    gen.generatedSections(), gen.averageMillis(), gen.queued()));
        }
        var detail = s.detail();
        if (detail != null) {
            right.add("[Vantage] " + detail.describe());
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        if (mc.level != null) {
            right.add(String.format(Locale.ROOT, "[Vantage] distance %.1f km",
                    lodDistance(cam.y, mc.level.getSeaLevel(), sky(mc.level.dimension()).radius()) / 1000.0));
        }
    }
}
