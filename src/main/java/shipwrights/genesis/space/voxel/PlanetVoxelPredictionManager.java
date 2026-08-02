package shipwrights.genesis.space.voxel;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.MapColor;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded background generation of deterministic 3D predicted terrain bricks.
 * Minecraft generator references are captured on the server thread, sampling
 * happens on two low-priority workers, and pyramid mutation remains elsewhere
 * on the single PlanetVoxelService worker.
 */
final class PlanetVoxelPredictionManager implements AutoCloseable {
    static final int COMPLETE_COVERAGE_LOD = 6;

    private static final int WORKER_COUNT = 2;
    private static final int WORK_QUEUE_CAPACITY = 8;
    private static final int COMPLETED_CAPACITY = 64;
    private static final int MAX_PENDING_REQUESTS = 4096;
    private static final int MAX_DISPATCH_PER_TICK = 4;
    private static final int VIEWPORT_PRIORITY = 100_000;
    private static final int GLOBAL_PRIORITY = 1_000;
    private static final int MAX_RETRIES = 3;

    record Result(PlanetVoxelBrickKey key, PlanetVoxelBrick brick, Throwable error) {
    }

    private final PlanetVoxelStore store;
    private final CubeNetSurfaceTransform transform;
    private final AtomicLong revisions;
    private final int minimumBuildHeight;
    private final int maximumBuildHeight;
    private final PlanetVoxelPredictionSampler.ColumnSource terrain;
    private final PriorityQueue<Request> requests = new PriorityQueue<>(
            Comparator.comparingInt(Request::priority).reversed()
                    .thenComparingLong(Request::sequence));
    private final Map<PlanetVoxelBrickKey, Integer> pendingPriorities = new HashMap<>();
    private final Map<PlanetVoxelBrickKey, Integer> retries = new HashMap<>();
    private final Set<PlanetVoxelBrickKey> globalKeys = new HashSet<>();
    private final BlockingQueue<Request> work = new ArrayBlockingQueue<>(WORK_QUEUE_CAPACITY);
    private final BlockingQueue<Result> completed = new ArrayBlockingQueue<>(COMPLETED_CAPACITY);
    private final Set<Thread> workers = ConcurrentHashMap.newKeySet();
    private long sequence;
    private volatile boolean closed;

    PlanetVoxelPredictionManager(ServerLevel level, CubeNetSurfaceTransform transform,
                                 PlanetVoxelStore store, AtomicLong revisions) {
        this.store = store;
        this.transform = transform;
        this.revisions = revisions;
        this.minimumBuildHeight = level.getMinBuildHeight();
        this.maximumBuildHeight = level.getMaxBuildHeight();
        this.terrain = new GeneratorTerrainSource(level);
        queueCompleteCoverage();
        for (int index = 0; index < WORKER_COUNT; index++) {
            Thread thread = new Thread(this::workerLoop,
                    "Genesis-PlanetPrediction-" + (index + 1));
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            workers.add(thread);
            thread.start();
        }
    }

    void requestViewport(Set<PlanetVoxelBrickKey> keys) {
        if (closed || keys == null) return;
        for (PlanetVoxelBrickKey key : keys) {
            enqueue(key, VIEWPORT_PRIORITY + (COMPLETE_COVERAGE_LOD - key.lod()) * 100);
        }
    }

    void tick() {
        if (closed) return;
        for (int admitted = 0; admitted < MAX_DISPATCH_PER_TICK; ) {
            Request request = requests.poll();
            if (request == null) break;
            Integer currentPriority = pendingPriorities.get(request.key());
            if (currentPriority == null || currentPriority != request.priority()) continue;
            if (store.contains(request.key())) {
                pendingPriorities.remove(request.key());
                retries.remove(request.key());
                continue;
            }
            if (!work.offer(request)) {
                requests.offer(request);
                break;
            }
            admitted++;
        }
    }

    Result pollCompleted() {
        return completed.poll();
    }

    void markIntegrated(PlanetVoxelBrickKey key) {
        pendingPriorities.remove(key);
        retries.remove(key);
    }

    void markFailed(PlanetVoxelBrickKey key) {
        pendingPriorities.remove(key);
        int attempts = retries.merge(key, 1, Integer::sum);
        if (!closed && globalKeys.contains(key) && attempts <= MAX_RETRIES) {
            enqueue(key, GLOBAL_PRIORITY - attempts);
        }
    }

    int pendingCount() {
        return pendingPriorities.size();
    }

    int completedCount() {
        return completed.size();
    }

    private void queueCompleteCoverage() {
        int halfSpan = exactInt(transform.faceHalfSpan(), "faceHalfSpan");
        int brickSpan = PlanetVoxelBrick.EDGE << COMPLETE_COVERAGE_LOD;
        int minimumHorizontal = Math.floorDiv(-halfSpan, brickSpan);
        int maximumHorizontal = ceilDiv(halfSpan, brickSpan) - 1;
        int minimumY = Math.floorDiv(minimumBuildHeight, brickSpan);
        int maximumY = Math.floorDiv(maximumBuildHeight - 1, brickSpan);
        int maximumRadius = Math.max(Math.abs(minimumHorizontal),
                Math.abs(maximumHorizontal));
        for (int radius = 0; radius <= maximumRadius; radius++) {
            for (int y = minimumY; y <= maximumY; y++) {
                for (int v = minimumHorizontal; v <= maximumHorizontal; v++) {
                    for (int u = minimumHorizontal; u <= maximumHorizontal; u++) {
                        if (Math.max(Math.abs(u), Math.abs(v)) != radius) continue;
                        for (CubeNetSurfaceTransform.Face face
                                : CubeNetSurfaceTransform.Face.values()) {
                            PlanetVoxelBrickKey key = new PlanetVoxelBrickKey(face,
                                    COMPLETE_COVERAGE_LOD, u, y, v);
                            globalKeys.add(key);
                            enqueue(key, GLOBAL_PRIORITY - radius);
                        }
                    }
                }
            }
        }
    }

