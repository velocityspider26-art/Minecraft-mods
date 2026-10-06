package com.vantage.net;

import com.vantage.Vantage;
import com.vantage.VantageServerConfig;
import com.vantage.gen.WorldGenSandbox;
import com.vantage.util.WorkerPool;
import com.vantage.world.ChunkNbt;
import com.vantage.world.ColumnSource;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server side of real distant terrain for players whose game cannot make it (everyone but the
 * host of a game opened to LAN or Essential, and everyone on a server): answers
 * {@link DetailRequest}s with {@link ChunkSkin}s. Explored chunks are sent as they are, from
 * memory or from the save; for the rest the world generator makes them in throwaway chunks
 * ({@link WorldGenSandbox}), which are remembered for a while so other players get them at once.
 */
public final class DetailService {
    /** Units one player may have waiting; more are turned away and asked for again later. */
    private static final int MAX_PENDING = 3;
    private static final int UNIT = DetailRequest.UNIT;

    private static final Map<ResourceKey<Level>, Dimension> DIMENSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, AtomicInteger> PENDING = new ConcurrentHashMap<>();
    private static volatile @Nullable WorkerPool pool;

    private DetailService() {
    }

    /** How far this player gets real terrain from the server, in blocks; 0 if not at all. */
    public static int servedDistance(ServerPlayer player) {
        return VantageServerConfig.SERVE_DETAIL.get() && player.connection.hasChannel(DetailRequest.TYPE)
                ? VantageServerConfig.DETAIL_DISTANCE.get() : 0;
    }

