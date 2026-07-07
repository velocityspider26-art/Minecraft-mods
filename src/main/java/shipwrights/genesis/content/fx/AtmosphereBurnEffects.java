package shipwrights.genesis.content.fx;

import net.minecraft.core.BlockPos;
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

        // Extent of the hull along the direction of travel, and the leading point.
        double alongExtent = hx * Math.abs(dir.x) + hy * Math.abs(dir.y) + hz * Math.abs(dir.z);
        Vector3d lead = new Vector3d(dir).mul(alongExtent + 0.6).add(center);

        // Basis across the leading face.
        Vector3d up = Math.abs(dir.y) > 0.99 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(dir).cross(up).normalize();
        Vector3d across = new Vector3d(right).cross(dir).normalize();
        double crossR = Math.max(1.0, (hx + hy + hz - alongExtent) * 0.5);

        // Plasma streaks washing back over the hull, denser and hotter as heat builds.
        int streaks = (int) (3 + 24 * intensity);
        double wash = 0.35 + 0.9 * intensity;
        for (int i = 0; i < streaks; i++) {
            double ox = (random.nextDouble() * 2 - 1) * crossR;
            double oy = (random.nextDouble() * 2 - 1) * crossR;
            double px = lead.x + right.x * ox + across.x * oy;
            double py = lead.y + right.y * ox + across.y * oy;
            double pz = lead.z + right.z * ox + across.z * oy;

            double vs = wash * (0.5 + random.nextDouble());
            double vx = -dir.x * vs + (random.nextDouble() - 0.5) * 0.08;
            double vy = -dir.y * vs + (random.nextDouble() - 0.5) * 0.08;
            double vz = -dir.z * vs + (random.nextDouble() - 0.5) * 0.08;

            // Hotter plasma whitens: mostly flame, blue-white core streaks at high heat.
            var type = intensity > 0.55 && random.nextFloat() < 0.45f ? ParticleTypes.SOUL_FIRE_FLAME
                    : ParticleTypes.FLAME;
            level.sendParticles(type, px, py, pz, 0, vx, vy, vz, 1.0);

            if (intensity > 0.7 && random.nextFloat() < 0.15f) {
                level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 0, vx * 1.4, vy * 1.4, vz * 1.4, 1.0);
            }
        }

        // Incandescent shock layer hugging the leading face.
        if (intensity > 0.35) {
            level.sendParticles(ParticleTypes.LAVA, lead.x, lead.y, lead.z,
                    (int) (intensity * 2), crossR * 0.4, crossR * 0.4, crossR * 0.4, 0.0);
        }

        // Smoke/ionisation trail shed behind the craft.
        Vector3d tail = new Vector3d(dir).mul(-(alongExtent + 1.0)).add(center);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, tail.x, tail.y, tail.z,
                (int) (1 + intensity * 3), crossR * 0.3, crossR * 0.3, crossR * 0.3, 0.02);

        // Roar, swelling with the heat.
        if (intensity > 0.2 && level.getGameTime() % 16 == 0) {
            level.playSound(null, BlockPos.containing(center.x, center.y, center.z),
                    SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL,
                    (float) (0.5 + 1.5 * intensity), 0.55f + 0.35f * (float) intensity);
        }
    }
}
