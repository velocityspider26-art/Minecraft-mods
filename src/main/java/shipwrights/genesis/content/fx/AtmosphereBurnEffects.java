package shipwrights.genesis.content.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsContraptionLookup;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.properties.PlanetProperties;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Atmospheric heating for Sable physics objects (Create Aeronautics constructs).
 *
 * <p>When a construct moves fast through a celestial's atmosphere — burning down
 * during re-entry or punching up toward space — plasma builds along its leading
 * face and streams past the hull, Kerbal-style. Heat ramps with speed and local
 * air density (fading to nothing at the atmosphere exit height) and builds and
 * decays smoothly rather than switching on and off.</p>
 *
 * <p>This applies <b>only</b> to Sable sub-levels: the constructs enumerated by
 * {@link AeronauticsContraptionLookup}. Players, mobs, items and other entities
 * are never touched.</p>
 */
public class AtmosphereBurnEffects {

    /** Speed (blocks/second) where heating begins. */
    private static final double MIN_BURN_SPEED = 15.0;
    /** Speed (blocks/second) of maximum plasma. */
    private static final double MAX_BURN_SPEED = 65.0;
    /** Per-tick smoothing toward the target heat — a natural several-second build-up. */
    private static final double HEAT_LERP = 0.06;

    private final Map<UUID, Double> heat = new HashMap<>();

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (GenesisMod.isSpaceDimension(level)) return;
        if (level.players().isEmpty()) return;

        Celestial body = GenesisMod.getCelestialForLevel(level);
        if (body == null) return;
        double atmosphereDensity = body.properties() instanceof PlanetProperties pp && pp.atmosphere() != null
                ? Mth.clamp(pp.atmosphere().density(), 0.0, 1.0) : 0.0;
        if (atmosphereDensity <= 0.0) return;

        int entryHeight = GenesisCommonConfig.getAtmosphereEntryHeight();
        int exitHeight = GenesisCommonConfig.getAtmosphereExitHeight();

        for (AeronauticsConstruct construct : AeronauticsContraptionLookup.getSortedConstructs(level)) {
            if (construct.isRemoved()) continue;

            Vector3dc velocity = construct.linearVelocity(); // blocks per second
            double speed = velocity.length();

            var bounds = construct.worldBounds();
            double cx = (bounds.minX() + bounds.maxX()) * 0.5;
            double cy = (bounds.minY() + bounds.maxY()) * 0.5;
            double cz = (bounds.minZ() + bounds.maxZ()) * 0.5;

            // Air thins with altitude and is gone at the exit height.
            double airDensity = atmosphereDensity * Mth.clamp(1.0 - (cy - entryHeight) / (double) (exitHeight - entryHeight), 0.0, 1.0);

            double target = Mth.clamp((speed - MIN_BURN_SPEED) / (MAX_BURN_SPEED - MIN_BURN_SPEED), 0.0, 1.0) * airDensity;

            double current = heat.merge(construct.id(), 0.0, Double::sum);
            current = current + (target - current) * HEAT_LERP;
            heat.put(construct.id(), current);

            if (current > 0.03 && speed > 1.0e-3) {
                emitPlasma(level, construct, velocity, speed, current);
            }
        }