    /** Network thread. */
    public static void handle(DetailRequest request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!VantageServerConfig.SERVE_DETAIL.get() || !player.level().dimension().equals(request.dimension()) || request.wanted() == 0) {
            context.reply(DetailChunks.refusal(request, DetailChunks.REFUSED));
            return;
        }
        double distance = distance(request, player);
        if (distance > VantageServerConfig.DETAIL_DISTANCE.get() + UNIT * 16) {
            context.reply(DetailChunks.refusal(request, DetailChunks.REFUSED));
            return;
        }
        AtomicInteger pending = PENDING.computeIfAbsent(player.getUUID(), u -> new AtomicInteger());
        if (pending.incrementAndGet() > MAX_PENDING) {
            pending.decrementAndGet();
            context.reply(DetailChunks.refusal(request, DetailChunks.BUSY));
            return;
        }
        pool().submit(distance, () -> {
            try {
                answer(request, player);
            } catch (RuntimeException e) {
                Vantage.LOGGER.warn("Real terrain request failed: {}", e.toString());
                PacketDistributor.sendToPlayer(player, DetailChunks.refusal(request, DetailChunks.BUSY));
            } finally {
                pending.decrementAndGet();
            }
        });
    }

    private static double distance(DetailRequest r, ServerPlayer player) {
        double cx = (r.ux() + 0.5) * UNIT * 16 - player.getX();
        double cz = (r.uz() + 0.5) * UNIT * 16 - player.getZ();
        return Math.sqrt(cx * cx + cz * cz);
    }

    private static void answer(DetailRequest r, ServerPlayer player) {
        ServerLevel level = player.server.getLevel(r.dimension());
        if (level == null || player.isRemoved() || distance(r, player) > VantageServerConfig.DETAIL_DISTANCE.get() + UNIT * 32) {
            PacketDistributor.sendToPlayer(player, DetailChunks.refusal(r, DetailChunks.REFUSED));
            return;
        }
        Dimension d = DIMENSIONS.computeIfAbsent(level.dimension(), k -> new Dimension(level));
        byte[][] skins = new byte[64][];
        long missing = 0;
        for (int i = 0; i < 64; i++) {
            if ((r.wanted() >>> i & 1) == 0) {
                continue;
            }
            int cx = r.ux() * UNIT + (i % UNIT);
            int cz = r.uz() * UNIT + (i / UNIT);
            byte[] skin = d.real(cx, cz);
            if (skin == null) {
                skin = d.cache.get(ChunkPos.asLong(cx, cz));
            }
            if (skin == null) {
                missing |= 1L << i;
            } else {
                skins[i] = skin;
            }
        }
        if (missing != 0) {
            Map<Long, byte[]> made = d.generate(r.ux(), r.uz());
            for (int i = 0; i < 64; i++) {
                if ((missing >>> i & 1) != 0) {
                    long key = ChunkPos.asLong(r.ux() * UNIT + (i % UNIT), r.uz() * UNIT + (i / UNIT));
                    skins[i] = made != null ? made.get(key) : d.cache.get(key);
                }
            }
        }
        send(player, r, skins);
    }

    /** Sends the skins, in as many messages as their size needs. */
    private static void send(ServerPlayer player, DetailRequest r, byte[][] skins) {
        ByteArrayOutputStream part = new ByteArrayOutputStream();
        long answered = 0;
        int count = 0;
        for (int i = 0; i < 64; i++) {
            byte[] skin = skins[i];
            if (skin == null) {
                continue;
            }
            if (count > 0 && part.size() + skin.length > DetailChunks.MAX_BYTES) {
                PacketDistributor.sendToPlayer(player, new DetailChunks(r.dimension(), r.ux(), r.uz(), DetailChunks.OK, answered, false, count,
                        part.toByteArray()));
                part.reset();
                answered = 0;
                count = 0;
            }
            part.writeBytes(skin);
            answered |= 1L << i;
            count++;
        }
        PacketDistributor.sendToPlayer(player, new DetailChunks(r.dimension(), r.ux(), r.uz(), DetailChunks.OK, answered, true, count,
                part.toByteArray()));
    }

    /** Whether others may join this game and get real terrain from it (then the host shares what it makes). */
    public static boolean sharing(ServerLevel level) {
        return VantageServerConfig.SERVE_DETAIL.get() && level.getServer().isPublished();
    }

    /**
     * The host's own game made this chunk's skin (see the client's detail generator): keep it for
     * the other players. Any thread.
     */
    public static void offer(ServerLevel level, int cx, int cz, byte[] skin) {
        if (sharing(level)) {
            DIMENSIONS.computeIfAbsent(level.dimension(), k -> new Dimension(level)).cache.put(ChunkPos.asLong(cx, cz), skin);
        }
    }

    /** A skin made earlier for another player, if still remembered. Any thread. */
    public static @Nullable byte[] cached(ServerLevel level, int cx, int cz) {
        Dimension d = DIMENSIONS.get(level.dimension());
        return d == null || d.level != level ? null : d.cache.get(ChunkPos.asLong(cx, cz));
    }

    public static void forget(UUID player) {
        PENDING.remove(player);
    }

    /** Server stopped: let go of everything. */
    public static void stop() {
        WorkerPool p = pool;
        pool = null;
        if (p != null) {
            p.close();
        }
        DIMENSIONS.clear();
        PENDING.clear();
    }

    private static WorkerPool pool() {
        WorkerPool p = pool;
        if (p == null) {
            synchronized (DetailService.class) {
                p = pool;
                if (p == null) {
                    pool = p = new WorkerPool("Vantage terrain server detail", VantageServerConfig.detailThreads());
                }
            }
        }
        return p;
    }

    /** What the service keeps per dimension. */
    private static final class Dimension {
        final ServerLevel level;
        final Registry<Biome> biomes;
        final Holder<Biome> plains;
        final ChunkNbt nbt;
        final SkinCache cache = new SkinCache((long) VantageServerConfig.DETAIL_CACHE_MB.get() << 20);
        /** Units being made, so two players asking at once wait for one generation. */
        final Map<Long, CompletableFuture<Void>> making = new ConcurrentHashMap<>();
        private volatile @Nullable WorldGenSandbox sandbox;
        private volatile boolean sandboxFailed;

        Dimension(ServerLevel level) {
            this.level = level;
            this.biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
            this.plains = this.biomes.getHolderOrThrow(Biomes.PLAINS);
            this.nbt = new ChunkNbt(this.biomes, this.plains, level.getMinSection(), level.getSectionsCount());
        }

        /** The real chunk's skin, if it exists: loaded, or saved far enough along. */
        @Nullable
        byte[] real(int cx, int cz) {
            int slot = (cx & (UNIT - 1)) | (cz & (UNIT - 1)) << 4;
            ChunkHolder holder = this.level.getChunkSource().chunkMap.getVisibleChunkIfPresent(ChunkPos.asLong(cx, cz));
            ChunkAccess loaded = holder == null ? null : holder.getLatestChunk();
            if (loaded != null && loaded.getPersistedStatus().isOrAfter(ChunkStatus.FEATURES)) {
                try {
                    // Read while the game may change it, as its light engine does; a torn read throws, and then the save is used.
                    return ChunkSkin.encode(ChunkSkin.of(loaded), this.level.getMinBuildHeight(), this.level.getMinBuildHeight(), slot,
                            ChunkSkin.REAL, this.biomes);
                } catch (RuntimeException e) {
                    // Fall through.
                }
            }
            try {
                Optional<CompoundTag> tag = this.level.getChunkSource().chunkMap.read(new ChunkPos(cx, cz)).get(30, TimeUnit.SECONDS);
                if (tag.isEmpty()) {
                    return null;
                }
                ColumnSource src = this.nbt.parse(tag.get(), cx, cz, true);
                return src == null ? null : ChunkSkin.encode(ChunkSkin.of(src, this.level.getMinBuildHeight(), this.plains),
                        this.level.getMinBuildHeight(), this.level.getMinBuildHeight(), slot, ChunkSkin.REAL, this.biomes);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Makes the unit's chunks with the world generator; returns their skins (also kept in the
         * cache), or null if another request was making them (then they are in the cache) or the
         * generator cannot be used.
         */
        @Nullable
        Map<Long, byte[]> generate(int ux, int uz) {
            WorldGenSandbox sb = this.sandbox();
            if (sb == null) {
                return null;
            }
            long key = ChunkPos.asLong(ux, uz);
            CompletableFuture<Void> mine = new CompletableFuture<>();
            CompletableFuture<Void> other = this.making.putIfAbsent(key, mine);
            if (other != null) {
                other.join();
                return null;
            }
            try {
                Map<Long, byte[]> made = new java.util.HashMap<>();
                for (WorldGenSandbox.Chunk c : sb.generate(ux * UNIT, uz * UNIT)) {
                    int slot = (c.pos().x & (UNIT - 1)) | (c.pos().z & (UNIT - 1)) << 4;
                    byte[] skin = ChunkSkin.encode(ChunkSkin.of(c.chunk()), this.level.getMinBuildHeight(), c.bottom(), slot, 0, this.biomes);
                    this.cache.put(c.pos().toLong(), skin);
                    made.put(c.pos().toLong(), skin);
                }
                return made;
            } finally {
                this.making.remove(key);
                mine.complete(null);
            }
        }

        @Nullable
        private WorldGenSandbox sandbox() {
            WorldGenSandbox sb = this.sandbox;
            if (sb == null && !this.sandboxFailed) {
                synchronized (this) {
                    sb = this.sandbox;
                    if (sb == null && !this.sandboxFailed) {
                        try {
                            this.sandbox = sb = new WorldGenSandbox(this.level);
                        } catch (RuntimeException e) {
                            this.sandboxFailed = true;
                            Vantage.LOGGER.warn("Real terrain for players is unavailable in {}: {}", this.level.dimension().location(), e.toString());
                        }
                    }
                }
            }
            return sb;
        }
    }

    /** Skins by chunk, least recently used dropped first beyond a memory budget. */
    private static final class SkinCache {
        private final long budget;
        private final LinkedHashMap<Long, byte[]> map = new LinkedHashMap<>(256, 0.75f, true);
        private long bytes;

        SkinCache(long budget) {
            this.budget = budget;
        }

        synchronized @Nullable byte[] get(long key) {
            return this.map.get(key);
        }

        synchronized void put(long key, byte[] skin) {
            if (this.budget <= 0) {
                return;
            }
            byte[] old = this.map.put(key, skin);
            this.bytes += skin.length - (old == null ? 0 : old.length);
            var it = this.map.values().iterator();
            while (this.bytes > this.budget && it.hasNext()) {
                this.bytes -= it.next().length;
                it.remove();
            }
        }
    }
}
