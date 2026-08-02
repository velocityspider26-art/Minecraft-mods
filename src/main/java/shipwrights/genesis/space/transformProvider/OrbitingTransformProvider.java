package shipwrights.genesis.space.transformProvider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import shipwrights.genesis.space.Celestial;

import java.util.Optional;
import java.util.Random;

/**
 * Transform provider that simulates orbital mechanics for celestial bodies.
 * <br>
 * Uses the seed parameter to generate random but deterministic values for
 * orbital angles and base rotation (matching OrbitingBody behavior).
 */
public class OrbitingTransformProvider implements CelestialTransformProvider {
    public static final ResourceLocation TYPE = ResourceLocation.parse("genesis:orbiting");

    private final ResourceLocation parentID;
    private final int seed;

    // Configurable orbital parameters (from OrbitingBody)
    private final double orbitDistance;
    private final double orbitTime;
    private final double dayLength;
    /** Explicit starting angle on the orbit (degrees); empty = derived from seed. */
    private final Optional<Double> phaseDegrees;
    private final Optional<Double> inclinationDegrees;
    private final Optional<Double> ascendingNodeDegrees;
    /** Orbit shape: 0 is a circle, approaching 1 is an increasingly elongated ellipse. */
    private final Optional<Double> eccentricity;
    /** Where periapsis sits within the orbital plane (degrees). */
    private final Optional<Double> argumentOfPeriapsisDegrees;
    /**
     * Ticks for periapsis to swing once around the orbit (apsidal precession).
     * Earth's Moon takes 8.85 years; Mercury's is the famous relativistic one.
     * Negative runs it backwards; empty or zero holds periapsis fixed.
     */
    private final Optional<Double> apsidalPrecessionTicks;
    /**
     * Ticks for the ascending node to travel once around (nodal precession).
     * The Moon's nodes regress over 18.6 years, which is what paces eclipse
     * seasons. Negative regresses; empty or zero holds the node fixed.
     */
    private final Optional<Double> nodalPrecessionTicks;
    /**
     * Fraction of the semi-major axis lost per completed orbit. Positive
     * spirals inward (Phobos toward Mars), negative spirals outward (our Moon
     * receding). Empty or zero keeps the orbit stable.
     */
    private final Optional<Double> orbitDecayPerOrbit;
    /**
     * Tilt of the spin axis away from the orbital normal (degrees) — obliquity.
     * Earth's 23.44 degrees is what gives it seasons; Uranus is on its side at 97.
     */
    private final Optional<Double> axialTiltDegrees;

    // Random parameters derived from seed
    private final double orbitalTheta;
    private final double inclinationRadians;
    private final double ascendingNodeRadians;
    private final double eccentricityValue;
    private final double argumentOfPeriapsisRadians;
    private final double apsidalPrecessionRate;
    private final double nodalPrecessionRate;
    private final double decayPerOrbit;
    private final double axialTiltRadians;
    private final Quaterniondc baseRotation;

    /** Keeps a decaying orbit from collapsing through zero and breaking the math. */
    private static final double MIN_DECAY_FACTOR = 0.05;
    private static final double MAX_GROWTH_FACTOR = 20.0;

    /** Newton-Raphson limits for inverting Kepler's equation. */
    private static final int KEPLER_MAX_ITERATIONS = 8;
    private static final double KEPLER_TOLERANCE = 1.0E-10;
    /** Beyond this the solver needs far more care than a playable orbit warrants. */
    private static final double MAX_ECCENTRICITY = 0.95;

