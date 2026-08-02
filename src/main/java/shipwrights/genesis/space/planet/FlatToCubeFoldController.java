package shipwrights.genesis.space.planet;

import net.minecraft.util.Mth;
import shipwrights.genesis.config.GenesisClientConfig;

/**
 * Decides how far the world has folded into a cube, and how visible the LOD
 * terrain beyond vanilla's render distance is.
 *
 * <p>The fold factor is deliberately a single per-frame scalar rather than a
 * per-brick or per-vertex quantity. Anything that varies between neighbouring
 * pieces of geometry tears them apart at their shared edge, and a cracked
 * planet is far more obvious than a slightly different fold schedule. The
 * camera-centred flat zone the design calls for is obtained for free instead:
 * {@link CubeSurfaceProjection#foldMatrix} collapses to the identity projection
 * on the face the camera is standing on, so terrain around the craft is exactly
 * where vanilla puts it at every fold value, while the five remote faces wrap
 * around it.</p>
 *
 * <p>Schedule, all configurable:</p>
 * <pre>
 *   ground .. WorldLodStartHeight      vanilla chunks only
 *   .. WorldLodFullHeight              LOD terrain fades in beyond them
 *   .. WorldCubeFoldStartHeight        flat world, LOD out to the horizon
 *   .. WorldCubeFoldFullHeight         remote faces fold around the player
 *   above                              complete six-face cube
 * </pre>
 */
public final class FlatToCubeFoldController {

    /** Everything the renderers need to agree on for one frame. */
    public record FoldState(float lodAlpha,
                            float fold,
                            float distanceCompression,
                            double altitude,
                            boolean cubeComplete) {
    }

    private FlatToCubeFoldController() {
    }

    /**
     * @param cameraY       camera altitude in the planet dimension
     * @param seaLevel      the planet's sea level
     * @param vanillaRadius vanilla render distance in blocks
     */
    public static FoldState evaluate(double cameraY, int seaLevel, int vanillaRadius) {
        double altitude = cameraY - seaLevel;

        int lodStart = GenesisClientConfig.getWorldLodStartHeight();
        int lodFull = GenesisClientConfig.getWorldLodFullHeight();
        float lodAlpha = smoothstep((cameraY - lodStart) / Math.max(1.0, lodFull - lodStart));

        int foldStart = GenesisClientConfig.getWorldCubeFoldStartHeight();
        int foldFull = GenesisClientConfig.getWorldCubeFoldFullHeight();
        float fold = smoothstep((cameraY - foldStart) / Math.max(1.0, foldFull - foldStart));

        return new FoldState(lodAlpha, fold,
                (float) distanceCompressionInfluence(cameraY),
                altitude, fold >= 0.999f);
    }

    /**
     * How strongly distant geometry is pulled toward the camera to stay inside
     * the vanilla far plane. Uniform scaling of camera-relative vectors leaves
     * apparent size and direction untouched, so this never changes what the
     * player sees — unlike shrinking a standalone planet model, which does.
     */
    public static double distanceCompressionInfluence(double cameraY) {
        double start = Math.max(512.0, GenesisClientConfig.getWorldLodStartHeight());
        double full = Math.max(start + 1.0, GenesisClientConfig.getWorldCubeCompressionFullHeight());
        return smoothstep((cameraY - start) / (full - start));
    }

    /**
     * Uniform camera-relative scale applied to LOD geometry so a cube face
     * 8 km away still lands inside Minecraft's far plane.
     */
    public static double distanceCompression(double distance, double cameraY, int vanillaRadius) {
        double target = Math.max(640.0, vanillaRadius * 3.25);
        double fullScale = distance <= target ? 1.0 : target / distance;
        return Mth.lerp(distanceCompressionInfluence(cameraY), 1.0, fullScale);
    }

    private static float smoothstep(double value) {
        float clamped = Mth.clamp((float) value, 0.0f, 1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }
}
