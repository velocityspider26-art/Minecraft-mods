package com.vantage.client;

import com.vantage.Vantage;
import com.vantage.client.gen.DistantGenerator;
import com.vantage.client.gen.LocalSource;
import com.vantage.client.gen.RemoteSource;
import com.vantage.client.net.ClientTerrain;
import com.vantage.net.PlanetInfo;
import net.minecraft.server.level.ServerLevel;
import com.vantage.client.ingest.ChunkCapture;
import com.vantage.client.ingest.ColumnSource;
import com.vantage.client.ingest.ColumnVoxelizer;
import com.vantage.client.ingest.IngestScheduler;
import com.vantage.client.ingest.RegionImporter;
import com.vantage.client.render.CoverageMap;
import com.vantage.client.render.LodRenderer;
import com.vantage.client.render.MeshManager;
import com.vantage.client.render.Planner;
import com.vantage.client.visual.VisualAnalyzer;
import com.vantage.client.visual.VisualRegistry;
import com.vantage.storage.DataVersion;
import com.vantage.util.WorkerPool;
import com.vantage.world.LodWorld;
import com.vantage.world.VoxelColumn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
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

    /** Voxelizes a column and stores it. Any thread. */
    public void ingest(ColumnSource source) {
        VoxelColumn column = this.voxelizers.get().voxelize(source, this.caveCulling);
        this.world.insert(column);
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
        this.ingest.tick(level);
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
        this.planner.close();
        this.pool.close();
        this.world.close();
        this.visuals.close();
        if (this.renderer != null) {
            this.renderer.close();
            this.renderer = null;
        }
    }
}
