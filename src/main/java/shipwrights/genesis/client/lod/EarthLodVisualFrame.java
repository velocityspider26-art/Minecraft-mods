package shipwrights.genesis.client.lod;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;
import shipwrights.genesis.teleportation.SpaceTravelManager;

/**
 * Shared display frame for the overworld ascent and orbital Earth renderers.
 *
 * <p>The simulation keeps Genesis' existing 1:16 planet/space coordinates.
 * The renderer does not. It displays one Minecraft surface block as one render
 * block and compresses only the otherwise enormous empty altitude above the
 * terrain. This is the important distinction between a tiny painted planet and
 * a low-orbit view of the actual Minecraft LOD chunks.</p>
 */
public final class EarthLodVisualFrame {
    private static final double LINEAR_ALTITUDE_LIMIT = 1024.0;
    private static final double LOG_ALTITUDE_SCALE = 384.0;
    private static final double LOG_ALTITUDE_DIVISOR = 1024.0;
    private static final double FAR_ALTITUDE_START = 32768.0;
    private static final double FAR_ALTITUDE_DIVISOR = 16.0;

    private EarthLodVisualFrame() {
    }

    /**
     * Keeps normal flight unchanged near terrain, then turns tens of thousands
     * of empty vertical blocks into a low-orbit visual altitude. The mapping is
     * continuous and monotonic, so ascent never reverses or snaps.
     */
    public static double visualAltitudeFromPlanetBlocks(double altitudeBlocks) {
        double altitude = Math.max(0.0, altitudeBlocks);
        if (altitude <= LINEAR_ALTITUDE_LIMIT) {
            return altitude;
        }
        double logarithmic = LINEAR_ALTITUDE_LIMIT
                + LOG_ALTITUDE_SCALE * Math.log1p(
                (Math.min(altitude, FAR_ALTITUDE_START) - LINEAR_ALTITUDE_LIMIT)
                        / LOG_ALTITUDE_DIVISOR);
        if (altitude <= FAR_ALTITUDE_START) {
            return logarithmic;
        }
        return logarithmic + (altitude - FAR_ALTITUDE_START) / FAR_ALTITUDE_DIVISOR;
    }

    /** Camera used only by the LOD renderer; gameplay/collision coordinates stay untouched. */
    public static Vec3 overworldRenderCamera(Vec3 actualCamera, int seaLevel, float influence) {
        double altitude = Math.max(0.0, actualCamera.y - seaLevel);
        double visualY = seaLevel + visualAltitudeFromPlanetBlocks(altitude);
        double y = Mth.lerp(Mth.clamp(influence, 0.0f, 1.0f), actualCamera.y, visualY);
        return new Vec3(actualCamera.x, y, actualCamera.z);
    }

    /**
     * Re-expresses the simulated 1:16 orbital location in a 1:1 surface-detail
     * render frame. The direction to Earth is unchanged, while the cube edge
     * becomes the real cube-net face span and the altitude uses the same curve
     * as pre-crossing ascent.
     */
    public static OrbitFrame orbitFrame(Vector3d simulatedPlanetPosition,
                                        double physicalHalfExtent,
                                        CubeNetSurfaceTransform transform) {
        double distance = simulatedPlanetPosition.length();
        Vector3d direction = distance > 1.0E-9
                ? new Vector3d(simulatedPlanetPosition).div(distance)
                : new Vector3d(0.0, -1.0, 0.0);
        double simulatedAltitude = Math.max(0.0, distance - physicalHalfExtent);
        double altitudePlanetBlocks = simulatedAltitude
                / SpaceTravelManager.SPACE_BLOCKS_PER_PLANET_BLOCK;
        double visualHalfExtent = transform.faceHalfSpan();
        double visualAltitude = visualAltitudeFromPlanetBlocks(altitudePlanetBlocks);
        Vector3d visualPosition = direction.mul(visualHalfExtent + visualAltitude);
        return new OrbitFrame(visualPosition, visualHalfExtent, 1.0, visualAltitude);
    }

    public record OrbitFrame(Vector3d planetPosition,
                             double halfExtent,
                             double worldToRendered,
                             double altitude) {
    }
}
