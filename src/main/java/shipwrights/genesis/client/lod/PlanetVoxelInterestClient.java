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
        // The packet validates hard because it also parses untrusted input from
        // the network. Legitimate local views can still sit outside those
        // bounds — the orbital survey narrows the real camera FOV to as little
        // as half a degree at maximum zoom, which is well past the 20 degree
        // floor — so clamp here rather than letting a supported feature throw
        // on the render thread and take the client down.
        float aspect = clamp((float) aspectRatio, 0.2f, 8.0f);
        float fov = clamp((float) verticalFovRadians,
                (float) Math.toRadians(20.0), (float) Math.toRadians(150.0));
        float target = clamp((float) targetVoxelPixels, 0.25f, 16.0f);
        int bricks = Math.max(128, Math.min(8192, maximumBricks));
        int height = Math.max(180, Math.min(16_384, viewportHeight));
        if (!Float.isFinite(aspect) || !Float.isFinite(fov) || !Float.isFinite(target)
                || !camera.isFinite()) {
            return;
        }
        boolean changed = LAST_CAMERA.distanceSquared(camera) >= 64.0 * 64.0
                || LAST_LOOK.dot(normalizedLook) < 0.995
                || lastViewportHeight != height
                || lastMaximumBricks != bricks
                || Math.abs(lastAspect - aspect) > 0.01f
                || Math.abs(lastFov - fov) > 0.01f
                || Math.abs(lastTargetPixels - target) > 0.05f;
        if (!changed && now - lastSendNanos < KEEPALIVE_NANOS) return;

        PacketDistributor.sendToServer(new PlanetVoxelInterestPacket(
                EARTH_ID, camera.x(), camera.y(), camera.z(),
                (float) normalizedLook.x, (float) normalizedLook.y, (float) normalizedLook.z,
                height, aspect, fov, target, bricks));
        LAST_CAMERA.set(camera);
        LAST_LOOK.set(normalizedLook);
        lastViewportHeight = height;
        lastMaximumBricks = bricks;
        lastAspect = aspect;
        lastFov = fov;
        lastTargetPixels = target;
        lastSendNanos = now;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
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
