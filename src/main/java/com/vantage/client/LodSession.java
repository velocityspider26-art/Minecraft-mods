package com.vantage.client;

import com.vantage.Vantage;
import com.vantage.client.gen.Detail;
import com.vantage.client.gen.DetailGenerator;
import com.vantage.client.gen.DistantGenerator;
import com.vantage.client.gen.LocalSource;
import com.vantage.client.gen.RemoteDetail;
import com.vantage.client.gen.RemoteSource;
import com.vantage.client.ingest.ChunkCapture;
import com.vantage.client.ingest.ColumnVoxelizer;
import com.vantage.client.ingest.IngestScheduler;
import com.vantage.client.ingest.RegionImporter;
import com.vantage.client.net.ClientTerrain;
import com.vantage.client.render.CoverageMap;
import com.vantage.client.render.LodRenderer;
import com.vantage.client.render.MeshManager;
import com.vantage.client.render.Planner;
import com.vantage.client.visual.VisualAnalyzer;
import com.vantage.client.visual.VisualRegistry;
import com.vantage.gen.WorldGenSandbox;
import com.vantage.net.PlanetInfo;
import com.vantage.storage.DataVersion;
import com.vantage.util.WorkerPool;
import com.vantage.world.ChunkStates;
import com.vantage.world.ColumnSource;
import com.vantage.world.LodWorld;
import com.vantage.world.VoxelColumn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Locale;

/** Everything Vantage keeps for the dimension the player is in. Created and closed on the main thread. */
public final class LodSession implements AutoCloseable {
    public final ResourceKey<Level> dimension;
    public final Path dir;
    public final VisualAnalyzer analyzer;
    public final VisualRegistry visuals;
    public final LodWorld world;
    public final WorkerPool pool;
    public final MeshManager meshes;
    public final Planner planner;
    public final IngestScheduler ingest;
    public final CoverageMap coverage = new CoverageMap();
    public final boolean caveCulling;
    private final ThreadLocal<ColumnVoxelizer> voxelizers;
    private @Nullable RegionImporter importer;
    private volatile @Nullable DistantGenerator generator;
    private volatile @Nullable Detail detail;
    private final ChunkStates chunkStates;
    /** Real and generated data for one chunk go in one at a time, so generated never lands on real. */
    private final Object[] chunkLocks = new Object[64];
    private @Nullable LodRenderer renderer;
    private boolean rendererFailed;
    private volatile double camX, camY, camZ;

    private LodSession(ClientLevel level, Path dir) {
        this.dimension = level.dimension();
        this.dir = dir;
        this.caveCulling = VantageConfig.CAVE_CULLING.get();
        boolean sky = level.dimensionType().hasSkyLight();
        Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        this.analyzer = new VisualAnalyzer();
        biomes.getHolder(Biomes.PLAINS).ifPresent(this.analyzer::setFallbackBiome);
        this.visuals = new VisualRegistry(dir.resolve("visuals.txt"), this.analyzer, biomes);
        this.world = new LodWorld(dir, level.getMinBuildHeight(), level.getHeight(), sky, this.visuals,
                (long) VantageConfig.CACHE_MB.get() << 20);
        this.pool = new WorkerPool("Vantage worker", VantageConfig.workerThreads());
        this.meshes = new MeshManager(this.world, this.pool, this.visuals, this.caveCulling && sky);
        this.world.setListener(this.meshes);
        this.planner = new Planner(this.world, this.meshes);
        this.voxelizers = ThreadLocal.withInitial(() -> new ColumnVoxelizer(this.visuals, sky));
        this.ingest = new IngestScheduler(this::submitCapture);
        this.chunkStates = new ChunkStates(dir.resolve("chunks.bin"));
        for (int i = 0; i < this.chunkLocks.length; i++) {
            this.chunkLocks[i] = new Object();
        }
        Vec3 pos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        this.camX = pos.x;
        this.camY = pos.y;
        this.camZ = pos.z;
    }

