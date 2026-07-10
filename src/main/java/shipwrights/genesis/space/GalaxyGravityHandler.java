package shipwrights.genesis.space;

import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsContraptionLookup;
import shipwrights.genesis.compat.aeronautics.AeronauticsForceHandler;

/**
 * Real Newtonian gravity for Sable physics objects inside the {@code great_unknown} 3D space dimension.
 *
 * <p>The space dimension is a vacuum (no ambient gravity, no drag — set by its Sable dimension-physics
 * config), so this handler is the <em>only</em> gravity out there: every tick it sums the inverse-square
 * pull of every massive celestial (Sun, Earth, Moon…) acting on each construct — a true gravity vector
 * field, exactly the "sum of all nearby mass sources" the design calls for. A craft with enough sideways
 * speed settles into a real orbit; a slow one falls toward the nearest body and is captured by its
 * atmosphere ({@link shipwrights.genesis.teleportation.integration.SpaceToPlanetTeleporter}).</p>
 *
 * <p>{@code GM} for each body is its surface gravity × radius² (so the field is continuous with the
 * body's own surface gravity). Constants are plain fields for live tuning.</p>
 */
public final class GalaxyGravityHandler {

    private static final double DT = 1.0 / 20.0;
    /** Overall strength multiplier for the orbital field — turn up/down to taste. */
    private static final double GRAVITY_SCALE = 1.0;
    /** Don't apply gravity from a body you're essentially inside (avoids a singularity at r→0). */
    private static final double MIN_RADIUS_FACTOR = 0.5;

    private final boolean gameTest;

    public GalaxyGravityHandler(boolean gameTest) {
        this.gameTest = gameTest;
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!GenesisMod.isSpaceDimension(level)) return;   // only the galaxy (great_unknown)
        if (!gameTest && level.players().isEmpty()) return;

        try {
            Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
            long ticks = GenesisMod.getTicks(level);

            for (AeronauticsConstruct construct : AeronauticsContraptionLookup.getSortedConstructs(level)) {
                if (construct.isRemoved()) continue;
                Vector3dc p = construct.positionInWorld();
                Vector3d accel = gravityAt(registry, ticks, new Vector3d(p.x(), p.y(), p.z()));
                if (accel.lengthSquared() < 1.0e-12) continue;
                AeronauticsForceHandler.addVelocity(construct, accel.mul(DT), new Vector3d());
            }
        } catch (Throwable t) {
            if (level.getGameTime() % 100 == 0) GenesisMod.LOGGER.error("Galaxy gravity tick failed", t);
        }
    }

    /** Summed inverse-square gravitational acceleration (blocks/s²) from every massive body at {@code point}. */
    public static Vector3d gravityAt(Registry<Celestial> registry, long ticks, Vector3dc point) {
        Vector3d total = new Vector3d();
        for (Celestial c : registry) {
            double surfaceG = c.gravity();
            if (surfaceG <= 0.0) continue;
            double radius = c.getActualSize() * 0.5;
            double gm = surfaceG * radius * radius * GRAVITY_SCALE;

            Vector3dc bodyPos = c.getPosition(ticks, registry);
            Vector3d toBody = new Vector3d(bodyPos.x() - point.x(), bodyPos.y() - point.y(), bodyPos.z() - point.z());
            double r = toBody.length();
            double minR = Math.max(1.0, radius * MIN_RADIUS_FACTOR);
            if (r < minR) continue;

            double a = gm / (r * r);
            total.add(toBody.div(r).mul(a));
        }
        return total;
    }
}