    private void enqueue(PlanetVoxelBrickKey key, int priority) {
        if (closed || key == null || !intersectsPlanet(key) || store.contains(key)) return;
        Integer previous = pendingPriorities.get(key);
        if (previous != null && previous >= priority) return;
        if (previous == null && pendingPriorities.size() >= MAX_PENDING_REQUESTS) return;
        pendingPriorities.put(key, priority);
        requests.offer(new Request(key, priority, sequence++));
    }

    private boolean intersectsPlanet(PlanetVoxelBrickKey key) {
        long half = exactInt(transform.faceHalfSpan(), "faceHalfSpan");
        long span = key.brickSpan();
        boolean horizontal = key.minU() < half && key.minU() + span > -half
                && key.minV() < half && key.minV() + span > -half;
        return horizontal && key.minY() < maximumBuildHeight
                && key.minY() + span > minimumBuildHeight;
    }

    private void workerLoop() {
        try {
            while (!closed) {
                Request request = work.take();
                Result result;
                try {
                    PlanetVoxelBrickKey key = request.key();
                    PlanetVoxelBrick brick = PlanetVoxelPredictionSampler.sample(key,
                            revisions.incrementAndGet(),
                            exactInt(transform.faceCenterX(key.face()), "faceCenterX"),
                            exactInt(transform.faceCenterZ(key.face()), "faceCenterZ"),
                            minimumBuildHeight, maximumBuildHeight, terrain);
                    result = new Result(key, brick, null);
                } catch (Throwable error) {
                    result = new Result(request.key(), null, error);
                }
                completed.put(result);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            workers.remove(Thread.currentThread());
        }
    }

    @Override
    public void close() {
        closed = true;
        for (Thread worker : Set.copyOf(workers)) worker.interrupt();
        work.clear();
        completed.clear();
        requests.clear();
        pendingPriorities.clear();
        retries.clear();
        globalKeys.clear();
    }

    private static int ceilDiv(int value, int divisor) {
        return -Math.floorDiv(-value, divisor);
    }

    private static int exactInt(double value, String name) {
        int rounded = (int) Math.rint(value);
        if (Math.abs(value - rounded) > 1.0E-6) {
            throw new IllegalStateException(name + " is not block-aligned: " + value);
        }
        return rounded;
    }

    private record Request(PlanetVoxelBrickKey key, int priority, long sequence) {
    }

    /** Immutable world-generator bridge captured before any worker starts. */
    private static final class GeneratorTerrainSource
            implements PlanetVoxelPredictionSampler.ColumnSource {
        private final LevelHeightAccessor heightAccessor;
        private final RandomState randomState;
        private final ChunkGenerator generator;
        private final BiomeSource biomeSource;
        private final Climate.Sampler climateSampler;

        private GeneratorTerrainSource(ServerLevel level) {
            this.heightAccessor = level;
            ServerChunkCache cache = level.getChunkSource();
            this.randomState = cache.randomState();
            this.generator = cache.getGenerator();
            this.biomeSource = generator.getBiomeSource();
            this.climateSampler = randomState.sampler();
        }

        @Override
        public PlanetVoxelPredictionSampler.Column column(int worldX, int worldZ) {
            NoiseColumn column = generator.getBaseColumn(
                    worldX, worldZ, heightAccessor, randomState);
            return worldY -> material(column.getBlock(worldY), worldX, worldY, worldZ);
        }

        private long material(BlockState state, int worldX, int worldY, int worldZ) {
            if (state.isAir()) return PlanetVoxelMaterial.AIR;
            MapColor mapColor = state.getBlock().defaultMapColor();
            int rgb;
            if (!state.getFluidState().isEmpty() || mapColor == MapColor.WATER
                    || mapColor == MapColor.GRASS || mapColor == MapColor.PLANT
                    || mapColor == MapColor.NONE || mapColor.col == 0) {
                Holder<Biome> biome = biomeSource.getNoiseBiome(
                        QuartPos.fromBlock(worldX), QuartPos.fromBlock(worldY),
                        QuartPos.fromBlock(worldZ), climateSampler);
                if (!state.getFluidState().isEmpty() || mapColor == MapColor.WATER) {
                    rgb = biome.value().getWaterColor();
                } else if (mapColor == MapColor.PLANT) {
                    rgb = biome.value().getFoliageColor();
                } else {
                    rgb = biome.value().getGrassColor(worldX, worldZ);
                }
            } else {
                rgb = mapColor.col;
            }
            return PlanetVoxelMaterial.pack(Block.getId(state), rgb, 15,
                    Mth.clamp(state.getLightEmission(), 0, 15),
                    PlanetVoxelChunkIngestor.flags(state));
        }
    }
}
