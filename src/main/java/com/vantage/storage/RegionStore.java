package com.vantage.storage;

import com.vantage.core.Lod;
import com.vantage.core.SectionKey;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * On-disk section store.
 *
 * <p>Each level is split into region files of 16×16 section columns. A region file starts with an
 * index of one {@code long} per section slot, followed by 512-byte sectors holding compressed
 * blobs. Index entries are:
 * <ul>
 *   <li>{@code 0} — absent</li>
 *   <li>bit 63 set — uniform section, low 32 bits are the voxel value (no sectors used)</li>
 *   <li>bit 62 set — blob: bits 0..31 first sector, bits 32..55 byte length; bit 61 set if some
 *       voxel columns are unknown, bit 60 set if every voxel is air</li>
 * </ul>
 * A rewrite always goes to free sectors before the index is updated, so a crash leaves either the
 * old or the new version, never a torn one.
 */
public final class RegionStore implements AutoCloseable {
    public static final int SECTOR = 512;
    private static final int MAGIC = 0x564C4F44;
    private static final int VERSION = 1;
    private static final int FIXED_HEADER = 16;
    private static final long UNIFORM_BIT = 1L << 63;
    private static final long BLOB_BIT = 1L << 62;
    private static final long INCOMPLETE_BIT = 1L << 61;
    private static final long AIR_BIT = 1L << 60;
    private static final int MAX_CACHED_REGIONS = 2048;
    private static final int MAX_OPEN_CHANNELS = 48;
    private static final long CHANNEL_IDLE_NANOS = 5_000_000_000L;

    private final Path root;
    private final int[] yCounts = new int[Lod.LEVELS];
    private final LinkedHashMap<Long, Region> regions = new LinkedHashMap<>(256, 0.75f, true);

    /**
     * @param root        directory for this dimension's data
     * @param worldHeight build height of the dimension in blocks
     */
    public RegionStore(Path root, int worldHeight) {
        this.root = root;
        for (int l = 0; l < Lod.LEVELS; l++) {
            this.yCounts[l] = Lod.verticalSections(l, worldHeight);
        }
    }

    public static boolean isUniform(long entry) {
        return (entry & UNIFORM_BIT) != 0;
    }

    public static int uniformValue(long entry) {
        return (int) entry;
    }

    public static boolean isPresent(long entry) {
        return entry != 0;
    }

    /** For blob entries: some voxel columns are unknown. */
    public static boolean isIncomplete(long entry) {
        return (entry & BLOB_BIT) != 0 && (entry & INCOMPLETE_BIT) != 0;
    }

    /** For blob entries: every voxel is air. */
    public static boolean isAllAir(long entry) {
        return (entry & BLOB_BIT) != 0 && (entry & AIR_BIT) != 0;
    }

    /** Index entry for a section; loads the region index on first use. */
    public long entry(long key) {
        Region r = this.region(key);
        int slot = r.slot(key);
        return slot < 0 ? 0 : r.get(slot);
    }

    /** Reads the blob of a section whose entry is a blob, or {@code null} if it changed meanwhile. */
    public byte[] readBlob(long key) throws IOException {
        Region r = this.region(key);
        int slot = r.slot(key);
        return slot < 0 ? null : r.read(slot);
    }

    public void writeUniform(long key, int value) throws IOException {
        Region r = this.region(key);
        int slot = r.slot(key);
        if (slot >= 0) {
            r.write(slot, UNIFORM_BIT | (value & 0xFFFFFFFFL), null);
        }
    }

    public void writeBlob(long key, byte[] blob) throws IOException {
        this.writeBlob(key, blob, false, false);
    }

    public void writeBlob(long key, byte[] blob, boolean allAir, boolean incomplete) throws IOException {
        Region r = this.region(key);
        int slot = r.slot(key);
        if (slot >= 0) {
            r.write(slot, BLOB_BIT | (allAir ? AIR_BIT : 0) | (incomplete ? INCOMPLETE_BIT : 0), blob);
        }
    }

