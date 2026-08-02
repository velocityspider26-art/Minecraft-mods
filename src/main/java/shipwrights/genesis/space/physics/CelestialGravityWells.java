package shipwrights.genesis.space.physics;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.SpaceLevel;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The custom gravity layer for the space dimension, built on Sable's rigid
 * body API (no engine fork needed):
 *
 * <ul>
 *   <li><b>Gravity wells</b> — every celestial with a {@code gravity} value pulls
 *       ships and free-floating players toward it, falling off with the square
 *       of distance from its surface. The base dimension gravity is zero (see
 *       {@code data/genesis/dimension_physics/}), so all gravity in space comes
 *       from these wells.</li>
 *   <li><b>Orbit assist</b> — a ship moving roughly tangentially at near-orbital
 *       speed has its radial drift damped, so imperfect manual orbits settle
 *       into stable ones instead of decaying.</li>
 *   <li><b>Station anchoring</b> — a ship that comes to rest inside a well is
 *       pinned to its offset from the celestial and follows it along its orbit,
 *       so parked stations drift with their planet instead of falling in.</li>
 * </ul>
 *
 * Celestial positions live in the y=0 plane of the space dimension
 * ({@link SpaceLevel#toCelestialSpace}), so well pulls are horizontal.
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class CelestialGravityWells {
    /**
     * Surface acceleration of a gravity-1.0 body, in m/s^2. Deliberately far
     * below real physics: our distances are compressed ~100x, so honest
     * inverse-square from a 9.8-based surface value turns every star into a
     * black hole. Wells should read as a drift you fight with thrusters, not
     * an event horizon.
     */
    private static final double SURFACE_ACCEL_SCALE = 3.0;
    /** Pulls weaker than this are ignored (keeps far space truly weightless). */
    private static final double MIN_ACCEL = 0.02;
    private static final double MAX_ACCEL = 4.0;
    private static final double TICK_SECONDS = 0.05;

    /** Ships slower than this (m/s) inside a well get anchored in place. */
    private static final double ANCHOR_SPEED = 1.0;
    /** An anchored ship dragged this far off its mark is released (was pushed). */
    private static final double ANCHOR_BREAK_DISTANCE = 12.0;

    /** Orbit assist engages between these fractions of circular orbit speed. */
    private static final double ASSIST_MIN_FRACTION = 0.45;
    private static final double ASSIST_MAX_FRACTION = 1.75;
    private static final double RADIAL_DAMPING = 0.06;
    private static final double TANGENTIAL_CORRECTION = 0.018;

    private static final Map<ServerSubLevel, Anchor> ANCHORS = new WeakHashMap<>();

    private CelestialGravityWells() {
    }

    private record Anchor(Celestial celestial, Vector3d offset) {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !GenesisMod.isSpaceDimension(level)) {
            return;
        }

        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        long ticks = GenesisMod.getTicks(level);

        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null) {
            for (ServerSubLevel ship : container.getAllSubLevels()) {
                tickShip(ship, registry, ticks);
            }
        }

        for (ServerPlayer player : level.players()) {
            tickFreePlayer(player, registry, ticks);
        }
    }

    private static void tickShip(ServerSubLevel ship, Registry<Celestial> registry, long ticks) {
        RigidBodyHandle handle = RigidBodyHandle.of(ship);
        if (handle == null || !handle.isValid()) {
            return;
        }

        Vector3dc shipPos = ship.logicalPose().position();
        Well well = dominantWell(registry, ticks, shipPos.x(), shipPos.y(), shipPos.z());
        if (well == null) {
            ANCHORS.remove(ship);
            return;
        }

        Vector3d velocity = handle.getLinearVelocity(new Vector3d());
        Vector3d bodyVelocity = wellDriftVelocity(well.celestial(), registry, ticks);
        Vector3d relativeVelocity = new Vector3d(velocity).sub(bodyVelocity);

        Anchor anchor = ANCHORS.get(ship);
        if (anchor != null && anchor.celestial() == well.celestial()) {
            if (tickAnchored(ship, handle, anchor, velocity, registry, ticks, shipPos)) {
                return;
            }
            ANCHORS.remove(ship);
        }

        // Newly at rest inside the well? Anchor instead of letting it fall.
        if (relativeVelocity.length() < ANCHOR_SPEED) {
            Vector3dc bodyPos = well.celestial().getPosition(ticks, registry);
            Vector3d offset = new Vector3d(shipPos.x() - bodyPos.x(), shipPos.y(), shipPos.z() - bodyPos.z());
            ANCHORS.put(ship, new Anchor(well.celestial(), offset));
            return;
        }

        // Gravity pull: sum every body's contribution (multi-body / N-body),
        // not just the dominant one, so a ship between the Earth and Moon feels
        // both — more physically honest than snapping to one attractor.
        Gravity net = netGravity(registry, ticks, shipPos.x(), shipPos.y(), shipPos.z());
        Vector3d dv = new Vector3d(net.ax(), net.ay(), net.az()).mul(TICK_SECONDS);

        // Orbit assist (about the dominant body): damp radial drift when moving
        // near circular-orbit speed so hand-flown orbits stay stable.
        // Worked in full 3D so the assist supports INCLINED orbits — including
        // polar ones — rather than only orbits lying in the celestial plane.
        // The orbital plane is whatever plane the craft's own radius and
        // velocity span, so the tangent is simply the velocity with its radial
        // component removed.
        Vector3d toBody = new Vector3d(well.dirX(), well.dirY(), well.dirZ());
        double vRadial = relativeVelocity.dot(toBody);
        Vector3d tangential = new Vector3d(relativeVelocity).fma(-vRadial, toBody);
        double vTangential = tangential.length();
        double orbitalSpeed = Math.sqrt(well.accel() * well.distance());
        if (vTangential > orbitalSpeed * ASSIST_MIN_FRACTION && vTangential < orbitalSpeed * ASSIST_MAX_FRACTION) {
            // Damp drift along the radius so a hand-flown orbit stops decaying.
            dv.fma(-vRadial * RADIAL_DAMPING, toBody);

            // Gently converge on circular-orbit speed while keeping the heading
            // the pilot chose — evaluated relative to the moving planet, so a
            // craft follows an orbiting parent instead of being left behind.
            Vector3d tangentDirection = new Vector3d(tangential).div(vTangential);
            double tangentialError = orbitalSpeed - vTangential;
            dv.fma(tangentialError * TANGENTIAL_CORRECTION, tangentDirection);
        }

        // Slight angular damping so ships in vacuum stay controllable.
        Vector3d angular = handle.getAngularVelocity(new Vector3d()).mul(-0.005);
        handle.addLinearAndAngularVelocity(dv, angular);
    }

    /** @return true while the anchor holds; false if it should be released. */
    private static boolean tickAnchored(ServerSubLevel ship, RigidBodyHandle handle, Anchor anchor,
                                        Vector3d velocity, Registry<Celestial> registry, long ticks, Vector3dc shipPos) {
        Vector3dc bodyPos = anchor.celestial().getPosition(ticks, registry);
        Vector3d target = new Vector3d(bodyPos.x() + anchor.offset().x(), anchor.offset().y(), bodyPos.z() + anchor.offset().z());
        Vector3d error = target.sub(shipPos.x(), shipPos.y(), shipPos.z(), new Vector3d());

        if (error.length() > ANCHOR_BREAK_DISTANCE) {
            return false;
        }

        // Follow the celestial's drift plus a gentle correction toward the mark
        // (1 m/s per meter of error, capped), replacing residual velocity.
        Vector3d correction = error.mul(1.0, new Vector3d());
        if (correction.length() > 4.0) {
            correction.normalize(4.0);
        }
        Vector3d follow = wellDriftVelocity(anchor.celestial(), registry, ticks).add(correction);
        handle.addLinearAndAngularVelocity(
                follow.sub(velocity),
                handle.getAngularVelocity(new Vector3d()).mul(-0.10)
        );
        return true;
    }

    /** The celestial's own velocity along its orbit, in m/s. */
    private static Vector3d wellDriftVelocity(Celestial celestial, Registry<Celestial> registry, long ticks) {
        Vector3dc now = celestial.getPosition(ticks, registry);
        Vector3dc prev = celestial.getPosition(ticks - 1, registry);
        return new Vector3d(now.x() - prev.x(), 0.0, now.z() - prev.z()).mul(1.0 / TICK_SECONDS);
    }

    private record Well(Celestial celestial, double accel, double distance,
                        double dirX, double dirY, double dirZ) {
    }

    /** Gravitational acceleration in full 3D, so orbits can be inclined. */
    private record Gravity(double ax, double ay, double az) {
    }

    /** Summed gravitational acceleration from ALL celestials at a point (N-body). */
    private static Gravity netGravity(Registry<Celestial> registry, long ticks, double x, double y, double z) {
        double ax = 0.0;
        double ay = 0.0;
        double az = 0.0;
        for (Celestial celestial : registry) {
            if (celestial.gravity() <= 0.0) {
                continue;
            }
            Vector3dc bodyPos = celestial.getPosition(ticks, registry);
            double dx = bodyPos.x() - x;
            double dy = bodyPos.y() - y;
            double dz = bodyPos.z() - z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double radius = celestial.getActualSize() / 2.0;
            if (distance < radius * 1.02 || distance < 1.0E-3) {
                continue;
            }
            double accel = Math.min(MAX_ACCEL,
                    celestial.gravity() * SURFACE_ACCEL_SCALE * (radius * radius) / (distance * distance));
            if (accel < MIN_ACCEL) {
                continue;
            }
            ax += accel * dx / distance;
            ay += accel * dy / distance;
            az += accel * dz / distance;
        }
        return new Gravity(ax, ay, az);
    }

    /** Strongest gravity well acting at the given celestial-plane position, if any. */
    /** How a craft's current trajectory relates to the body it is near. */
    public enum TrajectoryClass {
        /** Bound, and the low point dips into the body — it will hit. */
        SUBORBITAL,
        /** Bound and clear of the surface: a real orbit. */
        ELLIPTICAL,
        /** Bound, and near enough to circular to count as one. */
        CIRCULAR,
        /** At or past escape speed — leaving for good. */
        ESCAPE
    }

    /**
     * A full solution of the craft's two-body state, from the vis-viva equation
     * and the specific orbital energy.
     *
     * @param body            the dominating body
     * @param distance        current distance from its centre
     * @param speed           current speed
     * @param circularSpeed   speed needed to hold a circular orbit here
     * @param escapeSpeed     speed needed to leave for good: sqrt(2) x circular
     * @param apoapsis        highest point of the resulting orbit (NaN if escaping)
     * @param periapsis       lowest point of the resulting orbit (NaN if escaping)
     * @param eccentricity    shape of the resulting orbit (>= 1 means unbound)
     * @param trajectory      what kind of path the craft is actually on
     */
    public record OrbitalState(Celestial body, double distance, double speed,
                               double circularSpeed, double escapeSpeed,
                               double apoapsis, double periapsis,
                               double eccentricity, TrajectoryClass trajectory) {
    }

    /**
     * Solves the craft's orbit the way it is actually done: derive the standard
     * gravitational parameter from the local pull, then read the trajectory out
     * of the specific orbital energy and angular momentum.
     *
     * @return the orbital state, or null if nothing meaningful dominates here
     */
    @Nullable
    public static OrbitalState orbitalState(Level level, Vector3dc position, Vector3dc velocity) {
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        long ticks = GenesisMod.getTicks(level);
        Well well = dominantWell(registry, ticks, position.x(), position.y(), position.z());
        if (well == null) {
            return null;
        }

        double radius = well.distance();
        // mu = a * r^2 recovers the gravitational parameter from the local pull.
        double mu = well.accel() * radius * radius;

        // Work relative to the body: an orbit is about relative motion. Full 3D,
        // so an inclined or polar orbit reads correctly instead of being
        // measured as if it lay in the celestial plane.
        Vector3d bodyDrift = wellDriftVelocity(well.celestial(), registry, ticks);
        double vx = velocity.x() - bodyDrift.x;
        double vy = velocity.y() - bodyDrift.y;
        double vz = velocity.z() - bodyDrift.z;
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);

        double circularSpeed = Math.sqrt(mu / radius);
        double escapeSpeed = Math.sqrt(2.0 * mu / radius);

        // Specific orbital energy: negative is bound, zero or positive escapes.
        double energy = speed * speed * 0.5 - mu / radius;

        // Specific angular momentum h = r x v, taken in 3D; its magnitude is
        // what the conic needs, whatever plane the orbit lies in.
        Vector3d r = new Vector3d(-well.dirX(), -well.dirY(), -well.dirZ()).mul(radius);
        Vector3d v = new Vector3d(vx, vy, vz);
        double angularMomentum = new Vector3d(r).cross(v).length();

        double apoapsis = Double.NaN;
        double periapsis = Double.NaN;
        double eccentricity;
        TrajectoryClass trajectory;

        if (energy >= -1.0E-9) {
            eccentricity = 1.0;
            trajectory = TrajectoryClass.ESCAPE;
        } else {
            double semiMajorAxis = -mu / (2.0 * energy);
            double eSquared = 1.0 + (2.0 * energy * angularMomentum * angularMomentum) / (mu * mu);
            eccentricity = Math.sqrt(Math.max(0.0, eSquared));
            apoapsis = semiMajorAxis * (1.0 + eccentricity);
            periapsis = semiMajorAxis * (1.0 - eccentricity);

            double surface = well.celestial().getActualSize() / 2.0;
            if (periapsis <= surface) {
                trajectory = TrajectoryClass.SUBORBITAL;
            } else if (eccentricity < 0.05) {
                trajectory = TrajectoryClass.CIRCULAR;
            } else {
                trajectory = TrajectoryClass.ELLIPTICAL;
            }
        }

        return new OrbitalState(well.celestial(), radius, speed, circularSpeed, escapeSpeed,
                apoapsis, periapsis, eccentricity, trajectory);
    }

    /**
     * A craft's future path, solved as a conic section rather than stepped
     * forward — so it is exact, and costs the same whether the orbit takes a
     * minute or a week.
     *
     * @param body      the body the path is around
     * @param points    the path in the celestial plane, relative to that body's
     *                  centre, ordered along the direction of travel
     * @param closed    true for a bound orbit (join the ends), false if escaping
     * @param periapsis lowest point of the path, relative to the body
     * @param apoapsis  highest point, relative to the body; null when escaping
     */
    public record PredictedPath(Celestial body, java.util.List<Vector3d> points, boolean closed,
                                Vector3d periapsis, @Nullable Vector3d apoapsis) {
    }

    /** How far past the craft's current radius an escape path is drawn. */
    private static final double ESCAPE_PATH_REACH = 5.0;

    /**
     * Solves the craft's trajectory into a drawable path.
     *
     * <p>The orbit is recovered the textbook way: specific angular momentum
     * {@code h = r x v} fixes the semi-latus rectum, and the eccentricity
     * vector — which points at periapsis — fixes the shape and where the low
     * point sits. Those give the conic {@code r(θ) = p / (1 + e·cos θ)}
     * directly, so the whole orbit is known from one instant of position and
     * velocity, exactly as it is for a real spacecraft.</p>
     *
     * @param samples how many points to sample along the path
     * @return the path, or null if nothing dominates here or the craft is
     *         falling straight down (no orbit to speak of)
     */
    @Nullable
    public static PredictedPath predictedPath(Level level, Vector3dc position, Vector3dc velocity, int samples) {
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        long ticks = GenesisMod.getTicks(level);
        Well well = dominantWell(registry, ticks, position.x(), position.y(), position.z());
        if (well == null) {
            return null;
        }

        double radius = well.distance();
        double mu = well.accel() * radius * radius;

        Vector3d bodyDrift = wellDriftVelocity(well.celestial(), registry, ticks);
        Vector3d v = new Vector3d(velocity.x() - bodyDrift.x,
                velocity.y() - bodyDrift.y,
                velocity.z() - bodyDrift.z);

        // Radial vector from the body out to the craft (well.dir points inward).
        Vector3d r = new Vector3d(-well.dirX(), -well.dirY(), -well.dirZ()).mul(radius);

        // The orbit lies in the plane spanned by r and v; its normal is h.
        Vector3d h = new Vector3d(r).cross(v);
        if (h.length() < 1.0E-6) {
            return null; // purely radial: a straight line, not an orbit
        }

        double speedSquared = v.lengthSquared();
        double radialDotVelocity = r.dot(v);

        // Eccentricity vector: magnitude is the eccentricity, direction points
        // from the body toward periapsis.
        Vector3d e = new Vector3d(r).mul(speedSquared - mu / radius)
                .fma(-radialDotVelocity, v).div(mu);
        double eccentricity = e.length();

        double angularMomentum = h.length();
        double semiLatusRectum = angularMomentum * angularMomentum / mu;

        // Reference direction for the true anomaly. A perfectly circular orbit
        // has no periapsis, so measure from where the craft is instead.
        Vector3d periapsisDirection = eccentricity < 1.0E-6
                ? new Vector3d(r).div(radius)
                : new Vector3d(e).div(eccentricity);
        // In-plane axis perpendicular to periapsis, in the direction of travel,
        // so sweeping the true anomaly traces the orbit the way the craft flies.
        Vector3d planeNormal = new Vector3d(h).normalize();
        Vector3d perpendicular = new Vector3d(planeNormal).cross(periapsisDirection).normalize();
        boolean closed = eccentricity < 1.0 - 1.0E-6;
        // An open path has an asymptote; stop just short of it, and again once
        // it runs far enough out to have clearly left.
        double sweep = closed
                ? Math.PI
                : Math.acos(-1.0 / Math.max(eccentricity, 1.0 + 1.0E-9)) * 0.985;
        double reach = radius * ESCAPE_PATH_REACH;

        java.util.List<Vector3d> points = new java.util.ArrayList<>(samples + 1);
        for (int sample = 0; sample <= samples; sample++) {
            double trueAnomaly = -sweep + 2.0 * sweep * sample / samples;
            double denominator = 1.0 + eccentricity * Math.cos(trueAnomaly);
            if (denominator <= 1.0E-9) {
                continue;
            }
            double radiusAt = semiLatusRectum / denominator;
            if (!closed && radiusAt > reach) {
                continue;
            }
            points.add(conicPoint(periapsisDirection, perpendicular, trueAnomaly, radiusAt));
        }
        if (points.size() < 2) {
            return null;
        }

        Vector3d periapsis = conicPoint(periapsisDirection, perpendicular, 0.0,
                semiLatusRectum / (1.0 + eccentricity));
        Vector3d apoapsis = closed
                ? conicPoint(periapsisDirection, perpendicular, Math.PI,
                        semiLatusRectum / (1.0 - eccentricity))
                : null;

        return new PredictedPath(well.celestial(), points, closed, periapsis, apoapsis);
    }

    /**
     * A point on the conic at {@code trueAnomaly}, built from the orbit's own
     * in-plane axes so the path follows the real (possibly inclined) orbital
     * plane rather than being flattened onto the celestial plane.
     */
    private static Vector3d conicPoint(Vector3dc periapsisDirection, Vector3dc perpendicular,
                                       double trueAnomaly, double length) {
        return new Vector3d(periapsisDirection).mul(Math.cos(trueAnomaly) * length)
                .fma(Math.sin(trueAnomaly) * length, perpendicular);
    }

    /**
     * The velocity a craft at {@code position} would need to hold a circular
     * orbit around whichever body dominates there — the speed that exactly
     * balances gravity, {@code sqrt(a·r)}, aimed along the tangent and carried
     * along by the body's own motion so the orbit follows a moving planet.
     *
     * <p>Handed the craft's current velocity so the orbit keeps the direction
     * it is already travelling rather than snapping to an arbitrary one.</p>
     *
     * @return the target velocity, or null if no body has a meaningful pull here
     */
    @Nullable
    public static Vector3d circularOrbitVelocity(Level level, Vector3dc position, Vector3dc currentVelocity) {
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        long ticks = GenesisMod.getTicks(level);
        Well well = dominantWell(registry, ticks, position.x(), position.y(), position.z());
        if (well == null) {
            return null;
        }

        // Tangent is perpendicular to the radial direction; pick the sense that
        // matches how the craft is already moving.
        double tangentX = -well.dirZ();
        double tangentZ = well.dirX();
        double along = currentVelocity.x() * tangentX + currentVelocity.z() * tangentZ;
        if (along < 0.0) {
            tangentX = -tangentX;
            tangentZ = -tangentZ;
        }

        double orbitalSpeed = Math.sqrt(well.accel() * well.distance());
        Vector3d bodyDrift = wellDriftVelocity(well.celestial(), registry, ticks);
        return new Vector3d(
                bodyDrift.x + tangentX * orbitalSpeed,
                0.0,
                bodyDrift.z + tangentZ * orbitalSpeed);
    }

    private static Well dominantWell(Registry<Celestial> registry, long ticks, double x, double y, double z) {
        Well best = null;
        for (Celestial celestial : registry) {
            if (celestial.gravity() <= 0.0) {
                continue;
            }

            Vector3dc bodyPos = celestial.getPosition(ticks, registry);
            double dx = bodyPos.x() - x;
            double dy = bodyPos.y() - y;
            double dz = bodyPos.z() - z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double radius = celestial.getActualSize() / 2.0;
            if (distance < radius * 1.02 || distance < 1.0E-3) {
                continue; // inside the body: the entry teleporter takes over
            }

            double accel = Math.min(MAX_ACCEL,
                    celestial.gravity() * SURFACE_ACCEL_SCALE * (radius * radius) / (distance * distance));
            if (accel < MIN_ACCEL) {
                continue;
            }

            if (best == null || accel > best.accel()) {
                best = new Well(celestial, accel, distance, dx / distance, dy / distance, dz / distance);
            }
        }
        return best;
    }

    private static void tickFreePlayer(ServerPlayer player, Registry<Celestial> registry, long ticks) {
        if (player.isSpectator() || player.getAbilities().flying || player.getRootVehicle() != player) {
            return;
        }

        Vector3d position = SpaceLevel.toCelestialSpace(player.level(), player.position());
        Gravity net = netGravity(registry, ticks, position.x(), position.y(), position.z());
        if (net.ax() == 0.0 && net.ay() == 0.0 && net.az() == 0.0) {
            return;
        }

        // Entity velocities are blocks/tick: dv = a * dt^2.
        Vec3 delta = player.getDeltaMovement().add(
                net.ax() * TICK_SECONDS * TICK_SECONDS,
                net.ay() * TICK_SECONDS * TICK_SECONDS,
                net.az() * TICK_SECONDS * TICK_SECONDS);
        player.setDeltaMovement(delta);
        player.resetFallDistance();
    }
}
