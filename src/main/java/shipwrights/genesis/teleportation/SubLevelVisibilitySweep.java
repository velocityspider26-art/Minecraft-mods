package shipwrights.genesis.teleportation;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3dc;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import shipwrights.genesis.GenesisMod;

import java.util.Collection;
import java.util.List;

/**
 * Makes sure a ship a player is standing next to is actually visible to them.
 *
 * <p>A sub-level is announced to a client once, when it is added. If that
 * announcement is missed — most often because the client was still swapping
 * dimensions when its ship was transferred — the client rejects every later
 * update ("movement packet for a non-existent sub-level") and the craft stays
 * invisible until the world is reloaded. Rather than depending on any one
 * transfer path getting the timing right, this sweep watches for the symptom
 * directly: a sub-level with players close by, none of whom are tracking it.
 * That state is only ever produced by a lost announcement, so re-announcing is
 * always the right response.</p>
 *
 * <p>Deliberately narrow, because re-announcing to a client that already has
 * the plot allocated crashes it ("Plot already exists"). The sweep fires only
 * when <em>no</em> nearby player tracks the sub-level, so the announcement can
 * only reach clients that genuinely lack it.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class SubLevelVisibilitySweep {
    /** How often the sweep runs, in ticks. */
    private static final int INTERVAL = 40;
    /**
     * How often each ship's chunks are re-claimed. Must be comfortably shorter
     * than the ticket lifetime so the claim never lapses while a craft is
     * flying.
     */
    private static final int CHUNK_HOLD_INTERVAL = 5;
    /** Chunk radius held around each craft. */
    private static final int CHUNK_HOLD_RADIUS = 5;
    /** Seconds of travel to claim ahead of a moving craft. */
    private static final double LEAD_SECONDS = 1.5;
    /** Chunk radius force-loaded around a craft's footprint. */
    private static final int FORCE_RADIUS = 2;
    /**
     * Keeps the chunks under every craft loaded.
     *
     * <p>Sable's physics ticket manager evicts any sub-level standing in a
     * chunk that is not loaded — it moves the craft into its holding store and
     * removes it from the container. In the space dimension almost nothing is
     * loaded by default, and a craft drifts, so claiming only its arrival spot
     * buys a couple of seconds before it flies out of the claim and is deleted
     * mid-flight. The claim therefore has to follow the craft.</p>
     */
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> CRAFT_TICKET =
            net.minecraft.server.level.TicketType.create("genesis_craft_hold",
                    java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 200);
    /** How close a player must be for a ship to be considered theirs to see. */
    private static final double NEARBY = 160.0;

    private SubLevelVisibilitySweep() {
    }

    /** Re-claims the chunks under every craft so none is evicted mid-flight. */
    private static void holdCraftChunks(ServerLevel level, ServerSubLevelContainer container) {
        try {
            java.util.Set<Long> wanted = new java.util.HashSet<>();
            for (ServerSubLevel subLevel : List.copyOf(container.getAllSubLevels())) {
                if (subLevel.isRemoved()) {
                    continue;
                }
                Vector3dc pose = subLevel.logicalPose().position();
                claim(level, pose.x(), pose.z(), wanted);

                // Claim ahead of the craft as well. A launch leaves real speed on
                // the hull, and a claim placed only where it currently is gets
                // outrun between passes — the craft crosses into unclaimed chunks
                // and is evicted mid-flight.
                // Report when a claim has not taken effect: Sable evicts a craft
                // whose chunk is not ticking, so this distinguishes "the claim is
                // wrong" from "something else is removing it".
                if (level.getGameTime() % 40 == 0) {
                    net.minecraft.core.BlockPos at =
                            net.minecraft.core.BlockPos.containing(pose.x(), pose.y(), pose.z());
                    if (!level.isPositionEntityTicking(at)) {
                        GenesisMod.LOGGER.warn("[HOLD] craft {} at ({}, {}) chunk NOT ticking in {}",
                                subLevel.getUniqueId(), (long) pose.x(), (long) pose.z(),
                                level.dimension().location());
                    }
                }

                Vector3dc last = subLevel.lastPose().position();
                double vx = (pose.x() - last.x()) * 20.0;
                double vz = (pose.z() - last.z()) * 20.0;
                if (vx * vx + vz * vz > 1.0) {
                    claim(level, pose.x() + vx * LEAD_SECONDS, pose.z() + vz * LEAD_SECONDS, wanted);
                }
            }
            releaseUnused(level, wanted);
        } catch (Throwable t) {
            GenesisMod.LOGGER.warn("[SWEEP] could not hold craft chunks", t);
        }
    }

    /** Chunks currently force-loaded for a craft, so they can be released again. */
    private static final java.util.Map<ResourceKey<Level>, java.util.Set<Long>> FORCED =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Claims one region so Sable counts it as block-ticking.
     *
     * <p>A region ticket alone was not enough in practice — the craft kept being
     * evicted for standing in a chunk Sable did not consider ticking. Forcing
     * the chunk is the strongest guarantee the game offers and is exactly what
     * the check is asking about, so the footprint is force-loaded as well as
     * ticketed. Forced chunks are tracked and released once no craft needs
     * them, so this cannot leak loaded chunks over a session.</p>
     */
    private static void claim(ServerLevel level, double x, double z, java.util.Set<Long> wanted) {
        net.minecraft.world.level.ChunkPos centre = new net.minecraft.world.level.ChunkPos(
                net.minecraft.core.BlockPos.containing(x, 0.0, z));
        level.getChunkSource().addRegionTicket(CRAFT_TICKET, centre, CHUNK_HOLD_RADIUS, centre);

        for (int dx = -FORCE_RADIUS; dx <= FORCE_RADIUS; dx++) {
            for (int dz = -FORCE_RADIUS; dz <= FORCE_RADIUS; dz++) {
                int cx = centre.x + dx;
                int cz = centre.z + dz;
                long key = net.minecraft.world.level.ChunkPos.asLong(cx, cz);
                wanted.add(key);
                if (!level.getForcedChunks().contains(key)) {
                    level.setChunkForced(cx, cz, true);
                }
            }
        }
    }

    /** Releases chunks forced for a craft that no longer needs them. */
    private static void releaseUnused(ServerLevel level, java.util.Set<Long> wanted) {
        java.util.Set<Long> held = FORCED.computeIfAbsent(level.dimension(),
                key -> java.util.concurrent.ConcurrentHashMap.newKeySet());
        for (java.util.Iterator<Long> it = held.iterator(); it.hasNext(); ) {
            long key = it.next();
            if (!wanted.contains(key)) {
                net.minecraft.world.level.ChunkPos pos = new net.minecraft.world.level.ChunkPos(key);
                level.setChunkForced(pos.x, pos.z, false);
                it.remove();
            }
        }
        held.addAll(wanted);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }

        // Held regardless of whether anyone is here: a craft left behind still
        // has to survive, and during a dimension change the crew can briefly be
        // absent from the level the craft just landed in.
        if (level.getGameTime() % CHUNK_HOLD_INTERVAL == 0) {
            holdCraftChunks(level, container);
        }
        if (level.getGameTime() % INTERVAL != 0 || level.players().isEmpty()) {
            return;
        }

        for (ServerSubLevel subLevel : List.copyOf(container.getAllSubLevels())) {
            if (subLevel.isRemoved()) {
                continue;
            }

            Vector3dc pose = subLevel.logicalPose().position();
            Collection<java.util.UUID> tracking = subLevel.getTrackingPlayers();

            boolean anyNearby = false;
            boolean anyTracking = false;
            for (ServerPlayer player : level.players()) {
                if (player.position().distanceToSqr(pose.x(), pose.y(), pose.z()) > NEARBY * NEARBY) {
                    continue;
                }
                anyNearby = true;
                if (tracking.contains(player.getUUID())) {
                    anyTracking = true;
                    break;
                }
            }

            // Nothing near it at all: report it once in a while, because a ship
            // stranded far from its crew looks identical to an invisible one
            // from the player's side, and the two need opposite fixes.
            if (!anyNearby && level.getGameTime() % (INTERVAL * 10L) == 0L) {
                double nearest = Double.MAX_VALUE;
                for (ServerPlayer player : level.players()) {
                    nearest = Math.min(nearest,
                            Math.sqrt(player.position().distanceToSqr(pose.x(), pose.y(), pose.z())));
                }
                GenesisMod.LOGGER.info("[SWEEP] ship {} in {} at ({}, {}, {}) has no player within {} (nearest {})",
                        subLevel.getUniqueId(), level.dimension().location(),
                        (long) pose.x(), (long) pose.y(), (long) pose.z(), (long) NEARBY,
                        nearest == Double.MAX_VALUE ? "none" : (long) nearest);
            }

            // Players are right next to it and not one of them can see it: the
            // announcement was lost. Re-announcing now reaches exactly those
            // clients and cannot duplicate a plot anyone already holds.
            if (anyNearby && !anyTracking
                    && !SpaceTravelManager.isVisibilityManaged(level, subLevel.getUniqueId())) {
                try {
                    container.trackingSystem().onSubLevelAdded(subLevel);
                    GenesisMod.LOGGER.info("[SWEEP] re-announced untracked ship {} in {}",
                            subLevel.getUniqueId(), level.dimension().location());
                } catch (Throwable t) {
                    GenesisMod.LOGGER.warn("[SWEEP] could not re-announce sub-level", t);
                }
            }
        }
    }
}