    /**
     * Creates an orbiting transform provider with specified orbital parameters.
     * The seed is used to generate random orbital angles and rotation.
     *
     * @param parentID the parent celestial body to orbit around
     * @param seed the seed for generating deterministic random orbital angles and rotation
     * @param orbitDistance the orbit radius in blocks
     * @param orbitTime the orbit period in ticks
     * @param dayLength the day length in ticks
     */
    public OrbitingTransformProvider(ResourceLocation parentID, int seed, double orbitDistance,
                                     double orbitTime, double dayLength,
                                     Optional<Double> phaseDegrees,
                                     Optional<Double> inclinationDegrees,
                                     Optional<Double> ascendingNodeDegrees,
                                     Optional<Double> eccentricity,
                                     Optional<Double> argumentOfPeriapsisDegrees,
                                     Optional<Double> apsidalPrecessionTicks,
                                     Optional<Double> nodalPrecessionTicks,
                                     Optional<Double> orbitDecayPerOrbit,
                                     Optional<Double> axialTiltDegrees) {
        if (!Double.isFinite(orbitDistance) || orbitDistance <= 0.0) {
            throw new IllegalArgumentException("orbitDistance must be finite and greater than zero");
        }
        if (!Double.isFinite(orbitTime) || orbitTime <= 0.0) {
            throw new IllegalArgumentException("orbitTime must be finite and greater than zero");
        }
        if (!Double.isFinite(dayLength) || dayLength < 0.0) {
            throw new IllegalArgumentException("dayLength must be finite and non-negative");
        }
        double requestedEccentricity = eccentricity.orElse(0.0);
        if (!Double.isFinite(requestedEccentricity)
                || requestedEccentricity < 0.0 || requestedEccentricity > MAX_ECCENTRICITY) {
            throw new IllegalArgumentException(
                    "eccentricity must be finite and within [0, " + MAX_ECCENTRICITY + "]");
        }

        this.parentID = parentID;
        this.seed = seed;
        this.orbitDistance = orbitDistance;
        this.orbitTime = orbitTime;
        this.dayLength = dayLength;
        this.phaseDegrees = phaseDegrees;
        this.inclinationDegrees = inclinationDegrees;
        this.ascendingNodeDegrees = ascendingNodeDegrees;
        this.eccentricity = eccentricity;
        this.argumentOfPeriapsisDegrees = argumentOfPeriapsisDegrees;
        this.apsidalPrecessionTicks = apsidalPrecessionTicks;
        this.nodalPrecessionTicks = nodalPrecessionTicks;
        this.orbitDecayPerOrbit = orbitDecayPerOrbit;
        this.axialTiltDegrees = axialTiltDegrees;
        this.eccentricityValue = requestedEccentricity;
        this.argumentOfPeriapsisRadians = Math.toRadians(argumentOfPeriapsisDegrees.orElse(0.0));
        // Precession is stored as an angular rate so a zero/absent period simply
        // means "does not precess" instead of dividing by zero.
        this.apsidalPrecessionRate = precessionRate(apsidalPrecessionTicks, "apsidalPrecessionTicks");
        this.nodalPrecessionRate = precessionRate(nodalPrecessionTicks, "nodalPrecessionTicks");
        this.decayPerOrbit = orbitDecayPerOrbit.orElse(0.0);
        if (!Double.isFinite(this.decayPerOrbit)) {
            throw new IllegalArgumentException("orbitDecayPerOrbit must be finite");
        }
        this.axialTiltRadians = Math.toRadians(axialTiltDegrees.orElse(0.0));
        if (!Double.isFinite(this.axialTiltRadians)) {
            throw new IllegalArgumentException("axialTiltDegrees must be finite");
        }

        // Generate random parameters from seed (similar to OrbitingBody)
        Random rand = new Random(seed);

        // Advance RNG like OrbitingBody does for consistency
        for (int i = 0; i < rand.nextInt(10); i++) {
            rand.nextDouble();
        }

        // Generate random base rotation
        this.baseRotation = new Quaterniond();
//        this.baseRotation = new Quaterniond().rotationXYZ(
//            rand.nextDouble(Math.PI),
//            rand.nextDouble(Math.PI),
//            rand.nextDouble(Math.PI)
//        );

        // Starting angle on the orbit: explicit phase when given (lets data pin
        // e.g. a full moon at world start), otherwise random from the seed.
        this.orbitalTheta = phaseDegrees.map(Math::toRadians)
                .orElse(rand.nextDouble() * 2 * Math.PI);   // longitude
        this.inclinationRadians = Math.toRadians(inclinationDegrees.orElse(0.0));
        this.ascendingNodeRadians = Math.toRadians(ascendingNodeDegrees.orElse(0.0));
    }

    /** The body this one orbits, for tools that need to draw the orbit. */
    public ResourceLocation parentId() {
        return this.parentID;
    }

    /** Ticks for one complete orbit — the sampling window for drawing the path. */
    public double orbitTicks() {
        return this.orbitTime;
    }

    private Celestial getParent(Registry<Celestial> registry) {
        Celestial parent = registry.get(parentID);
        if (parent == null) throw new IllegalStateException("Parent celestial not found in registry: " + parentID);
        return parent;
    }

