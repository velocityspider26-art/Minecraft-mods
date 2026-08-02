package shipwrights.genesis.teleportation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import shipwrights.genesis.GenesisMod;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a committed dimension crossing until its destination has genuinely
 * finished generating, so the traveler arrives into terrain that already exists.
 *
 * <p>This is what removes the loading screen — by removing the wait, not the
 * screen. Vanilla shows {@code ReceivingLevelScreen} from the moment a player
 * changes dimension and dismisses it once the section they are standing in has
 * been received and compiled (see {@code LevelLoadStatusManager}). Every second
 * the server spends generating the arrival chunk is a second of that screen.
 * Cancelling the screen instead was tried and is strictly worse: it leaves the
 * player looking at an empty world while the same generation happens anyway,
 * and an earlier attempt at suppressing it hung the client on a permanent 100%
 * loading screen.</p>
 *
 * <p>A hold can never strand anyone. If the destination has not caught up
 * within {@link #MAX_HOLD_TICKS}, the crossing fires regardless — exactly the
 * behaviour before this class existed, screen and all.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class ArrivalGate {
    /**
     * Chunk radius claimed around an arrival while the destination is prepared.
     * Wider than {@link #READY_RADIUS} on purpose: a chunk cannot reach FULL
     * until its neighbours have reached the earlier generation stages, so the
     * claim has to cover the ring that the ready check does not test.
     */
    private static final int PREP_RADIUS = 3;
    /**
     * Chunk radius that must be generated before a crossing is let through.
     * Two covers the 5x5 that {@link SpaceTravelManager} force-loads for an
     * arriving craft, so that blocking load finds everything already present
     * instead of generating it on the server thread.
     */
    private static final int READY_RADIUS = 2;
    /**
     * Longest a crossing may be held (5 seconds). Reaching this means the
     * destination is generating far slower than an approach can cover, so the
     * traveler goes anyway rather than hanging in place.
     */
    private static final int MAX_HOLD_TICKS = 100;
    /** Belt and braces: forget a hold whose destination stopped ticking entirely. */
    private static final int ABANDON_TICKS = 1200;

    private static final TicketType<ChunkPos> ARRIVAL_PREP = TicketType.create(
            "genesis_arrival_prep", Comparator.comparingLong(ChunkPos::toLong), 600);

    /** The crossing itself, deferred until the destination is ready to receive it. */
    @FunctionalInterface
    public interface Crossing {
        /**
         * Performs the move. Runs on the server thread, potentially several
         * ticks after the crossing was committed, so it must re-resolve the
         * traveler rather than close over a stale reference.
         */
        void perform();
    }

    private record Held(ResourceKey<Level> dimension, Vec3 arrival, long deadline, Crossing crossing) {
    }

    private static final Map<UUID, Held> HELD = new ConcurrentHashMap<>();

    /**
     * When a crossing last fired, so background work can stay out of the way.
     *
     * <p>A dimension change is the most allocation-heavy thing the game does:
     * a whole client level is torn down and rebuilt, and every section around
     * the arrival is meshed at once. Anything else churning memory at that
     * moment shows up as a freeze, because a garbage collection stops the
     * threads the player can feel along with the one doing the churning.</p>
     */
    private static volatile long lastCrossingMillis = Long.MIN_VALUE / 2;
    /** How long after a crossing background work stays out of the way. */
    private static final long CROSSING_QUIET_MS = 8_000L;

    private ArrivalGate() {
    }

    /**
     * Whether a crossing is in flight or has just happened. Background work
     * that allocates heavily should check this and wait.
     */
    public static boolean crossingInProgress() {
        return !HELD.isEmpty()
                || System.currentTimeMillis() - lastCrossingMillis < CROSSING_QUIET_MS;
    }

    private static void markCrossed() {
        lastCrossingMillis = System.currentTimeMillis();
    }

    /**
     * Whether this traveler has a crossing already committed and waiting. Entry
     * and exit tests must skip anyone in this state: the crossing decision has
     * been made and consumed, and re-running the test would either duplicate it
     * or throw away the crossing-detection state that produced it.
     */
    public static boolean isHeld(UUID travellerId) {
        return travellerId != null && HELD.containsKey(travellerId);
    }

    /**
     * Commits a crossing, running it now if the destination is already
     * generated and holding it until it is otherwise.
     *
     * @param travellerId player or sub-level UUID; a second hold for the same
     *                    traveler replaces the first
     */
    public static void hold(UUID travellerId, ServerLevel target, Vec3 arrival, Crossing crossing) {
        if (travellerId == null || target == null || arrival == null || crossing == null) {
            return;
        }

        prepare(target, arrival);
        if (ready(target, arrival)) {
            markCrossed();
            crossing.perform();
            return;
        }

        HELD.put(travellerId, new Held(
                target.dimension(), arrival, target.getGameTime() + MAX_HOLD_TICKS, crossing));
        GenesisMod.LOGGER.info("[GATE] holding {} for {} until its arrival chunks exist",
                travellerId, target.dimension().location());
    }

    /** Drops a hold without performing it — for a traveler that is no longer eligible. */
    public static void release(UUID travellerId) {
        if (travellerId != null) {
            HELD.remove(travellerId);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (HELD.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        long now = level.getGameTime();
        for (Map.Entry<UUID, Held> entry : HELD.entrySet()) {
            Held held = entry.getValue();
            if (!held.dimension().equals(level.dimension())) {
                // Game time is shared across levels, so a hold whose own
                // destination has stopped ticking can still be spotted here.
                if (now > held.deadline() + ABANDON_TICKS) {
                    HELD.remove(entry.getKey(), held);
                    GenesisMod.LOGGER.warn("[GATE] abandoned held crossing for {} — {} is not ticking",
                            entry.getKey(), held.dimension().location());
                }
                continue;
            }

            boolean ready = ready(level, held.arrival());
            boolean expired = now >= held.deadline();
            if (!ready && !expired) {
                prepare(level, held.arrival()); // keep the claim alive while we wait
                continue;
            }
            if (!HELD.remove(entry.getKey(), held)) {
                continue; // superseded on another thread; the newer hold owns it
            }

            if (ready) {
                GenesisMod.LOGGER.info("[GATE] {} ready for {}; crossing now",
                        level.dimension().location(), entry.getKey());
            } else {
                GenesisMod.LOGGER.warn("[GATE] {} still generating after {} ticks; letting {} cross anyway",
                        level.dimension().location(), MAX_HOLD_TICKS, entry.getKey());
            }
            try {
                markCrossed();
                held.crossing().perform();
            } catch (Throwable t) {
                GenesisMod.LOGGER.error("[GATE] held crossing failed for {}", entry.getKey(), t);
            }
        }
    }

    /**
     * Claims the arrival area and starts generating it. Safe to call every tick
     * — a region ticket at the same position and radius is idempotent, and the
     * repeat is what refreshes its expiry.
     */
    public static void prepare(ServerLevel target, Vec3 arrival) {
        if (target == null || arrival == null) {
            return;
        }
        ChunkPos centre = new ChunkPos(BlockPos.containing(arrival.x, arrival.y, arrival.z));
        target.getChunkSource().addRegionTicket(ARRIVAL_PREP, centre, PREP_RADIUS, centre);
    }

    /**
     * Whether the arrival area exists right now, without generating any of it.
     *
     * <p>{@code hasChunk} is not usable here: it only reports the chunk's ticket
     * level, so it answers yes the moment generation is <em>requested</em> — the
     * exact thing being waited on. {@code getChunkNow} returns a chunk only once
     * it has actually reached FULL.</p>
     */
    public static boolean ready(ServerLevel target, Vec3 arrival) {
        if (target == null || arrival == null) {
            return true;
        }
        ChunkPos centre = new ChunkPos(BlockPos.containing(arrival.x, arrival.y, arrival.z));
        for (int dx = -READY_RADIUS; dx <= READY_RADIUS; dx++) {
            for (int dz = -READY_RADIUS; dz <= READY_RADIUS; dz++) {
                if (target.getChunkSource().getChunkNow(centre.x + dx, centre.z + dz) == null) {
                    return false;
                }
            }
        }
        return true;
    }
}
