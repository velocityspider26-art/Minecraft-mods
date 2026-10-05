package com.vantage.client.ingest;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.function.Consumer;

/**
 * Decides when a live chunk is copied for LOD building. Runs on the main thread only.
 *
 * <p>New chunks are taken as soon as their light arrives. Changed chunks wait for a quiet period so
 * a burst of block or light updates costs one rebuild, but never longer than {@link #MAX_DELAY_MS}.
 * Copying is capped at {@link #BUDGET_NANOS} per tick.
 */
public final class IngestScheduler {
    private static final long CHANGE_DELAY_MS = 1500;
    private static final long MAX_DELAY_MS = 6000;
    private static final long LIGHT_WAIT_MS = 10_000;
    private static final long BUDGET_NANOS = 2_500_000;

    private final Long2LongOpenHashMap due = new Long2LongOpenHashMap();
    private final Long2LongOpenHashMap firstSeen = new Long2LongOpenHashMap();
    private final Consumer<ChunkCapture> sink;
    private long captured;

    public IngestScheduler(Consumer<ChunkCapture> sink) {
        this.sink = sink;
    }

    public void onChunkLoaded(int x, int z) {
        long key = ChunkPos.asLong(x, z);
        long now = System.currentTimeMillis();
        this.due.put(key, now);
        this.firstSeen.putIfAbsent(key, now);
    }

    public void onChunkChanged(int x, int z) {
        long key = ChunkPos.asLong(x, z);
        long now = System.currentTimeMillis();
        long first = this.firstSeen.get(key);
        if (!this.firstSeen.containsKey(key)) {
            first = now;
            this.firstSeen.put(key, now);
        }
        this.due.put(key, Math.min(now + CHANGE_DELAY_MS, first + MAX_DELAY_MS));
    }

    /** Takes a pending change before the chunk goes away so it is not lost. */
    public void onChunkUnloaded(ClientLevel level, LevelChunk chunk) {
        long key = chunk.getPos().toLong();
        if (this.due.containsKey(key)) {
            this.due.remove(key);
            this.firstSeen.remove(key);
            if (ChunkCapture.lightReady(level, chunk.getPos().x, chunk.getPos().z)) {
                this.sink.accept(ChunkCapture.capture(level, chunk));
                this.captured++;
            }
        }
    }

    public void tick(ClientLevel level) {
        if (this.due.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        long start = System.nanoTime();
        var it = this.due.long2LongEntrySet().fastIterator();
        while (it.hasNext()) {
            Long2LongMap.Entry e = it.next();
            if (e.getLongValue() > now) {
                continue;
            }
            long key = e.getLongKey();
            int x = ChunkPos.getX(key);
            int z = ChunkPos.getZ(key);
            LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
            if (chunk == null) {
                it.remove();
                this.firstSeen.remove(key);
                continue;
            }
            if (!ChunkCapture.lightReady(level, x, z) && now - this.firstSeen.get(key) < LIGHT_WAIT_MS) {
                e.setValue(now + 250);
                continue;
            }
            it.remove();
            this.firstSeen.remove(key);
            this.sink.accept(ChunkCapture.capture(level, chunk));
            this.captured++;
            if (System.nanoTime() - start > BUDGET_NANOS) {
                break;
            }
        }
    }

    public int pending() {
        return this.due.size();
    }

    public long captured() {
        return this.captured;
    }

    public void clear() {
        this.due.clear();
        this.firstSeen.clear();
    }
}
