package shipwrights.genesis.teleportation;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.ryanhcode.sable.util.SableNBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.networking.ShipVisibilityProbePacket;
import shipwrights.genesis.teleportation.impl.EntityTeleporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * The travel manager for Sable ships. A ship, the sub-levels it depends on,
 * the entities living inside its plots (Create seats, contraptions, armor
 * stands — and the players riding them), and the crew standing on its deck in
 * logical space are all moved as ONE travel unit. Velocity, rotation, riding
 * state, and look direction survive the trip.
 *
 * <p>Crew players are deliberately teleported a few ticks AFTER the ship:
 * moving them in the same tick races Sable's client sync — the client changes
 * dimension while the sub-level ADD is in flight, misses it, and then drops
 * every subsequent movement packet ("non-existent sub-level" log spam, ship
 * invisible until relog). Arriving slightly late, the client's join snapshot
 * already contains the ship.</p>
 *
 * <p>Sable's own {@code SubLevelSerializer} carries blocks, block entities,
 * pose, and velocities — but NOT entities. Seats are tagged
 * {@code sable:retain_in_sub_level}, so they live at plot coordinates in the
 * shadow region; a seated player's real server position is inside the plot,
 * not on the visible ship. That is why naive teleports split the crew from the
 * craft: this class is the fix, and every Genesis travel path (hyperdrive,
 * atmosphere exit, planet entry) must go through it.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class SpaceTravelManager {
    /** Space-dimension blocks per planet-dimension block (1:16 map). */
    public static final double SPACE_BLOCKS_PER_PLANET_BLOCK = 1.0 / 16.0;
    /**
     * How far outside a celestial's surface (in space blocks) atmosphere entry
     * triggers — a 1000-block invisible cube around the planet. The region is a
     * CUBE around the rotating celestial on purpose: which face you sink into
     * decides where on the planet you arrive. Arrival from a planet lands
     * inside this cube (close to the planet), so the entry hysteresis keeps a
     * fresh arrival from instantly bouncing back.
     */
    public static final double PLANET_ENTRY_PADDING = 1000.0;
    /**
     * Where an atmosphere exit drops you in space, just off the celestial's
     * surface, so the planet fills the view below you right after you leave.
     */
    public static final double ATMOSPHERE_EXIT_CLEARANCE = 400.0;
    /**
     * After arriving in space, suppress the re-entry trigger until the traveler
     * has moved this far. This is only a secondary guard: {@link
     * #crossedIntoEntryZone} already stops a launch from bouncing back (you
     * arrive inside the zone, so you were never outside to cross in from). It is
     * kept small on purpose — a large value blocked legitimate approaches to
     * small planets, where a hyperdrive standoff sits only a few hundred blocks
     * outside the entry zone and never cleared a 1000-block buffer.
     */
    public static final double SPACE_ENTRY_HYSTERESIS = 300.0;
    /**
     * How close a player's own position must be to a ship's logical pose to
     * count as riding it. Generous so deck crew standing away from the ship's
     * centre are still carried across the atmosphere boundary, not left behind
     * while the ship travels without them.
     */
    private static final double ABOARD_RADIUS = 48.0;
    /**
     * Radius (from the celestial's center) where atmosphere exits and
     * hyperspace jumps place a traveler. 1.5x the entry cube's half-extent
     * clears the cube even at its corners (diagonal = sqrt(2) ~ 1.414x), so an
     * arrival can never re-trigger entry — the "leave, freeze, fall back down"
     * loop.
     */
    public static double arrivalRingRadius(double celestialHalfExtent) {
        return (celestialHalfExtent + entryPadding(celestialHalfExtent * 2.0)) * 1.5;
    }

    /** Bodies at or above this size keep the full, wide entry band. */
    private static final double LARGE_BODY_SIZE = 400.0;
    /**
     * Entry band for a small body. Wide enough to sit clearly outside the
     * rendered model rather than against its surface: at 50 the trigger was
     * only half a radius out from something Mercury-sized, so entry fired with
     * the planet already filling the view and read as being inside it.
     */
    private static final double SMALL_BODY_ENTRY_PADDING = 200.0;

    /**
     * How far outside a body's surface entry triggers, given its size.
     *
     * <p>A single global padding does not suit every world: 1000 blocks around
     * Earth reads as "approaching the atmosphere", but the same band around a
     * body as small as Mercury triggers while the planet is still a distant
     * speck — its entry zone was over ten times its own radius. Small bodies
     * therefore get a band that hugs the model instead.</p>
     */
    public static double entryPadding(double celestialSize) {
        return celestialSize >= LARGE_BODY_SIZE ? PLANET_ENTRY_PADDING : SMALL_BODY_ENTRY_PADDING;
    }

    /** Where each traveler last arrived in space, for the entry-suppression buffer. */
    private static final java.util.Map<UUID, Vec3> SPACE_ARRIVALS = new java.util.concurrent.ConcurrentHashMap<>();
    /** Travelers known to be OUTSIDE a planet's entry zone, for crossing detection. */
    private static final java.util.Set<UUID> OUTSIDE_ENTRY_ZONE =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Last time each tracked traveler was seen, so dead entries can be dropped. */
    private static final java.util.Map<UUID, Long> LAST_SEEN =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final int TRACKING_PRUNE_THRESHOLD = 128;
    private static final long TRACKING_STALE_MS = 120_000;
    private static final int CREW_SYNC_DELAY_TICKS = 3;
    /** Fallback if the client never acknowledges that its transition finished. */
    private static final int VISIBILITY_SYNC_FALLBACK_TICKS = 100;
    /** Keeps the generic visibility sweep away while destination visibility is verified. */
    private static final int VISIBILITY_SWEEP_GUARD_TICKS = 40;
    private static final ConcurrentLinkedQueue<PendingCrewTransfer> PENDING_CREW_TRANSFERS =
            new ConcurrentLinkedQueue<>();
    private static final java.util.Map<UUID, PendingVisibilitySync> PENDING_VISIBILITY_SYNCS =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<UUID, Long> VISIBILITY_MANAGED_UNTIL =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Ticks to wait after the crew arrives before the ship follows them across.
     * Long enough for the client to finish loading the destination dimension.
     */
    private static final int SHIP_FOLLOW_DELAY_TICKS = 25;
    private static final ConcurrentLinkedQueue<PendingShipTransfer> PENDING_SHIP_TRANSFERS =
            new ConcurrentLinkedQueue<>();

    /**
     * Keeps the destination chunks loaded around a ship's arrival.
     *
     * <p>Sable unloads a sub-level whose chunk is not held — it serialises the
     * craft into its holding store and removes it from the container. A ship
     * transferred into a dimension where nothing has claimed the arrival area
     * therefore vanishes a couple of seconds after landing: the transfer
     * reports success, movement packets flow briefly, and then the container is
     * empty. Holding a ticket over the arrival is what keeps the craft alive
     * long enough for the dimension to take ownership of it normally.</p>
     */
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> ARRIVAL_TICKET =
            net.minecraft.server.level.TicketType.create(
                    "genesis_ship_arrival", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 600);
    /** Chunk radius held around an arriving ship. */
    private static final int ARRIVAL_TICKET_RADIUS = 4;
    /** Chunk radius loaded synchronously so the craft never lands in an unloaded chunk. */
    private static final int ARRIVAL_LOAD_RADIUS = 2;

    /**
     * Claims the arrival area AND loads it synchronously.
     *
     * <p>A region ticket only <em>requests</em> a load, which completes on a
     * later tick. Sable's physics ticket manager checks
     * {@code isChunkLoadedEnough} every tick and immediately moves any
     * sub-level standing in a not-yet-loaded chunk into its holding store —
     * which deleted transferred craft a fraction of a second after they landed,
     * long before an async load could finish. Forcing the chunks in before the
     * craft arrives is what makes the check pass.</p>
     */
    private static void holdArrivalChunks(ServerLevel targetLevel, Vec3 arrival) {
        try {
            ChunkPos centre = new ChunkPos(BlockPos.containing(arrival.x, arrival.y, arrival.z));
            targetLevel.getChunkSource().addRegionTicket(
                    ARRIVAL_TICKET, centre, ARRIVAL_TICKET_RADIUS, centre);

            // Blocking load/generate, so the chunks genuinely exist NOW.
            for (int dx = -ARRIVAL_LOAD_RADIUS; dx <= ARRIVAL_LOAD_RADIUS; dx++) {
                for (int dz = -ARRIVAL_LOAD_RADIUS; dz <= ARRIVAL_LOAD_RADIUS; dz++) {
                    targetLevel.getChunk(centre.x + dx, centre.z + dz);
                }
            }
        } catch (Throwable t) {
            GenesisMod.LOGGER.warn("Could not hold arrival chunks", t);
        }
    }

    private SpaceTravelManager() {
    }

    /**
     * Moves the crew now and has their ship follow a moment later.
     *
     * <p>Transferring the ship while the crew's client is mid dimension-change is
     * what leaves it invisible: Sable announces the sub-level, the announcement
     * lands on a client that is still swapping worlds, and it is dropped — after
     * which every movement update is rejected ("movement packet for a
     * non-existent sub-level"). Assembling a ship in a dimension you are already
     * standing in works fine, so this reproduces those conditions: the crew
     * arrives and finishes loading first, and only then does the ship appear —
     * announced to a client that is ready to receive it.</p>
     */
    public static void transferShipAfterCrew(ServerSubLevel ship, ServerLevel targetLevel, Vec3 arrival) {
        if (ship == null || targetLevel == null || arrival == null) {
            return;
        }

        // Ship and crew MUST move in the same operation. Moving the crew first
        // and having the ship follow was tried and fails outright: the moment the
        // last player leaves, Sable unloads the sub-level from the origin
        // dimension, so a moment later there is no ship left to bring across
        // ("no longer in <origin>; nothing to bring across") and the craft is
        // gone for good. Visibility is instead checked after arrival and only
        // repaired for clients that genuinely missed Sable's announcement.
        boolean moved = transferShip(ship, targetLevel, arrival);
        GenesisMod.LOGGER.info("[FOLLOW] ship {} + crew -> {} : {}",
                ship.getUniqueId(), targetLevel.dimension().location(), moved ? "OK" : "FAILED");
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || (PENDING_CREW_TRANSFERS.isEmpty() && PENDING_VISIBILITY_SYNCS.isEmpty()
                    && PENDING_SHIP_TRANSFERS.isEmpty())) {
            return; // nothing queued — skip the per-level work entirely
        }

        int shipCount = PENDING_SHIP_TRANSFERS.size();
        for (int i = 0; i < shipCount; i++) {
            PendingShipTransfer pending = PENDING_SHIP_TRANSFERS.poll();
            if (pending == null) {
                break;
            }
            if (!pending.targetDimension().equals(level.dimension()) || level.getGameTime() < pending.readyGameTime()) {
                PENDING_SHIP_TRANSFERS.offer(pending);
                continue;
            }
            runFollowTransfer(level, pending);
        }

        int pendingCount = PENDING_CREW_TRANSFERS.size();
        for (int i = 0; i < pendingCount; i++) {
            PendingCrewTransfer pending = PENDING_CREW_TRANSFERS.poll();
            if (pending == null) {
                break;
            }
            if (!pending.targetDimension().equals(level.dimension()) || level.getGameTime() < pending.readyGameTime()) {
                PENDING_CREW_TRANSFERS.offer(pending);
                continue;
            }

            List<ServerPlayer> crew = pending.crewIds().stream()
                    .map(level.getServer().getPlayerList()::getPlayer)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            reconcileCrew(crew, level, pending.arrival(), pending.shipIds());
        }

        for (var entry : List.copyOf(PENDING_VISIBILITY_SYNCS.entrySet())) {
            PendingVisibilitySync pending = entry.getValue();
            if (!pending.targetDimension().equals(level.dimension())) {
                continue;
            }

            boolean reported = !pending.crewIds().isEmpty()
                    && pending.reportedCrewIds().containsAll(pending.crewIds());
            if (!reported && level.getGameTime() < pending.fallbackGameTime()) {
                continue;
            }
            if (!PENDING_VISIBILITY_SYNCS.remove(entry.getKey(), pending)) {
                continue;
            }

            List<ServerPlayer> crew = pending.crewIds().stream()
                    .map(level.getServer().getPlayerList()::getPlayer)
                    .filter(java.util.Objects::nonNull)
                    .filter(player -> player.level() == level)
                    .toList();
            if (reported) {
                repairMissingShips(level, crew, pending);
            } else {
                retrackShips(level, crew, pending.shipIds());
            }
            long guardedUntil = level.getGameTime() + VISIBILITY_SWEEP_GUARD_TICKS;
            for (UUID shipId : pending.shipIds()) {
                VISIBILITY_MANAGED_UNTIL.put(shipId, guardedUntil);
            }
            GenesisMod.LOGGER.info("[RETRACK] visibility handoff for {} completed via {}",
                    pending.shipIds(), reported ? "ordered client probe" : "server fallback");
        }
    }

    /**
     * Called after the destination has rendered several real frames. The probe
     * sent here is ordered behind Sable's natural tracking packets, so its
     * answer cannot race a packet that was already in flight.
     */
    public static void onClientTransitionReady(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        ResourceKey<Level> dimension = player.serverLevel().dimension();
        for (var entry : PENDING_VISIBILITY_SYNCS.entrySet()) {
            PendingVisibilitySync pending = entry.getValue();
            if (pending.targetDimension().equals(dimension) && pending.crewIds().contains(playerId)) {
                if (pending.probedCrewIds().add(playerId)) {
                    GenesisNetworking.sendToPlayer(player,
                            new ShipVisibilityProbePacket(entry.getKey(), pending.shipIds()));
                }
            }
        }
    }

    /** Accepts the ordered probe response and records only genuinely absent craft. */
    public static void onClientVisibilityStatus(ServerPlayer player, UUID syncId,
                                                Collection<UUID> missingShipIds) {
        if (player == null || syncId == null) {
            return;
        }
        PendingVisibilitySync pending = PENDING_VISIBILITY_SYNCS.get(syncId);
        UUID playerId = player.getUUID();
        if (pending == null
                || !pending.targetDimension().equals(player.serverLevel().dimension())
                || !pending.crewIds().contains(playerId)
                || !pending.probedCrewIds().contains(playerId)) {
            return;
        }

        Set<UUID> expected = new HashSet<>(pending.shipIds());
        Set<UUID> missing = new HashSet<>();
        for (UUID shipId : missingShipIds) {
            if (expected.contains(shipId)) {
                missing.add(shipId);
            }
        }
        pending.missingShipsByCrew().put(playerId, Set.copyOf(missing));
        pending.reportedCrewIds().add(playerId);
        GenesisMod.LOGGER.info("[RETRACK] client {} reports {}/{} arriving ships missing",
                player.getGameProfile().getName(), missing.size(), pending.shipIds().size());
    }

    /**
     * Whether a transferred craft currently owns its visibility handoff. The
     * broad sweep must not independently re-add it during this window: Sable's
     * addition queue does not deduplicate entries, and two full-sync bundles for
     * one plot crash the client with "Plot already exists".
     */
    public static boolean isVisibilityManaged(ServerLevel level, UUID shipId) {
        Long until = VISIBILITY_MANAGED_UNTIL.get(shipId);
        if (until == null) {
            return false;
        }
        if (level.getGameTime() <= until) {
            return true;
        }
        VISIBILITY_MANAGED_UNTIL.remove(shipId, until);
        return false;
    }

    /** Records that a traveler just arrived in space, arming the entry buffer. */
    public static void recordSpaceArrival(UUID id, Vec3 position) {
        if (id != null && position != null) {
            SPACE_ARRIVALS.put(id, position);
            OUTSIDE_ENTRY_ZONE.remove(id); // arrived inside; must leave before re-entering
            LAST_SEEN.put(id, System.currentTimeMillis());
            pruneStaleTracking();
        }
    }

    /**
     * Drops tracking for travelers we haven't heard from in a while. Ships get a
     * fresh UUID every time one is assembled, so without this the entry-zone
     * bookkeeping would grow without bound over a long session.
     */
    private static void pruneStaleTracking() {
        if (LAST_SEEN.size() <= TRACKING_PRUNE_THRESHOLD) {
            return;
        }
        long cutoff = System.currentTimeMillis() - TRACKING_STALE_MS;
        LAST_SEEN.entrySet().removeIf(entry -> {
            if (entry.getValue() >= cutoff) {
                return false;
            }
            SPACE_ARRIVALS.remove(entry.getKey());
            OUTSIDE_ENTRY_ZONE.remove(entry.getKey());
            return true;
        });
    }

    /**
     * Atmosphere entry fires only when a traveler CROSSES INTO the entry zone
     * from outside it — never merely for being inside.
     *
     * <p>This is what keeps a ship you just assembled next to a planet from
     * teleporting you straight down: a brand-new sub-level has never been seen
     * outside the zone, so it is treated as "already inside" and must leave
     * before entry can trigger. It also covers arrivals, which land inside the
     * zone by design.</p>
     *
     * @param inside whether the traveler is inside the entry zone right now
     * @return true only on an outside → inside transition
     */
    public static boolean crossedIntoEntryZone(UUID id, boolean inside) {
        if (id == null) {
            return false;
        }
        LAST_SEEN.put(id, System.currentTimeMillis());
        if (!inside) {
            OUTSIDE_ENTRY_ZONE.add(id);
            pruneStaleTracking();
            return false;
        }
        // Inside now: only a traveler previously known to be outside has crossed.
        return OUTSIDE_ENTRY_ZONE.remove(id);
    }

    /**
     * Whether atmosphere entry should be suppressed for this traveler because
     * they only just arrived in space and haven't yet moved the buffer distance
     * away — stops a fresh arrival from instantly bouncing back to the planet.
     */
    public static boolean entrySuppressed(UUID id, Vec3 position) {
        Vec3 arrival = SPACE_ARRIVALS.get(id);
        if (arrival == null) {
            return false;
        }
        if (position.distanceToSqr(arrival) > SPACE_ENTRY_HYSTERESIS * SPACE_ENTRY_HYSTERESIS) {
            SPACE_ARRIVALS.remove(id); // moved far enough; buffer spent
            return false;
        }
        return true;
    }

    /** Finds the ship whose plot contains the given (plot-space) block, e.g. a hyperdrive block. */
    @Nullable
    public static ServerSubLevel findShipContaining(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        try {
            SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
            return subLevel instanceof ServerSubLevel serverSubLevel ? serverSubLevel : null;
        } catch (Throwable t) {
            GenesisMod.LOGGER.warn("Failed to look up Sable sub-level at {}", pos, t);
            return null;
        }
    }

    @Nullable
    public static ServerSubLevel getShip(ServerLevel level, UUID shipId) {
        if (level == null || shipId == null) {
            return null;
        }
        try {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                return null;
            }
            SubLevel subLevel = container.getSubLevel(shipId);
            return subLevel instanceof ServerSubLevel serverSubLevel ? serverSubLevel : null;
        } catch (Throwable t) {
            GenesisMod.LOGGER.warn("Failed to look up Sable sub-level {}", shipId, t);
            return null;
        }
    }

    /**
     * Whether the entity is part of a ship's travel unit — riding a seat inside
     * a plot, or standing on a deck in logical space.
     */
    public static boolean isAboardAnyShip(ServerLevel level, Entity entity) {
        return shipCarrying(level, entity) != null;
    }

    /**
     * The ship this entity is traveling with, or null. Checks the plot (seated
     * riders), the ship's reported bounding box, AND a radius around the
     * logical pose — the box alone proved unreliable for deck crew, and a
     * missed match here is what used to leave ships behind at the atmosphere
     * boundary.
     */
    @Nullable
    public static ServerSubLevel shipCarrying(ServerLevel level, Entity entity) {
        try {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                return null;
            }
            Vec3 position = entity.position();
            for (ServerSubLevel subLevel : container.getAllSubLevels()) {
                LevelPlot plot = subLevel.getPlot();
                if (plot != null && plot.contains(position)) {
                    return subLevel;
                }
                if (logicalBounds(subLevel).contains(position)) {
                    return subLevel;
                }
                Vector3dc pose = subLevel.logicalPose().position();
                if (position.distanceToSqr(pose.x(), pose.y(), pose.z()) < ABOARD_RADIUS * ABOARD_RADIUS) {
                    return subLevel;
                }
            }
        } catch (Throwable t) {
            GenesisMod.LOGGER.warn("Ship-aboard check failed", t);
        }
        return null;
    }

    /** Logs every sub-level in a level with its position and distance to a point — a transfer-debug aid. */
    public static void logShips(ServerLevel level, Vec3 position, String tag) {
        try {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                GenesisMod.LOGGER.info("[SHIPS {}] no sub-level container in {}", tag, level.dimension().location());
                return;
            }
            int count = 0;
            for (ServerSubLevel subLevel : container.getAllSubLevels()) {
                Vector3dc p = subLevel.logicalPose().position();
                double dist = Math.sqrt(position.distanceToSqr(p.x(), p.y(), p.z()));
                GenesisMod.LOGGER.info("[SHIPS {}] {} at ({}, {}, {}) dist={} from traveler",
                        tag, subLevel.getUniqueId(), (long) p.x(), (long) p.y(), (long) p.z(), (long) dist);
                count++;
            }
            if (count == 0) {
                GenesisMod.LOGGER.info("[SHIPS {}] no sub-levels present in {}", tag, level.dimension().location());
            }
        } catch (Throwable t) {
            GenesisMod.LOGGER.warn("Ship dump failed", t);
        }
    }

    /**
     * The nearest ship whose logical pose (or inflated bounds) sits within
     * {@code radius} of a point — a looser net than {@link #shipCarrying} for
     * catching crew standing well off a large deck.
     */
    @Nullable
    public static ServerSubLevel nearestShip(ServerLevel level, Vec3 position, double radius) {
        return nearestShip(level, position, radius, null);
    }

    /**
     * Candidate-restricted form used after a cross-dimension warp. Dimensional
     * Sable gives the arrived craft new UUIDs, so seat restoration must select
     * from that exact set instead of an unrelated ship near the same point.
     */
    @Nullable
    static ServerSubLevel nearestShip(ServerLevel level, Vec3 position, double radius,
                                      @Nullable Collection<UUID> candidateShipIds) {
        try {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                return null;
            }
            Set<UUID> allowed = candidateShipIds == null ? null : new HashSet<>(candidateShipIds);
            ServerSubLevel best = null;
            double bestDistanceSq = radius * radius;
            for (ServerSubLevel subLevel : container.getAllSubLevels()) {
                if (allowed != null && !allowed.contains(subLevel.getUniqueId())) {
                    continue;
                }
                if (logicalBounds(subLevel).inflate(radius * 0.5).contains(position)) {
                    return subLevel;
                }
                Vector3dc pose = subLevel.logicalPose().position();
                double d = position.distanceToSqr(pose.x(), pose.y(), pose.z());
                if (d < bestDistanceSq) {
                    bestDistanceSq = d;
                    best = subLevel;
                }
            }
            return best;
        } catch (Throwable t) {
            GenesisMod.LOGGER.warn("Nearest-ship check failed", t);
            return null;
        }
    }

    /** Players belonging to the ship's travel unit (seated in the plot or standing on deck). */
    public static List<ServerPlayer> crewPlayers(ServerLevel level, ServerSubLevel primary) {
        List<ServerPlayer> crew = new ArrayList<>();
        try {
            for (ServerSubLevel subLevel : SubLevelHelper.getLoadingDependencyChain(primary)) {
                for (Entity entity : collectPlotEntities(level, subLevel)) {
                    addCrewPlayers(entity, crew);
                }
                for (Entity entity : collectLogicalRiders(level, subLevel)) {
                    addCrewPlayers(entity, crew);
                }
            }
        } catch (Throwable t) {
            GenesisMod.LOGGER.warn("Crew lookup failed", t);
        }
        return crew;
    }

    private static void addCrewPlayers(Entity root, List<ServerPlayer> out) {
        for (Entity entity : root.getSelfAndPassengers().toList()) {
            if (entity instanceof ServerPlayer player && !out.contains(player)) {
                out.add(player);
            }
        }
    }

    /**
     * Teleports the ship's whole travel unit — dependency chain, plot entities,
     * seated players, standing crew — so the primary lands at {@code arrival} in
     * {@code targetLevel}. Works for both same-dimension and cross-dimension moves.
     *
     * @return true if the ship was moved
     */
    public static boolean transferShip(ServerSubLevel primary, ServerLevel targetLevel, Vec3 arrival) {
        return transferShip(primary, targetLevel, arrival, null);
    }

    /**
     * @param orientation if non-null, the primary (and its chain) arrives with
     *                    this orientation — used to face ships into their
     *                    travel direction when entering hyperspace.
     */
    public static boolean transferShip(ServerSubLevel primary, ServerLevel targetLevel, Vec3 arrival,
                                       @Nullable org.joml.Quaterniondc orientation) {
        if (primary == null || targetLevel == null || arrival == null) {
            return false;
        }

        try {
            ServerLevel originLevel = primary.getLevel() instanceof ServerLevel serverLevel ? serverLevel : null;
            if (originLevel == null) {
                return false;
            }
            ServerSubLevelContainer originContainer = SubLevelContainer.getContainer(originLevel);
            ServerSubLevelContainer targetContainer = SubLevelContainer.getContainer(targetLevel);
            if (originContainer == null || targetContainer == null) {
                GenesisMod.LOGGER.warn("Ship transfer aborted: missing sub-level container ({} -> {})",
                        originLevel.dimension().location(), targetLevel.dimension().location());
                return false;
            }

            Collection<ServerSubLevel> chain = SubLevelHelper.getLoadingDependencyChain(primary);
            if (chain.isEmpty()) {
                return false;
            }
            List<UUID> chainIds = chain.stream().map(SubLevel::getUniqueId).toList();

            Vector3d anchor = new Vector3d(primary.logicalPose().position());
            List<LogicalRider> riders = new ArrayList<>();
            for (ServerSubLevel subLevel : chain) {
                for (Entity rider : collectLogicalRiders(originLevel, subLevel)) {
                    riders.add(new LogicalRider(rider, new Vector3d(
                            rider.getX() - anchor.x, rider.getY() - anchor.y, rider.getZ() - anchor.z)));
                }
            }

            // Capture every crew player BEFORE the move. Seat entities can end
            // up outside their plot's collection bounds (a seated player's raw
            // position is a shadow-region plot coord), so relying on the plot
            // sweep to carry them is unreliable — we reconcile each captured
            // player onto the ship afterward instead.
            List<ServerPlayer> crew = crewPlayers(originLevel, primary);

            boolean crossDimension = originLevel != targetLevel;
            // Who was sitting on what, before anything moves. Used only to put
            // back riders the move dropped; anyone still seated is left alone.
            List<RidingPreservation.SeatedRider> seated =
                    RidingPreservation.snapshot(originLevel, primary, crew);
            boolean moved;
            List<UUID> arrivedIds = chainIds;
            if (!crossDimension) {
                moved = teleportLoadedChain(chain, primary, arrival, orientation);
            } else if (DimensionalSableBridge.isAvailable()) {
                // Dimensional Sable performs the move inside the engine, so the
                // craft is never a stranger to Sable's own bookkeeping — which
                // is what made the hand-rolled version get unloaded moments
                // after it arrived. It carries the connected chain and the
                // craft's entities across as part of the same operation.
                //
                // Nothing is done to the craft afterwards. Restoring momentum
                // and repositioning the crew were both tried and both made
                // things worse: the craft either bolted away from its crew or
                // the deck slid out from under them. Left alone, the warp is
                // what worked.
                holdArrivalChunks(targetLevel, arrival);
                Set<UUID> targetIdsBeforeWarp = subLevelIds(targetContainer);
                int movedCount = DimensionalSableBridge.warp(primary, targetLevel,
                        new Vector3d(arrival.x, arrival.y, arrival.z));
                arrivedIds = resolveArrivedSubLevelIds(
                        targetContainer, targetIdsBeforeWarp, arrival, movedCount);
                moved = movedCount > 0 && !arrivedIds.isEmpty();
                if (moved) {
                    for (UUID arrivedId : arrivedIds) {
                        CraftKeepAlive.protect(arrivedId);
                    }
                } else if (movedCount > 0) {
                    GenesisMod.LOGGER.error(
                            "[WARP] Dimensional Sable reported {} moved sub-levels, but none appeared in {}",
                            movedCount, targetLevel.dimension().location());
                }
            } else {
                arrivedIds = transferChainToTargetLevel(
                        originLevel, originContainer, targetContainer, primary, chain, arrival, orientation, crew);
                moved = !arrivedIds.isEmpty();
            }
            if (!moved) {
                return false;
            }

            // Standing crew rides along at the same offset from the ship anchor.
            for (LogicalRider rider : riders) {
                if (rider.entity() instanceof ServerPlayer) {
                    continue; // players handled by the reconciliation below
                }
                // Dimensional Sable already moves entities in the craft's
                // logical bounds. Cross-dimension teleports may either move the
                // same instance or replace it and remove the old one; touching
                // either again duplicates or detaches the rider.
                if (crossDimension
                        && (rider.entity().isRemoved() || rider.entity().level() == targetLevel)) {
                    continue;
                }
                Vec3 riderTarget = new Vec3(
                        arrival.x + rider.offset().x, arrival.y + rider.offset().y, arrival.z + rider.offset().z);
                Vec3 velocity = rider.entity().getDeltaMovement();
                Entity arrived = EntityTeleporter.teleportEntityAndPassengers(rider.entity(), targetLevel, riderTarget, new Quaterniond());
                if (arrived != null) {
                    arrived.setDeltaMovement(velocity);
                    arrived.resetFallDistance();
                }
            }

            // Deterministic crew carry: guarantee every captured player ends up
            // on the ship at its arrival, seated if a seat is there. Same-dim
            // moves leave already-correct players alone; cross-dim moves catch
            // anyone the plot sweep missed.
            if (crossDimension) {
                RidingPreservation.restore(targetLevel, arrival, seated, arrivedIds);
                queueCrewReconciliation(crew, targetLevel, arrival, arrivedIds);
                // Queue the visibility re-sync INDEPENDENTLY of crew capture.
                // crewPlayers() can legitimately come back empty (a player
                // standing just outside the ship's logical bounds), and the
                // reconciliation used to bail out in that case — taking the
                // re-sync with it and leaving the ship permanently invisible.
                // Announcing the ship is needed whether or not we identified
                // anyone as crew, so it no longer rides on that.
                GenesisMod.LOGGER.info("[FOLLOW] arrived sub-levels: {}", arrivedIds);
                queueVisibilitySync(targetLevel,
                        crew.stream().map(Entity::getUUID).distinct().toList(), arrivedIds);
            } else {
                reconcileCrew(crew, targetLevel, arrival, chainIds);
            }

            return true;
        } catch (Throwable t) {
            GenesisMod.LOGGER.error("Ship transfer failed", t);
            return false;
        }
    }

    /**
     * Forces every missed crew player onto the ship at its new position. Plot
     * membership, rather than distance to the visible pose, identifies riders
     * that already crossed while seated and preserves that riding relationship.
     */
    private static void reconcileCrew(List<ServerPlayer> crew, ServerLevel targetLevel, Vec3 arrival,
                                      List<UUID> transferredShipIds) {
        for (ServerPlayer player : crew) {
            if (player.level() == targetLevel) {
                // A seated player's raw server coordinates remain inside the
                // Sable shadow plot, not near the ship's visible arrival pose.
                // Treat membership in a transferred plot as authoritative or
                // reconciliation would unseat a correctly carried passenger.
                ServerSubLevel carriedBy = shipCarrying(targetLevel, player);
                if (carriedBy != null) {
                    // Any craft carrying the player is authoritative, not just
                    // one whose id we recognise. The warp rebuilds the craft in
                    // the destination under a NEW uuid, so matching against the
                    // departing ids never succeeds after a dimension change —
                    // and a player who arrived correctly, still in their seat,
                    // was being unseated here and dropped at the craft's anchor.
                    continue;
                }
                if (player.position().distanceToSqr(arrival) < 48.0 * 48.0) {
                    continue;
                }
            }

            if (player.isPassenger()) {
                player.stopRiding();
            }
            player.teleportTo(targetLevel, arrival.x, arrival.y + 1.5, arrival.z,
                    player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO);
            player.resetFallDistance();
        }

    }

    /** Schedules one visibility handoff after the arriving clients are ready. */
    private static void queueVisibilitySync(ServerLevel targetLevel, List<UUID> crewIds, List<UUID> shipIds) {
        if (shipIds.isEmpty()) {
            return;
        }
        List<UUID> ships = List.copyOf(shipIds);
        List<UUID> crew = List.copyOf(crewIds);
        long fallback = targetLevel.getGameTime() + VISIBILITY_SYNC_FALLBACK_TICKS;
        Set<UUID> probedCrew = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Set<UUID> reportedCrew = java.util.concurrent.ConcurrentHashMap.newKeySet();
        java.util.Map<UUID, Set<UUID>> missingShipsByCrew =
                new java.util.concurrent.ConcurrentHashMap<>();
        PENDING_VISIBILITY_SYNCS.put(ships.get(0),
                new PendingVisibilitySync(targetLevel.dimension(), fallback, crew,
                        probedCrew, reportedCrew, missingShipsByCrew, ships));
        long guardedUntil = fallback + VISIBILITY_SWEEP_GUARD_TICKS;
        for (UUID shipId : ships) {
            VISIBILITY_MANAGED_UNTIL.put(shipId, guardedUntil);
        }
    }

    /**
     * Repairs only the player/ship pairs the client proved were absent. Clients
     * that already hold a plot receive no stop packet and no second full craft
     * snapshot, which keeps the first destination frames responsive.
     */
    private static void repairMissingShips(ServerLevel targetLevel, List<ServerPlayer> crew,
                                           PendingVisibilitySync pending) {
        try {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(targetLevel);
            if (container == null) {
                return;
            }

            for (UUID shipId : pending.shipIds()) {
                if (!(container.getSubLevel(shipId) instanceof ServerSubLevel subLevel)) {
                    GenesisMod.LOGGER.warn("[RETRACK] missing destination ship {} while repairing visibility",
                            shipId);
                    continue;
                }

                int repairCount = 0;
                for (ServerPlayer player : crew) {
                    Set<UUID> missing = pending.missingShipsByCrew().get(player.getUUID());
                    if (missing != null && missing.contains(shipId)) {
                        subLevel.getTrackingPlayers().remove(player.getUUID());
                        repairCount++;
                    }
                }
                if (repairCount == 0) {
                    continue;
                }

                container.trackingSystem().onSubLevelAdded(subLevel);
                GenesisMod.LOGGER.info("[RETRACK] repaired ship {} visibility for {} players in {}",
                        shipId, repairCount, targetLevel.dimension().location());
            }
        } catch (Throwable t) {
            GenesisMod.LOGGER.warn("Could not repair transferred sub-level visibility", t);
        }
    }

    /**
     * Forces one clean re-sync after the destination client level is ready.
     * Every currently tracking player is stopped first, not just the captured
     * crew, because Sable sends a full bundle to its entire tracking set when a
     * sub-level is added.
     */
    private static void retrackShips(ServerLevel targetLevel, List<ServerPlayer> crew, List<UUID> shipIds) {
        if (shipIds.isEmpty()) {
            return;
        }
        try {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(targetLevel);
            if (container == null) {
                return;
            }
            org.joml.Vector2i origin = container.getOrigin();
            for (UUID shipId : shipIds) {
                if (!(container.getSubLevel(shipId) instanceof ServerSubLevel subLevel)
                        || subLevel.getPlot() == null) {
                    // Silence here is what hid the real failure for so long:
                    // report the miss, and dump what IS present to compare.
                    GenesisMod.LOGGER.warn("[RETRACK] ship {} NOT FOUND in {} ({} sub-levels present)",
                            shipId, targetLevel.dimension().location(),
                            container.getAllSubLevels().size());
                    for (ServerSubLevel present : container.getAllSubLevels()) {
                        Vector3dc pp = present.logicalPose().position();
                        double nearest = Double.MAX_VALUE;
                        for (ServerPlayer p : targetLevel.players()) {
                            nearest = Math.min(nearest,
                                    Math.sqrt(p.position().distanceToSqr(pp.x(), pp.y(), pp.z())));
                        }
                        GenesisMod.LOGGER.warn("[RETRACK]   present: {} at ({}, {}, {}) nearestPlayer={}",
                                present.getUniqueId(), (long) pp.x(), (long) pp.y(), (long) pp.z(),
                                nearest == Double.MAX_VALUE ? "none" : (long) nearest);
                    }
                    continue;
                }
                ChunkPos plotPos = subLevel.getPlot().plotPos;
                long plotCoordinate = ChunkPos.asLong(plotPos.x - origin.x, plotPos.z - origin.y);
                var stopPacket = new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
                        new dev.ryanhcode.sable.network.packets.tcp.ClientboundStopTrackingSubLevelPacket(plotCoordinate));

                java.util.Map<UUID, ServerPlayer> recipients = new java.util.LinkedHashMap<>();
                for (UUID trackingId : List.copyOf(subLevel.getTrackingPlayers())) {
                    ServerPlayer trackingPlayer = targetLevel.getServer().getPlayerList().getPlayer(trackingId);
                    if (trackingPlayer != null && trackingPlayer.level() == targetLevel) {
                        recipients.put(trackingId, trackingPlayer);
                    } else {
                        subLevel.getTrackingPlayers().remove(trackingId);
                    }
                }
                for (ServerPlayer player : crew) {
                    if (player.level() != targetLevel) {
                        continue;
                    }
                    recipients.put(player.getUUID(), player);
                }
                for (ServerPlayer player : recipients.values()) {
                    player.connection.send(stopPacket);
                    subLevel.getTrackingPlayers().remove(player.getUUID());
                }
                container.trackingSystem().onSubLevelAdded(subLevel);
                GenesisMod.LOGGER.info("[RETRACK] re-synced ship {} (plot {}) to {} players in {}",
                        shipId, plotCoordinate, recipients.size(), targetLevel.dimension().location());
            }
        } catch (Throwable t) {
            GenesisMod.LOGGER.warn("Could not re-sync transferred sub-levels for tracking", t);
        }
    }

    private static void queueCrewReconciliation(List<ServerPlayer> crew, ServerLevel targetLevel, Vec3 arrival,
                                                List<UUID> transferredShipIds) {
        List<UUID> crewIds = crew.stream().map(Entity::getUUID).distinct().toList();
        if (crewIds.isEmpty()) {
            return;
        }
        PENDING_CREW_TRANSFERS.offer(new PendingCrewTransfer(
                targetLevel.dimension(), targetLevel.getGameTime() + CREW_SYNC_DELAY_TICKS,
                crewIds, arrival, List.copyOf(transferredShipIds)));
    }

    // --- Same-dimension move: plots (and seats inside them) stay put ---

    private static boolean teleportLoadedChain(Collection<ServerSubLevel> chain, ServerSubLevel primary, Vec3 arrival,
                                               @Nullable org.joml.Quaterniondc orientation) {
        Vector3d anchor = new Vector3d(primary.logicalPose().position());
        Vector3d arrivalPosition = new Vector3d(arrival.x, arrival.y, arrival.z);

        for (ServerSubLevel subLevel : chain) {
            RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
            if (handle == null || !handle.isValid()) {
                GenesisMod.LOGGER.warn("Ship teleport aborted: sub-level {} has no valid rigid body", subLevel.getUniqueId());
                return false;
            }

            Vector3d targetPosition = new Vector3d(subLevel.logicalPose().position()).sub(anchor).add(arrivalPosition);
            handle.teleport(targetPosition, orientation != null ? orientation : subLevel.logicalPose().orientation());
        }

        return true;
    }

    // --- Cross-dimension move: serialize chain, move plot entities by plot delta ---

    private static Set<UUID> subLevelIds(ServerSubLevelContainer container) {
        Set<UUID> ids = new HashSet<>();
        for (ServerSubLevel subLevel : container.getAllSubLevels()) {
            ids.add(subLevel.getUniqueId());
        }
        return ids;
    }

    /**
     * Resolves the identities Dimensional Sable assigned to a warped chain.
     * Its public result is only a count; the UUIDs are obtained by comparing the
     * destination container immediately before and after its synchronous warp.
     */
    private static List<UUID> resolveArrivedSubLevelIds(ServerSubLevelContainer targetContainer,
                                                        Set<UUID> idsBeforeWarp, Vec3 arrival,
                                                        int expectedCount) {
        if (expectedCount <= 0) {
            return List.of();
        }

        List<ServerSubLevel> arrived = new ArrayList<>();
        for (ServerSubLevel subLevel : targetContainer.getAllSubLevels()) {
            if (!idsBeforeWarp.contains(subLevel.getUniqueId())) {
                arrived.add(subLevel);
            }
        }
        arrived.sort(Comparator.comparingDouble(subLevel -> {
            Vector3dc position = subLevel.logicalPose().position();
            return arrival.distanceToSqr(position.x(), position.y(), position.z());
        }));

        if (arrived.size() != expectedCount) {
            GenesisMod.LOGGER.warn("[WARP] expected {} new sub-levels in {}, found {}",
                    expectedCount, targetContainer.getLevel().dimension().location(), arrived.size());
        }
        if (arrived.size() > expectedCount) {
            arrived = new ArrayList<>(arrived.subList(0, expectedCount));
        }
        return arrived.stream().map(SubLevel::getUniqueId).toList();
    }

    private static List<UUID> transferChainToTargetLevel(ServerLevel originLevel,
                                                         ServerSubLevelContainer originContainer,
                                                         ServerSubLevelContainer targetContainer,
                                                         ServerSubLevel primary,
                                                         Collection<ServerSubLevel> chain, Vec3 arrival,
                                                         @Nullable org.joml.Quaterniondc orientation,
                                                         List<ServerPlayer> crew) {
        ServerLevel targetLevel = targetContainer.getLevel() instanceof ServerLevel serverLevel ? serverLevel : null;
        if (targetLevel == null) {
            return List.of();
        }

        // Claim the destination before anything lands there, so the freshly
        // loaded craft is never sitting in an unheld chunk.
        holdArrivalChunks(targetLevel, arrival);
        List<UUID> dependencies = chain.stream().map(SubLevel::getUniqueId).toList();
        List<PreparedSubLevel> prepared = new ArrayList<>();
        Vector3d anchor = new Vector3d(primary.logicalPose().position());
        Vector3d arrivalPosition = new Vector3d(arrival.x, arrival.y, arrival.z);

        for (ServerSubLevel subLevel : chain) {
            int[] targetPlot = findEmptyPlot(targetContainer);
            if (targetPlot == null) {
                GenesisMod.LOGGER.warn("Ship transfer aborted: no free sub-level plot in {}",
                        targetLevel.dimension().location());
                return List.of();
            }

            SubLevelData originalData = SubLevelSerializer.toData(subLevel, dependencies);
            CompoundTag tag = originalData.fullTag().copy();
            CompoundTag plotTag = tag.getCompound("plot").copy();
            plotTag.putInt("plot_x", targetPlot[0]);
            plotTag.putInt("plot_z", targetPlot[1]);
            tag.put("plot", plotTag);

            var pose = SableNBTUtils.readPose3d(tag.getCompound("pose"));
            Vector3d targetPosition = new Vector3d(subLevel.logicalPose().position()).sub(anchor).add(arrivalPosition);
            pose.position().set(targetPosition);
            if (orientation != null) {
                pose.orientation().set(orientation);
            }
            tag.put("pose", SableNBTUtils.writePose3d(pose));

            SubLevelData movedData = new SubLevelData(originalData.uuid(), originalData.bounds(), pose, originalData.dependencies(), tag);
            BlockPos oldPlotOrigin = plotOrigin(subLevel);
            // Only TOP-LEVEL plot entities (seats, stands). Their riders travel
            // WITH them via teleportEntityAndPassengers, which re-seats the
            // player on the moved seat automatically — no manual unseat/re-seat.
            List<Entity> plotEntities = new ArrayList<>();
            for (Entity entity : collectPlotEntities(originLevel, subLevel)) {
                if (!entity.isPassenger()) {
                    plotEntities.add(entity);
                }
            }
            prepared.add(new PreparedSubLevel(subLevel, movedData, oldPlotOrigin, plotEntities));
            // Reserve the plot so findEmptyPlot doesn't hand it out twice while planning the chain.
            targetContainer.getOccupancy().set(targetContainer.getIndex(targetPlot[0], targetPlot[1]));
        }

        // Release the reservations; fullyLoad claims plots itself from the plot tag.
        for (PreparedSubLevel preparedSubLevel : prepared) {
            targetContainer.getOccupancy().clear(targetContainer.getIndex(
                    preparedSubLevel.data().fullTag().getCompound("plot").getInt("plot_x"),
                    preparedSubLevel.data().fullTag().getCompound("plot").getInt("plot_z")
            ));
        }

        List<ServerSubLevel> loaded = new ArrayList<>();
        try {
            for (PreparedSubLevel preparedSubLevel : prepared) {
                ServerSubLevel loadedSubLevel = SubLevelSerializer.fullyLoad(targetLevel, preparedSubLevel.data());
                if (loadedSubLevel == null) {
                    GenesisMod.LOGGER.warn("Ship transfer failed to load sub-level {} in target dimension",
                            preparedSubLevel.data().uuid());
                    removeLoaded(targetContainer, loaded);
                    return List.of();
                }
                loaded.add(loadedSubLevel);
                // Refuse the unload for this craft while the destination settles.
                CraftKeepAlive.protect(loadedSubLevel.getUniqueId());

                // Seats (and their seated players), contraptions, and mob riders
                // move by the plot-to-plot offset so they stay glued to the same
                // blocks. teleportEntityAndPassengers carries riders across and
                // re-seats them on the moved seat, so a seated player travels
                // with the ship and stays seated — no unseat, no re-seat race.
                BlockPos newPlotOrigin = plotOrigin(loadedSubLevel);
                if (newPlotOrigin != null && preparedSubLevel.plotOrigin() != null) {
                    Vec3 delta = new Vec3(
                            newPlotOrigin.getX() - preparedSubLevel.plotOrigin().getX(),
                            0.0,
                            newPlotOrigin.getZ() - preparedSubLevel.plotOrigin().getZ());
                    for (Entity entity : preparedSubLevel.plotEntities()) {
                        Vec3 velocity = entity.getDeltaMovement();
                        Entity arrived = EntityTeleporter.teleportEntityAndPassengers(
                                entity, targetLevel, entity.position().add(delta), new Quaterniond());
                        if (arrived != null) {
                            arrived.setDeltaMovement(velocity);
                            arrived.resetFallDistance();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            GenesisMod.LOGGER.error("Ship transfer failed while loading sub-levels", t);
            removeLoaded(targetContainer, loaded);
            return List.of();
        }

        List<UUID> crewIds = crew.stream().map(Entity::getUUID).distinct().toList();
        for (PreparedSubLevel preparedSubLevel : prepared) {
            // Sable's stop-tracking packet identifies a client sub-level by
            // plot alone, not by dimension. Do not send the old dimension's
            // removal to players who are following this same ship into the
            // destination; it can otherwise delete the freshly loaded copy.
            preparedSubLevel.original().getTrackingPlayers().removeAll(crewIds);
            originContainer.removeSubLevel(preparedSubLevel.original(), SubLevelRemovalReason.REMOVED);
        }

        return loaded.stream().map(SubLevel::getUniqueId).toList();
    }

    // --- Crew collection ---

    private static final Predicate<Entity> TRAVELS_WITH_SHIP =
            entity -> !entity.isPassenger() && !entity.isSpectator() && !(entity instanceof ServerSubLevelMarker);

    /** Marker interface no Genesis entity implements; keeps the predicate readable. */
    private interface ServerSubLevelMarker {
    }

    /** Top-level entities inside the sub-level's plot (shadow region): seats, stands, and their riders. */
    private static List<Entity> collectPlotEntities(ServerLevel level, ServerSubLevel subLevel) {
        LevelPlot plot = subLevel.getPlot();
        if (plot == null) {
            return List.of();
        }
        ChunkPos min = plot.getChunkMin();
        ChunkPos max = plot.getChunkMax();
        AABB box = new AABB(
                min.getMinBlockX(), level.getMinBuildHeight(), min.getMinBlockZ(),
                max.getMaxBlockX() + 1.0, level.getMaxBuildHeight(), max.getMaxBlockZ() + 1.0);
        return level.getEntities((Entity) null, box, TRAVELS_WITH_SHIP);
    }

    /** Top-level entities standing on/around the ship in logical space (deck crew). */
    private static List<Entity> collectLogicalRiders(ServerLevel level, ServerSubLevel subLevel) {
        return level.getEntities((Entity) null, logicalBounds(subLevel), TRAVELS_WITH_SHIP);
    }

    /**
     * The ship's actual logical-space bounding box, slightly inflated so someone
     * standing on the deck counts as aboard. Deliberately tight — a player merely
     * flying NEAR a ship must not be treated as crew (it would exempt them from
     * atmosphere teleports).
     */
    private static AABB logicalBounds(ServerSubLevel subLevel) {
        var box = subLevel.boundingBox();
        if (box != null && box.maxX() > box.minX()) {
            return new AABB(box.minX() - 2.0, box.minY() - 2.0, box.minZ() - 2.0,
                    box.maxX() + 2.0, box.maxY() + 3.0, box.maxZ() + 2.0);
        }
        Vector3dc position = subLevel.logicalPose().position();
        return new AABB(
                position.x() - 24.0, position.y() - 24.0, position.z() - 24.0,
                position.x() + 24.0, position.y() + 24.0, position.z() + 24.0);
    }

    private static BlockPos plotOrigin(ServerSubLevel subLevel) {
        LevelPlot plot = subLevel.getPlot();
        if (plot == null) {
            return null;
        }
        ChunkPos min = plot.getChunkMin();
        return new BlockPos(min.getMinBlockX(), 0, min.getMinBlockZ());
    }

    // --- Plumbing ---

    private static int[] findEmptyPlot(ServerSubLevelContainer container) {
        int side = 1 << container.getLogSideLength();
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                if (!container.getOccupancy().get(container.getIndex(x, z))) {
                    return new int[]{x, z};
                }
            }
        }
        return null;
    }

    private static void removeLoaded(ServerSubLevelContainer container, List<ServerSubLevel> loaded) {
        for (ServerSubLevel subLevel : loaded) {
            try {
                container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
            } catch (Throwable t) {
                GenesisMod.LOGGER.warn("Failed to roll back partially transferred sub-level", t);
            }
        }
    }

    private record PreparedSubLevel(ServerSubLevel original, SubLevelData data, BlockPos plotOrigin,
                                    List<Entity> plotEntities) {
    }

    private record LogicalRider(Entity entity, Vector3d offset) {
    }

    private record PendingCrewTransfer(ResourceKey<Level> targetDimension, long readyGameTime,
                                       List<UUID> crewIds, Vec3 arrival, List<UUID> shipIds) {
    }

    private record PendingVisibilitySync(ResourceKey<Level> targetDimension, long fallbackGameTime,
                                         List<UUID> crewIds, Set<UUID> probedCrewIds,
                                         Set<UUID> reportedCrewIds,
                                         java.util.Map<UUID, Set<UUID>> missingShipsByCrew,
                                         List<UUID> shipIds) {
    }

    private record PendingShipTransfer(ResourceKey<Level> targetDimension, long readyGameTime,
                                       UUID shipId, ResourceKey<Level> originDimension,
                                       Vec3 arrival, List<UUID> crewIds) {
    }

    /** Brings the ship across now that its crew has finished loading in. */
    private static void runFollowTransfer(ServerLevel targetLevel, PendingShipTransfer pending) {
        try {
            ServerLevel originLevel = targetLevel.getServer().getLevel(pending.originDimension());
            if (originLevel == null) {
                return;
            }
            ServerSubLevelContainer originContainer = SubLevelContainer.getContainer(originLevel);
            if (originContainer == null
                    || !(originContainer.getSubLevel(pending.shipId()) instanceof ServerSubLevel ship)) {
                GenesisMod.LOGGER.warn("[FOLLOW] ship {} no longer in {}; nothing to bring across",
                        pending.shipId(), pending.originDimension().location());
                return;
            }

            boolean moved = transferShip(ship, targetLevel, pending.arrival());
            GenesisMod.LOGGER.info("[FOLLOW] ship {} -> {} : {}", pending.shipId(),
                    targetLevel.dimension().location(), moved ? "OK" : "FAILED");

            // Put the crew back on the deck now that the ship has arrived.
            if (moved) {
                for (UUID crewId : pending.crewIds()) {
                    ServerPlayer player = targetLevel.getServer().getPlayerList().getPlayer(crewId);
                    if (player != null && player.level() == targetLevel) {
                        player.teleportTo(targetLevel, pending.arrival().x, pending.arrival().y + 2.0,
                                pending.arrival().z, player.getYRot(), player.getXRot());
                        player.setDeltaMovement(Vec3.ZERO);
                        player.resetFallDistance();
                    }
                }
            }
        } catch (Throwable t) {
            GenesisMod.LOGGER.error("[FOLLOW] delayed ship transfer failed", t);
        }
    }
}
