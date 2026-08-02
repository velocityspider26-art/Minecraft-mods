package shipwrights.genesis.space.planet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import shipwrights.genesis.space.voxel.PlanetVoxelService;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides when a craft may actually be handed from the space dimension into the
 * planet dimension.
 *
 * <p>The rule is not "Y crossed a number". A crossing is allowed only once
 * everything the player would notice is already in place: the face and surface
 * coordinates are resolved, the destination chunks exist at FULL, and the voxel
 * bricks covering the landing column are in the pyramid so the terrain drawn
 * before the swap is the same terrain drawn after it.</p>
 *
 * <p>Every unmet condition is reported by name rather than collapsed into a
 * boolean, because "the crossing is waiting" and "the crossing is waiting on
 * chunk generation at 214, -3011" are very different things to debug.</p>
 */
public final class SeamlessPlanetHandoffController {

    /** How far around the landing column exact voxel data is required. */
    private static final int REQUIRED_BRICK_RADIUS = 1;

    /**
     * @param face          resolved destination face, null while unresolved
     * @param blockers      human-readable names of every unmet condition
     */
    public record Readiness(boolean ready,
                            CubeNetSurfaceTransform.Face face,
                            double surfaceX,
                            double surfaceZ,
                            boolean faceResolved,
                            boolean destinationResolved,
                            boolean chunksReady,
                            boolean voxelBricksReady,
                            List<String> blockers) {

        public String describe() {
            return ready ? "ready" : String.join(", ", blockers);
        }
    }

    private SeamlessPlanetHandoffController() {
    }

    /**
     * Evaluates the handoff conditions for one arrival.
     *
     * @param planetLevel the destination planet dimension
     * @param arrival     the arrival position in that dimension
     * @param projection  the destination's cube mapping
     * @param chunksReady result of the existing arrival-chunk gate
     */
    public static Readiness evaluate(ServerLevel planetLevel, Vec3 arrival,
                                     CubeSurfaceProjection projection, boolean chunksReady) {
        List<String> blockers = new ArrayList<>(4);
        if (planetLevel == null || arrival == null || projection == null) {
            blockers.add("destination unresolved");
            return new Readiness(false, null, 0.0, 0.0, false, false, chunksReady, false, blockers);
        }

        CubeSurfaceProjection.PlanetSurfacePosition target =
                projection.worldToFace(arrival.x, arrival.y, arrival.z);
        boolean faceResolved = projection.isInsideNet(arrival.x, arrival.z);
        if (!faceResolved) blockers.add("arrival outside the cube net");

        boolean destinationResolved = Double.isFinite(arrival.x) && Double.isFinite(arrival.y)
                && Double.isFinite(arrival.z);
        if (!destinationResolved) blockers.add("arrival coordinates not finite");
        if (!chunksReady) blockers.add("destination chunks still generating");

        boolean bricksReady = faceResolved && PlanetVoxelService.hasExactColumnCoverage(
                planetLevel, arrival.x, arrival.z, REQUIRED_BRICK_RADIUS);
        if (!bricksReady) blockers.add("landing-column voxel bricks not captured");

        boolean ready = faceResolved && destinationResolved && chunksReady && bricksReady;
        return new Readiness(ready, target.face(), target.surfaceX(), target.surfaceZ(),
                faceResolved, destinationResolved, chunksReady, bricksReady, blockers);
    }
}
