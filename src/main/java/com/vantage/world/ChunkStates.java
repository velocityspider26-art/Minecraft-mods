package com.vantage.world;

import com.vantage.Vantage;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Where each chunk's LOD data came from: nothing yet, Vantage's own generation, or the real
 * chunk (explored, or read from the save). Real data must never be replaced by generated data,
 * and generated chunks need not be made again. Kept per 32×32-chunk region, saved with the LODs.
 */
public final class ChunkStates {
    public static final byte NONE = 0;
    public static final byte GENERATED = 1;
    public static final byte REAL = 2;
    private static final int VERSION = 1;

    private final Path file;
    private final Long2ObjectMap<byte[]> regions = new Long2ObjectOpenHashMap<>();
    private boolean dirty;
    private long savedAt = System.nanoTime();

    public ChunkStates(Path file) {
        this.file = file;
        this.load();
    }

    public synchronized byte get(int cx, int cz) {
        byte[] r = this.regions.get(regionKey(cx, cz));
        return r == null ? NONE : r[index(cx, cz)];
    }

    public synchronized void markReal(int cx, int cz) {
        this.set(cx, cz, REAL);
    }

    /** Marks a chunk generated unless it is real; returns false (and changes nothing) if it is. */
    public synchronized boolean markGeneratedUnlessReal(int cx, int cz) {
        if (this.get(cx, cz) == REAL) {
            return false;
        }
        this.set(cx, cz, GENERATED);
        return true;
    }

    private void set(int cx, int cz, byte state) {
        byte[] r = this.regions.computeIfAbsent(regionKey(cx, cz), k -> new byte[1024]);
        int i = index(cx, cz);
        if (r[i] != state) {
            r[i] = state;
            this.dirty = true;
        }
    }

    private static long regionKey(int cx, int cz) {
        return (long) (cx >> 5) << 32 | ((cz >> 5) & 0xFFFFFFFFL);
    }

    private static int index(int cx, int cz) {
        return (cz & 31) << 5 | (cx & 31);
    }

    /** Saves if anything changed in the last {@code intervalNanos}. */
    public void saveIfDue(long intervalNanos) {
        if (System.nanoTime() - this.savedAt >= intervalNanos) {
            this.save();
        }
    }

    public void save() {
        byte[][] data;
        long[] keys;
        synchronized (this) {
            this.savedAt = System.nanoTime();
            if (!this.dirty) {
                return;
            }
            this.dirty = false;
            keys = this.regions.keySet().toLongArray();
            data = new byte[keys.length][];
            for (int i = 0; i < keys.length; i++) {
                data[i] = this.regions.get(keys[i]).clone();
            }
        }
        Path tmp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
        try {
            Files.createDirectories(this.file.getParent());
            try (OutputStream os = Files.newOutputStream(tmp);
                 DataOutputStream out = new DataOutputStream(new DeflaterOutputStream(os))) {
                out.writeInt(VERSION);
                out.writeInt(keys.length);
                for (int i = 0; i < keys.length; i++) {
                    out.writeLong(keys[i]);
                    out.write(data[i]);
                }
            }
            Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Vantage.LOGGER.warn("Could not save {}: {}", this.file, e.toString());
            synchronized (this) {
                this.dirty = true;
            }
        }
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        try (InputStream is = Files.newInputStream(this.file);
             DataInputStream in = new DataInputStream(new InflaterInputStream(is))) {
            if (in.readInt() != VERSION) {
                return;
            }
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                long key = in.readLong();
                byte[] r = new byte[1024];
                in.readFully(r);
                this.regions.put(key, r);
            }
        } catch (IOException e) {
            Vantage.LOGGER.warn("Could not read {}, starting over: {}", this.file, e.toString());
            this.regions.clear();
        }
    }
}
