package com.vantage.client;

import com.vantage.Vantage;
import com.vantage.client.ingest.RegionImporter;
import com.vantage.client.render.LodRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Development aid, inactive unless started with {@code -Dvantage.autotest=true}: waits for LODs to
 * build, takes screenshots with Vantage on and off from a few viewpoints, logs frame timings, then
 * quits. Used to check rendering on machines without a display.
 */
public final class AutoTest {
    private static final boolean ENABLED = Boolean.getBoolean("vantage.autotest");
    private static final long SETTLE_MS = Long.getLong("vantage.autotest.settleMs", 240_000L);
    private static final float[][] VIEWS = {{0f, 8f}, {90f, 8f}, {180f, 8f}, {270f, 8f}, {45f, 35f}};
    private static final int HEIGHT = Integer.getInteger("vantage.autotest.height", 150);

    private static long joinedAt = -1;
    private static int stage = -1;
    private static int view;
    private static long stageAt;
    private static final List<Long> frameNanos = new ArrayList<>();
    private static long lastFrame;
    static volatile boolean lodSuppressed;

    private AutoTest() {
    }

    public static void init() {
        if (!ENABLED) {
            return;
        }
        Vantage.LOGGER.info("[autotest] enabled");
        NeoForge.EVENT_BUS.addListener(AutoTest::onTick);
        NeoForge.EVENT_BUS.addListener(AutoTest::onFrame);
    }

    private static void onFrame(RenderFrameEvent.Post event) {
        long now = System.nanoTime();
        if (lastFrame != 0 && stage >= 0) {
            frameNanos.add(now - lastFrame);
        }
        lastFrame = now;
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (joinedAt < 0) {
            joinedAt = now;
            stageAt = now;
            mc.options.hideGui = true;
            Vantage.LOGGER.info("[autotest] joined world at {}", player.blockPosition());
            var server = mc.getSingleplayerServer();
            if (server != null) {
                String name = player.getGameProfile().getName();
                // Spectator: flies, no gravity, no hand in screenshots. Raised so trees do not block the view.
                server.execute(() -> {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator " + name);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                            "tp " + name + " " + player.getBlockX() + " " + HEIGHT + " " + player.getBlockZ());
                });
            }
        }
        if (mc.screen != null) {
            mc.setScreen(null);
        }
        if (stage == -1) {
            player.setDeltaMovement(0, 0, 0);
            if (now - stageAt > 5_000 && (now - joinedAt) % 10_000 < 60) {
                logStats("settling");
            }
            if (now - joinedAt > SETTLE_MS || (now - joinedAt > 60_000 && settled())) {
                logStats("settled");
                stage = 0;
                view = 0;
                stageAt = now;
            }
            return;
        }
        player.setDeltaMovement(0, 0, 0);
        if (view >= VIEWS.length) {
            logFrames();
            Vantage.LOGGER.info("[autotest] finished");
            mc.stop();
            return;
        }
        float[] v = VIEWS[view];
        player.setYRot(v[0]);
        player.setXRot(v[1]);
        player.yRotO = v[0];
        player.xRotO = v[1];
        long since = now - stageAt;
        // stage 0: LOD on, wait, shoot; stage 1: LOD off, wait, shoot.
        lodSuppressed = stage == 1;
        if (since > 4_000) {
            String name = String.format(Locale.ROOT, "vantage_view%d_%s.png", view, stage == 0 ? "on" : "off");
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> Vantage.LOGGER.info("[autotest] {}", msg.getString()));
            logFrames();
            logStats(name);
            frameNanos.clear();
            stageAt = now;
            if (stage == 0) {
                stage = 1;
            } else {
                stage = 0;
                view++;
            }
        }
    }

    private static boolean settled() {
        LodSession s = VantageClient.session();
        if (s == null) {
            return false;
        }
        RegionImporter imp = s.importer();
        boolean importing = imp != null && (imp.scanning() || imp.total() == 0);
        return !importing && s.pool.queued() == 0 && s.meshes.inFlight() == 0 && s.meshes.pendingUploads() == 0
                && s.ingest.pending() == 0;
    }

    private static void logStats(String label) {
        LodSession s = VantageClient.session();
        if (s == null) {
            Vantage.LOGGER.info("[autotest] {}: no session", label);
            return;
        }
        LodRenderer r = s.renderer();
        RegionImporter imp = s.importer();
        Vantage.LOGGER.info("[autotest] {}: sections={} draws={} quads={} cpu={}ms plan={} visited={} planMs={} gpuMiB={} meshes={} residentQuads={} jobs={} meshing={} uploads={} cache={} dirty={} import={}/{} visuals={} fps={}",
                label,
                r == null ? -1 : r.lastSections, r == null ? -1 : r.lastDraws, r == null ? -1 : r.lastQuads,
                r == null ? -1 : String.format(Locale.ROOT, "%.2f", r.lastCpuMillis),
                s.planner.plan().entries.length, s.planner.plan().visited, String.format(Locale.ROOT, "%.1f", s.planner.plan().nanos / 1e6),
                r == null ? -1 : r.gpuBytesUsed() >> 20, s.meshes.residentMeshes(), s.meshes.residentQuads(),
                s.pool.queued(), s.meshes.inFlight(), s.meshes.pendingUploads(), s.world.cachedSections(), s.world.dirtyCount(),
                imp == null ? 0 : imp.done(), imp == null ? 0 : imp.total(), s.visuals.size(), Minecraft.getInstance().getFps());
    }

    private static void logFrames() {
        if (frameNanos.isEmpty()) {
            return;
        }
        long[] f = frameNanos.stream().mapToLong(Long::longValue).sorted().toArray();
        double avg = frameNanos.stream().mapToLong(Long::longValue).average().orElse(0) / 1e6;
        Vantage.LOGGER.info(String.format(Locale.ROOT, "[autotest] frames=%d avg=%.1fms p50=%.1fms p95=%.1fms max=%.1fms",
                f.length, avg, f[f.length / 2] / 1e6, f[(int) (f.length * 0.95)] / 1e6, f[f.length - 1] / 1e6));
    }
}
