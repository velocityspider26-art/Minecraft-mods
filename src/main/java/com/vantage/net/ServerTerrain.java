package com.vantage.net;

import com.vantage.Vantage;
import com.vantage.VantageServerConfig;
import com.vantage.core.Lod;
import com.vantage.gen.BiomeLook;
import com.vantage.gen.ColumnSampler;
import com.vantage.worldgen.PlanetChunkGenerator;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server side of distant terrain: answers players' {@link TerrainRequest}s from the world
 * generator on a few background threads, and tells every player about the dimension they are in
 * ({@link PlanetInfo}). Works the same on dedicated servers and on a game opened to friends.
 */
public final class ServerTerrain {
    /** Requests further than this from the player are refused (about the LOD distance limit). */
    private static final double MAX_DISTANCE = 600_000.0;

    private static final Map<ResourceKey<Level>, ColumnSampler> SAMPLERS = new ConcurrentHashMap<>();
    private static final Map<UUID, AtomicInteger> PENDING = new ConcurrentHashMap<>();
    private static volatile ExecutorService executor;

    private ServerTerrain() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ServerTerrain::onLogin);
        NeoForge.EVENT_BUS.addListener(ServerTerrain::onLogout);
        NeoForge.EVENT_BUS.addListener(ServerTerrain::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(ServerTerrain::onRespawn);
        NeoForge.EVENT_BUS.addListener(ServerTerrain::onStopped);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sendInfo(p, p.serverLevel());
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            ServerLevel to = p.server.getLevel(event.getTo());
            if (to != null) {
                sendInfo(p, to);
            }
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sendInfo(p, p.serverLevel());
        }
    }

    private static void onStopped(ServerStoppedEvent event) {
        SAMPLERS.clear();
        PENDING.clear();
        ExecutorService e = executor;
        executor = null;
        if (e != null) {
            e.shutdownNow();
        }
    }

    private static void sendInfo(ServerPlayer player, ServerLevel level) {
        if (!player.connection.hasChannel(PlanetInfo.TYPE)) {
            return;
        }
        int radius = level.getChunkSource().getGenerator() instanceof PlanetChunkGenerator planet ? planet.planet().radius() : 0;
        ColumnSampler sampler = sampler(level);
        PacketDistributor.sendToPlayer(player, new PlanetInfo(level.dimension(), radius, VantageServerConfig.SERVE_TERRAIN.get(),
                sampler.seaLevel(), Block.getId(sampler.defaultBlock())));
    }

    /** Network thread. */
    public static void handle(TerrainRequest request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!VantageServerConfig.SERVE_TERRAIN.get() || !valid(request, player)) {
            context.reply(TerrainColumns.refusal(request, TerrainColumns.REFUSED));
            return;
        }
        AtomicInteger pending = PENDING.computeIfAbsent(player.getUUID(), u -> new AtomicInteger());
        if (pending.incrementAndGet() > VantageServerConfig.MAX_PENDING.get()) {
            pending.decrementAndGet();
            context.reply(TerrainColumns.refusal(request, TerrainColumns.BUSY));
            return;
        }
        try {
            executor().execute(() -> {
                try {
                    TerrainColumns answer = answer(request, player);
                    PacketDistributor.sendToPlayer(player, answer);
                } catch (RuntimeException e) {
                    Vantage.LOGGER.warn("Distant terrain request failed: {}", e.toString());
                } finally {
                    pending.decrementAndGet();
                }
            });
        } catch (RuntimeException e) {
            pending.decrementAndGet();
            context.reply(TerrainColumns.refusal(request, TerrainColumns.BUSY));
        }
    }

    private static boolean valid(TerrainRequest r, ServerPlayer player) {
        if (r.level() < 0 || r.level() > Lod.MAX_LEVEL || r.columns().length != TerrainRequest.MASK_LONGS) {
            return false;
        }
        if (!player.level().dimension().equals(r.dimension())) {
            return false;
        }
        double span = Lod.sectionBlocks(r.level());
        double cx = (r.sx() + 0.5) * span - player.getX();
        double cz = (r.sz() + 0.5) * span - player.getZ();
        return Math.sqrt(cx * cx + cz * cz) <= MAX_DISTANCE + span;
    }

    private static TerrainColumns answer(TerrainRequest r, ServerPlayer player) {
        ServerLevel level = player.server.getLevel(r.dimension());
        if (level == null || player.isRemoved()) {
            return TerrainColumns.refusal(r, TerrainColumns.REFUSED);
        }
        ColumnSampler sampler = sampler(level);
        boolean[] wanted = TerrainRequest.unmask(r.columns());
        int[] heights = new int[Lod.AREA];
        @SuppressWarnings("unchecked")
        Holder<Biome>[] biomes = new Holder[Lod.AREA];
        sampler.sample(r.level(), r.sx(), r.sz(), wanted, heights, biomes, null);

        Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Int2IntOpenHashMap paletteIndex = new Int2IntOpenHashMap();
        paletteIndex.defaultReturnValue(-1);
        int[] palette = new int[256];
        int paletteSize = 0;
        boolean[] sampled = new boolean[Lod.AREA];
        int n = 0;
        for (int i = 0; i < Lod.AREA; i++) {
            if (heights[i] != Integer.MIN_VALUE) {
                sampled[i] = true;
                n++;
            }
        }
        short[] h = new short[n];
        byte[] b = new byte[n];
        int k = 0;
        for (int i = 0; i < Lod.AREA; i++) {
            if (!sampled[i]) {
                continue;
            }
            int id = registry.getId(biomes[i].value());
            int p = paletteIndex.get(id);
            if (p < 0) {
                if (paletteSize == palette.length) {
                    // More than 256 biomes in one section column: leave the rest for another time.
                    sampled[i] = false;
                    continue;
                }
                p = paletteSize;
                palette[paletteSize++] = id;
                paletteIndex.put(id, p);
            }
            h[k] = (short) heights[i];
            b[k] = (byte) p;
            k++;
        }
        if (k < n) {
            h = java.util.Arrays.copyOf(h, k);
            b = java.util.Arrays.copyOf(b, k);
        }
        int[] looks = new int[paletteSize * 4];
        float[] canopy = new float[paletteSize];
        byte[] treeHeight = new byte[paletteSize];
        for (int p = 0; p < paletteSize; p++) {
            Holder<Biome> biome = registry.getHolder(palette[p]).map(x -> (Holder<Biome>) x).orElseThrow();
            BiomeLook look = sampler.look(biome);
            looks[p * 4] = stateId(look.top());
            looks[p * 4 + 1] = stateId(look.under());
            looks[p * 4 + 2] = stateId(look.seabed());
            looks[p * 4 + 3] = stateId(look.leaves());
            canopy[p] = look.canopy();
            treeHeight[p] = (byte) Math.min(127, look.treeHeight());
        }
        return new TerrainColumns(r.dimension(), r.level(), r.sx(), r.sz(), TerrainColumns.OK, TerrainRequest.mask(sampled), h, b,
                java.util.Arrays.copyOf(palette, paletteSize), looks, canopy, treeHeight);
    }

    private static ColumnSampler sampler(ServerLevel level) {
        return SAMPLERS.computeIfAbsent(level.dimension(), k -> new ColumnSampler(level));
    }

    private static int stateId(BlockState state) {
        return state == null ? 0 : Block.getId(state) + 1;
    }

    private static ExecutorService executor() {
        ExecutorService e = executor;
        if (e == null) {
            synchronized (ServerTerrain.class) {
                e = executor;
                if (e == null) {
                    int threads = VantageServerConfig.threads();
                    AtomicInteger counter = new AtomicInteger();
                    ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), r -> {
                        Thread t = new Thread(r, "Vantage terrain server " + counter.incrementAndGet());
                        t.setDaemon(true);
                        t.setPriority(Thread.MIN_PRIORITY + 1);
                        return t;
                    });
                    pool.allowCoreThreadTimeOut(true);
                    executor = e = pool;
                }
            }
        }
        return e;
    }
}
