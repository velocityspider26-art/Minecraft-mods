package shipwrights.genesis.space.renderer;

import net.minecraft.core.Registry;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.client.PlanetSurfaceTextures;
import shipwrights.genesis.space.surface.PlanetSurfaceData;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;
import shipwrights.genesis.teleportation.SpaceTravelManager;

public final class CelestialRenderCoordinates {
    private static final double SURFACE_WORLD_TO_CELESTIAL_SCALE = 1.0 / 16.0;
    private static final double MAX_PLANET_RENDER_DISTANCE = 384.0;

    private CelestialRenderCoordinates() {
    }

    public static RenderTransform getRenderTransform(Celestial celestial, VantagePoint vantagePoint, Vec3 cameraPosition, long ticks, float partialTick, Registry<Celestial> registry) {
        Vector3d observerPosition = getObserverCelestialPosition(vantagePoint, cameraPosition);
        Quaterniond observerRotation = getObserverCelestialRotation(vantagePoint);
        Quaterniond inverseObserverRotation = new Quaterniond(observerRotation).conjugate();

        Vector3d renderPosition = new Vector3d(celestial.getPosition(ticks, partialTick, registry)).sub(observerPosition);
        inverseObserverRotation.transform(renderPosition);

        Quaterniond renderRotation = new Quaterniond(inverseObserverRotation)
                .mul(new Quaterniond(celestial.getRotation(ticks, partialTick, registry)));

        return new RenderTransform(renderPosition, renderRotation);
    }

    public static Vector3d getObserverCelestialPosition(VantagePoint vantagePoint, Vec3 cameraPosition) {
        if (vantagePoint instanceof VantagePoint.OnCelestial onCelestial) {
            Quaterniond surfaceRotation = new Quaterniond(onCelestial.getRotation());
            Vector3d observerPosition = new Vector3d(onCelestial.getPosition());

            Vector3d up = rotatedAxis(surfaceRotation, 0.0, 1.0, 0.0);
            Vector3d east = rotatedAxis(surfaceRotation, 1.0, 0.0, 0.0);
            Vector3d south = rotatedAxis(surfaceRotation, 0.0, 0.0, 1.0);

            CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
                    onCelestial.celestial().getActualSize(),
                    SpaceTravelManager.entryPadding(onCelestial.celestial().getActualSize()));
            CubeNetSurfaceTransform.Face face = transform.faceContaining(cameraPosition.x, cameraPosition.z);
            if (face == null) {
                face = CubeNetSurfaceTransform.Face.UP;
            }
            double localX = cameraPosition.x - transform.faceCenterX(face);
            double localZ = cameraPosition.z - transform.faceCenterZ(face);
            var celestialKey = onCelestial.registry().getResourceKey(onCelestial.celestial());
            PlanetSurfaceData surface = celestialKey
                    .map(key -> PlanetSurfaceTextures.surface(key.location()))
                    .orElse(null);
            double surfaceBaseY = surface != null ? surface.seaLevel() : 0.0;
            double altitude = (cameraPosition.y - surfaceBaseY) * SURFACE_WORLD_TO_CELESTIAL_SCALE;
            double surfaceRadius = onCelestial.celestial().getActualSize() / 2.0;

            observerPosition.fma(surfaceRadius + altitude, up);
            observerPosition.fma(localX * SURFACE_WORLD_TO_CELESTIAL_SCALE, east);
            observerPosition.fma(localZ * SURFACE_WORLD_TO_CELESTIAL_SCALE, south);
            return observerPosition;
        }

        return new Vector3d(vantagePoint.getPosition());
    }

    public static Quaterniond getObserverCelestialRotation(VantagePoint vantagePoint) {
        return new Quaterniond(vantagePoint.getRotation());
    }

    public static double getVisualHalfExtent(Celestial celestial) {
        return celestial.getActualSize() * 0.5 * GenesisClientConfig.getCelestialVisualScale();
    }

    /** Depth spread given to clamped bodies so distant ones stay distinct in depth. */
    private static final double RENDER_DISTANCE_SPREAD = 48.0;

    public static double getPlanetRenderDistanceScale(Vector3dc renderPosition) {
        double distance = renderPosition.length();
        if (distance <= MAX_PLANET_RENDER_DISTANCE || distance < 1.0E-6) {
            return 1.0;
        }

        // Map far bodies into [MAX, MAX+SPREAD] preserving their distance order,
        // instead of clamping them all to exactly MAX. Two bodies clamped to the
        // same distance collide in the depth buffer and z-fight — which is why
        // the sun bled through a planet sitting in front of it. Spreading them
        // keeps the nearer body's depth in front, so it occludes the farther.
        // Angular size is unchanged: both position and size scale by the result.
        double renderDistance = MAX_PLANET_RENDER_DISTANCE
                + RENDER_DISTANCE_SPREAD * (1.0 - MAX_PLANET_RENDER_DISTANCE / distance);
        return renderDistance / distance;
    }

    public static Vector3d scaleRenderPosition(Vector3dc renderPosition, double scale) {
        return new Vector3d(renderPosition).mul(scale);
    }

    public static float getCurrentPlanetAlpha(Vec3 cameraPosition) {
        // The detached current-planet renderer must not appear while the player is still in the ordinary
        // atmosphere. Earth now uses WorldLodEarthRenderer instead; this remains for other bodies. It begins emerging at 16,000 blocks by default and
        // reaches full opacity over the next 1,000 blocks, before the 20,000
        // block atmosphere exit. The reveal height is client-configurable.
        float fadeStart = GenesisClientConfig.getCurrentPlanetRevealHeight();
        float boundary = GenesisCommonConfig.getAtmosphereExitHeight();
        float fadeEnd = Math.min(boundary, fadeStart + 1000.0f);
        if (fadeEnd <= fadeStart) {
            return cameraPosition.y >= fadeStart ? 1.0f : 0.0f;
        }

        float alpha = (float) ((cameraPosition.y - fadeStart) / (fadeEnd - fadeStart));
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        // Smoothstep removes the visible speed change at both ends of the fade.
        return alpha * alpha * (3.0f - 2.0f * alpha);
    }

    private static Vector3d rotatedAxis(Quaterniondc rotation, double x, double y, double z) {
        Vector3d axis = new Vector3d(x, y, z);
        rotation.transform(axis);
        return axis;
    }

    public record RenderTransform(Vector3dc position, Quaterniondc rotation) {
    }
}
