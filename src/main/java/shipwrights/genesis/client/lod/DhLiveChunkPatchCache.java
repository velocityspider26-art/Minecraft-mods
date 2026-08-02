package shipwrights.genesis.client.lod;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataRepo;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.surface.PlanetLodVolumeTile;
import shipwrights.genesis.space.surface.SparsePlanetLodService;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Persistent high-detail overlay for DH's exact 16x16 chunk columns.
 *
 * <p>The six-face base atlas is intentionally coarse because it has to cover
 * 402 million square blocks. Player edits cannot survive that reduction: on
 * Earth one 64x64 atlas texel represents 128x128 blocks. This cache keeps the
 * chunks DH actually knows at block-column resolution and renders them over the
 * base cube. DH's chunk-modified event makes placement, breaking, explosions,
 * terrain loading and multiplayer updates dirty the exact affected chunk.</p>
 *
 * <p>Nothing scans the whole world every tick. A bounded worker consumes exact
 * dirty chunks and performs a slow rotating sweep around the player so missed
 * notifications heal themselves. The cache is stored beside DH's own database,
 * therefore returning to space after a restart does not erase already observed
 * structures.</p>
 */
public final class DhLiveChunkPatchCache {
    public static final int PATCH_RESOLUTION = 16;
    public static final int CELL_COUNT = PATCH_RESOLUTION * PATCH_RESOLUTION;

    private static final int FILE_MAGIC = 0x474C4F44; // GLOD
    private static final int FILE_VERSION = 2;
    private static final int MAX_PATCHES = 16384;
    private static final int MAX_QUEUE_BATCH = 96;
    private static final long SWEEP_INTERVAL_NANOS = 350_000_000L;
    private static final long SAVE_INTERVAL_NANOS = 5_000_000_000L;
    private static final int SWEEP_RADIUS_CHUNKS = 48;
    private static final int[][] SWEEP_OFFSETS = buildSpiral(SWEEP_RADIUS_CHUNKS);

