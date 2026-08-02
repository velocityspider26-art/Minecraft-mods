package shipwrights.genesis.space.surface;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.networking.PlanetSurfacePacket;
import shipwrights.genesis.space.Celestial;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Samples each planet from its own world once, and hands the result to whoever
 * is in a position to look at it.
 *
 * <p>Sampling runs from the server tick, for as long as anyone is in the space
 * dimension. It used to hang off the atmosphere-approach check instead, which
 * meant it effectively never ran: that check only fires for a traveler already
 * <em>in</em> space and already inside a body's approach radius, and then only
 * once every twenty ticks — so a planet needed something over a quarter of a
 * minute of unbroken approach to finish, and climbing up from a planet never
 * reached it at all. It also put the work on the dimension-transition path,
 * which is the one place where a fault does not merely lose a texture.</p>
 *
 * <p>Each planet is sampled once and kept. A finished six-face LOD surface is
 * sent to every player who can see it and not again. The payload is roughly
 * 150 KB at the current 64x64-per-face resolution, so it is a one-shot transfer
 * rather than a continuously streamed texture.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class PlanetSurfaceService {
    /**
     * Blocks of planet surface per block of space. Atmosphere entry maps a
     * body's surface onto a square this many times its own size, so a body of
     * size {@code s} covers {@code s * 16} blocks of world.
     */
    private static final int PLANET_BLOCKS_PER_SPACE_BLOCK = 16;
    private static final ResourceLocation OVERWORLD_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    /** Set when the pass fails structurally, so it reports once and stands down. */
    private static volatile boolean disabled;

    /**
     * The one thread this feature is ever allowed to use.
     *
     * <p>It emphatically must not be {@link net.minecraft.Util#backgroundExecutor()}.
     * That pool is where Minecraft generates chunks, and handing it three
     * planet samples at once did exactly what handing a worldgen pool a minute
     * of unrelated work does: the overworld sample finished in a second, two
     * others were still running a minute later, and with those threads held the
     * server could no longer generate the chunks a respawn needed. The game
     * froze on the respawn button. One dedicated, low-priority, daemon thread
     * cannot do that to anyone — the worst it can cost is its own progress.</p>
     */
    private static final ExecutorService SAMPLER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "genesis-surface-sampler");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private static final Map<ResourceLocation, PlanetSurfaceData> SAMPLED = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<ResourceLocation>> SENT = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, CompletableFuture<PlanetSurfaceData>> IN_PROGRESS =
            new ConcurrentHashMap<>();
    /** Bodies whose sampling failed; never retried, they keep their painted texture. */
    private static final Set<ResourceLocation> ABANDONED = ConcurrentHashMap.newKeySet();

    private PlanetSurfaceService() {
    }

    /**
     * Advances one planet's sample and sends out anything finished.
     *
     * <p>Only one body is advanced per tick, so the budget above is the whole
     * cost of this feature per tick rather than the cost per planet.</p>
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (disabled || !GenesisCommonConfig.isPlanetSurfaceSamplingEnabled()) {
            return;
        }
        // Never start a sample near a crossing. A dimension change rebuilds a
        // whole client level and meshes every section around the arrival; a
        // background job churning noise columns at the same moment is felt as
        // a freeze, whatever thread it is on.
        if (shipwrights.genesis.teleportation.ArrivalGate.crossingInProgress()) {
            return;
        }
        MinecraftServer server = event.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return; // no eyes on any of it
        }

        // Wrapped whole: a planet that will not sample keeps its painted look,
        // and that is a perfectly good outcome. It must never take the tick with
        // it.
        try {
            Registry<Celestial> registry = GenesisMod.getCelestialRegistry(server.overworld());

            for (Celestial celestial : registry) {
                if (!celestial.type().isVisitable()) {
                    continue;
                }
                ResourceLocation planet = registry.getResourceKey(celestial)
                        .map(ResourceKey::location).orElse(null);
                if (planet == null || ABANDONED.contains(planet)) {
                    continue;
                }
                // This pass is intentionally Earth-only. The Moon and Mercury
                // generators are known to take too long for a global sample;
                // leaving them painted prevents a cosmetic task from occupying
                // the one sampler thread for 90 seconds each.
                if (!OVERWORLD_ID.equals(planet)) {
                    continue;
                }

                PlanetSurfaceData surface = SAMPLED.get(planet);
                if (surface == null) {
                    collectOrStart(server, planet, celestial);
                    continue;
                }

                // Everyone, not just travelers in orbit. Planets are drawn in
                // the sky of other planets too, so a surface withheld until
                // someone visits space is a surface missing from every view of
                // it that a player is most likely to have.
                for (ServerPlayer player : players) {
                    Set<ResourceLocation> already = SENT.computeIfAbsent(
                            player.getUUID(), id -> ConcurrentHashMap.newKeySet());
                    if (already.add(planet)) {
                        PacketDistributor.sendToPlayer(player, PlanetSurfacePacket.of(planet, surface));
                    }
                }
            }
        } catch (Throwable error) {
            // Once, not once per tick. Whatever went wrong here is structural
            // (the celestial registry is not there), so it will go wrong again
            // twenty times a second and bury the log that explains it.
            disabled = true;
            GenesisMod.LOGGER.warn("[SURFACE] sampling disabled; planets keep their painted textures", error);
        }
    }

    /**
     * Picks up a finished sample, or starts one. Never blocks: an unfinished
     * job is simply left for a later tick.
     */
    private static void collectOrStart(MinecraftServer server, ResourceLocation planet, Celestial celestial) {
        CompletableFuture<PlanetSurfaceData> pending = IN_PROGRESS.get(planet);
        if (pending == null) {
            // Strictly one planet in flight at a time. Queueing them all at
            // once is what turned a slow feature into a frozen game.
            if (!IN_PROGRESS.isEmpty()) {
                return;
            }
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, planet));
            if (level == null) {
                ABANDONED.add(planet); // set dressing with no world behind it
                return;
            }
            int span = (int) Math.round(celestial.getActualSize() * PLANET_BLOCKS_PER_SPACE_BLOCK);
            // Constructed here, on the server thread, because it reads the
            // level's chunk source; only the sampling itself leaves it.
            PlanetSurfaceSampler.Job job = new PlanetSurfaceSampler.Job(level, span);
            IN_PROGRESS.put(planet, CompletableFuture.supplyAsync(job::sample, SAMPLER));
            GenesisMod.LOGGER.info("[SURFACE] building six-face world LOD for {} from six {}-block world regions", planet, span);
            return;
        }

        if (!pending.isDone()) {
            return;
        }

        IN_PROGRESS.remove(planet);
        PlanetSurfaceData surface;
        try {
            surface = pending.getNow(null);
        } catch (Throwable error) {
            surface = null;
            GenesisMod.LOGGER.warn("[SURFACE] sampling {} failed", planet, error);
        }

        if (surface == null) {
            ABANDONED.add(planet);
            GenesisMod.LOGGER.warn("[SURFACE] giving up on {}; it keeps its painted texture", planet);
        } else {
            SAMPLED.put(planet, surface);
            GenesisMod.LOGGER.info("[SURFACE] built six-face world LOD for {} from generated block columns; relief y={}..{}, sea={}",
                    planet, surface.minHeight(), surface.maxHeight(), surface.seaLevel());
        }
    }

    /** Forgets a departing player so a reconnect re-sends what they need. */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    /** Drops everything on shutdown so a new world starts clean. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        reset();
    }

    public static void reset() {
        SAMPLED.clear();
        SENT.clear();
        IN_PROGRESS.clear();
        ABANDONED.clear();
        disabled = false;
    }
}
