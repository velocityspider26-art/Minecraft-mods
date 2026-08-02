package shipwrights.genesis.space.voxel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.networking.PlanetVoxelInterestPacket;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.teleportation.ArrivalGate;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;
import shipwrights.genesis.teleportation.SpaceTravelManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-authoritative sparse 3D planet voxel engine.
 *
 * <p>Only already-loaded chunks are ingested. A changed chunk updates exact
 * LOD0 section bricks and the logarithmic chain of affected parent bricks;
 * there is no world scan and no forced chunk generation.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class PlanetVoxelService {
    public static final ResourceLocation EARTH_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    private static final int MAX_CHUNKS_PER_TICK = 1;
    private static final int MAX_PREDICTIONS_PER_TICK = 2;
    private static final int MAX_PENDING_JOBS = 4;
    private static final int MAX_RESTORED_BRICKS = 100_000;
    private static final int MAX_LOD = 9;
    private static final long MEMORY_BUDGET = 2L * 1024L * 1024L * 1024L;
    private static final long FLUSH_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(2L);

    private static final ConcurrentLinkedQueue<Long> DIRTY = new ConcurrentLinkedQueue<>();
    private static final Set<Long> DIRTY_SET = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<Long, PlanetVoxelAuthority> DIRTY_AUTHORITY =
            new ConcurrentHashMap<>();
    private static final AtomicLong REVISIONS = new AtomicLong();
    private static final AtomicInteger PENDING_JOBS = new AtomicInteger();

    private static PlanetVoxelStore memory;
    private static PlanetVoxelRegionStore disk;
    private static PlanetVoxelPyramid pyramid;
    private static PlanetVoxelInterestManager interests;
    private static PlanetVoxelPredictionManager predictions;
    private static MinecraftServer owner;
    private static ExecutorService worker;
    private static long generation;
    private static long lastFlushNanos;
    private static boolean logged;
    private static volatile boolean restoreInProgress;

    private PlanetVoxelService() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension().equals(Level.OVERWORLD)
                && event.getChunk() instanceof LevelChunk chunk) {
            enqueueDirty(chunk.getPos().x, chunk.getPos().z,
                    PlanetVoxelAuthority.GENERATED_EXACT);
        }
    }

    /** Called by the chunk block-change mixin. */
    public static void markChunkDirty(ServerLevel level, int chunkX, int chunkZ) {
        if (level == null || !level.dimension().equals(Level.OVERWORLD)) return;
        enqueueDirty(chunkX, chunkZ, PlanetVoxelAuthority.PLAYER_MODIFIED);
    }

    private static void enqueueDirty(int chunkX, int chunkZ,
                                     PlanetVoxelAuthority authority) {
        long packed = ChunkPos.asLong(chunkX, chunkZ);
        enqueueDirty(packed, authority);
    }

    private static void enqueueDirty(long packed, PlanetVoxelAuthority authority) {
        DIRTY_AUTHORITY.merge(packed, authority,
                (existing, incoming) -> incoming.outranks(existing) ? incoming : existing);
        if (DIRTY_SET.add(packed)) DIRTY.offer(packed);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld == null) return;
        try {
            ensureStarted(server);
            Celestial earth = GenesisMod.getCelestialForLevel(overworld);
            if (earth == null) return;
            CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
                    earth.getActualSize(), SpaceTravelManager.entryPadding(earth.getActualSize()));
            if (interests != null) {
                interests.updateGeometry(transform.faceHalfSpan(), overworld.getSeaLevel());
                interests.tick();
            }
            if (ArrivalGate.crossingInProgress() || restoreInProgress) return;
            ensurePredictions(overworld, transform);
            for (int processed = 0; processed < MAX_CHUNKS_PER_TICK
                    && PENDING_JOBS.get() < MAX_PENDING_JOBS; processed++) {
                Long packed = DIRTY.poll();
                if (packed == null) break;
                PlanetVoxelAuthority authority = DIRTY_AUTHORITY.remove(packed);
                DIRTY_SET.remove(packed);
                // A block update may race the queue drain. If it arrived while
                // this entry was still marked queued, preserve it as a second job.
                if (DIRTY_AUTHORITY.containsKey(packed) && DIRTY_SET.add(packed)) {
                    DIRTY.offer(packed);
                }
                if (authority == null) authority = PlanetVoxelAuthority.GENERATED_EXACT;
                int chunkX = ChunkPos.getX(packed);
                int chunkZ = ChunkPos.getZ(packed);
                LevelChunk chunk = overworld.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                queueIngest(server, overworld, chunk, transform, packed, authority);
            }
            PlanetVoxelPredictionManager targetPredictions = predictions;
            if (targetPredictions != null) {
                targetPredictions.tick();
                for (int processed = 0; processed < MAX_PREDICTIONS_PER_TICK
                        && PENDING_JOBS.get() < MAX_PENDING_JOBS; processed++) {
                    PlanetVoxelPredictionManager.Result result =
                            targetPredictions.pollCompleted();
                    if (result == null) break;
                    if (result.error() != null || result.brick() == null) {
                        targetPredictions.markFailed(result.key());
                        GenesisMod.LOGGER.debug(
                                "[PLANET-VOXEL] predicted brick generation failed for {}",
                                result.key(), result.error());
                        continue;
                    }
                    queuePrediction(server, targetPredictions, result.brick());
                }
            }
            if (!logged && memory.size() > 0) {
                logged = true;
                GenesisMod.LOGGER.info("[PLANET-VOXEL] clean-room 3D voxel pyramid active; {} bricks, {} MiB resident",
                        memory.size(), memory.usedBytes() / (1024L * 1024L));
            }
        } catch (Throwable error) {
            GenesisMod.LOGGER.error("[PLANET-VOXEL] server ingestion failed", error);
        }
    }

    private static void queueIngest(MinecraftServer server, ServerLevel level, LevelChunk chunk,
                                    CubeNetSurfaceTransform transform, long packed,
                                    PlanetVoxelAuthority authority) {
        long baseRevision = REVISIONS.addAndGet(level.getSectionsCount() + 1L);
        PlanetVoxelChunkIngestor.Capture capture = PlanetVoxelChunkIngestor.capture(
                level, chunk, transform, baseRevision, authority);
        List<PlanetVoxelBrickKey> removals = new ArrayList<>();
        for (int sectionY : capture.emptySectionYs()) {
            PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(capture.face(), 0,
                    capture.brickU(), sectionY, capture.brickV());
            removals.add(key);
        }
        PlanetVoxelPyramid targetPyramid = pyramid;
        PlanetVoxelRegionStore targetDisk = disk;
        ExecutorService targetWorker = worker;
        long targetGeneration = generation;
        if (targetPyramid == null || targetWorker == null) return;

        PENDING_JOBS.incrementAndGet();
        try {
            targetWorker.execute(() -> processIngest(server, targetPyramid, targetDisk,
                    targetGeneration, capture, removals));
        } catch (RejectedExecutionException error) {
            PENDING_JOBS.decrementAndGet();
            enqueueDirty(packed, authority);
        }
    }

    private static void processIngest(MinecraftServer server,
                                      PlanetVoxelPyramid targetPyramid,
                                      PlanetVoxelRegionStore targetDisk,
                                      long targetGeneration,
                                      PlanetVoxelChunkIngestor.Capture capture,
                                      List<PlanetVoxelBrickKey> removals) {
        try {
            PlanetVoxelPyramid.Update update =
                    targetPyramid.applyBatch(capture.bricks(), removals);
            long now = System.nanoTime();
            if (targetDisk != null && now - lastFlushNanos >= FLUSH_INTERVAL_NANOS) {
                targetDisk.flush();
                lastFlushNanos = now;
            }
            server.execute(() -> publishUpdate(server, targetGeneration, update));
        } catch (Throwable error) {
            GenesisMod.LOGGER.error("[PLANET-VOXEL] asynchronous ingestion failed for chunk {},{}",
                    capture.chunkX(), capture.chunkZ(), error);
        } finally {
            PENDING_JOBS.decrementAndGet();
        }
    }

    private static void queuePrediction(MinecraftServer server,
                                        PlanetVoxelPredictionManager source,
                                        PlanetVoxelBrick brick) {
        PlanetVoxelPyramid targetPyramid = pyramid;
        PlanetVoxelRegionStore targetDisk = disk;
        ExecutorService targetWorker = worker;
        long targetGeneration = generation;
        if (targetPyramid == null || targetWorker == null) {
            source.markFailed(brick.key());
            return;
        }
        PENDING_JOBS.incrementAndGet();
        try {
            targetWorker.execute(() -> processPrediction(server, targetPyramid,
                    targetDisk, source, targetGeneration, brick));
        } catch (RejectedExecutionException error) {
            PENDING_JOBS.decrementAndGet();
            source.markFailed(brick.key());
        }
    }

    private static void processPrediction(MinecraftServer server,
                                          PlanetVoxelPyramid targetPyramid,
                                          PlanetVoxelRegionStore targetDisk,
                                          PlanetVoxelPredictionManager source,
                                          long targetGeneration,
                                          PlanetVoxelBrick brick) {
        try {
            PlanetVoxelPyramid.Update update = targetPyramid.acceptBrick(brick);
            long now = System.nanoTime();
            if (targetDisk != null && now - lastFlushNanos >= FLUSH_INTERVAL_NANOS) {
                targetDisk.flush();
                lastFlushNanos = now;
            }
            server.execute(() -> finishPrediction(server, source, targetGeneration,
                    brick.key(), update, null));
        } catch (Throwable error) {
            server.execute(() -> finishPrediction(server, source, targetGeneration,
                    brick.key(), null, error));
        } finally {
            PENDING_JOBS.decrementAndGet();
        }
    }

    private static void finishPrediction(MinecraftServer server,
                                         PlanetVoxelPredictionManager source,
                                         long targetGeneration,
                                         PlanetVoxelBrickKey key,
                                         PlanetVoxelPyramid.Update update,
                                         Throwable error) {
        if (owner != server || generation != targetGeneration || predictions != source) return;
        if (error != null) {
            source.markFailed(key);
            GenesisMod.LOGGER.warn("[PLANET-VOXEL] failed to integrate predicted brick {}",
                    key, error);
            return;
        }
        source.markIntegrated(key);
        publishUpdate(server, targetGeneration, update);
    }

    private static void publishUpdate(MinecraftServer server, long targetGeneration,
                                      PlanetVoxelPyramid.Update update) {
        if (owner != server || generation != targetGeneration) return;
        PlanetVoxelInterestManager targetInterests = interests;
        if (targetInterests == null) return;
        for (PlanetVoxelBrick brick : update.upserts()) {
            targetInterests.publishUpsert(EARTH_ID, brick);
        }
        long removalRevision = REVISIONS.incrementAndGet();
        for (PlanetVoxelBrickKey key : update.removals()) {
            targetInterests.publishRemoval(EARTH_ID, key, removalRevision);
        }
    }

    /** Receives a validated, throttled camera request from one client. */
    public static void updateInterest(ServerPlayer player, PlanetVoxelInterestPacket packet) {
        if (player == null || packet == null || !EARTH_ID.equals(packet.planet())) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        try {
            ensureStarted(server);
            ServerLevel overworld = server.overworld();
            Celestial earth = GenesisMod.getCelestialForLevel(overworld);
            if (earth == null || interests == null) return;
            CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
                    earth.getActualSize(), SpaceTravelManager.entryPadding(earth.getActualSize()));
            interests.updateGeometry(transform.faceHalfSpan(), overworld.getSeaLevel());
            interests.update(player, packet);
        } catch (Throwable error) {
            GenesisMod.LOGGER.warn("[PLANET-VOXEL] rejected viewport interest from {}",
                    player.getGameProfile().getName(), error);
        }
    }

    private static void requestPredictions(Set<PlanetVoxelBrickKey> keys) {
        PlanetVoxelPredictionManager target = predictions;
        if (target != null) target.requestViewport(keys);
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        try {
            ensureStarted(server);
            ServerLevel overworld = server.overworld();
            Celestial earth = GenesisMod.getCelestialForLevel(overworld);
            if (earth == null || interests == null) return;
            CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
                    earth.getActualSize(), SpaceTravelManager.entryPadding(earth.getActualSize()));
            interests.updateGeometry(transform.faceHalfSpan(), overworld.getSeaLevel());
            interests.onLogin(player, EARTH_ID);
        } catch (Throwable error) {
            GenesisMod.LOGGER.warn("[PLANET-VOXEL] failed to prepare login replay for {}",
                    player.getGameProfile().getName(), error);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && interests != null) {
            interests.onLogout(player);
        }
    }

    private static void ensureStarted(MinecraftServer server) throws IOException {
        if (owner == server && pyramid != null) return;
        closeStorage();
        owner = server;
        generation++;
        memory = new PlanetVoxelStore(MEMORY_BUDGET);
        Path root = server.getWorldPath(LevelResource.ROOT)
                .resolve("genesis").resolve("planet_voxels");
        disk = new PlanetVoxelRegionStore(root);
        pyramid = new PlanetVoxelPyramid(memory, disk, REVISIONS, MAX_LOD);
        interests = new PlanetVoxelInterestManager(server, memory,
                PlanetVoxelService::requestPredictions);
        worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Genesis-PlanetVoxelWorker");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
        queuePersistentRestore(server, memory, disk, worker, generation);
        lastFlushNanos = System.nanoTime();
    }

    private static void ensurePredictions(ServerLevel level,
                                          CubeNetSurfaceTransform transform) {
        if (predictions != null) return;
        predictions = new PlanetVoxelPredictionManager(level, transform, memory, REVISIONS);
        if (interests != null) interests.refreshAll();
        GenesisMod.LOGGER.info("[PLANET-VOXEL] deterministic 3D terrain prediction active; "
                + "complete LOD {} cube coverage queued with bounded workers",
                PlanetVoxelPredictionManager.COMPLETE_COVERAGE_LOD);
    }

    private static void queuePersistentRestore(MinecraftServer server,
                                               PlanetVoxelStore targetMemory,
                                               PlanetVoxelRegionStore targetDisk,
                                               ExecutorService targetWorker,
                                               long targetGeneration) {
        restoreInProgress = true;
        try {
            targetWorker.execute(() -> {
                try {
                    PlanetVoxelRegionStore.LoadResult restored =
                            targetDisk.loadAll(MAX_RESTORED_BRICKS);
                    long maximumRevision = REVISIONS.get();
                    for (PlanetVoxelBrick brick : restored.bricks()) {
                        targetMemory.put(brick);
                        maximumRevision = Math.max(maximumRevision, brick.revision());
                    }
                    REVISIONS.accumulateAndGet(maximumRevision, Math::max);
                    server.execute(() -> finishPersistentRestore(server, targetGeneration,
                            restored));
                } catch (Throwable error) {
                    server.execute(() -> failPersistentRestore(server, targetGeneration, error));
                }
            });
        } catch (RejectedExecutionException rejected) {
            restoreInProgress = false;
            GenesisMod.LOGGER.warn("[PLANET-VOXEL] persistent restore queue rejected", rejected);
        }
    }

    private static void finishPersistentRestore(MinecraftServer server, long targetGeneration,
                                                PlanetVoxelRegionStore.LoadResult restored) {
        if (owner != server || generation != targetGeneration) return;
        restoreInProgress = false;
        if (interests != null) interests.refreshAll();
        if (!restored.bricks().isEmpty() || restored.corruptRecords() > 0) {
            GenesisMod.LOGGER.info("[PLANET-VOXEL] restored {} persisted bricks from {} regions; "
                            + "{} corrupt records isolated",
                    restored.bricks().size(), restored.scannedRegions(), restored.corruptRecords());
        }
    }

    private static void failPersistentRestore(MinecraftServer server, long targetGeneration,
                                              Throwable error) {
        if (owner != server || generation != targetGeneration) return;
        restoreInProgress = false;
        GenesisMod.LOGGER.error("[PLANET-VOXEL] persistent restore failed; live chunk ingestion will continue",
                error);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DIRTY.clear();
        DIRTY_SET.clear();
        DIRTY_AUTHORITY.clear();
        logged = false;
        restoreInProgress = false;
        try {
            closeStorage();
        } catch (IOException error) {
            GenesisMod.LOGGER.warn("[PLANET-VOXEL] failed to close voxel storage", error);
        }
    }

    private static void closeStorage() throws IOException {
        generation++;
        owner = null;
        pyramid = null;
        PlanetVoxelPredictionManager closingPredictions = predictions;
        predictions = null;
        if (closingPredictions != null) closingPredictions.close();
        PlanetVoxelInterestManager closingInterests = interests;
        interests = null;
        if (closingInterests != null) closingInterests.close();
        ExecutorService closingWorker = worker;
        worker = null;
        if (closingWorker != null) {
            closingWorker.shutdown();
            try {
                if (!closingWorker.awaitTermination(30L, TimeUnit.SECONDS)) {
                    GenesisMod.LOGGER.warn("[PLANET-VOXEL] worker did not drain in 30 seconds; cancelling queued work");
                    closingWorker.shutdownNow();
                    closingWorker.awaitTermination(5L, TimeUnit.SECONDS);
                }
            } catch (InterruptedException interrupted) {
                closingWorker.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        PENDING_JOBS.set(0);
        restoreInProgress = false;
        if (memory != null) memory.clear();
        memory = null;
        if (disk != null) {
            disk.flush();
            disk.close();
        }
        disk = null;
    }
}
