package shipwrights.genesis.space;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsContraptionLookup;
import shipwrights.genesis.compat.aeronautics.AeronauticsForceHandler;
import shipwrights.genesis.config.GenesisCommonConfig;

/**
 * Real orbital mechanics for Sable physics objects above the atmosphere.
 *
 * <p>Leaving Earth is seamless, so the craft never leaves the (flat) overworld — where gravity would
 * normally pull straight down and nothing could orbit. Above {@link #spaceGravityStart()} this handler
 * takes over for constructs: it treats the planet as a point mass at a fixed centre far below the world
 * and applies <b>radial</b> gravity {@code a = -GM/r²} toward it (cancelling the flat downward pull), so
 * a construct with enough sideways speed settles into a real, stable orbit — and one that's too slow
 * falls back into the atmosphere. Gravity blends in across a band above the start height so there's no
 * jolt at the boundary.</p>
 *
 * <p>Circular-orbit speed at radius {@code r} is {@code v = R·√(g0/r)}; with the defaults below that's
 * ~40–50 blocks/s just above the atmosphere, well within a powered CA vehicle's reach. All constants are
 * plain fields so the feel can be tuned against a live client.</p>
 */
public final class OrbitalGravityHandler {

    // --- tunables (blocks, seconds) ---
    /** Fixed planet centre in the overworld: directly under the origin, one planet-radius below the surface. */
    private static final double CENTER_X = 0.0;
    private static final double CENTER_Z = 0.0;
    private static final double SURFACE_Y = 64.0;
    /** Planet radius used purely for the gravity field (not the visual size). */
    private static final double PLANET_RADIUS = 6000.0;
    /** Surface gravity magnitude of the orbital field (blocks/s²) — gentle so orbits are controllable. */
    private static final double SURFACE_GRAVITY = 0.5;
    /** Fallback for Sable's straight-down gravity (blocks/s²) if its physics config can't be read. */
    private static final double AMBIENT_GRAVITY_FALLBACK = 11.0;
    /** Band (blocks) over which radial gravity fades in above the start height. */
    private static final double BLEND_BAND = 600.0;

    private static final double DT = 1.0 / 20.0;
    private static final double CENTER_Y = SURFACE_Y - PLANET_RADIUS;
    private static final double GM = SURFACE_GRAVITY * PLANET_RADIUS * PLANET_RADIUS;

    private final boolean gameTest;

    public OrbitalGravityHandler(boolean gameTest) {
        this.gameTest = gameTest;
    }

    /** Altitude (blocks) at which orbital gravity begins to replace the flat downward pull. */
    public static int spaceGravityStart() {
        return GenesisCommonConfig.getAtmosphereExitHeight();
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        // Only the home planet's own (flat) space needs the fake radial field; the scaled space
        // dimensions already live in real celestial coordinates.
        if (GenesisMod.isSpaceDimension(level) || GenesisMod.isSubSpaceDimension(level)) return;
        if (GenesisMod.getCelestialForLevel(level) == null) return;
        if (!gameTest && level.players().isEmpty()) return;

        double start = spaceGravityStart();

        for (AeronauticsConstruct construct : AeronauticsContraptionLookup.getSortedConstructs(level)) {
            if (construct.isRemoved()) continue;
            Vector3dc pos = construct.positionInWorld();
            if (pos.y() <= start) continue;

            // Vector from the planet centre out to the craft.
            Vector3d rVec = new Vector3d(pos.x() - CENTER_X, pos.y() - CENTER_Y, pos.z() - CENTER_Z);
            double r = rVec.length();
            if (r < 1.0) continue;

            // Blend factor: 0 at the start height, 1 once fully in space.
            double blend = Math.min(1.0, (pos.y() - start) / BLEND_BAND);

            // Radial gravity toward the centre, replacing the flat downward pull (both faded by blend).
            double gMag = GM / (r * r);
            Vector3d accel = new Vector3d(rVec).div(r).mul(-gMag * blend);   // toward centre
            // Cancel Sable's *actual* ambient gravity (read from its dimension physics; the previous
            // hard-coded 9.81 under-cancelled the real -11 and left craft slowly falling out of orbit).
            Vector3d ambient = ambientGravity(level, pos);
            accel.sub(ambient.mul(blend));

            Vector3d deltaV = accel.mul(DT);
            AeronauticsForceHandler.addVelocity(construct, deltaV, new Vector3d());
        }
    }

    /** Sable's own gravity vector at a point (blocks/s²), with a safe fallback if its API isn't reachable. */
    private static Vector3d ambientGravity(ServerLevel level, Vector3dc pos) {
        try {
            return new Vector3d(dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData
                    .getGravity(level, new Vector3d(pos)));
        } catch (Throwable t) {
            return new Vector3d(0.0, -AMBIENT_GRAVITY_FALLBACK, 0.0);
        }
    }

    /** World-space gravity centre, for any renderer/UI that wants to point "down" at the planet. */
    public static Vec3 planetCenter() {
        return new Vec3(CENTER_X, CENTER_Y, CENTER_Z);
    }
}
