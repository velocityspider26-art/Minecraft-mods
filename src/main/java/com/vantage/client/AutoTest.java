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
    private static final float[][] VIEWS = {{0f, 8f}, {90f, 8f}, {180f, 8f}, {270f, 8f}, {45f, 35f}, {0f, 89.9f}};
    private static final int HEIGHT = Integer.getInteger("vantage.autotest.height", 150);
    private static final long FLIGHT_MS = 30_000;
    private static final double FLIGHT_SPEED = Double.parseDouble(System.getProperty("vantage.autotest.flightSpeed", "30"));
    private static final int COMPARE_RD = Integer.getInteger("vantage.autotest.compareRd", 0);
    private static int compareStage;
    private static long compareAt;
    private static long compareMeasureAt = -1;
    private static int originalRd;
    private static long flightStart = -1;
    private static net.minecraft.world.phys.Vec3 flightOrigin;
    private static long lastFlightLog = -1;

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
        if (view >= VIEWS.length && COMPARE_RD > 0 && compareStage < 2) {
            // Vanilla-only at a large render distance, same viewpoint as view 0, for a cost comparison.
            if (compareStage == 0) {
                compareStage = 1;
                compareAt = now;
                originalRd = mc.options.renderDistance().get();
                mc.options.renderDistance().set(COMPARE_RD);
                mc.options.broadcastOptions();
                lodSuppressed = true;
                player.setYRot(VIEWS[0][0]);
                player.setXRot(VIEWS[0][1]);
                frameNanos.clear();
                Vantage.LOGGER.info("[autotest] comparing with vanilla render distance {}", COMPARE_RD);
                return;
            }
            int expected = (int) (0.8 * Math.PI * COMPARE_RD * COMPARE_RD);
            boolean loaded = mc.level.getChunkSource().getLoadedChunksCount() >= expected
                    && mc.levelRenderer.hasRenderedAllSections() && now - compareAt > 20_000;
            if (loaded || now - compareAt > 360_000) {
                if (compareMeasureAt < 0) {
                    compareMeasureAt = now;
                    frameNanos.clear();
                    return;
                }
                if (now - compareMeasureAt < 6_000) {
                    return;
                }
                logFrames();
                Screenshot.grab(mc.gameDirectory, "vantage_vanilla_rd" + COMPARE_RD + ".png", mc.getMainRenderTarget(),
                        msg -> Vantage.LOGGER.info("[autotest] {}", msg.getString()));
                Vantage.LOGGER.info("[autotest] vanilla rd{}: {} chunks loaded, sections {}", COMPARE_RD,
                        mc.level.getChunkSource().getLoadedChunksCount(), mc.levelRenderer.getSectionStatistics());
                mc.options.renderDistance().set(originalRd);
                mc.options.broadcastOptions();
                lodSuppressed = false;
                compareStage = 2;
                frameNanos.clear();
            }
            return;
        }
        if (view >= VIEWS.length) {
            if (flightStart < 0) {
                logFrames();
                frameNanos.clear();
                flightStart = now;
                flightOrigin = player.position();
                lodSuppressed = false;
                Vantage.LOGGER.info("[autotest] flight start at {}", player.blockPosition());
            }
            long t = now - flightStart;
            if (t < FLIGHT_MS) {
                // Fly south at FLIGHT_SPEED blocks per second, looking ahead.
                double dist = t / 1000.0 * FLIGHT_SPEED;
                player.setPos(flightOrigin.x, HEIGHT, flightOrigin.z + dist);
                player.setYRot(0f);
                player.setXRot(10f);
                player.yRotO = 0f;
                player.xRotO = 10f;
                if (t / 2000 != lastFlightLog) {
                    lastFlightLog = t / 2000;
                    logFrames();
                    frameNanos.clear();
                    logStats(String.format(Locale.ROOT, "flight %.0fm", dist));
                }
                return;
            }
            if (t < FLIGHT_MS + 20_000 && !settled()) {
                return;
            }
            Screenshot.grab(mc.gameDirectory, "vantage_after_flight.png", mc.getMainRenderTarget(), msg -> Vantage.LOGGER.info("[autotest] {}", msg.getString()));
            logStats("after flight");
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
        // stage 0: LOD on, wait, shoot; stage 1: LOD off, wait, shoot. Waits for vanilla to finish
        // building the sections that came into view so its own holes do not show up.
        lodSuppressed = stage == 1;
        boolean vanillaReady = mc.levelRenderer.hasRenderedAllSections();
        if (since > 4_000 && (vanillaReady || since > 30_000)) {
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
        int[] perLevel = new int[7];
        double[] nearest = new double[7];
        java.util.Arrays.fill(nearest, Double.MAX_VALUE);
        var plan = s.planner.plan();
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (var e : plan.entries) {
            int l = com.vantage.core.SectionKey.level(e.key);
            perLevel[l]++;
            int span = com.vantage.core.Lod.sectionBlocks(l);
            double cx = (com.vantage.core.SectionKey.x(e.key) + 0.5) * span - cam.x;
            double cz = (com.vantage.core.SectionKey.z(e.key) + 0.5) * span - cam.z;
            nearest[l] = Math.min(nearest[l], Math.sqrt(cx * cx + cz * cz) - span * 0.7071);
        }
        StringBuilder levels = new StringBuilder();
        for (int l = 0; l < 7; l++) {
            if (perLevel[l] > 0) {
                levels.append(String.format(Locale.ROOT, " L%d=%d(>%.0f)", l, perLevel[l], nearest[l]));
            }
        }
        Vantage.LOGGER.info("[autotest] {} plan levels:{} coverage={}", label, levels, s.coverage.current().size());
        Vantage.LOGGER.info("[autotest] {}: sections={} draws={} quads={} cpu={}ms build={}ms plan={} visited={} planMs={} gpuMiB={} meshes={} residentQuads={} jobs={} meshing={} uploads={} cache={} dirty={} import={}/{} visuals={} fps={}",
                label,
                r == null ? -1 : r.lastSections, r == null ? -1 : r.lastDraws, r == null ? -1 : r.lastQuads,
                r == null ? -1 : String.format(Locale.ROOT, "%.2f", r.lastCpuMillis),
                r == null ? -1 : String.format(Locale.ROOT, "%.2f", r.lastBuildMillis),
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