    private static final ThreadFactory THREAD_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "Genesis-DH-Live-Cube");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    };
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(THREAD_FACTORY);
    private static final AtomicBoolean WORK_SCHEDULED = new AtomicBoolean();
    private static final AtomicInteger GENERATION = new AtomicInteger();
    private static final ConcurrentLinkedQueue<Long> DIRTY_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Set<Long> QUEUED = ConcurrentHashMap.newKeySet();

    private static final Object PATCH_LOCK = new Object();
    private static final LinkedHashMap<Long, Patch> PATCHES = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Patch> eldest) {
            return size() > MAX_PATCHES;
        }
    };

    private static volatile IDhApiLevelWrapper activeWrapper;
    private static volatile IDhApiTerrainDataCache dataCache;
    private static volatile CubeNetSurfaceTransform transform;
    private static volatile Path persistenceFile;
    private static volatile int seaLevel = 63;
    private static volatile int sweepCursor;
    private static volatile int sweepCenterChunkX;
    private static volatile int sweepCenterChunkZ;
    private static volatile long nextSweepNanos;
    private static volatile long lastSaveNanos;
    private static volatile int unsavedChanges;
    private static volatile long revision;

    private DhLiveChunkPatchCache() {
    }

    public record Patch(int chunkX, int chunkZ, CubeNetSurfaceTransform.Face face,
                        short[] heights, int[] colours, int minHeight, int maxHeight,
                        long revision) {
        public Patch {
            Objects.requireNonNull(face, "face");
            if (heights == null || heights.length != CELL_COUNT
                    || colours == null || colours.length != CELL_COUNT) {
                throw new IllegalArgumentException("A live DH patch must contain exactly 16x16 columns");
            }
        }

        public int index(int x, int z) {
            return z * PATCH_RESOLUTION + x;
        }

        public int worldMinX() {
            return chunkX << 4;
        }

        public int worldMinZ() {
            return chunkZ << 4;
        }
    }

    /**
     * Called every high-altitude frame. Repository work is event-driven: DH's
     * chunk-modified event and Genesis' server sparse-tile stream identify the
     * exact places that changed. The old rotating 49x49-chunk sweep was removed
     * because it still behaved like a continuous local scan.
     */
    public static void update(IDhApiLevelWrapper wrapper, CubeNetSurfaceTransform nextTransform,
                              ClientLevel level, double cameraX, double cameraZ) {
        if (wrapper == null || nextTransform == null || level == null) {
            return;
        }
        if (activeWrapper != wrapper) {
            attach(wrapper, nextTransform, level);
        } else {
            transform = nextTransform;
        }
        scheduleWorker();
    }

    /** Exact chunk coordinates come directly from DhApiChunkModifiedEvent. */
    public static void markDirty(int chunkX, int chunkZ) {
        long key = pack(chunkX, chunkZ);
        if (QUEUED.add(key)) {
            DIRTY_QUEUE.offer(key);
        }
        scheduleWorker();
    }

    public static Patch[] snapshot() {
        synchronized (PATCH_LOCK) {
            return PATCHES.values().toArray(Patch[]::new);
        }
    }


    public static boolean hasPatchAt(CubeNetSurfaceTransform.Face face, double worldX, double worldZ) {
        int chunkX = Math.floorDiv(floorToInt(worldX), 16);
        int chunkZ = Math.floorDiv(floorToInt(worldZ), 16);
        synchronized (PATCH_LOCK) {
            Patch patch = PATCHES.get(pack(chunkX, chunkZ));
            return patch != null && patch.face() == face;
        }
    }

    public static int patchCount() {
        synchronized (PATCH_LOCK) {
            return PATCHES.size();
        }
    }

    public static int seaLevel() {
        return seaLevel;
    }

    private static synchronized void attach(IDhApiLevelWrapper wrapper,
                                            CubeNetSurfaceTransform nextTransform,
                                            ClientLevel level) {
        int generation = GENERATION.incrementAndGet();
        closeCache();
        activeWrapper = wrapper;
        transform = nextTransform;
        seaLevel = level.getSeaLevel();
        sweepCursor = 0;
        nextSweepNanos = 0L;
        unsavedChanges = 0;
        revision = 0L;
        DIRTY_QUEUE.clear();
        QUEUED.clear();
        synchronized (PATCH_LOCK) {
            PATCHES.clear();
        }

        IDhApiTerrainDataRepo repo = DhApi.Delayed.terrainRepo;
        if (repo != null) {
            try {
                dataCache = repo.createSoftCache();
            } catch (Throwable error) {
                GenesisMod.LOGGER.warn("[DH-LIVE] could not create patch cache", error);
            }
        }
        try {
            Path folder = wrapper.getDhSaveFolder().toPath();
            Files.createDirectories(folder);
            persistenceFile = folder.resolve("genesis-cube-live-v2.bin");
        } catch (Throwable error) {
            persistenceFile = null;
            GenesisMod.LOGGER.warn("[DH-LIVE] persistent patch file unavailable", error);
        }

        WORKER.execute(() -> loadPersisted(generation, wrapper));
        GenesisMod.LOGGER.info("[DH-LIVE] attached live 16x16 chunk overlay to {}", wrapper.getDhIdentifier());
    }

    private static void scheduleWorker() {
        if (activeWrapper == null || dataCache == null || DIRTY_QUEUE.isEmpty()) {
            return;
        }
        if (WORK_SCHEDULED.compareAndSet(false, true)) {
            int generation = GENERATION.get();
            WORKER.execute(() -> drainQueue(generation));
        }
    }

    private static void drainQueue(int generation) {
        try {
            int processed = 0;
            while (processed < MAX_QUEUE_BATCH && generation == GENERATION.get()) {
                Long key = DIRTY_QUEUE.poll();
                if (key == null) {
                    break;
                }
                QUEUED.remove(key);
                refreshPatch(generation, unpackX(key), unpackZ(key));
                processed++;
            }
            long now = System.nanoTime();
            if (unsavedChanges > 0 && now - lastSaveNanos >= SAVE_INTERVAL_NANOS) {
                savePersisted(generation);
                lastSaveNanos = now;
            }
        } finally {
            WORK_SCHEDULED.set(false);
            if (!DIRTY_QUEUE.isEmpty()) {
                scheduleWorker();
            }
        }
    }

    private static void refreshPatch(int generation, int chunkX, int chunkZ) {
        IDhApiLevelWrapper wrapper = activeWrapper;
        IDhApiTerrainDataRepo repo = DhApi.Delayed.terrainRepo;
        IDhApiTerrainDataCache cache = dataCache;
        CubeNetSurfaceTransform activeTransform = transform;
        if (generation != GENERATION.get() || wrapper == null || repo == null
                || cache == null || activeTransform == null) {
            return;
        }

        double centerX = (chunkX << 4) + 8.0;
        double centerZ = (chunkZ << 4) + 8.0;
        CubeNetSurfaceTransform.Face face = activeTransform.faceContaining(centerX, centerZ);
        if (face == null) {
            return;
        }

        try {
            DhApiResult<DhApiTerrainDataPoint[][][]> result =
                    repo.getAllTerrainDataAtChunkPos(wrapper, chunkX, chunkZ, cache);
            if (result == null || !result.success || result.payload == null
                    || result.payload.length == 0) {
                return;
            }
            short[] heights = new short[CELL_COUNT];
            int[] colours = new int[CELL_COUNT];
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            int valid = 0;

            for (int x = 0; x < PATCH_RESOLUTION; x++) {
                for (int z = 0; z < PATCH_RESOLUTION; z++) {
                    DhApiTerrainDataPoint[] column = columnAt(result.payload, x, z);
                    DhApiTerrainDataPoint top = topPoint(column);
                    int index = z * PATCH_RESOLUTION + x;
                    if (top == null) {
                        heights[index] = (short) seaLevel;
                        colours[index] = 0x00000000;
                        continue;
                    }
                    int height = clampShort(top.topYBlockPos - 1);
                    heights[index] = (short) height;
                    colours[index] = colourFor(top, (chunkX << 4) + x, height, (chunkZ << 4) + z);
                    min = Math.min(min, height);
                    max = Math.max(max, height);
                    valid++;
                }
            }
            if (valid == 0 || generation != GENERATION.get()) {
                return;
            }
            long nextRevision = ++revision;
            PlanetLodVolumeTile volume = buildVolumeTile(chunkX, chunkZ, face, result.payload, nextRevision);
            if (volume != null && volume.segmentCount() > 0) {
                PlanetLodVolumeClientCache.accept(SparsePlanetLodService.EARTH_ID, volume);
            }
            Patch patch = new Patch(chunkX, chunkZ, face, heights, colours,
                    min == Integer.MAX_VALUE ? seaLevel : min,
                    max == Integer.MIN_VALUE ? seaLevel : max,
                    nextRevision);
            synchronized (PATCH_LOCK) {
                PATCHES.put(pack(chunkX, chunkZ), patch);
            }
            unsavedChanges++;
            if ((nextRevision & 31L) == 1L) {
                GenesisMod.LOGGER.info("[DH-LIVE] {} exact chunks cached; latest {},{} on {}",
                        patchCount(), chunkX, chunkZ, face);
            }
        } catch (Throwable ignored) {
            // A missing DH source is normal while its database is still filling.
        }
    }

    private static PlanetLodVolumeTile buildVolumeTile(int chunkX, int chunkZ,
                                                            CubeNetSurfaceTransform.Face face,
                                                            DhApiTerrainDataPoint[][][] area,
                                                            long tileRevision) {
        int[] offsets = new int[PlanetLodVolumeTile.OFFSET_COUNT];
        List<DhVolumeSegment> segments = new ArrayList<>(768);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int z = 0; z < PlanetLodVolumeTile.RESOLUTION; z++) {
            for (int x = 0; x < PlanetLodVolumeTile.RESOLUTION; x++) {
                int columnIndex = z * 16 + x;
                offsets[columnIndex] = segments.size();
                DhApiTerrainDataPoint[] column = columnAt(area, x, z);
                if (column != null) {
                    List<DhApiTerrainDataPoint> visible = new ArrayList<>();
                    int highest = Integer.MIN_VALUE;
                    for (DhApiTerrainDataPoint point : column) {
                        if (point == null || point.blockStateWrapper == null
                                || point.blockStateWrapper.isAir()) continue;
                        visible.add(point);
                        highest = Math.max(highest, point.topYBlockPos);
                    }
                    visible.sort((a, b) -> Integer.compare(b.topYBlockPos, a.topYBlockPos));
                    int cutoff = highest == Integer.MIN_VALUE ? Integer.MAX_VALUE : highest - 192;
                    for (DhApiTerrainDataPoint point : visible) {
                        if (segments.size() - offsets[columnIndex]
                                >= PlanetLodVolumeTile.MAX_SEGMENTS_PER_COLUMN) break;
                        int top = point.topYBlockPos;
                        int bottom = Math.max(point.bottomYBlockPos, cutoff);
                        if (top <= bottom) continue;
                        Object wrapped = point.blockStateWrapper.getWrappedMcObject();
                        if (!(wrapped instanceof BlockState state)) continue;
                        int stateId = Block.getId(state);
                        int colour = colourFor(point, originX + x, top - 1, originZ + z);
                        int sky = Math.max(0, Math.min(15, point.skyLightLevel));
                        int block = Math.max(0, Math.min(15, point.blockLightLevel));
                        byte light = (byte) ((sky << 4) | block);
                        segments.add(new DhVolumeSegment(bottom, top, stateId, colour, light,
                                flagsFor(state, point.blockStateWrapper)));
                    }
                }
                offsets[columnIndex + 1] = segments.size();
            }
        }
        if (segments.isEmpty() || segments.size() > PlanetLodVolumeTile.MAX_SEGMENTS) return null;
        short[] bottoms = new short[segments.size()];
        short[] tops = new short[segments.size()];
        int[] stateIds = new int[segments.size()];
        int[] colours = new int[segments.size()];
        byte[] lights = new byte[segments.size()];
        byte[] flags = new byte[segments.size()];
        for (int i = 0; i < segments.size(); i++) {
            DhVolumeSegment segment = segments.get(i);
            bottoms[i] = (short) segment.bottom();
            tops[i] = (short) segment.top();
            stateIds[i] = segment.stateId();
            colours[i] = segment.colour();
            lights[i] = segment.light();
            flags[i] = segment.flags();
        }
        return new PlanetLodVolumeTile(face, chunkX, chunkZ, tileRevision, offsets,
                bottoms, tops, stateIds, colours, lights, flags);
    }

    private static byte flagsFor(BlockState state, IDhApiBlockStateWrapper wrapper) {
        byte flags = 0;
        if (wrapper.isLiquid() || !state.getFluidState().isEmpty()) flags |= PlanetLodVolumeTile.FLAG_LIQUID;
        if (!wrapper.isSolid() || !state.canOcclude()) flags |= PlanetLodVolumeTile.FLAG_TRANSLUCENT;
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

    private record DhVolumeSegment(int bottom, int top, int stateId, int colour,
                                   byte light, byte flags) {
    }

    private static DhApiTerrainDataPoint[] columnAt(DhApiTerrainDataPoint[][][] area, int x, int z) {
        if (x < area.length && area[x] != null && z < area[x].length) {
            return area[x][z];
        }
        if (z < area.length && area[z] != null && x < area[z].length) {
            return area[z][x];
        }
        return null;
    }

    private static DhApiTerrainDataPoint topPoint(DhApiTerrainDataPoint[] column) {
        if (column == null) {
            return null;
        }
        DhApiTerrainDataPoint top = null;
        for (DhApiTerrainDataPoint point : column) {
            if (point == null || point.blockStateWrapper == null || point.blockStateWrapper.isAir()) {
                continue;
            }
            if (top == null || point.topYBlockPos > top.topYBlockPos) {
                top = point;
            }
        }
        return top;
    }

    private static int colourFor(DhApiTerrainDataPoint point, int x, int y, int z) {
        IDhApiBlockStateWrapper state = point.blockStateWrapper;
        try {
            Object value = state.getClass().getMethod("getMapColor").invoke(state);
            if (value instanceof Color colour) {
                int rgb = (colour.getRed() << 16) | (colour.getGreen() << 8) | colour.getBlue();
                String serial = Objects.toString(state.getSerialString(), "").toLowerCase(Locale.ROOT);
                String biome = point.biomeWrapper == null ? ""
                        : Objects.toString(point.biomeWrapper.getName(), "").toLowerCase(Locale.ROOT);
                if (state.isLiquid()) return tint(rgb, 0x315D9B, 0.72);
                if (serial.contains("grass") || serial.contains("leaves") || serial.contains("moss")) {
                    return tint(rgb, biomeGreen(biome), 0.58);
                }
                return 0xFF000000 | rgb;
            }
        } catch (Throwable ignored) {
        }
        return fallbackColour(state, point.biomeWrapper == null ? "" : point.biomeWrapper.getName());
    }

    private static int biomeGreen(String biome) {
        String lower = Objects.toString(biome, "").toLowerCase(Locale.ROOT);
        if (lower.contains("desert") || lower.contains("badlands")) return 0xFF9E9B55;
        if (lower.contains("snow") || lower.contains("frozen") || lower.contains("ice")) return 0xFFB8C8B2;
        if (lower.contains("swamp") || lower.contains("mangrove")) return 0xFF4F6B3A;
        if (lower.contains("jungle")) return 0xFF2F8B3F;
        if (lower.contains("savanna")) return 0xFF8A9A46;
        return 0xFF5E9346;
    }

    private static int tint(int first, int second, double secondWeight) {
        double w = Math.max(0.0, Math.min(1.0, secondWeight));
        int r = (int) (((first >> 16) & 0xFF) * (1.0 - w) + ((second >> 16) & 0xFF) * w);
        int g = (int) (((first >> 8) & 0xFF) * (1.0 - w) + ((second >> 8) & 0xFF) * w);
        int b = (int) ((first & 0xFF) * (1.0 - w) + (second & 0xFF) * w);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int fallbackColour(IDhApiBlockStateWrapper state, String biomeName) {
        String serial = state == null ? "" : Objects.toString(state.getSerialString(), "").toLowerCase(Locale.ROOT);
        if (state != null && state.isLiquid()) return 0xFF315D9B;
        if (serial.contains("snow") || serial.contains("ice")) return 0xFFE9F0F2;
        if (serial.contains("sand") || serial.contains("sandstone")) return 0xFFC9B36A;
        if (serial.contains("stone") || serial.contains("deepslate")) return 0xFF777773;
        if (serial.contains("dirt") || serial.contains("mud")) return 0xFF71503A;
        if (serial.contains("leaves") || serial.contains("grass") || serial.contains("moss")) return biomeGreen(biomeName);
        if (serial.contains("wood") || serial.contains("log") || serial.contains("planks")) return 0xFF856443;
        return 0xFF6E7D58;
    }

    private static void loadPersisted(int generation, IDhApiLevelWrapper wrapper) {
        Path file = persistenceFile;
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        List<Patch> loaded = new ArrayList<>();
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (input.readInt() != FILE_MAGIC || input.readInt() != FILE_VERSION) {
                return;
            }
            int count = Math.max(0, Math.min(MAX_PATCHES, input.readInt()));
            for (int i = 0; i < count; i++) {
                int chunkX = input.readInt();
                int chunkZ = input.readInt();
                int ordinal = input.readUnsignedByte();
                int min = input.readShort();
                int max = input.readShort();
                long patchRevision = input.readLong();
                short[] heights = new short[CELL_COUNT];
                int[] colours = new int[CELL_COUNT];
                for (int cell = 0; cell < CELL_COUNT; cell++) heights[cell] = input.readShort();
                for (int cell = 0; cell < CELL_COUNT; cell++) colours[cell] = input.readInt();
                CubeNetSurfaceTransform.Face[] faces = CubeNetSurfaceTransform.Face.values();
                if (ordinal < faces.length) {
                    loaded.add(new Patch(chunkX, chunkZ, faces[ordinal], heights, colours,
                            min, max, patchRevision));
                }
            }
        } catch (EOFException ignored) {
            loaded.clear();
        } catch (Throwable error) {
            GenesisMod.LOGGER.warn("[DH-LIVE] could not read persistent chunk patches", error);
            loaded.clear();
        }
        if (generation != GENERATION.get() || wrapper != activeWrapper || loaded.isEmpty()) {
            return;
        }
        synchronized (PATCH_LOCK) {
            for (Patch patch : loaded) {
                PATCHES.put(pack(patch.chunkX(), patch.chunkZ()), patch);
                revision = Math.max(revision, patch.revision());
            }
        }
        GenesisMod.LOGGER.info("[DH-LIVE] restored {} exact terrain chunks from DH-side cache", loaded.size());
    }

    private static void savePersisted(int generation) {
        Path file = persistenceFile;
        if (file == null || generation != GENERATION.get()) {
            return;
        }
        Patch[] patches = snapshot();
        if (writeSnapshot(file, patches)) {
            unsavedChanges = 0;
        }
    }

    private static boolean writeSnapshot(Path file, Patch[] patches) {
        if (file == null) {
            return false;
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (DataOutputStream output = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                output.writeInt(FILE_MAGIC);
                output.writeInt(FILE_VERSION);
                output.writeInt(patches.length);
                for (Patch patch : patches) {
                    output.writeInt(patch.chunkX());
                    output.writeInt(patch.chunkZ());
                    output.writeByte(patch.face().ordinal());
                    output.writeShort(patch.minHeight());
                    output.writeShort(patch.maxHeight());
                    output.writeLong(patch.revision());
                    for (short height : patch.heights()) output.writeShort(height);
                    for (int colour : patch.colours()) output.writeInt(colour);
                }
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Throwable error) {
            GenesisMod.LOGGER.warn("[DH-LIVE] could not persist chunk patches", error);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
            }
            return false;
        }
    }

    public static synchronized void clear() {
        // Logging out is a render-thread path. The old implementation wrote a
        // multi-megabyte cache and closed DH synchronously here, making world
        // exit look like a crash. Snapshot immutable records, detach instantly,
        // then let the existing low-priority worker save and close in order.
        GENERATION.incrementAndGet();
        Path file = persistenceFile;
        Patch[] patches = unsavedChanges > 0 ? snapshot() : new Patch[0];
        IDhApiTerrainDataCache cache = dataCache;

        activeWrapper = null;
        transform = null;
        persistenceFile = null;
        dataCache = null;
        unsavedChanges = 0;
        DIRTY_QUEUE.clear();
        QUEUED.clear();
        synchronized (PATCH_LOCK) {
            PATCHES.clear();
        }

        WORKER.execute(() -> {
            if (patches.length > 0 && file != null) {
                writeSnapshot(file, patches);
            }
            closeCache(cache);
        });
    }

    private static void closeCache() {
        IDhApiTerrainDataCache cache = dataCache;
        dataCache = null;
        closeCache(cache);
    }

    private static void closeCache(IDhApiTerrainDataCache cache) {
        if (cache != null) {
            try {
                cache.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static int[][] buildSpiral(int radius) {
        List<int[]> positions = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        positions.add(new int[]{0, 0});
        for (int ring = 1; ring <= radius; ring++) {
            for (int x = -ring; x <= ring; x++) positions.add(new int[]{x, -ring});
            for (int z = -ring + 1; z <= ring; z++) positions.add(new int[]{ring, z});
            for (int x = ring - 1; x >= -ring; x--) positions.add(new int[]{x, ring});
            for (int z = ring - 1; z > -ring; z--) positions.add(new int[]{-ring, z});
        }
        return positions.toArray(int[][]::new);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long key) {
        return (int) (key >> 32);
    }

    private static int unpackZ(long key) {
        return (int) key;
    }

    private static int floorToInt(double value) {
        if (value <= Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.floor(value);
    }

    private static int clampShort(int value) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }
}
