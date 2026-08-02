package shipwrights.genesis.client.lod;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.networking.PlanetVoxelInterestPacket;

/** Throttles viewport interest updates so rendering never becomes a packet-per-frame path. */
public final class PlanetVoxelInterestClient {
    private static final ResourceLocation EARTH_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final long MIN_SEND_NANOS = 250_000_000L;
    private static final long KEEPALIVE_NANOS = 3_000_000_000L;
    private static final Vector3d LAST_CAMERA = new Vector3d(Double.NaN);
    private static final Vector3d LAST_LOOK = new Vector3d(Double.NaN);
    private static long lastSendNanos;
    private static int lastViewportHeight = -1;
    private static int lastMaximumBricks = -1;
    private static float lastAspect = Float.NaN;
    private static float lastFov = Float.NaN;
    private static float lastTargetPixels = Float.NaN;

    private PlanetVoxelInterestClient() {
    }

    public static void update(Vector3dc camera, Vector3dc look,
                              int viewportHeight, double aspectRatio,
                              double verticalFovRadians, double targetVoxelPixels,
                              int maximumBricks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || camera == null || look == null) return;
        long now = System.nanoTime();
        if (now - lastSendNanos < MIN_SEND_NANOS) return;

        Vector3d normalizedLook = new Vector3d(look);
        if (normalizedLook.lengthSquared() < 1.0E-9) return;
        normalizedLook.normalize();
        float aspect = (float) aspectRatio;
        float fov = (float) verticalFovRadians;
        float target = (float) targetVoxelPixels;
        boolean changed = LAST_CAMERA.distanceSquared(camera) >= 64.0 * 64.0
                || LAST_LOOK.dot(normalizedLook) < 0.995
                || lastViewportHeight != viewportHeight
                || lastMaximumBricks != maximumBricks
                || Math.abs(lastAspect - aspect) > 0.01f
                || Math.abs(lastFov - fov) > 0.01f
                || Math.abs(lastTargetPixels - target) > 0.05f;
        if (!changed && now - lastSendNanos < KEEPALIVE_NANOS) return;

        PacketDistributor.sendToServer(new PlanetVoxelInterestPacket(
                EARTH_ID, camera.x(), camera.y(), camera.z(),
                (float) normalizedLook.x, (float) normalizedLook.y, (float) normalizedLook.z,
                Math.max(180, Math.min(16_384, viewportHeight)),
                aspect, fov, target, maximumBricks));
        LAST_CAMERA.set(camera);
        LAST_LOOK.set(normalizedLook);
        lastViewportHeight = viewportHeight;
        lastMaximumBricks = maximumBricks;
        lastAspect = aspect;
        lastFov = fov;
        lastTargetPixels = target;
        lastSendNanos = now;
    }

    public static void reset() {
        LAST_CAMERA.set(Double.NaN);
        LAST_LOOK.set(Double.NaN);
        lastSendNanos = 0L;
        lastViewportHeight = -1;
        lastMaximumBricks = -1;
        lastAspect = Float.NaN;
        lastFov = Float.NaN;
        lastTargetPixels = Float.NaN;
    }
}
