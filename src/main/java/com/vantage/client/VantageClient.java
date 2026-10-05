package com.vantage.client;

import com.vantage.Vantage;
import com.vantage.client.ingest.RegionImporter;
import com.vantage.client.render.LodRenderer;
import com.vantage.client.render.Planner;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
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
        NeoForge.EVENT_BUS.addListener(VantageClient::onDebugText);
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
        float renderDistance = VantageConfig.RENDER_DISTANCE.get() * 16f;
        int height = Math.max(1, mc.getMainRenderTarget().height);
        double tanHalfFov = 1.0 / Math.max(1e-3, Math.abs(event.getProjectionMatrix().m11()));
        double radiansPerPixel = 2.0 * Math.atan(tanHalfFov) / height;
        s.planner.setView(new Planner.View(pos.x, pos.y, pos.z, renderDistance, radiansPerPixel,
                VantageConfig.DETAIL.get(), s.coverage.current()));

        Planner.Plan plan = s.planner.plan();
        renderer.upload(s.meshes, (long) VantageConfig.UPLOAD_BUDGET_KB.get() << 10, plan.id);
        if (!lodVisible(camera)) {
            return;
        }
        LodRenderer.Frame frame = new LodRenderer.Frame(event.getModelViewMatrix(), event.getProjectionMatrix(),
                pos.x, pos.y, pos.z, renderDistance, VantageConfig.FOG_START.get().floatValue());
        renderer.render(frame, plan, s.world, s.visuals, s.coverage.current());
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
        float renderDistance = VantageConfig.RENDER_DISTANCE.get() * 16f;
        event.setNearPlaneDistance(renderDistance * VantageConfig.FOG_START.get().floatValue());
        event.setFarPlaneDistance(renderDistance);
        event.setCanceled(true);
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
    }
}