    /** Closes file handles that have been idle for a while. Call periodically. */
    public void sweep() {
        List<Region> snapshot;
        synchronized (this.regions) {
            snapshot = new ArrayList<>(this.regions.values());
        }
        long now = System.nanoTime();
        int open = 0;
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            Region r = snapshot.get(i);
            if (r.closeIfIdle(now, open >= MAX_OPEN_CHANNELS)) {
                continue;
            }
            if (r.hasChannel()) {
                open++;
            }
        }
    }

    @Override
    public void close() {
        synchronized (this.regions) {
            for (Region r : this.regions.values()) {
                r.closeChannel();
            }
            this.regions.clear();
        }
    }

    private Region region(long key) {
        int level = SectionKey.level(key);
        int rx = SectionKey.x(key) >> 4;
        int rz = SectionKey.z(key) >> 4;
        long rk = SectionKey.of(level, rx, 0, rz);
        synchronized (this.regions) {
            Region r = this.regions.get(rk);
            if (r == null) {
                r = new Region(this.root.resolve("L" + level).resolve(rx + "." + rz + ".vlod"), this.yCounts[level]);
                this.regions.put(rk, r);
                if (this.regions.size() > MAX_CACHED_REGIONS) {
                    var it = this.regions.entrySet().iterator();
                    Map.Entry<Long, Region> eldest = it.next();
                    it.remove();
                    eldest.getValue().closeChannel();
                }
            }
            return r;
        }
    }

    private static final class Region {
        private final Path path;
        private int yCount;
        private long[] entries;
        private BitSet used;
        private int headerSectors;
        private FileChannel channel;
        private long lastUse;
        private boolean rewriteHeader;

        Region(Path path, int yCount) {
            this.path = path;
            this.yCount = yCount;
        }

        int slot(long key) {
            int y = SectionKey.y(key);
            this.ensureLoaded();
            if (y >= this.yCount) {
                return -1;
            }
            return (y * 16 + (SectionKey.z(key) & 15)) * 16 + (SectionKey.x(key) & 15);
        }

        synchronized long get(int slot) {
            return this.entries[slot];
        }

        synchronized byte[] read(int slot) throws IOException {
            long e = this.entries[slot];
            if ((e & BLOB_BIT) == 0) {
                return null;
            }
            int sector = (int) e;
            int length = (int) ((e >>> 32) & 0xFFFFFF);
            ByteBuffer buf = ByteBuffer.allocate(length);
            FileChannel ch = this.channel();
            long pos = (long) sector * SECTOR;
            while (buf.hasRemaining()) {
                if (ch.read(buf, pos + buf.position()) < 0) {
                    throw new IOException("Truncated region file " + this.path);
                }
            }
            return buf.array();
        }

        synchronized void write(int slot, long kind, byte[] blob) throws IOException {
            long old = this.entries[slot];
            long entry;
            FileChannel ch = this.channel();
            if (blob != null) {
                int sectors = (blob.length + SECTOR - 1) / SECTOR;
                int start = this.allocate(sectors);
                ch.write(ByteBuffer.wrap(blob), (long) start * SECTOR);
                entry = kind | ((long) blob.length << 32) | (start & 0xFFFFFFFFL);
            } else {
                entry = kind;
            }
            ByteBuffer e = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(0, entry);
            ch.write(e, FIXED_HEADER + (long) slot * 8);
            this.entries[slot] = entry;
            if ((old & BLOB_BIT) != 0) {
                int oldStart = (int) old;
                int oldSectors = (int) ((((old >>> 32) & 0xFFFFFF) + SECTOR - 1) / SECTOR);
                this.used.clear(oldStart, oldStart + oldSectors);
            }
        }

        private int allocate(int sectors) {
            int start = this.headerSectors;
            while (true) {
                start = this.used.nextClearBit(start);
                int end = this.used.nextSetBit(start);
                if (end < 0 || end - start >= sectors) {
                    this.used.set(start, start + sectors);
                    return start;
                }
                start = end;
            }
        }

        private synchronized void ensureLoaded() {
            if (this.entries != null) {
                return;
            }
            int slots = 16 * 16 * this.yCount;
            this.entries = new long[slots];
            this.used = new BitSet();
            if (Files.exists(this.path)) {
                try (FileChannel ch = FileChannel.open(this.path, StandardOpenOption.READ)) {
                    ByteBuffer head = ByteBuffer.allocate(FIXED_HEADER).order(ByteOrder.LITTLE_ENDIAN);
                    readFully(ch, head, 0);
                    if (head.getInt(0) == MAGIC && head.getInt(4) == VERSION) {
                        this.yCount = head.getInt(8);
                        slots = 16 * 16 * this.yCount;
                        this.entries = new long[slots];
                        ByteBuffer idx = ByteBuffer.allocate(slots * 8).order(ByteOrder.LITTLE_ENDIAN);
                        readFully(ch, idx, FIXED_HEADER);
                        idx.flip();
                        idx.asLongBuffer().get(this.entries);
                    } else {
                        this.rewriteHeader = true;
                    }
                } catch (IOException ignored) {
                    // Unreadable index: start over, the data will be regenerated.
                    this.entries = new long[slots];
                    this.rewriteHeader = true;
                }
            }
            this.headerSectors = (FIXED_HEADER + slots * 8 + SECTOR - 1) / SECTOR;
            this.used.set(0, this.headerSectors);
            for (long e : this.entries) {
                if ((e & BLOB_BIT) != 0) {
                    int start = (int) e;
                    int n = (int) ((((e >>> 32) & 0xFFFFFF) + SECTOR - 1) / SECTOR);
                    this.used.set(start, start + n);
                }
            }
        }

        private FileChannel channel() throws IOException {
            this.lastUse = System.nanoTime();
            if (this.channel == null) {
                boolean fresh = !Files.exists(this.path);
                if (fresh) {
                    Files.createDirectories(this.path.getParent());
                }
                this.channel = FileChannel.open(this.path, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                if (fresh || this.rewriteHeader || this.channel.size() < (long) this.headerSectors * SECTOR) {
                    this.rewriteHeader = false;
                    ByteBuffer head = ByteBuffer.allocate(this.headerSectors * SECTOR).order(ByteOrder.LITTLE_ENDIAN);
                    head.putInt(0, MAGIC).putInt(4, VERSION).putInt(8, this.yCount).putInt(12, 0);
                    for (int i = 0; i < this.entries.length; i++) {
                        head.putLong(FIXED_HEADER + i * 8, this.entries[i]);
                    }
                    this.channel.write(head, 0);
                }
            }
            return this.channel;
        }

        synchronized boolean hasChannel() {
            return this.channel != null;
        }

        synchronized boolean closeIfIdle(long now, boolean force) {
            if (this.channel != null && (force || now - this.lastUse > CHANNEL_IDLE_NANOS)) {
                this.closeChannel();
                return true;
            }
            return false;
        }

        synchronized void closeChannel() {
            if (this.channel != null) {
                try {
                    this.channel.close();
                } catch (IOException ignored) {
                }
                this.channel = null;
            }
        }

        private static void readFully(FileChannel ch, ByteBuffer buf, long pos) throws IOException {
            while (buf.hasRemaining()) {
                if (ch.read(buf, pos + buf.position()) < 0) {
                    throw new IOException("EOF");
                }
            }
        }
    }
}
