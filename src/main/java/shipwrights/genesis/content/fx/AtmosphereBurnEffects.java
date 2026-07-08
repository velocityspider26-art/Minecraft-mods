package shipwrights.genesis.content.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
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

    /**
     * The <b>visual</b> plasma envelope is now a real rendered shell on the client
     * ({@code ReentryPlasmaRenderer}) rather than particles — this server side only carries the audible
     * roar, swelling with heat, so the effect is heard by everyone nearby even off-screen.
     */
    private void emitPlasma(ServerLevel level, AeronauticsConstruct construct, Vector3dc velocity, double speed, double intensity) {
        var bounds = construct.worldBounds();
        double cx = (bounds.minX() + bounds.maxX()) * 0.5;
        double cy = (bounds.minY() + bounds.maxY()) * 0.5;
        double cz = (bounds.minZ() + bounds.maxZ()) * 0.5;
        if (intensity > 0.2 && level.getGameTime() % 16 == 0) {
            level.playSound(null, BlockPos.containing(cx, cy, cz),
                    SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL,
                    (float) (0.5 + 1.5 * intensity), 0.55f + 0.35f * (float) intensity);
        }
    }
}
