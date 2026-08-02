package shipwrights.genesis.space.surface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.networking.PlanetLodVolumeTilePacket;
import shipwrights.genesis.networking.SparsePlanetLodTilePacket;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.teleportation.ArrivalGate;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;
import shipwrights.genesis.teleportation.SpaceTravelManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-owned sparse world-to-planet LOD.
 *
 * <p>Normal Minecraft chunks remain authoritative around players. Outside that
 * area this service asks the real overworld chunk generator for lightweight
 * surface columns without creating chunks. When a real chunk is loaded or
 * changed, a 16x16 exact tile replaces the prediction and is broadcast to every
 * client, including players already in orbit.</p>
 *
 * <p>No whole-world scan exists. One small tile request is scheduled per player
 * per tick, cycling through three detail rings. The cache therefore grows only
 * where players actually travel while the existing six-face global surface
 * remains the fallback for everything else.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class SparsePlanetLodService {
    public static final ResourceLocation EARTH_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    private static final int[] PROCEDURAL_CELL_SIZES = {4, 16, 64};
    /** Complete seed-predicted fallback: 64x64 columns per cube face, staged. */
    private static final int GLOBAL_CELL_SIZE = 128;
    private static final int GLOBAL_TILE_SPAN = SparsePlanetLodTile.RESOLUTION * GLOBAL_CELL_SIZE;
    private static final int GLOBAL_TILES_PER_AXIS = 4;
    private static final int GLOBAL_TILE_COUNT = CubeNetSurfaceTransform.Face.values().length
            * GLOBAL_TILES_PER_AXIS * GLOBAL_TILES_PER_AXIS;
    private static final int[] PROCEDURAL_RADII = {8, 10, 6};
    private static final int MAX_SERVER_TILES = 24_576;
    private static final int MAX_VOLUME_TILES = 16_384;
    private static final int MAX_VOLUME_SCAN_DEPTH = 192;
    private static final int MAX_QUEUED = 4096;
    private static final int COMPLETED_PER_TICK = 6;
    private static final int DIRTY_PER_TICK = 4;
    private static final int REPLAY_PER_TICK = 6;
    private static final int VOLUME_REPLAY_PER_TICK = 4;

    private static final ThreadFactory THREAD_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "Genesis-Sparse-Planet-LOD");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    };
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(THREAD_FACTORY);
    private static final AtomicLong REVISION = new AtomicLong();

    private static final Object TILE_LOCK = new Object();
    private static final LinkedHashMap<SparsePlanetLodTile.Key, SparsePlanetLodTile> SERVER_TILES =
            new LinkedHashMap<>(512, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<SparsePlanetLodTile.Key, SparsePlanetLodTile> eldest) {
                    return size() > MAX_SERVER_TILES;
                }
            };
    private static final LinkedHashMap<PlanetLodVolumeTile.Key, PlanetLodVolumeTile> VOLUME_TILES =
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<PlanetLodVolumeTile.Key, PlanetLodVolumeTile> eldest) {
                    return size() > MAX_VOLUME_TILES;
                }
            };
    private static final Set<SparsePlanetLodTile.Key> QUEUED = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<CompletedTile> COMPLETED = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Long> DIRTY_CHUNKS = new ConcurrentLinkedQueue<>();
    private static final Set<Long> DIRTY_SET = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, PlayerCursor> PLAYER_CURSORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> REPLAY_CURSORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> VOLUME_REPLAY_CURSORS = new ConcurrentHashMap<>();

    private static volatile int generation;
    private static volatile boolean loggedActive;
    private static volatile int globalCursor;
    private static volatile boolean globalCoverageComplete;

    private SparsePlanetLodService() {
    }

    /**
     * A generated/loaded chunk becomes an exact planetary tile without a scan.
     * The event only queues its coordinates; the normal per-tick budget performs
     * the block-column capture later, after the chunk is fully available.
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension().equals(Level.OVERWORLD)
                && event.getChunk() instanceof LevelChunk chunk) {
            markChunkDirty(level, chunk.getPos().x, chunk.getPos().z);
        }
    }

    /** Called by the LevelChunk mixin after an actual server-side block change. */
    public static void markChunkDirty(ServerLevel level, int chunkX, int chunkZ) {
        if (level == null || !level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        long key = ChunkPos.asLong(chunkX, chunkZ);
        if (DIRTY_SET.add(key)) {
            DIRTY_CHUNKS.offer(key);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }

        applyCompleted(server);
        replayCachedTiles(server);
        replayCachedVolumeTiles(server);

        // Crossings are the highest-priority latency path. Keep completed data
        // flowing to clients, but do not start generator work or exact chunk
        // captures while ArrivalGate is transferring a craft/player. Dirty
        // chunks remain queued and are processed on the first quiet tick.
        if (ArrivalGate.crossingInProgress()) {
            return;
        }

        processDirty(overworld, server);

        Celestial earth = GenesisMod.getCelestialForLevel(overworld);
        if (earth == null) {
            return;
        }
        CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
                earth.getActualSize(), SpaceTravelManager.entryPadding(earth.getActualSize()));
        scheduleGlobalTile(overworld, transform);
        long tick = overworld.getGameTime();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.level().dimension().equals(Level.OVERWORLD)) {
                continue;
            }
            schedulePlayerTile(overworld, transform, player, tick);
            if ((tick + player.getId()) % 20L == 0L) {
                captureExactChunk(overworld, transform, player.chunkPosition().x, player.chunkPosition().z, server);
            }
        }

        if (!loggedActive && !SERVER_TILES.isEmpty()) {
            loggedActive = true;
            GenesisMod.LOGGER.info(
                    "[SPARSE-LOD] active: real chunks near players, generator-predicted 3D tiles outside them, global cube fallback beyond the sparse cache");
        }
    }

    /**
     * Builds the undiscovered planet without generating real chunks. Ninety-six
     * small jobs cover all six faces at 128-block spacing; one is admitted at a
     * time and the ordinary player-centred rings remain higher priority.
     */
    private static void scheduleGlobalTile(ServerLevel level, CubeNetSurfaceTransform transform) {
        if (globalCoverageComplete || QUEUED.size() >= MAX_QUEUED) {
            return;
        }
        boolean allPresent = true;
        int perFace = GLOBAL_TILES_PER_AXIS * GLOBAL_TILES_PER_AXIS;
        for (int attempt = 0; attempt < GLOBAL_TILE_COUNT; attempt++) {
            int index = Math.floorMod(globalCursor++, GLOBAL_TILE_COUNT);
            CubeNetSurfaceTransform.Face face = CubeNetSurfaceTransform.Face.values()[index / perFace];
            int withinFace = index % perFace;
            int tileX = withinFace % GLOBAL_TILES_PER_AXIS;
            int tileZ = withinFace / GLOBAL_TILES_PER_AXIS;
            int faceMinX = Mth.floor(transform.faceCenterX(face) - transform.faceHalfSpan());
            int faceMinZ = Mth.floor(transform.faceCenterZ(face) - transform.faceHalfSpan());
            int originX = faceMinX + tileX * GLOBAL_TILE_SPAN;
            int originZ = faceMinZ + tileZ * GLOBAL_TILE_SPAN;
            SparsePlanetLodTile.Key key = new SparsePlanetLodTile.Key(
                    face, originX, originZ, GLOBAL_CELL_SIZE);
            synchronized (TILE_LOCK) {
                if (SERVER_TILES.containsKey(key)) {
                    continue;
                }
            }
            allPresent = false;
            // A job already in flight is not coverage. If it is cancelled by a
            // dimension crossing its key is removed and this pass will retry it.
            if (!QUEUED.add(key)) {
                continue;
            }
            int currentGeneration = generation;
            TileJob job = new TileJob(level, face, originX, originZ,
                    GLOBAL_CELL_SIZE, currentGeneration);
            WORKER.execute(() -> {
                SparsePlanetLodTile tile = job.sample();
                QUEUED.remove(key);
                if (tile != null && currentGeneration == generation) {
                    COMPLETED.offer(new CompletedTile(EARTH_ID, tile));
                }
            });
            return;
        }
        if (allPresent) {
            globalCoverageComplete = true;
            GenesisMod.LOGGER.info(
                    "[SPARSE-LOD] complete seed-predicted 64x64 terrain is available on all six Earth faces");
        }
    }

    private static void schedulePlayerTile(ServerLevel level, CubeNetSurfaceTransform transform,
                                           ServerPlayer player, long tick) {
        if (QUEUED.size() >= MAX_QUEUED) {
            return;
        }
        int tier = Math.floorMod((int) (tick + player.getId()), PROCEDURAL_CELL_SIZES.length);
        int cellSize = PROCEDURAL_CELL_SIZES[tier];
        int radius = PROCEDURAL_RADII[tier];
        int span = SparsePlanetLodTile.RESOLUTION * cellSize;

        CubeNetSurfaceTransform.Face face = transform.faceContaining(player.getX(), player.getZ());
        if (face == null) {
            face = CubeNetSurfaceTransform.Face.UP;
        }
        int centerOriginX = Math.floorDiv(Mth.floor(player.getX()), span) * span;
        int centerOriginZ = Math.floorDiv(Mth.floor(player.getZ()), span) * span;

        PlayerCursor cursor = PLAYER_CURSORS.computeIfAbsent(player.getUUID(), ignored -> new PlayerCursor());
        int[] offset = cursor.next(tier, centerOriginX, centerOriginZ, radius);
        int originX = centerOriginX + offset[0] * span;
        int originZ = centerOriginZ + offset[1] * span;
        if (!insideFace(transform, face, originX, originZ, span)) {
            return;
        }

        SparsePlanetLodTile.Key key = new SparsePlanetLodTile.Key(face, originX, originZ, cellSize);
        synchronized (TILE_LOCK) {
            if (SERVER_TILES.containsKey(key)) {
                return;
            }
        }
        if (!QUEUED.add(key)) {
            return;
        }

        int currentGeneration = generation;
        TileJob job = new TileJob(level, face, originX, originZ, cellSize, currentGeneration);
        WORKER.execute(() -> {
            SparsePlanetLodTile tile = job.sample();
            QUEUED.remove(key);
            if (tile != null && currentGeneration == generation) {
                COMPLETED.offer(new CompletedTile(EARTH_ID, tile));
            }
        });
    }

    private static boolean insideFace(CubeNetSurfaceTransform transform,
                                      CubeNetSurfaceTransform.Face face,
                                      int originX, int originZ, int span) {
        double minX = transform.faceCenterX(face) - transform.faceHalfSpan();
        double minZ = transform.faceCenterZ(face) - transform.faceHalfSpan();
        double maxX = minX + transform.faceSpan();
        double maxZ = minZ + transform.faceSpan();
        return originX >= minX && originZ >= minZ
                && originX + span <= maxX && originZ + span <= maxZ;
    }

    private static void processDirty(ServerLevel level, MinecraftServer server) {
        Celestial earth = GenesisMod.getCelestialForLevel(level);
        if (earth == null) {
            return;
        }
        CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
                earth.getActualSize(), SpaceTravelManager.entryPadding(earth.getActualSize()));
        for (int i = 0; i < DIRTY_PER_TICK; i++) {
            Long packed = DIRTY_CHUNKS.poll();
            if (packed == null) {
                return;
            }
            DIRTY_SET.remove(packed);
            captureExactChunk(level, transform, ChunkPos.getX(packed), ChunkPos.getZ(packed), server);
        }
    }

    /** Reads only an already-loaded real chunk; never creates or loads one. */
    private static void captureExactChunk(ServerLevel level, CubeNetSurfaceTransform transform,
                                          int chunkX, int chunkZ, MinecraftServer server) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) {
            return;
        }
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        CubeNetSurfaceTransform.Face face = transform.faceContaining(originX + 8.0, originZ + 8.0);
        if (face == null || !insideFace(transform, face, originX, originZ, 16)) {
            return;
        }

        short[] heights = new short[SparsePlanetLodTile.CELL_COUNT];
        int[] colours = new int[SparsePlanetLodTile.CELL_COUNT];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int worldX = originX + x;
                int worldZ = originZ + z;
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                surfaceY = Mth.clamp(surfaceY, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
                pos.set(worldX, surfaceY, worldZ);
                BlockState state = level.getBlockState(pos);
                Holder<Biome> biome = level.getBiome(pos);
                int index = z * 16 + x;
                heights[index] = (short) surfaceY;
                colours[index] = colourFor(state, biome, surfaceY, level.getSeaLevel());
            }
        }

        long revision = REVISION.incrementAndGet();
        SparsePlanetLodTile tile = new SparsePlanetLodTile(face, originX, originZ, 1,
                true, revision, heights, colours);
        storeAndBroadcast(server, EARTH_ID, tile);

        PlanetLodVolumeTile volume = captureVolumeTile(level, chunk, face, chunkX, chunkZ, revision);
        if (volume != null && volume.segmentCount() > 0) {
            storeVolumeAndBroadcast(server, EARTH_ID, volume);
        }
    }

    /**
     * Captures the real vertical surface shell of an already-loaded chunk.
     * The scan is capped to the top 192 blocks and twenty-four material runs per
     * column, which preserves roofs, walls, trees and towers without shipping
     * caves or the entire underground volume to orbital clients.
     */
    private static PlanetLodVolumeTile captureVolumeTile(ServerLevel level, LevelChunk chunk,
                                                         CubeNetSurfaceTransform.Face face,
                                                         int chunkX, int chunkZ, long revision) {
        int[] offsets = new int[PlanetLodVolumeTile.OFFSET_COUNT];
        List<VolumeSegment> segments = new ArrayList<>(768);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;

        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int column = z * 16 + x;
                offsets[column] = segments.size();
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                top = Mth.clamp(top, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
                int bottomLimit = Math.max(level.getMinBuildHeight(), top - MAX_VOLUME_SCAN_DEPTH + 1);
                List<VolumeSegment> columnSegments = new ArrayList<>(PlanetLodVolumeTile.MAX_SEGMENTS_PER_COLUMN);

                int runStateId = -1;
                int runBottom = 0;
                int runTop = 0;
                BlockState runState = null;
                for (int y = top; y >= bottomLimit; y--) {
                    BlockState state = chunk.getBlockState(pos.set(originX + x, y, originZ + z));
                    if (state.isAir()) {
                        if (runState != null) {
                            appendRun(level, pos, columnSegments, runState, runStateId, runBottom, runTop,
                                    originX + x, originZ + z);
                            runState = null;
                            runStateId = -1;
                        }
                        continue;
                    }
                    int stateId = Block.getId(state);
                    if (runState != null && stateId == runStateId && y + 1 == runBottom) {
                        runBottom = y;
                    } else {
                        if (runState != null) {
                            appendRun(level, pos, columnSegments, runState, runStateId, runBottom, runTop,
                                    originX + x, originZ + z);
                        }
                        runState = state;
                        runStateId = stateId;
                        runBottom = y;
                        runTop = y + 1;
                    }
                }
                if (runState != null) {
                    appendRun(level, pos, columnSegments, runState, runStateId, runBottom, runTop,
                            originX + x, originZ + z);
                }

                // Runs were discovered top-down. Keep the highest material
                // segments because those are the ones visible from altitude.
                int keep = Math.min(PlanetLodVolumeTile.MAX_SEGMENTS_PER_COLUMN, columnSegments.size());
                for (int i = 0; i < keep; i++) {
                    segments.add(columnSegments.get(i));
                }
                offsets[column + 1] = segments.size();
            }
        }

        if (segments.isEmpty() || segments.size() > PlanetLodVolumeTile.MAX_SEGMENTS) {
            return null;
        }
        short[] bottoms = new short[segments.size()];
        short[] tops = new short[segments.size()];
        int[] stateIds = new int[segments.size()];
        int[] colours = new int[segments.size()];
        byte[] lights = new byte[segments.size()];
        byte[] flags = new byte[segments.size()];
        for (int i = 0; i < segments.size(); i++) {
            VolumeSegment segment = segments.get(i);
            bottoms[i] = (short) segment.bottom();
            tops[i] = (short) segment.top();
            stateIds[i] = segment.stateId();
            colours[i] = segment.colour();
            lights[i] = segment.light();
            flags[i] = segment.flags();
        }
        return new PlanetLodVolumeTile(face, chunkX, chunkZ, revision, offsets,
                bottoms, tops, stateIds, colours, lights, flags);
    }

    private static void appendRun(ServerLevel level, BlockPos.MutableBlockPos pos,
                                  List<VolumeSegment> output, BlockState state, int stateId,
                                  int bottom, int top, int worldX, int worldZ) {
        if (output.size() >= PlanetLodVolumeTile.MAX_SEGMENTS_PER_COLUMN) {
            return;
        }
        // A huge continuous terrain run contributes only its upper shell. The
        // sparse height tile already represents the mountain/body underneath.
        int clampedBottom = Math.max(bottom, top - 32);
        pos.set(worldX, top - 1, worldZ);
        Holder<Biome> biome = level.getBiome(pos);
        int colour = colourFor(state, biome, top - 1, level.getSeaLevel());
        int sky = Mth.clamp(level.getBrightness(LightLayer.SKY, pos), 0, 15);
        int block = Mth.clamp(level.getBrightness(LightLayer.BLOCK, pos), 0, 15);
        byte light = (byte) ((sky << 4) | block);
        output.add(new VolumeSegment(clampedBottom, top, stateId, colour, light, flagsFor(state)));
    }

    private static byte flagsFor(BlockState state) {
        byte flags = 0;
        if (!state.getFluidState().isEmpty()) flags |= PlanetLodVolumeTile.FLAG_LIQUID;
        if (!state.canOcclude()) flags |= PlanetLodVolumeTile.FLAG_TRANSLUCENT;
        if (state.getLightEmission() > 0) flags |= PlanetLodVolumeTile.FLAG_EMISSIVE;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id == null ? "" : id.getPath();
        boolean foliage = path.contains("leaves") || path.contains("grass") || path.contains("flower")
                || path.contains("vine") || path.contains("crop") || path.contains("sapling");
        if (foliage) flags |= PlanetLodVolumeTile.FLAG_FOLIAGE;
        boolean natural = path.contains("stone") || path.contains("deepslate") || path.contains("dirt")
                || path.contains("grass_block") || path.contains("sand") || path.contains("gravel")
                || path.contains("clay") || path.contains("terracotta") || path.contains("snow")
                || path.contains("ice") || path.contains("ore") || path.contains("mud");
        if (natural) flags |= PlanetLodVolumeTile.FLAG_NATURAL;
        else flags |= PlanetLodVolumeTile.FLAG_STRUCTURE;
        return flags;
    }

    private static void storeVolumeAndBroadcast(MinecraftServer server, ResourceLocation planet,
                                                PlanetLodVolumeTile tile) {
        boolean changed;
        synchronized (TILE_LOCK) {
            PlanetLodVolumeTile previous = VOLUME_TILES.get(tile.key());
            changed = previous == null || previous.revision() < tile.revision();
            if (changed) {
                VOLUME_TILES.put(tile.key(), tile);
            }
        }
        if (changed && !server.getPlayerList().getPlayers().isEmpty()) {
            PacketDistributor.sendToAllPlayers(PlanetLodVolumeTilePacket.of(planet, tile));
        }
    }

    private static void replayCachedVolumeTiles(MinecraftServer server) {
        if (VOLUME_REPLAY_CURSORS.isEmpty()) {
            return;
        }
        List<PlanetLodVolumeTile> snapshot;
        synchronized (TILE_LOCK) {
            snapshot = new ArrayList<>(VOLUME_TILES.values());
        }
        if (snapshot.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Integer cursor = VOLUME_REPLAY_CURSORS.get(player.getUUID());
            if (cursor == null) continue;
            int next = cursor;
            for (int i = 0; i < VOLUME_REPLAY_PER_TICK && next < snapshot.size(); i++, next++) {
                PacketDistributor.sendToPlayer(player,
                        PlanetLodVolumeTilePacket.of(EARTH_ID, snapshot.get(next)));
            }
            if (next >= snapshot.size()) VOLUME_REPLAY_CURSORS.remove(player.getUUID());
            else VOLUME_REPLAY_CURSORS.put(player.getUUID(), next);
        }
    }

    private record VolumeSegment(int bottom, int top, int stateId, int colour, byte light, byte flags) {
    }

    private static void applyCompleted(MinecraftServer server) {
        for (int i = 0; i < COMPLETED_PER_TICK; i++) {
            CompletedTile completed = COMPLETED.poll();
            if (completed == null) {
                return;
            }
            storeAndBroadcast(server, completed.planet(), completed.tile());
        }
    }

    private static void storeAndBroadcast(MinecraftServer server, ResourceLocation planet,
                                          SparsePlanetLodTile tile) {
        boolean changed;
        synchronized (TILE_LOCK) {
            SparsePlanetLodTile previous = SERVER_TILES.get(tile.key());
            changed = previous == null
                    || previous.exact() != tile.exact()
                    || !Arrays.equals(previous.heights(), tile.heights())
                    || !Arrays.equals(previous.colours(), tile.colours());
            if (changed) {
                SERVER_TILES.put(tile.key(), tile);
            }
        }
        if (changed && !server.getPlayerList().getPlayers().isEmpty()) {
            PacketDistributor.sendToAllPlayers(SparsePlanetLodTilePacket.of(planet, tile));
        }
    }

    private static void replayCachedTiles(MinecraftServer server) {
        if (REPLAY_CURSORS.isEmpty()) {
            return;
        }
        List<SparsePlanetLodTile> snapshot;
        synchronized (TILE_LOCK) {
            snapshot = new ArrayList<>(SERVER_TILES.values());
        }
        if (snapshot.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Integer cursor = REPLAY_CURSORS.get(player.getUUID());
            if (cursor == null) {
                continue;
            }
            int next = cursor;
            for (int i = 0; i < REPLAY_PER_TICK && next < snapshot.size(); i++, next++) {
                PacketDistributor.sendToPlayer(player,
                        SparsePlanetLodTilePacket.of(EARTH_ID, snapshot.get(next)));
            }
            if (next >= snapshot.size()) {
                REPLAY_CURSORS.remove(player.getUUID());
            } else {
                REPLAY_CURSORS.put(player.getUUID(), next);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        REPLAY_CURSORS.put(event.getEntity().getUUID(), 0);
        VOLUME_REPLAY_CURSORS.put(event.getEntity().getUUID(), 0);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        PLAYER_CURSORS.remove(id);
        REPLAY_CURSORS.remove(id);
        VOLUME_REPLAY_CURSORS.remove(id);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        generation++;
        QUEUED.clear();
        COMPLETED.clear();
        DIRTY_CHUNKS.clear();
        DIRTY_SET.clear();
        PLAYER_CURSORS.clear();
        REPLAY_CURSORS.clear();
        VOLUME_REPLAY_CURSORS.clear();
        synchronized (TILE_LOCK) {
            SERVER_TILES.clear();
            VOLUME_TILES.clear();
        }
        loggedActive = false;
        globalCursor = 0;
        globalCoverageComplete = false;
    }

    private record CompletedTile(ResourceLocation planet, SparsePlanetLodTile tile) {
    }

    private static final class PlayerCursor {
        private final int[] cursor = new int[PROCEDURAL_CELL_SIZES.length];
        private final int[] centerX = new int[PROCEDURAL_CELL_SIZES.length];
        private final int[] centerZ = new int[PROCEDURAL_CELL_SIZES.length];
        private final boolean[] initialized = new boolean[PROCEDURAL_CELL_SIZES.length];

        int[] next(int tier, int nextCenterX, int nextCenterZ, int radius) {
            if (!initialized[tier] || centerX[tier] != nextCenterX || centerZ[tier] != nextCenterZ) {
                initialized[tier] = true;
                centerX[tier] = nextCenterX;
                centerZ[tier] = nextCenterZ;
                cursor[tier] = 0;
            }
            int[][] offsets = SpiralCache.offsets(radius);
            return offsets[cursor[tier]++ % offsets.length];
        }
    }

    private static final class SpiralCache {
        private static final Map<Integer, int[][]> CACHE = new ConcurrentHashMap<>();

        static int[][] offsets(int radius) {
            return CACHE.computeIfAbsent(radius, SparsePlanetLodService::buildSpiral);
        }
    }

    private static int[][] buildSpiral(int radius) {
        List<int[]> values = new ArrayList<>();
        values.add(new int[]{0, 0});
        for (int ring = 1; ring <= radius; ring++) {
            for (int x = -ring; x <= ring; x++) values.add(new int[]{x, -ring});
            for (int z = -ring + 1; z <= ring; z++) values.add(new int[]{ring, z});
            for (int x = ring - 1; x >= -ring; x--) values.add(new int[]{x, ring});
            for (int z = ring - 1; z > -ring; z--) values.add(new int[]{-ring, z});
        }
        return values.toArray(int[][]::new);
    }

    /** Captures immutable generator references on the server thread, samples off-thread. */
    private static final class TileJob {
        private final CubeNetSurfaceTransform.Face face;
        private final int originX;
        private final int originZ;
        private final int cellSize;
        private final int generation;
        private final LevelHeightAccessor heightAccessor;
        private final int minBuildHeight;
        private final int maxBuildHeight;
        private final int seaLevel;
        private final RandomState randomState;
        private final ChunkGenerator generator;
        private final BiomeSource biomeSource;
        private final Climate.Sampler climateSampler;

        TileJob(ServerLevel level, CubeNetSurfaceTransform.Face face,
                int originX, int originZ, int cellSize, int generation) {
            this.face = face;
            this.originX = originX;
            this.originZ = originZ;
            this.cellSize = cellSize;
            this.generation = generation;
            this.heightAccessor = level;
            this.minBuildHeight = level.getMinBuildHeight();
            this.maxBuildHeight = level.getMaxBuildHeight();
            ServerChunkCache cache = level.getChunkSource();
            this.randomState = cache.randomState();
            this.generator = cache.getGenerator();
            this.biomeSource = generator.getBiomeSource();
            this.climateSampler = randomState.sampler();
            this.seaLevel = generator.getSeaLevel();
        }

        SparsePlanetLodTile sample() {
            try {
                short[] heights = new short[SparsePlanetLodTile.CELL_COUNT];
                int[] colours = new int[SparsePlanetLodTile.CELL_COUNT];
                for (int z = 0; z < SparsePlanetLodTile.RESOLUTION; z++) {
                    for (int x = 0; x < SparsePlanetLodTile.RESOLUTION; x++) {
                        if (generation != SparsePlanetLodService.generation
                                || ArrivalGate.crossingInProgress()) {
                            return null;
                        }
                        int worldX = originX + x * cellSize + cellSize / 2;
                        int worldZ = originZ + z * cellSize + cellSize / 2;
                        NoiseColumn column = generator.getBaseColumn(worldX, worldZ, heightAccessor, randomState);
                        int surfaceY = findSurfaceY(column);
                        BlockState state = column.getBlock(surfaceY);
                        Holder<Biome> biome = biomeSource.getNoiseBiome(
                                QuartPos.fromBlock(worldX), QuartPos.fromBlock(surfaceY),
                                QuartPos.fromBlock(worldZ), climateSampler);
                        int index = z * SparsePlanetLodTile.RESOLUTION + x;
                        heights[index] = (short) Mth.clamp(surfaceY, Short.MIN_VALUE, Short.MAX_VALUE);
                        colours[index] = colourFor(state, biome, surfaceY, seaLevel);
                    }
                }
                shade(colours, heights);
                return new SparsePlanetLodTile(face, originX, originZ, cellSize,
                        false, REVISION.incrementAndGet(), heights, colours);
            } catch (Throwable error) {
                GenesisMod.LOGGER.debug("[SPARSE-LOD] tile generation failed at {},{} cell={}",
                        originX, originZ, cellSize, error);
                return null;
            }
        }

        private int findSurfaceY(NoiseColumn column) {
            for (int y = maxBuildHeight - 1; y >= minBuildHeight; y--) {
                if (!column.getBlock(y).isAir()) {
                    return y;
                }
            }
            return minBuildHeight;
        }
    }

    private static int colourFor(BlockState state, Holder<Biome> biome, int height, int seaLevel) {
        MapColor mapColor = state.getBlock().defaultMapColor();
        int rgb;
        if (!state.getFluidState().isEmpty() || mapColor == MapColor.WATER) {
            rgb = biome.value().getWaterColor();
        } else if (mapColor == MapColor.GRASS) {
            rgb = biome.value().getGrassColor(0.0, 0.0);
        } else if (mapColor == MapColor.PLANT) {
            rgb = biome.value().getFoliageColor();
        } else if (mapColor != MapColor.NONE && mapColor.col != 0) {
            rgb = mapColor.col;
        } else {
            rgb = biome.value().getGrassColor(0.0, 0.0);
        }
        float altitude = Mth.clamp((height - seaLevel) / 160.0f, -0.25f, 1.0f);
        float factor = altitude >= 0.0f ? 1.0f + altitude * 0.16f : 1.0f + altitude * 0.10f;
        return 0xFF000000 | scaleRgb(rgb, factor);
    }

    private static int scaleRgb(int rgb, float factor) {
        int r = Mth.clamp((int) (((rgb >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp((int) (((rgb >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp((int) ((rgb & 0xFF) * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static void shade(int[] colours, short[] heights) {
        for (int z = 0; z < SparsePlanetLodTile.RESOLUTION; z++) {
            int z0 = Math.max(0, z - 1);
            int z1 = Math.min(SparsePlanetLodTile.RESOLUTION - 1, z + 1);
            for (int x = 0; x < SparsePlanetLodTile.RESOLUTION; x++) {
                int x0 = Math.max(0, x - 1);
                int x1 = Math.min(SparsePlanetLodTile.RESOLUTION - 1, x + 1);
                int dx = heights[z * 16 + x1] - heights[z * 16 + x0];
                int dz = heights[z1 * 16 + x] - heights[z0 * 16 + x];
                float factor = Mth.clamp(1.0f + (dx - dz) * 0.012f, 0.72f, 1.28f);
                int index = z * 16 + x;
                colours[index] = 0xFF000000 | scaleRgb(colours[index], factor);
            }
        }
    }
}