    @Override
    public Quaterniondc getRotation(long ticks, float subticks, Registry<Celestial> registry) {
        if (this.dayLength == 0.0) {
            // Tidally locked: -Z side always faces the parent
            Vector3d myPos = getPosition(ticks, subticks, registry);
            Vector3d parentPos = new Vector3d(getParent(registry).getPosition(ticks, subticks, registry));
            Vector3d toParent = parentPos.sub(myPos, new Vector3d());
            if (toParent.lengthSquared() < 1.0E-12) {
                return new Quaterniond(baseRotation);
            }
            return new Quaterniond(baseRotation)
                    .rotateTo(new Vector3d(0.0, 0.0, -1.0), toParent.normalize());
        }

        // Body rotation is independent from the orbital period. The previous
        // synodic-period formula divided by zero when the periods matched and
        // made ordinary day lengths drift as the orbit changed.
        //
        // The tilt is applied BEFORE the spin so the body turns about its own
        // (tilted) axis rather than the world vertical — that obliquity is what
        // produces seasons on a planet with a real axial tilt.
        return new Quaterniond(baseRotation)
                .rotateZ(this.axialTiltRadians)
                .rotateY(-Math.PI * 2.0 * (ticks + subticks) / this.dayLength);
    }

    /**
     * Converts a precession PERIOD in ticks into an angular rate. An absent or
     * zero period means the element is fixed, which avoids dividing by zero.
     */
    private static double precessionRate(Optional<Double> periodTicks, String name) {
        double period = periodTicks.orElse(0.0);
        if (!Double.isFinite(period)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return period == 0.0 ? 0.0 : Math.PI * 2.0 / period;
    }

    /**
     * Memoised position for one instant. Immutable and published through a
     * volatile field, so a race between the render and server threads can only
     * cost a recompute, never corruption.
     */
    private record PositionCache(long ticks, float subticks, double x, double y, double z) {
    }

    private volatile PositionCache positionCache;

    /**
     * Positions are requested many times per tick — once per renderer, once per
     * gravity sample, once per teleport check — and every call otherwise solves
     * Kepler's equation AND recurses into its parent, so a moon costs three
     * solves. Caching one instant collapses all of those repeats into a single
     * computation per body per tick.
     */
    @Override
    public Vector3d getPosition(long ticks, float subticks, Registry<Celestial> registry) {
        PositionCache cached = this.positionCache;
        if (cached != null && cached.ticks() == ticks && cached.subticks() == subticks) {
            // Fresh copy: callers routinely mutate the vector they are handed.
            return new Vector3d(cached.x(), cached.y(), cached.z());
        }

        Vector3d computed = computePosition(ticks, subticks, registry);
        // Store the components, not the vector, so a caller mutating the result
        // cannot corrupt the cache.
        this.positionCache = new PositionCache(ticks, subticks, computed.x, computed.y, computed.z);
        return computed;
    }

    private Vector3d computePosition(long ticks, float subticks, Registry<Celestial> registry) {
        double time = ticks + subticks;
        double cycles = time / this.orbitTime;
        // Mean anomaly advances at a constant rate; the true angle does not.
        double meanAnomaly = this.orbitalTheta + Math.PI * 2.0 * (cycles - Math.floor(cycles));

        // Orbits evolve: periapsis swings round (apsidal precession), the node
        // line drifts (nodal precession), and the orbit can spiral in or out.
        double argumentOfPeriapsis = this.argumentOfPeriapsisRadians + this.apsidalPrecessionRate * time;
        double ascendingNode = this.ascendingNodeRadians + this.nodalPrecessionRate * time;
        double semiMajorAxis = this.orbitDistance;
        if (this.decayPerOrbit != 0.0) {
            double factor = 1.0 - this.decayPerOrbit * cycles;
            semiMajorAxis *= Math.max(MIN_DECAY_FACTOR, Math.min(MAX_GROWTH_FACTOR, factor));
        }

        double angle;
        double radius;
        if (this.eccentricityValue <= 0.0) {
            // Circular fast path — no solver needed.
            angle = meanAnomaly + argumentOfPeriapsis;
            radius = semiMajorAxis;
        } else {
            // Kepler's second law: sweep equal areas in equal times, so the body
            // races through periapsis and crawls through apoapsis.
            double eccentricAnomaly = solveEccentricAnomaly(meanAnomaly, this.eccentricityValue);
            double halfE = eccentricAnomaly * 0.5;
            double trueAnomaly = 2.0 * Math.atan2(
                    Math.sqrt(1.0 + this.eccentricityValue) * Math.sin(halfE),
                    Math.sqrt(1.0 - this.eccentricityValue) * Math.cos(halfE));
            // orbitDistance is the semi-major axis; radius swings about it.
            radius = semiMajorAxis * (1.0 - this.eccentricityValue * Math.cos(eccentricAnomaly));
            angle = trueAnomaly + argumentOfPeriapsis;
        }

        Vector3d out = new Vector3d(
                Math.cos(angle) * radius,
                0.0,
                Math.sin(angle) * radius
        );
        // Inclination past 90 degrees tips the orbital plane over, which is the
        // standard way a retrograde orbit is expressed (Triton, Halley).
        out.rotateZ(this.inclinationRadians);
        out.rotateY(ascendingNode);

        // Preserve the full parent transform. The old setComponent(1, 0)
        // flattened every child to absolute Y=0, destroying inclined and
        // nested orbits such as a moon following an orbiting planet.
        return out.add(getParent(registry).getPosition(ticks, subticks, registry));
    }

    /**
     * Inverts Kepler's equation {@code M = E - e·sin(E)} for the eccentric
     * anomaly by Newton-Raphson. The sine seed puts the first guess close enough
     * that it converges in a couple of iterations at playable eccentricities.
     */
    private static double solveEccentricAnomaly(double meanAnomaly, double eccentricity) {
        double eccentricAnomaly = meanAnomaly + eccentricity * Math.sin(meanAnomaly);
        for (int iteration = 0; iteration < KEPLER_MAX_ITERATIONS; iteration++) {
            double error = eccentricAnomaly - eccentricity * Math.sin(eccentricAnomaly) - meanAnomaly;
            double derivative = 1.0 - eccentricity * Math.cos(eccentricAnomaly);
            if (Math.abs(derivative) < 1.0E-12) {
                break;
            }
            double step = error / derivative;
            eccentricAnomaly -= step;
            if (Math.abs(step) < KEPLER_TOLERANCE) {
                break;
            }
        }
        return eccentricAnomaly;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    // Codec for serialization/deserialization
    public static final MapCodec<OrbitingTransformProvider> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("parentID").forGetter(p -> p.parentID),
                    Codec.INT.fieldOf("seed").forGetter(p -> p.seed),
                    Codec.DOUBLE.fieldOf("orbitDistance").forGetter(p -> p.orbitDistance),
                    Codec.DOUBLE.fieldOf("orbitTime").forGetter(p -> p.orbitTime),
                    Codec.DOUBLE.optionalFieldOf("dayLength", 1.0).forGetter(p -> p.dayLength),
                    Codec.DOUBLE.optionalFieldOf("phaseDegrees").forGetter(p -> p.phaseDegrees),
                    Codec.DOUBLE.optionalFieldOf("inclinationDegrees").forGetter(p -> p.inclinationDegrees),
                    Codec.DOUBLE.optionalFieldOf("ascendingNodeDegrees").forGetter(p -> p.ascendingNodeDegrees),
                    Codec.DOUBLE.optionalFieldOf("eccentricity").forGetter(p -> p.eccentricity),
                    Codec.DOUBLE.optionalFieldOf("argumentOfPeriapsisDegrees")
                            .forGetter(p -> p.argumentOfPeriapsisDegrees),
                    Codec.DOUBLE.optionalFieldOf("apsidalPrecessionTicks")
                            .forGetter(p -> p.apsidalPrecessionTicks),
                    Codec.DOUBLE.optionalFieldOf("nodalPrecessionTicks")
                            .forGetter(p -> p.nodalPrecessionTicks),
                    Codec.DOUBLE.optionalFieldOf("orbitDecayPerOrbit")
                            .forGetter(p -> p.orbitDecayPerOrbit),
                    Codec.DOUBLE.optionalFieldOf("axialTiltDegrees").forGetter(p -> p.axialTiltDegrees)
            ).apply(instance, OrbitingTransformProvider::new)
    );
    public static final Codec<OrbitingTransformProvider> CODEC = MAP_CODEC.codec();

    // Example registration method (call this during mod initialization)
    public static void register() {
        CelestialTransformProvider.register(TYPE, MAP_CODEC);
    }
}
