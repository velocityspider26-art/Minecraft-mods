package shipwrights.genesis.client.lod;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataRepo;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiChunkModifiedEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetSurfaceTextures;
import shipwrights.genesis.space.surface.PlanetSurfaceData;
import shipwrights.genesis.space.surface.PlanetSurfaceSampler;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;

import java.awt.Color;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads the real Distant Horizons terrain repository and turns its cached
 * columns into Genesis' six-face cube LOD.
 *
 * <p>All repository reads happen on one minimum-priority daemon. DH's API may
 * wait for its asynchronous data provider, so calling it from the render thread
 * would recreate the exact flight freezes this project already eliminated.</p>
 */
public final class DhCubeLodBridge {
    private static final ResourceLocation OVERWORLD_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final int OUTPUT_RESOLUTION = PlanetSurfaceSampler.RESOLUTION;
    private static final int BASE_SAMPLE_RESOLUTION = 16;
    private static final int CURRENT_FACE_RESOLUTION = 32;
    private static final int FACE_LENGTH = OUTPUT_RESOLUTION * OUTPUT_RESOLUTION;

    private static final ThreadFactory THREAD_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "Genesis-DH-Cube-LOD");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    };
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(THREAD_FACTORY);
    private static final AtomicBoolean EVENTS_BOUND = new AtomicBoolean();
    private static final AtomicBoolean SAMPLING = new AtomicBoolean();
    private static final AtomicInteger GENERATION = new AtomicInteger();

    private static volatile IDhApiLevelWrapper activeWrapper;
    private static volatile IDhApiTerrainDataCache softCache;
    private static volatile CubeNetSurfaceTransform activeTransform;
    private static volatile PlanetSurfaceData surface;
    private static volatile CubeNetSurfaceTransform.Face prioritisedFace = CubeNetSurfaceTransform.Face.UP;
    private static volatile boolean[] completedFaces = new boolean[CubeNetSurfaceTransform.Face.values().length];
    private static volatile int[] faceSuccessfulColumns = new int[CubeNetSurfaceTransform.Face.values().length];
    private static volatile boolean[] refinedFaces = new boolean[CubeNetSurfaceTransform.Face.values().length];
    private static volatile boolean initialPassComplete;
    private static volatile int[] colours;
    private static volatile short[] heights;
    private static volatile int seaLevel = 63;
    private static volatile int successfulColumns;
    private static volatile int failedColumns;
    private static volatile long lastChunkChangeNanos;

    private DhCubeLodBridge() {
    }

    /** Called from the overworld render path; never blocks. */
    public static void update(ClientLevel level, CubeNetSurfaceTransform transform,
                              CubeNetSurfaceTransform.Face currentFace,
                              double cameraX, double cameraZ) {
        bindEventsOnce();
        if (level == null || transform == null) {
            return;
        }
        activeTransform = transform;
        prioritisedFace = currentFace == null ? CubeNetSurfaceTransform.Face.UP : currentFace;

        IDhApiLevelWrapper wrapper = findWrapper(level);
        if (wrapper == null) {
            return;
        }
        if (activeWrapper != wrapper) {
            resetForWrapper(wrapper, level);
        }
        long changedAt = lastChunkChangeNanos;
        if (initialPassComplete && changedAt != 0L
                && System.nanoTime() - changedAt > 2_000_000_000L) {
            refinedFaces[prioritisedFace.ordinal()] = false;
            lastChunkChangeNanos = 0L;
        }
        startSamplingIfNeeded(wrapper, transform, prioritisedFace, level);
        DhLiveChunkPatchCache.update(wrapper, transform, level, cameraX, cameraZ);
    }

    public static PlanetSurfaceData surface() {
        return surface;
    }

    public static boolean hasFace(CubeNetSurfaceTransform.Face face) {
        boolean[] completed = completedFaces;
        int[] faceCounts = faceSuccessfulColumns;
        return face != null && face.ordinal() < completed.length && completed[face.ordinal()]
                && faceCounts[face.ordinal()] >= BASE_SAMPLE_RESOLUTION * BASE_SAMPLE_RESOLUTION / 8;
    }

    public static boolean hasAnyDhTerrain() {
        return successfulColumns >= BASE_SAMPLE_RESOLUTION * BASE_SAMPLE_RESOLUTION / 8
                && surface != null && surface.isValid();
    }

    public static int successfulColumns() {
        return successfulColumns;
    }

    public static int failedColumns() {
        return failedColumns;
    }

    public static IDhApiLevelWrapper activeWrapper() {
        return activeWrapper;
    }

    private static void bindEventsOnce() {
        if (!EVENTS_BOUND.compareAndSet(false, true)) {
            return;
        }
        try {
            DhApi.events.bind(DhApiBeforeRenderEvent.class, new DhApiBeforeRenderEvent() {
                @Override
                public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event) {
                    DhApiRenderParam value = event.value;
                    if (value != null
                            && value.clientLevelWrapper == activeWrapper
                            && WorldLodEarthRenderer.shouldReplaceFlatDh()) {
                        event.cancelEvent();
                    }
                }
            });
            DhApi.events.bind(DhApiChunkModifiedEvent.class, new DhApiChunkModifiedEvent() {
                @Override
                public void onChunkModified(DhApiEventParam<EventParam> event) {
                    EventParam value = event.value;
                    if (value != null && value.levelWrapper == activeWrapper) {
                        // Do not rescan on every block/chunk update. Record a
                        // debounce signal; the next idle sampling pass refreshes
                        // the face from DH's now-updated cache.
                        lastChunkChangeNanos = System.nanoTime();
                        DhLiveChunkPatchCache.markDirty(value.chunkX, value.chunkZ);
                    }
                }
            });
            GenesisMod.LOGGER.info("[DH-CUBE] Distant Horizons terrain and render hooks installed (API {})",
                    DhApi.getModVersion());
        } catch (Throwable error) {
            EVENTS_BOUND.set(false);
            GenesisMod.LOGGER.error("[DH-CUBE] could not install Distant Horizons API hooks", error);
        }
    }

    private static IDhApiLevelWrapper findWrapper(ClientLevel level) {
        try {
            if (DhApi.Delayed.worldProxy == null || !DhApi.Delayed.worldProxy.worldLoaded()) {
                return null;
            }
            for (IDhApiLevelWrapper wrapper : DhApi.Delayed.worldProxy.getAllLoadedLevelWrappers()) {
                if (wrapper == null) {
                    continue;
                }
                Object wrapped = wrapper.getWrappedMcObject();
                if (wrapped == level) {
                    return wrapper;
                }
            }
            for (IDhApiLevelWrapper wrapper : DhApi.Delayed.worldProxy.getAllLoadedLevelWrappers()) {
                if (wrapper == null) {
                    continue;
                }
                String identifier = wrapper.getDhIdentifier();
                String name = wrapper.getDimensionName();
                if ((identifier != null && identifier.contains("minecraft:overworld"))
                        || (name != null && name.toLowerCase().contains("overworld"))) {
                    return wrapper;
                }
            }
        } catch (Throwable ignored) {
            // DH finishes attaching its level wrapper after the client level is
            // visible. A not-ready result is normal during those first frames.
        }
        return null;
    }

    private static synchronized void resetForWrapper(IDhApiLevelWrapper wrapper, ClientLevel level) {
        int generation = GENERATION.incrementAndGet();
        activeWrapper = wrapper;
        completedFaces = new boolean[CubeNetSurfaceTransform.Face.values().length];
        faceSuccessfulColumns = new int[CubeNetSurfaceTransform.Face.values().length];
        refinedFaces = new boolean[CubeNetSurfaceTransform.Face.values().length];
        initialPassComplete = false;
        successfulColumns = 0;
        failedColumns = 0;
        surface = null;
        seaLevel = 63;
        try {
            seaLevel = level.getSeaLevel();
        } catch (Throwable ignored) {
        }

        int expected = PlanetSurfaceData.expectedLength();
        colours = new int[expected];
        heights = new short[expected];

        PlanetSurfaceData previous = PlanetSurfaceTextures.surface(OVERWORLD_ID);
        if (previous != null && previous.isValid()) {
            System.arraycopy(previous.colours(), 0, colours, 0, expected);
            System.arraycopy(previous.heights(), 0, heights, 0, expected);
            seaLevel = previous.seaLevel();
        } else {
            Arrays.fill(colours, 0xFF345A3A);
            Arrays.fill(heights, (short) seaLevel);
        }

        closeCache();
        IDhApiTerrainDataRepo repo = DhApi.Delayed.terrainRepo;
        if (repo != null) {
            try {
                softCache = repo.createSoftCache();
            } catch (Throwable error) {
                GenesisMod.LOGGER.warn("[DH-CUBE] could not create DH soft cache", error);
            }
        }
        SAMPLING.set(false);
        GenesisMod.LOGGER.info("[DH-CUBE] attached to {} (generation {}, sea level {})",
                wrapper.getDhIdentifier(), generation, seaLevel);
    }

    private static void startSamplingIfNeeded(IDhApiLevelWrapper wrapper,
                                              CubeNetSurfaceTransform transform,
                                              CubeNetSurfaceTransform.Face currentFace,
                                              ClientLevel level) {
        if (DhApi.Delayed.terrainRepo == null || softCache == null || colours == null || heights == null) {
            return;
        }
        if (initialPassComplete && refinedFaces[currentFace.ordinal()]) {
            return;
        }
        if (!SAMPLING.compareAndSet(false, true)) {
            return;
        }
        int generation = GENERATION.get();
        if (!initialPassComplete) {
            WORKER.execute(() -> sampleAllFaces(generation, wrapper, transform, currentFace, level));
        } else if (!refinedFaces[currentFace.ordinal()]) {
            WORKER.execute(() -> refineCurrentFace(generation, wrapper, transform, currentFace, level));
        } else {
            SAMPLING.set(false);
        }
    }

    private static void sampleAllFaces(int generation, IDhApiLevelWrapper wrapper,
                                       CubeNetSurfaceTransform transform,
                                       CubeNetSurfaceTransform.Face currentFace,
                                       ClientLevel level) {
        long started = System.nanoTime();
        try {
            CubeNetSurfaceTransform.Face[] order = samplingOrder(currentFace);
            // First publish a very cheap current-face grid so the visual change
            // appears quickly, then refine that face while DH's own flat LOD is
            // still available underneath it.
            sampleFace(generation, wrapper, transform, currentFace, BASE_SAMPLE_RESOLUTION);
            completedFaces[currentFace.ordinal()] = true;
            publishSnapshot(level, currentFace);

            sampleFace(generation, wrapper, transform, currentFace, CURRENT_FACE_RESOLUTION);
            refinedFaces[currentFace.ordinal()] = true;
            publishSnapshot(level, currentFace);

            for (CubeNetSurfaceTransform.Face face : order) {
                if (face == currentFace) continue;
                if (generation != GENERATION.get() || wrapper != activeWrapper) return;
                sampleFace(generation, wrapper, transform, face, BASE_SAMPLE_RESOLUTION);
                completedFaces[face.ordinal()] = true;
                publishSnapshot(level, face);
            }
            initialPassComplete = true;
            double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
            GenesisMod.LOGGER.info(
                    "[DH-CUBE] six-face base LOD ready: {} successful columns, {} missing, {} s",
                    successfulColumns, failedColumns, String.format("%.2f", seconds));
        } catch (Throwable error) {
            GenesisMod.LOGGER.error("[DH-CUBE] terrain sampling failed", error);
        } finally {
            SAMPLING.set(false);
        }
    }

    private static void refineCurrentFace(int generation, IDhApiLevelWrapper wrapper,
                                          CubeNetSurfaceTransform transform,
                                          CubeNetSurfaceTransform.Face face,
                                          ClientLevel level) {
        try {
            sampleFace(generation, wrapper, transform, face, CURRENT_FACE_RESOLUTION);
            if (generation == GENERATION.get() && wrapper == activeWrapper) {
                refinedFaces[face.ordinal()] = true;
                completedFaces[face.ordinal()] = true;
                publishSnapshot(level, face);
                GenesisMod.LOGGER.info("[DH-CUBE] refined {} to {}x{} DH columns",
                        face, CURRENT_FACE_RESOLUTION, CURRENT_FACE_RESOLUTION);
            }
        } catch (Throwable error) {
            GenesisMod.LOGGER.warn("[DH-CUBE] could not refine {}", face, error);
        } finally {
            SAMPLING.set(false);
        }
    }

    private static CubeNetSurfaceTransform.Face[] samplingOrder(CubeNetSurfaceTransform.Face first) {
        CubeNetSurfaceTransform.Face[] values = CubeNetSurfaceTransform.Face.values();
        CubeNetSurfaceTransform.Face[] order = new CubeNetSurfaceTransform.Face[values.length];
        order[0] = first;
        int index = 1;
        for (CubeNetSurfaceTransform.Face value : values) {
            if (value != first) {
                order[index++] = value;
            }
        }
        return order;
    }

    private static void sampleFace(int generation, IDhApiLevelWrapper wrapper,
                                   CubeNetSurfaceTransform transform,
                                   CubeNetSurfaceTransform.Face face, int sampleResolution) {
        IDhApiTerrainDataRepo repo = DhApi.Delayed.terrainRepo;
        IDhApiTerrainDataCache cache = softCache;
        if (repo == null || cache == null) {
            return;
        }
        double faceSpan = transform.faceSpan();
        double centerX = transform.faceCenterX(face);
        double centerZ = transform.faceCenterZ(face);

        GenesisMod.LOGGER.info("[DH-CUBE] sampling {} from DH cached world columns", face);
        int faceSuccess = 0;
        for (int sampleZ = 0; sampleZ < sampleResolution; sampleZ++) {
            if (generation != GENERATION.get()) {
                return;
            }
            double worldZ = centerZ + ((sampleZ + 0.5) / sampleResolution - 0.5) * faceSpan;
            for (int sampleX = 0; sampleX < sampleResolution; sampleX++) {
                double worldX = centerX + ((sampleX + 0.5) / sampleResolution - 0.5) * faceSpan;
                Column column = readColumn(repo, wrapper, cache,
                        floorToInt(worldX), floorToInt(worldZ));
                if (column == null) {
                    failedColumns++;
                    continue;
                }
                successfulColumns++;
                faceSuccess++;
                writeSample(face, sampleX, sampleZ, sampleResolution, column);
            }
        }
        faceSuccessfulColumns[face.ordinal()] = faceSuccess;
    }

    private static Column readColumn(IDhApiTerrainDataRepo repo, IDhApiLevelWrapper wrapper,
                                     IDhApiTerrainDataCache cache, int worldX, int worldZ) {
        try {
            DhApiResult<DhApiTerrainDataPoint[]> result =
                    repo.getColumnDataAtBlockPos(wrapper, worldX, worldZ, cache);
            if (result == null || !result.success || result.payload == null) {
                return null;
            }
            DhApiTerrainDataPoint top = null;
            for (DhApiTerrainDataPoint point : result.payload) {
                if (point == null || point.blockStateWrapper == null || point.blockStateWrapper.isAir()) {
                    continue;
                }
                if (top == null || point.topYBlockPos > top.topYBlockPos) {
                    top = point;
                }
            }
            if (top == null) {
                return null;
            }
            int y = Math.max(-32768, Math.min(32767, top.topYBlockPos - 1));
            int colour = colourFor(wrapper, top, worldX, y, worldZ);
            return new Column((short) y, colour);
        } catch (Throwable error) {
            return null;
        }
    }

    private static int colourFor(IDhApiLevelWrapper wrapper, DhApiTerrainDataPoint point,
                                 int x, int y, int z) {
        IDhApiBlockStateWrapper state = point.blockStateWrapper;
        try {
            Object value = state.getClass().getMethod("getMapColor").invoke(state);
            if (value instanceof Color colour) {
                int rgb = (colour.getRed() << 16) | (colour.getGreen() << 8) | colour.getBlue();
                String serial = Objects.toString(state.getSerialString(), "").toLowerCase();
                String biome = point.biomeWrapper == null ? ""
                        : Objects.toString(point.biomeWrapper.getName(), "").toLowerCase();
                if (state.isLiquid()) return tint(rgb, 0x315D9B, 0.70);
                if (serial.contains("grass") || serial.contains("leaves") || serial.contains("moss")) {
                    int biomeGreen = biomeGreen(biome);
                    return tint(rgb, biomeGreen, 0.58);
                }
                return 0xFF000000 | rgb;
            }
        } catch (Throwable ignored) {
        }
        return fallbackColour(state, point.biomeWrapper == null ? "" : point.biomeWrapper.getName());
    }

    private static int biomeGreen(String biome) {
        if (biome.contains("desert") || biome.contains("badlands")) return 0xFF9E9B55;
        if (biome.contains("snow") || biome.contains("frozen") || biome.contains("ice")) return 0xFFB8C8B2;
        if (biome.contains("swamp") || biome.contains("mangrove")) return 0xFF4F6B3A;
        if (biome.contains("jungle")) return 0xFF2F8B3F;
        if (biome.contains("savanna")) return 0xFF8A9A46;
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
        String serial = state == null ? "" : Objects.toString(state.getSerialString(), "").toLowerCase();
        if (state != null && state.isLiquid()) {
            return 0xFF315D9B;
        }
        if (serial.contains("snow") || serial.contains("ice")) return 0xFFE9F0F2;
        if (serial.contains("sand") || serial.contains("sandstone")) return 0xFFC9B36A;
        if (serial.contains("stone") || serial.contains("deepslate")) return 0xFF777773;
        if (serial.contains("dirt") || serial.contains("mud")) return 0xFF71503A;
        if (serial.contains("leaves") || serial.contains("grass") || serial.contains("moss")) return biomeGreen(Objects.toString(biomeName, "").toLowerCase());
        if (serial.contains("wood") || serial.contains("log") || serial.contains("planks")) return 0xFF856443;
        return 0xFF6E7D58;
    }

    private static void writeSample(CubeNetSurfaceTransform.Face face, int sampleX, int sampleZ,
                                    int sampleResolution, Column column) {
        int outputsPerSample = OUTPUT_RESOLUTION / sampleResolution;
        int baseX = sampleX * outputsPerSample;
        int baseZ = sampleZ * outputsPerSample;
        for (int dz = 0; dz < outputsPerSample; dz++) {
            for (int dx = 0; dx < outputsPerSample; dx++) {
                int index = PlanetSurfaceData.index(face, baseX + dx, baseZ + dz);
                heights[index] = column.height;
                colours[index] = column.colour;
            }
        }
    }

    private static void publishSnapshot(ClientLevel level, CubeNetSurfaceTransform.Face completedFace) {
        int[] colourCopy = colours.clone();
        short[] heightCopy = heights.clone();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (short height : heightCopy) {
            min = Math.min(min, height);
            max = Math.max(max, height);
        }
        PlanetSurfaceData next = new PlanetSurfaceData(colourCopy, heightCopy, min, max, seaLevel);
        surface = next;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.level == level && next == surface) {
                PlanetSurfaceTextures.accept(OVERWORLD_ID, next);
                GenesisMod.LOGGER.info(
                        "[DH-CUBE] {} face published; the overworld and orbital Earth now use the same DH terrain",
                        completedFace);
            }
        });
    }

    public static synchronized void clear() {
        GENERATION.incrementAndGet();
        SAMPLING.set(false);
        activeWrapper = null;
        activeTransform = null;
        surface = null;
        colours = null;
        heights = null;
        completedFaces = new boolean[CubeNetSurfaceTransform.Face.values().length];
        faceSuccessfulColumns = new int[CubeNetSurfaceTransform.Face.values().length];
        refinedFaces = new boolean[CubeNetSurfaceTransform.Face.values().length];
        initialPassComplete = false;
        successfulColumns = 0;
        failedColumns = 0;
        lastChunkChangeNanos = 0L;
        closeCache();
        DhLiveChunkPatchCache.clear();
    }

    private static void closeCache() {
        IDhApiTerrainDataCache cache = softCache;
        softCache = null;
        if (cache != null) {
            try {
                cache.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static int floorToInt(double value) {
        if (value <= Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.floor(value);
    }

    private record Column(short height, int colour) {
    }
}
