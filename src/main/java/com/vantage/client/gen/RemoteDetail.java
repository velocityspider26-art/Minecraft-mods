package com.vantage.client.gen;

import com.vantage.Vantage;
import com.vantage.client.LodSession;
import com.vantage.client.net.ClientTerrain;
import com.vantage.core.SectionKey;
import com.vantage.net.ChunkSkin;
import com.vantage.net.DetailChunks;
import com.vantage.net.DetailRequest;
import com.vantage.net.PlanetInfo;
import com.vantage.world.ChunkStates;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real terrain near the player from the server ({@link com.vantage.net.DetailService}), for
 * players whose game cannot run the world generator: asks for the chunks the planner draws that
 * hold nothing real or generated yet, a few units ({@link DetailRequest#UNIT}² chunks) at a time,
 * nearest first.
 */
public final class RemoteDetail implements Detail {
    /** One unit is the chunks under one section column of this level. */
    private static final int UNIT_LEVEL = 2;
    private static final int UNIT = DetailRequest.UNIT;
    private static final int MAX_IN_FLIGHT = 3;
    private static final int MAX_CANDIDATES = 128;
    private static final long TIMEOUT_MS = 120_000;
    private static final long RETRY_MS = 20_000;

    private static final class Flight {
        final long wanted;
        final long sentAt;
        long answered;

        Flight(long wanted, long sentAt) {
            this.wanted = wanted;
            this.sentAt = sentAt;
        }
    }

    private final LodSession session;
    private final ResourceKey<Level> dimension;
    private final double maxDistance;
    private final Registry<Biome> biomes;
    private final Holder<Biome> plains;
    private final BlockState stone;
    private final int minY;
    private final int sectionCount;
    private final boolean sky;
    private final Map<Long, Double> candidates = new ConcurrentHashMap<>();
    private final Map<Long, Flight> inFlight = new ConcurrentHashMap<>();
    private final Map<Long, Long> retryAt = new ConcurrentHashMap<>();
    private final Set<Long> done = ConcurrentHashMap.newKeySet();
    private final AtomicLong stored = new AtomicLong();
    private final AtomicLong waitMillis = new AtomicLong();
    private final AtomicLong answeredChunks = new AtomicLong();
    private volatile boolean closed;
    private volatile boolean failed;

    public RemoteDetail(LodSession session, ClientLevel level, PlanetInfo info, double maxDistance) {
        this.session = session;
        this.dimension = level.dimension();
        this.maxDistance = maxDistance;
        this.biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        this.plains = this.biomes.getHolderOrThrow(Biomes.PLAINS);
        this.stone = Block.stateById(info.stone());
        this.minY = level.getMinBuildHeight();
        this.sectionCount = level.getSectionsCount();
        this.sky = level.dimensionType().hasSkyLight();
        ClientTerrain.INSTANCE.setDetailListener(this::answer);
    }

    /** Planner thread: section {@code key} is drawn (or wanted) at {@code distance} blocks. */
    @Override
    public void request(long key, double distance) {
        int level = SectionKey.level(key);
        if (this.closed || this.failed || level > UNIT_LEVEL || distance > this.maxDistance) {
            return;
        }
        int shift = UNIT_LEVEL - level;
        long unit = pack(SectionKey.x(key) >> shift, SectionKey.z(key) >> shift);
        if (this.done.contains(unit) || this.inFlight.containsKey(unit)) {
            return;
        }
        Long retry = this.retryAt.get(unit);
        if (retry != null && System.currentTimeMillis() < retry) {
            return;
        }
        if (this.candidates.size() < MAX_CANDIDATES || this.candidates.containsKey(unit)) {
            this.candidates.put(unit, distance);
        }
    }

    /** Main thread: gives up on late answers and asks for the nearest wanted units. */
    @Override
    public void tick() {
        if (this.closed) {
            return;
        }
        long now = System.currentTimeMillis();
        this.inFlight.entrySet().removeIf(e -> {
            if (now - e.getValue().sentAt > TIMEOUT_MS) {
                this.retryAt.put(e.getKey(), now + RETRY_MS);
                return true;
            }
            return false;
        });
        while (this.inFlight.size() < MAX_IN_FLIGHT && !this.candidates.isEmpty()) {
            long best = 0;
            double bestDistance = Double.MAX_VALUE;
            for (var e : this.candidates.entrySet()) {
                double d = this.distance(e.getKey());
                if (d > this.maxDistance + UNIT * 16) {
                    this.candidates.remove(e.getKey());
                } else if (d < bestDistance) {
                    bestDistance = d;
                    best = e.getKey();
                }
            }
            if (bestDistance == Double.MAX_VALUE) {
                break;
            }
            this.candidates.remove(best);
            long wanted = this.wanted(best);
            if (wanted == 0) {
                this.done.add(best);
                continue;
            }
            this.inFlight.put(best, new Flight(wanted, now));
            try {
                PacketDistributor.sendToServer(new DetailRequest(this.dimension, unpackX(best), unpackZ(best), wanted));
            } catch (RuntimeException e) {
                this.inFlight.remove(best);
                return;
            }
        }
    }

    private double distance(long unit) {
        double cx = (unpackX(unit) + 0.5) * UNIT * 16 - this.session.camX();
        double cz = (unpackZ(unit) + 0.5) * UNIT * 16 - this.session.camZ();
        return Math.sqrt(cx * cx + cz * cz);
    }

    /** Chunks of the unit with nothing real or generated yet, a bit each. */
    private long wanted(long unit) {
        int x0 = unpackX(unit) * UNIT;
        int z0 = unpackZ(unit) * UNIT;
        ChunkStates states = this.session.chunkStates();
        long wanted = 0;
        for (int i = 0; i < UNIT * UNIT; i++) {
            if (states.get(x0 + i % UNIT, z0 + i / UNIT) == ChunkStates.NONE) {
                wanted |= 1L << i;
            }
        }
        return wanted;
    }

    /** Network thread. */
    private void answer(DetailChunks a) {
        if (this.closed || !a.dimension().equals(this.dimension)) {
            return;
        }
        long unit = pack(a.ux(), a.uz());
        Flight flight = this.inFlight.get(unit);
        long now = System.currentTimeMillis();
        if (a.status() != DetailChunks.OK) {
            this.inFlight.remove(unit);
            this.retryAt.put(unit, now + (a.status() == DetailChunks.REFUSED ? 6 * RETRY_MS : RETRY_MS));
            return;
        }
        if (flight != null) {
            synchronized (flight) {
                flight.answered |= a.answered();
            }
            if (a.last()) {
                this.inFlight.remove(unit);
                this.waitMillis.addAndGet(now - flight.sentAt);
                this.answeredChunks.addAndGet(Long.bitCount(flight.answered));
                if ((flight.wanted & ~flight.answered) != 0) {
                    // The server could not answer for some of them (yet).
                    this.retryAt.put(unit, now + RETRY_MS);
                }
            }
        }
        if (a.count() > 0) {
            this.session.pool.submit(0, () -> this.store(a));
        }
    }

    /** Worker: decodes and stores the chunks of one answer. */
    private void store(DetailChunks a) {
        if (this.closed || this.failed) {
            return;
        }
        ByteBuf buf = Unpooled.wrappedBuffer(a.skins());
        try {
            for (int i = 0; i < a.count(); i++) {
                ChunkSkin.Decoded d = ChunkSkin.decode(buf, a.ux(), a.uz(), UNIT, this.minY, this.sectionCount, this.stone, this.biomes,
                        this.plains, this.sky);
                if (d.real()) {
                    this.session.ingest(d.source());
                    this.stored.incrementAndGet();
                } else if (this.session.ingestGenerated(d.source())) {
                    this.stored.incrementAndGet();
                }
            }
        } catch (RuntimeException e) {
            if (!this.failed) {
                this.failed = true;
                Vantage.LOGGER.error("Real terrain from the server could not be read; asking for no more this session", e);
            }
        } finally {
            buf.release();
        }
    }

    @Override
    public long chunks() {
        return this.stored.get();
    }

    @Override
    public double averageMillis() {
        long n = this.answeredChunks.get();
        return n == 0 ? 0 : this.waitMillis.get() / (double) n;
    }

    @Override
    public int queued() {
        return this.inFlight.size() + this.candidates.size();
    }

    @Override
    public String describe() {
        return String.format(Locale.ROOT, "real terrain %d chunks from the server, %d units asked, %d waiting", this.chunks(),
                this.inFlight.size(), this.candidates.size());
    }

    @Override
    public void close() {
        this.closed = true;
        ClientTerrain.INSTANCE.setDetailListener(null);
        Vantage.LOGGER.info("Real terrain from the server: {} chunks", this.stored.get());
    }

    private static long pack(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long unit) {
        return (int) (unit >> 32);
    }

    private static int unpackZ(long unit) {
        return (int) unit;
    }
}