        // Drop bookkeeping for constructs that no longer exist.
        if (level.getGameTime() % 200 == 0 && !heat.isEmpty()) {
            var alive = new java.util.HashSet<UUID>();
            for (ServerLevel l : level.getServer().getAllLevels()) {
                for (AeronauticsConstruct c : AeronauticsContraptionLookup.getSortedConstructs(l)) alive.add(c.id());
            }
            for (Iterator<UUID> it = heat.keySet().iterator(); it.hasNext(); ) {
                if (!alive.contains(it.next())) it.remove();
            }
        }
    }

    // Plasma colours (linear RGB). White-gold at the stagnation point, bleeding to orange then deep red
    // toward the shoulders of the shock — the gradient you see on a real re-entry capsule / the reference.
    private static final Vector3f PLASMA_CORE = new Vector3f(1.0f, 0.96f, 0.80f); // white-hot
    private static final Vector3f PLASMA_MID  = new Vector3f(1.0f, 0.50f, 0.12f); // orange
    private static final Vector3f PLASMA_RIM  = new Vector3f(0.85f, 0.12f, 0.03f); // deep red
    private static final Vector3f EMBER        = new Vector3f(0.35f, 0.05f, 0.02f); // cooled ember (transition target)

    private void emitPlasma(ServerLevel level, AeronauticsConstruct construct, Vector3dc velocity, double speed, double intensity) {
        RandomSource random = level.random;
        Vector3d dir = new Vector3d(velocity).div(speed);

        var bounds = construct.worldBounds();
        Vector3d center = new Vector3d(
                (bounds.minX() + bounds.maxX()) * 0.5,
                (bounds.minY() + bounds.maxY()) * 0.5,
                (bounds.minZ() + bounds.maxZ()) * 0.5);
        double hx = (bounds.maxX() - bounds.minX()) * 0.5;
        double hy = (bounds.maxY() - bounds.minY()) * 0.5;
        double hz = (bounds.maxZ() - bounds.minZ()) * 0.5;

        // Hull extent along travel, the stagnation point just ahead of it, and a basis across the face.
        double alongExtent = hx * Math.abs(dir.x) + hy * Math.abs(dir.y) + hz * Math.abs(dir.z);
        Vector3d up = Math.abs(dir.y) > 0.99 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(dir).cross(up).normalize();
        Vector3d across = new Vector3d(right).cross(dir).normalize();
        double faceR = Math.max(1.0, (hx + hy + hz - alongExtent) * 0.5);
        // The bright cap sits a touch ahead of the hull and bows backward at the shoulders.
        double standoff = 0.5 + 0.8 * intensity;
        Vector3d stag = new Vector3d(dir).mul(alongExtent + standoff).add(center);

        double wash = 0.6 + 1.4 * intensity;                 // backward stream speed
        float sizeBoost = (float) (1.1 + 1.3 * intensity);   // plasma puff size

        // ---- bow-shock plasma cap: a bright dome across the leading face, hottest at the centre ----
        int shell = (int) (14 + 120 * intensity);
        for (int i = 0; i < shell; i++) {
            // Denser toward the centre (sqrt keeps the disc uniform, bias pulls it inward).
            double rr = Math.pow(random.nextDouble(), 0.65);
            double ang = random.nextDouble() * Math.PI * 2.0;
            double ox = Math.cos(ang) * rr * faceR;
            double oy = Math.sin(ang) * rr * faceR;
            // Bow shape: the shell curves back toward the hull away from the stagnation point.
            double curveBack = (alongExtent + standoff) * rr * rr;
            double px = stag.x + right.x * ox + across.x * oy - dir.x * curveBack;
            double py = stag.y + right.y * ox + across.y * oy - dir.y * curveBack;
            double pz = stag.z + right.z * ox + across.z * oy - dir.z * curveBack;

            // Colour by radius: white-hot core -> orange -> deep-red rim.
            Vector3f hot = rr < 0.45 ? lerp(PLASMA_CORE, PLASMA_MID, (float) (rr / 0.45))
                                     : lerp(PLASMA_MID, PLASMA_RIM, (float) ((rr - 0.45) / 0.55));
            float scale = sizeBoost * (float) (0.7 + 0.6 * (1.0 - rr));
            var puff = new DustColorTransitionOptions(hot, EMBER, scale);

            double vs = wash * (0.4 + 0.8 * random.nextDouble());
            double vx = -dir.x * vs + (random.nextDouble() - 0.5) * 0.15;
            double vy = -dir.y * vs + (random.nextDouble() - 0.5) * 0.15;
            double vz = -dir.z * vs + (random.nextDouble() - 0.5) * 0.15;
            level.sendParticles(puff, px, py, pz, 0, vx, vy, vz, 1.0);
        }

        // ---- incandescent stagnation core: a small knot of white-hot sparks at the very front ----
        int coreSparks = (int) (2 + 10 * intensity);
        for (int i = 0; i < coreSparks; i++) {
            double jx = (random.nextDouble() - 0.5) * faceR * 0.5;
            double jy = (random.nextDouble() - 0.5) * faceR * 0.5;
            double px = stag.x + right.x * jx + across.x * jy;
            double py = stag.y + right.y * jx + across.y * jy;
            double pz = stag.z + right.z * jx + across.z * jy;
            var core = new DustColorTransitionOptions(PLASMA_CORE, PLASMA_MID, sizeBoost * 1.4f);
            level.sendParticles(core, px, py, pz, 0, -dir.x * wash, -dir.y * wash, -dir.z * wash, 1.0);
            if (intensity > 0.6 && random.nextFloat() < 0.35f) {
                level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 0,
                        -dir.x * wash * 1.5, -dir.y * wash * 1.5, -dir.z * wash * 1.5, 1.0);
            }
        }

        // ---- trailing plasma wake shed off the shoulders and streaming behind ----
        int wakePts = (int) (4 + 30 * intensity);
        for (int i = 0; i < wakePts; i++) {
            double back = (0.3 + random.nextDouble()) * (alongExtent + 2.0);
            double ox = (random.nextDouble() * 2 - 1) * faceR * 0.8;
            double oy = (random.nextDouble() * 2 - 1) * faceR * 0.8;
            double px = center.x - dir.x * back + right.x * ox + across.x * oy;
            double py = center.y - dir.y * back + right.y * ox + across.y * oy;
            double pz = center.z - dir.z * back + right.z * ox + across.z * oy;
            var trail = new DustColorTransitionOptions(PLASMA_MID, EMBER, sizeBoost * 0.9f);
            level.sendParticles(trail, px, py, pz, 0,
                    -dir.x * wash * 0.6, -dir.y * wash * 0.6, -dir.z * wash * 0.6, 1.0);
        }
        // A little dark smoke behind the fire so the wake reads against bright sky.
        Vector3d tail = new Vector3d(dir).mul(-(alongExtent + 1.5)).add(center);
        level.sendParticles(ParticleTypes.SMOKE, tail.x, tail.y, tail.z,
                (int) (1 + intensity * 4), faceR * 0.4, faceR * 0.4, faceR * 0.4, 0.01);

        // Roar, swelling with the heat.
        if (intensity > 0.2 && level.getGameTime() % 16 == 0) {
            level.playSound(null, BlockPos.containing(center.x, center.y, center.z),
                    SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL,
                    (float) (0.5 + 1.5 * intensity), 0.55f + 0.35f * (float) intensity);
        }
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        return new Vector3f(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }
}