    public static LodSession open(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        Path dir = mc.gameDirectory.toPath().resolve(Vantage.MODID).resolve(worldId(mc, level)).resolve(dimensionFolder(level.dimension()));
        Vantage.LOGGER.info("Opening LOD data in {}", dir);
        DataVersion.prepare(dir);
        LodSession session = new LodSession(level, dir);
        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null && VantageConfig.IMPORT_SAVES.get()) {
            Path regionDir = net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(), server.getWorldPath(LevelResource.ROOT)).resolve("region");
            session.importer = new RegionImporter(session, level, regionDir, dir.resolve("imported.bin"));
        }
        if (server != null && VantageConfig.DISTANT_GENERATION.get()) {
            ServerLevel serverLevel = server.getLevel(level.dimension());
            if (serverLevel != null) {
                try {
                    session.useGenerator(new DistantGenerator(session.world, session.pool, session.visuals,
                            new LocalSource(serverLevel, level.registryAccess().registryOrThrow(Registries.BIOME), session::importer)));
                    Vantage.LOGGER.info("Distant terrain generation on for {} (local)", level.dimension().location());
                } catch (RuntimeException e) {
                    Vantage.LOGGER.warn("Distant terrain generation unavailable for {}: {}", level.dimension().location(), e.toString());
                }
                if (VantageConfig.DETAILED_GENERATION.get()) {
                    try {
                        DetailGenerator detail = new DetailGenerator(session, new WorldGenSandbox(serverLevel), VantageConfig.detailThreads(),
                                VantageConfig.DETAIL_DISTANCE.get(), level.registryAccess().registryOrThrow(Registries.BIOME), session::importer);
                        session.detail = detail;
                        session.planner.setDetailer(detail);
                        Vantage.LOGGER.info("Detailed distant terrain on for {} ({} threads, {} blocks)", level.dimension().location(),
                                VantageConfig.detailThreads(), VantageConfig.DETAIL_DISTANCE.get());
                    } catch (RuntimeException e) {
                        Vantage.LOGGER.warn("Detailed distant terrain unavailable for {}: {}", level.dimension().location(), e.toString());
                    }
                }
            }
        }
        return session;
    }

    private static String worldId(Minecraft mc, ClientLevel level) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null) {
            Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            // The seed tells apart a new world that reuses a deleted world's folder name.
            long seed = it.unimi.dsi.fastutil.HashCommon.mix(server.getWorldData().worldGenOptions().seed());
            return "sp_" + sanitize(root.getFileName() == null ? "world" : root.getFileName().toString())
                    + "_" + Long.toHexString(seed & 0xFFFFFFFFL);
        }
        ServerData data = mc.getCurrentServer();
        String host = data != null ? data.ip : "unknown";
        return "mp_" + sanitize(host) + "_" + Long.toHexString(hashedSeed(level));
    }

    /** The hashed world seed servers send to clients; tells apart worlds behind one address. */
    private static long hashedSeed(ClientLevel level) {
        try {
            var field = net.minecraft.world.level.biome.BiomeManager.class.getDeclaredField("biomeZoomSeed");
            field.setAccessible(true);
            return field.getLong(level.getBiomeManager());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0L;
        }
    }

    private static String dimensionFolder(ResourceKey<Level> dim) {
        return sanitize(dim.location().getNamespace() + "_" + dim.location().getPath());
    }

    private static String sanitize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    public double camX() {
        return this.camX;
    }

    public double camZ() {
        return this.camZ;
    }

    public void setCamera(double x, double y, double z) {
        this.camX = x;
        this.camY = y;
        this.camZ = z;
    }

    private void submitCapture(ChunkCapture capture) {
        double dx = capture.chunkX() * 16 + 8 - this.camX;
        double dz = capture.chunkZ() * 16 + 8 - this.camZ;
        this.pool.submit(Math.sqrt(dx * dx + dz * dz), () -> this.ingest(capture.toSource()));
    }

    /** Voxelizes a real chunk (explored, or from the save) and stores it. Any thread. */
    public void ingest(ColumnSource source) {
        VoxelColumn column = this.voxelizers.get().voxelize(source, this.caveCulling);
        synchronized (this.chunkLock(source.chunkX, source.chunkZ)) {
            this.chunkStates.markReal(source.chunkX, source.chunkZ);
            this.world.insert(column);
        }
    }

    /**
     * Voxelizes a generated chunk and stores it, unless real data for it exists; returns whether it
     * was stored. Any thread.
     */
    public boolean ingestGenerated(ColumnSource source) {
        if (this.chunkStates.get(source.chunkX, source.chunkZ) == ChunkStates.REAL) {
            return false;
        }
        VoxelColumn column = this.voxelizers.get().voxelize(source, this.caveCulling);
        synchronized (this.chunkLock(source.chunkX, source.chunkZ)) {
            if (!this.chunkStates.markGeneratedUnlessReal(source.chunkX, source.chunkZ)) {
                return false;
            }
            this.world.insert(column);
        }
        return true;
    }

    private Object chunkLock(int cx, int cz) {
        return this.chunkLocks[(int) (it.unimi.dsi.fastutil.HashCommon.mix(net.minecraft.world.level.ChunkPos.asLong(cx, cz)) & (this.chunkLocks.length - 1))];
    }

    public ChunkStates chunkStates() {
        return this.chunkStates;
    }

    public @Nullable Detail detail() {
        return this.detail;
    }

    private void useGenerator(DistantGenerator generator) {
        this.generator = generator;
        this.planner.setGenerator(generator);
    }

    /** Main thread, every client tick. */
    public void tick(ClientLevel level) {
        if (this.generator == null && Minecraft.getInstance().getSingleplayerServer() == null && VantageConfig.DISTANT_GENERATION.get()) {
            // On a server: once it says it answers terrain requests, ask it for unexplored land.
            PlanetInfo info = ClientTerrain.INSTANCE.planet(this.dimension);
            if (info != null && info.servesTerrain()) {
                this.useGenerator(new DistantGenerator(this.world, this.pool, this.visuals,
                        new RemoteSource(this.dimension, level.registryAccess().registryOrThrow(Registries.BIOME), info)));
                Vantage.LOGGER.info("Distant terrain generation on for {} (from the server)", this.dimension.location());
            }
        }
        if (this.detail == null && Minecraft.getInstance().getSingleplayerServer() == null && VantageConfig.DETAILED_GENERATION.get()) {
            // On a server: once it says it sends real terrain, ask it for what is near.
            PlanetInfo info = ClientTerrain.INSTANCE.planet(this.dimension);
            if (info != null && info.detailDistance() > 0) {
                double distance = Math.min(VantageConfig.DETAIL_DISTANCE.get(), info.detailDistance());
                RemoteDetail remote = new RemoteDetail(this, level, info, distance);
                this.detail = remote;
                this.planner.setDetailer(remote);
                Vantage.LOGGER.info("Detailed distant terrain on for {} (from the server, {} blocks)", this.dimension.location(), distance);
            }
        }
        Detail d = this.detail;
        if (d != null) {
            d.tick();
        }
        this.ingest.tick(level);
        this.chunkStates.saveIfDue(60_000_000_000L);
        Minecraft mc = Minecraft.getInstance();
        var cam = mc.gameRenderer.getMainCamera();
        this.coverage.update(level, cam.getBlockPosition().getX() >> 4, cam.getBlockPosition().getZ() >> 4,
                mc.options.getEffectiveRenderDistance());
    }

    /** Render thread. Returns null if this GPU cannot run Vantage. */
    public @Nullable LodRenderer renderer() {
        if (this.renderer == null && !this.rendererFailed) {
            try {
                this.renderer = LodRenderer.create((long) VantageConfig.GPU_MEMORY_MB.get() << 20);
            } catch (RuntimeException e) {
                Vantage.LOGGER.error("Vantage could not start its renderer", e);
            }
            this.rendererFailed = this.renderer == null;
        }
        return this.renderer;
    }

    public @Nullable RegionImporter importer() {
        return this.importer;
    }

    public @Nullable DistantGenerator generator() {
        return this.generator;
    }

    @Override
    public void close() {
        Vantage.LOGGER.info("Closing LOD data for {}", this.dimension.location());
        this.ingest.clear();
        if (this.importer != null) {
            this.importer.close();
        }
        if (this.generator != null) {
            this.generator.close();
        }
        if (this.detail != null) {
            this.detail.close();
        }
        this.planner.close();
        this.pool.close();
        this.chunkStates.save();
        this.world.close();
        this.visuals.close();
        if (this.renderer != null) {
            this.renderer.close();
            this.renderer = null;
        }
    }
}
