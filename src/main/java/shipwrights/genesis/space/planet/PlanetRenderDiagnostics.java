package shipwrights.genesis.space.planet;

import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Live counters for the planet voxel engine, surfaced by the debug overlay and
 * the {@code /genesis planet} diagnostics command.
 *
 * <p>Everything here is written from the render thread or from workers and read
 * from the render thread, so counters are atomic and snapshots are taken as a
 * whole rather than field by field.</p>
 */
public final class PlanetRenderDiagnostics {

    /** Debug rendering modes. Never enabled by default. */
    public enum DebugMode {
        OFF,
        LOD,
        AUTHORITY,
        BRICK_BOUNDS,
        SEAMS,
        FOLD,
        MISSING;

        public static DebugMode parse(String name) {
            if (name == null) return OFF;
            try {
                return valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return OFF;
            }
        }
    }

    private static final AtomicLong MESH_JOBS_QUEUED = new AtomicLong();
    private static final AtomicLong MESH_JOBS_COMPLETED = new AtomicLong();
    private static final AtomicLong GPU_UPLOADS = new AtomicLong();
    private static final AtomicLong BRICKS_RECEIVED = new AtomicLong();
    private static final AtomicLong BYTES_RECEIVED = new AtomicLong();

    private static volatile CubeNetSurfaceTransform.Face currentFace;
    private static volatile CubeNetSurfaceTransform.Face destinationFace;
    private static volatile double surfaceX;
    private static volatile double surfaceZ;
    private static volatile double simulationAltitude;
    private static volatile double visualAltitude;
    private static volatile float fold;
    private static volatile String transitionState = "IDLE";
    private static volatile String handoffReadiness = "n/a";
    private static volatile int visibleBricks;
    private static volatile int residentBricks;
    private static volatile int pendingGpuUploads;
    private static volatile int queuedMeshJobs;
    private static volatile long clientCacheBytes;
    private static volatile long gpuCacheBytes;
    private static volatile int predictedBricks;
    private static volatile int exactBricks;
    private static volatile int fallbackBricks;
    private static final int[] LOD_COUNTS = new int[shipwrights.genesis.space.voxel
            .PlanetVoxelBrickKey.MAX_LOD + 1];

    private PlanetRenderDiagnostics() {
    }

    public static DebugMode debugMode() {
        return DebugMode.parse(shipwrights.genesis.config.GenesisClientConfig.getPlanetDebugMode());
    }

    public static void onMeshJobQueued() {
        MESH_JOBS_QUEUED.incrementAndGet();
    }

    public static void onMeshJobCompleted() {
        MESH_JOBS_COMPLETED.incrementAndGet();
    }

    public static void onGpuUpload() {
        GPU_UPLOADS.incrementAndGet();
    }

    public static void onBrickReceived(long bytes) {
        BRICKS_RECEIVED.incrementAndGet();
        BYTES_RECEIVED.addAndGet(Math.max(0L, bytes));
    }

    public static void setLocation(CubeNetSurfaceTransform.Face face,
                                   double faceLocalX, double faceLocalZ,
                                   double simulation, double visual) {
        currentFace = face;
        surfaceX = faceLocalX;
        surfaceZ = faceLocalZ;
        simulationAltitude = simulation;
        visualAltitude = visual;
    }

    public static void setFold(float value) {
        fold = value;
    }

    public static void setTransition(String state, CubeNetSurfaceTransform.Face destination,
                                     String readiness) {
        transitionState = state == null ? "IDLE" : state;
        destinationFace = destination;
        handoffReadiness = readiness == null ? "n/a" : readiness;
    }

    public static void setFrame(int visible, int resident, int pendingUploads, int queuedMeshes,
                                long clientBytes, long gpuBytes,
                                int predicted, int exact, int fallback, int[] lodCounts) {
        visibleBricks = visible;
        residentBricks = resident;
        pendingGpuUploads = pendingUploads;
        queuedMeshJobs = queuedMeshes;
        clientCacheBytes = clientBytes;
        gpuCacheBytes = gpuBytes;
        predictedBricks = predicted;
        exactBricks = exact;
        fallbackBricks = fallback;
        synchronized (LOD_COUNTS) {
            java.util.Arrays.fill(LOD_COUNTS, 0);
            if (lodCounts != null) {
                System.arraycopy(lodCounts, 0, LOD_COUNTS, 0,
                        Math.min(lodCounts.length, LOD_COUNTS.length));
            }
        }
    }

    public static void reset() {
        MESH_JOBS_QUEUED.set(0);
        MESH_JOBS_COMPLETED.set(0);
        GPU_UPLOADS.set(0);
        BRICKS_RECEIVED.set(0);
        BYTES_RECEIVED.set(0);
        currentFace = null;
        destinationFace = null;
        transitionState = "IDLE";
        handoffReadiness = "n/a";
        visibleBricks = 0;
        residentBricks = 0;
        pendingGpuUploads = 0;
        queuedMeshJobs = 0;
        clientCacheBytes = 0L;
        gpuCacheBytes = 0L;
        predictedBricks = 0;
        exactBricks = 0;
        fallbackBricks = 0;
        synchronized (LOD_COUNTS) {
            java.util.Arrays.fill(LOD_COUNTS, 0);
        }
    }

    /** Overlay lines, newest state each call. Cheap enough for a debug HUD. */
    public static List<String> overlayLines() {
        List<String> lines = new ArrayList<>(14);
        lines.add(String.format(Locale.ROOT, "face %s  surface %.1f / %.1f",
                currentFace == null ? "-" : currentFace.name(), surfaceX, surfaceZ));
        lines.add(String.format(Locale.ROOT, "altitude sim %.0f  visual %.0f  fold %.3f",
                simulationAltitude, visualAltitude, fold));
        lines.add(String.format(Locale.ROOT, "bricks visible %d  resident %d",
                visibleBricks, residentBricks));
        lines.add(String.format(Locale.ROOT, "authority exact %d  predicted %d  fallback %d",
                exactBricks, predictedBricks, fallbackBricks));
        lines.add("lod " + lodHistogram());
        lines.add(String.format(Locale.ROOT, "queues mesh %d  gpu-upload %d",
                queuedMeshJobs, pendingGpuUploads));
        lines.add(String.format(Locale.ROOT, "cache client %d MiB  gpu %d MiB",
                clientCacheBytes / (1024L * 1024L), gpuCacheBytes / (1024L * 1024L)));
        lines.add(String.format(Locale.ROOT, "net bricks %d  %d KiB",
                BRICKS_RECEIVED.get(), BYTES_RECEIVED.get() / 1024L));
        lines.add(String.format(Locale.ROOT, "mesh jobs %d/%d  gpu uploads %d",
                MESH_JOBS_COMPLETED.get(), MESH_JOBS_QUEUED.get(), GPU_UPLOADS.get()));
        lines.add("transition " + transitionState
                + "  destination " + (destinationFace == null ? "-" : destinationFace.name()));
        lines.add("handoff " + handoffReadiness);
        DebugMode mode = debugMode();
        if (mode != DebugMode.OFF) lines.add("debug render " + mode);
        return lines;
    }

    private static String lodHistogram() {
        StringBuilder builder = new StringBuilder();
        synchronized (LOD_COUNTS) {
            for (int lod = 0; lod < LOD_COUNTS.length; lod++) {
                if (LOD_COUNTS[lod] == 0) continue;
                if (builder.length() > 0) builder.append(' ');
                builder.append(lod).append(':').append(LOD_COUNTS[lod]);
            }
        }
        return builder.length() == 0 ? "-" : builder.toString();
    }
}
