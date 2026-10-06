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
    private static final boolean DIMENSION_HOP = Boolean.getBoolean("vantage.autotest.dimensionHop");
    /** Climb straight up instead of the views and flight, shooting the ground at several heights. */
    private static final boolean ASCENT = Boolean.getBoolean("vantage.autotest.ascent");
    private static final int[] ALTITUDES = altitudes(System.getProperty("vantage.autotest.altitudes", "150,400,1000,2500,6000,15000"));
    private static final float[] ASCENT_PITCH = pitches(System.getProperty("vantage.autotest.pitches", "25,70"));
    /** Also shoot every ascent view with Vantage off, to tell its artifacts from vanilla's. */
    private static final boolean ASCENT_COMPARE = Boolean.getBoolean("vantage.autotest.ascentCompare");
    private static int ascentIndex = -1;
    private static int ascentShot;
    private static long ascentAt;
    private static long ascentLog;
    private static int startX;
    private static int startZ;
    /** Switch Vantage off at join and back on shortly after, as if from the config screen. */
    private static final boolean TOGGLE = Boolean.getBoolean("vantage.autotest.toggle");
    private static long toggleAt = -1;
    private static int hopStage;
    private static long hopAt;
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

    private static float[] pitches(String list) {
        int[] p = altitudes(list);
        float[] out = new float[p.length];
        for (int i = 0; i < p.length; i++) {
            out[i] = p[i];
        }
        return out;
    }

    private static int[] altitudes(String list) {
        return java.util.Arrays.stream(list.split(",")).map(String::trim).filter(x -> !x.isEmpty()).mapToInt(Integer::parseInt).toArray();
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
        if (finished) {
            return;
        }
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
            startX = player.getBlockX();
            startZ = player.getBlockZ();
            String at = System.getProperty("vantage.autotest.at");
            if (at != null && at.contains(",")) {
                // Somewhere else, such as land nobody has explored yet.
                startX = Integer.parseInt(at.split(",")[0].trim());
                startZ = Integer.parseInt(at.split(",")[1].trim());
            }
            if (TOGGLE) {
                VantageConfig.ENABLED.set(false);
                VantageClient.closeSession();
                toggleAt = now + 5_000;
            }
            String name = player.getGameProfile().getName();
            // Spectator: flies, no gravity, no hand in screenshots. Raised so trees do not block the view.
            // On a server this needs operator rights.
            command(mc, "gamemode spectator " + name);
            // Same light in every run.
            command(mc, "gamerule doDaylightCycle false");
            command(mc, "time set 6000");
            command(mc, "tp " + name + " " + startX + " " + HEIGHT + " " + startZ);
        }
        if (mc.screen != null) {
            mc.setScreen(null);
        }
        if (toggleAt > 0 && now >= toggleAt) {
            toggleAt = -1;
            VantageConfig.ENABLED.set(true);
            Vantage.LOGGER.info("[autotest] switched Vantage back on");
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
        if (ASCENT) {
            ascent(mc, player, now);
            return;
        }
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
            if (hopStage == 0) {
                Screenshot.grab(mc.gameDirectory, "vantage_after_flight.png", mc.getMainRenderTarget(), msg -> Vantage.LOGGER.info("[autotest] {}", msg.getString()));
                logStats("after flight");
                lodSuppressed = true;
                hopStage = -1;
                hopAt = now;
                return;
            }
            if (hopStage == -1) {
                if (now - hopAt < 3_000) {
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "vantage_after_flight_off.png", mc.getMainRenderTarget(), msg -> Vantage.LOGGER.info("[autotest] {}", msg.getString()));
                lodSuppressed = false;
                if (!DIMENSION_HOP) {
                    finish(mc);
                    return;
                }
                // Nether and back: exercises closing and reopening LOD sessions on dimension changes.
                hopStage = 1;
                hopAt = now;
                command(mc, "execute in minecraft:the_nether run tp " + player.getGameProfile().getName() + " 0 80 0");
                return;
            }
            if (hopStage == 1) {
                if (now - hopAt > 15_000 && mc.level.dimension() == net.minecraft.world.level.Level.NETHER) {
                    logStats("nether");
                    hopStage = 2;
                    hopAt = now;
                    command(mc, "execute in minecraft:overworld run tp " + player.getGameProfile().getName() + " "
                            + flightOrigin.x + " " + HEIGHT + " " + (flightOrigin.z + FLIGHT_MS / 1000.0 * FLIGHT_SPEED) + " 0 10");
                } else if (now - hopAt > 120_000) {
                    Vantage.LOGGER.error("[autotest] never arrived in the nether");
                    finish(mc);
                }
                return;
            }
            if (mc.level.dimension() != net.minecraft.world.level.Level.OVERWORLD || now - hopAt < 20_000
                    || (!settled() && now - hopAt < 120_000) || !mc.levelRenderer.hasRenderedAllSections()) {
                return;
            }
            Screenshot.grab(mc.gameDirectory, "vantage_after_hop.png", mc.getMainRenderTarget(), msg -> Vantage.LOGGER.info("[autotest] {}", msg.getString()));
            logStats("after hop");
            finish(mc);
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

    private static void ascent(Minecraft mc, LocalPlayer player, long now) {
        String name = player.getGameProfile().getName();
        if (ascentIndex < 0) {
            ascentIndex = 0;
            ascentAt = now;
            frameNanos.clear();
            command(mc, "tp " + name + " " + startX + " " + ALTITUDES[0] + " " + startZ);
            return;
        }
        int alt = ALTITUDES[ascentIndex];
        int perPitch = ASCENT_COMPARE ? 2 : 1;
        float pitch = ASCENT_PITCH[ascentShot / perPitch];
        boolean off = ascentShot % perPitch == 1;
        lodSuppressed = off;
        player.setYRot(0f);
        player.setXRot(pitch);
        player.yRotO = 0f;
        player.xRotO = pitch;
        long since = now - ascentAt;
        boolean ready = off ? since > 3_000 && (mc.levelRenderer.hasRenderedAllSections() || since > 30_000)
                : since > 8_000 && Math.abs(player.getY() - alt) < 2 && (settled() || since > 150_000)
                && (mc.levelRenderer.hasRenderedAllSections() || since > 60_000);
        if (!ready) {
            if (now - ascentLog > 10_000) {
                ascentLog = now;
                logStats("ascent y=" + alt + " waiting");
            }
            return;
        }
        String shot = String.format(Locale.ROOT, "vantage_ascent_%05d_%02d%s.png", alt, (int) pitch, off ? "_off" : "");
        Screenshot.grab(mc.gameDirectory, shot, mc.getMainRenderTarget(), msg -> Vantage.LOGGER.info("[autotest] {}", msg.getString()));
        logFrames();
        logStats(shot);
        frameNanos.clear();
        ascentAt = now;
        if (++ascentShot < ASCENT_PITCH.length * perPitch) {
            return;
        }
        ascentShot = 0;
        if (++ascentIndex >= ALTITUDES.length) {
            finish(mc);
            return;
        }
        command(mc, "tp " + name + " " + startX + " " + ALTITUDES[ascentIndex] + " " + startZ);
    }

    private static boolean finished;

    private static void finish(Minecraft mc) {
        finished = true;
        Vantage.LOGGER.info("[autotest] finished");
        mc.stop();
    }

    private static void command(Minecraft mc, String command) {
        var server = mc.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
        } else if (mc.getConnection() != null) {
            mc.getConnection().sendCommand(command);
        }
    }

    private static boolean settled() {
        LodSession s = VantageClient.session();
        if (s == null) {
            return false;
        }
        RegionImporter imp = s.importer();
        boolean importing = imp != null && (imp.scanning() || imp.total() == 0);
        var gen = s.generator();
        var detail = s.detail();
        return !importing && s.pool.queued() == 0 && s.meshes.inFlight() == 0 && s.meshes.pendingUploads() == 0
                && s.ingest.pending() == 0 && (gen == null || gen.queued() == 0) && (detail == null || detail.queued() == 0);
    }

    private static void logStats(String label) {
        LodSession s = VantageClient.session();
        if (s == null) {
            Vantage.LOGGER.info("[autotest] {}: no session", label);
            return;
        }
        LodRenderer r = s.renderer();
        RegionImporter imp = s.importer();
        int[] perLevel = new int[com.vantage.core.Lod.LEVELS];
        double[] nearest = new double[com.vantage.core.Lod.LEVELS];
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
        for (int l = 0; l < com.vantage.core.Lod.LEVELS; l++) {
            if (perLevel[l] > 0) {
                levels.append(String.format(Locale.ROOT, " L%d=%d(>%.0f)", l, perLevel[l], nearest[l]));
            }
        }
        var gen = s.generator();
        Vantage.LOGGER.info("[autotest] {} vanilla: {}", label, Minecraft.getInstance().levelRenderer.getSectionStatistics());
        var detail = s.detail();
        Vantage.LOGGER.info("[autotest] {} plan levels:{} coverage={} generated={} ({} ms each, {} queued) real={} ({} ms each, {} queued)",
                label, levels, s.coverage.current().size(), gen == null ? 0 : gen.generatedSections(),
                gen == null ? 0 : String.format(Locale.ROOT, "%.1f", gen.averageMillis()), gen == null ? 0 : gen.queued(),
                detail == null ? 0 : detail.chunks(),
                detail == null ? 0 : String.format(Locale.ROOT, "%.1f", detail.averageMillis()), detail == null ? 0 : detail.queued());
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
