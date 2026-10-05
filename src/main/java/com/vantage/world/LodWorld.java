package com.vantage.world;

import com.vantage.Vantage;
import com.vantage.core.Lod;
import com.vantage.core.Mipper;
import com.vantage.core.SectionKey;
import com.vantage.core.VisualClass;
import com.vantage.core.Voxel;
import com.vantage.storage.RegionStore;
import com.vantage.storage.SectionCodec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * LOD data of one dimension: an in-memory section cache in front of a {@link RegionStore}, plus
 * the update path from voxelized chunks up through every level.
 */
public final class LodWorld implements AutoCloseable {
    public static final int ABSENT = 0;
    public static final int EMPTY = 1;
    public static final int PRESENT = 2;

    /** Receives every section change. {@code borderMask} has bit {@code f} set when face {@code f} was touched. */
    public interface Listener {
        void onSectionChanged(long key, int borderMask);
    }

    public final int minY;
    public final int height;
    public final boolean hasSkyLight;
    private final VisualClass.Table classes;
    private final RegionStore store;
    private final ConcurrentHashMap<Long, LodSection> cache = new ConcurrentHashMap<>();
    private final Set<Long> pendingMips = ConcurrentHashMap.newKeySet();
    private final ThreadLocal<Mipper> mippers;
    private final long memoryBudget;
    private final AtomicLong loads = new AtomicLong();
    private volatile Listener listener = (k, m) -> { };
    private final Thread saver;
    private volatile boolean running = true;

    public LodWorld(Path dir, int minY, int height, boolean hasSkyLight, VisualClass.Table classes, long memoryBudget) {
        this.minY = minY;
        this.height = height;
        this.hasSkyLight = hasSkyLight;
        this.classes = classes;
        this.memoryBudget = memoryBudget;
        this.store = new RegionStore(dir, height);
        this.mippers = ThreadLocal.withInitial(() -> new Mipper(classes));
        this.saver = new Thread(this::saverLoop, "Vantage saver");
        this.saver.setDaemon(true);
        this.saver.setPriority(Thread.NORM_PRIORITY - 1);
        this.saver.start();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int verticalSections(int level) {
        return Lod.verticalSections(level, this.height);
    }

    /** Number of 16-block chunk sections in this dimension. */
    public int chunkSections() {
        return (this.height + 15) >> 4;
    }

    /** Pins and returns a section, loading it from disk or creating it as unknown air. */
    public LodSection acquire(long key, boolean create) {
        LodSection s = this.cache.compute(key, (k, existing) -> {
            LodSection sec = existing != null ? existing : this.load(k, create);
            if (sec != null) {
                sec.pin();
            }
            return sec;
        });
        if (s != null) {
            s.lastAccess = System.nanoTime();
        }
        return s;
    }

    public void release(LodSection section) {
        section.unpin();
    }

    /** Copy of a section's voxels, or {@code null} if it does not exist. */
    public LodSection.Snapshot snapshot(long key) {
        LodSection s = this.acquire(key, false);
        if (s == null) {
            return null;
        }
        try {
            return s.snapshot();
        } finally {
            this.release(s);
        }
    }

    /** Copies a boundary layer of a section (see {@link LodSection#copyLayer}); false if absent. */
    public boolean copyLayer(long key, int axis, int layer, int[] dst) {
        LodSection s = this.acquire(key, false);
        if (s == null) {
            return false;
        }
        try {
            s.copyLayer(axis, layer, dst);
            return true;
        } finally {
            this.release(s);
        }
    }

    /** {@link #ABSENT}, {@link #EMPTY} (all air) or {@link #PRESENT}. Cheap; safe from any thread. */
    public int content(long key) {
        if (SectionKey.y(key) >= this.verticalSections(SectionKey.level(key))) {
            return ABSENT;
        }
        LodSection s = this.cache.get(key);
        if (s != null) {
            return s.isUniformAir() ? EMPTY : PRESENT;
        }
        long e = this.store.entry(key);
        if (!RegionStore.isPresent(e)) {
            return ABSENT;
        }
        if (RegionStore.isUniform(e) && Voxel.isAir(RegionStore.uniformValue(e))) {
            return EMPTY;
        }
        return PRESENT;
    }

    private LodSection load(long key, boolean create) {
        long e = this.store.entry(key);
        if (RegionStore.isPresent(e)) {
            this.loads.incrementAndGet();
            if (RegionStore.isUniform(e)) {
                return new LodSection(key, null, RegionStore.uniformValue(e));
            }
            try {
                byte[] blob = this.store.readBlob(key);
                if (blob != null) {
                    return new LodSection(key, SectionCodec.decode(blob, null), 0);
                }
            } catch (IOException | RuntimeException ex) {
                Vantage.LOGGER.warn("Dropping unreadable LOD section {}: {}", SectionKey.toString(key), ex.toString());
            }
        }
        if (!create) {
            return null;
        }
        LodSection fresh = new LodSection(key, null, Voxel.UNKNOWN_AIR);
        fresh.markDirty();
        return fresh;
    }

    /** Writes a voxelized column into levels 0..4 and schedules levels 5+. */
    public void insert(VoxelColumn column) {
        for (int cy = 0; cy < column.levels.length; cy++) {
            int[][] lv = column.levels[cy];
            if (lv == null) {
                continue;
            }
            for (int level = 0; level < Lod.CHUNK_LEVELS; level++) {
                int span = 1 << (level + 1);
                int size = 16 >> level;
                long key = SectionKey.of(level, column.chunkX >> (level + 1), cy >> (level + 1), column.chunkZ >> (level + 1));
                int ox = (column.chunkX & (span - 1)) * size;
                int oy = (cy & (span - 1)) * size;
                int oz = (column.chunkZ & (span - 1)) * size;
                LodSection s = this.acquire(key, true);
                boolean changed;
                try {
                    changed = s.writeCube(lv[level], 0, size, ox, oy, oz);
                } finally {
                    this.release(s);
                }
                if (!changed) {
                    break;
                }
                this.listener.onSectionChanged(key, borderMask(ox, oy, oz, size));
                if (level == Lod.CHUNK_LEVELS - 1) {
                    this.pendingMips.add(key);
                }
            }
        }
    }

    static int borderMask(int ox, int oy, int oz, int size) {
        int m = 0;
        if (oy == 0) m |= 1;
        if (oy + size == Lod.SIZE) m |= 2;
        if (oz == 0) m |= 4;
        if (oz + size == Lod.SIZE) m |= 8;
        if (ox == 0) m |= 16;
        if (ox + size == Lod.SIZE) m |= 32;
        return m;
    }

    /** Rebuilds the parents of sections that changed at the top chunk-derived level. */
    void processPendingMips() {
        for (int level = Lod.CHUNK_LEVELS - 1; level < Lod.MAX_LEVEL; level++) {
            List<Long> batch = new ArrayList<>();
            for (Long k : this.pendingMips) {
                if (SectionKey.level(k) == level) {
                    batch.add(k);
                }
            }
            if (batch.isEmpty()) {
                continue;
            }
            int[] child = new int[Lod.VOLUME];
            int[] half = new int[Lod.VOLUME / 8];
            Mipper mipper = this.mippers.get();
            for (long k : batch) {
                this.pendingMips.remove(k);
                LodSection src = this.acquire(k, false);
                if (src == null) {
                    continue;
                }
                try {
                    src.copyTo(child);
                } finally {
                    this.release(src);
                }
                mipper.downsample(child, Lod.SIZE, half, Lod.SIZE / 2, 0, 0, 0);
                long parent = SectionKey.parent(k);
                int ox = (SectionKey.x(k) & 1) * 16;
                int oy = (SectionKey.y(k) & 1) * 16;
                int oz = (SectionKey.z(k) & 1) * 16;
                LodSection dst = this.acquire(parent, true);
                boolean changed;
                try {
                    changed = dst.writeCube(half, 0, 16, ox, oy, oz);
                } finally {
                    this.release(dst);
                }
                if (changed) {
                    this.listener.onSectionChanged(parent, borderMask(ox, oy, oz, 16));
                    if (level + 1 < Lod.MAX_LEVEL) {
                        this.pendingMips.add(parent);
                    }
                }
            }
        }
    }

    /** Dirty sections not yet on disk; used to slow importers down when saving falls behind. */
    public int dirtyCount() {
        int n = 0;
        for (LodSection s : this.cache.values()) {
            if (s.isDirty()) {
                n++;
            }
        }
        return n;
    }

    public int cachedSections() {
        return this.cache.size();
    }

    public long cachedBytes() {
        long b = 0;
        for (LodSection s : this.cache.values()) {
            b += s.memoryBytes();
        }
        return b;
    }

    private void saverLoop() {
        while (this.running) {
            LockSupport.parkNanos(500_000_000L);
            try {
                this.processPendingMips();
                this.saveDirty();
                this.evict();
                this.store.sweep();
            } catch (Throwable t) {
                Vantage.LOGGER.error("Vantage saver failed", t);
            }
        }
    }

    private void saveDirty() {
        for (LodSection s : this.cache.values()) {
            LodSection.Snapshot snap = s.takeForSave();
            if (snap == null) {
                continue;
            }
            try {
                if (snap.data() == null) {
                    this.store.writeUniform(s.key, snap.uniform());
                } else {
                    Integer u = SectionCodec.uniformValue(snap.data());
                    if (u != null) {
                        this.store.writeUniform(s.key, u);
                    } else {
                        this.store.writeBlob(s.key, SectionCodec.encode(snap.data()));
                    }
                }
            } catch (IOException e) {
                Vantage.LOGGER.warn("Failed to save LOD section {}: {}", SectionKey.toString(s.key), e.toString());
                s.markDirty();
            }
        }
    }

    private void evict() {
        long bytes = this.cachedBytes();
        if (bytes <= this.memoryBudget) {
            return;
        }
        // Access times keep changing on other threads; sort on a snapshot of them.
        List<long[]> candidates = new ArrayList<>();
        List<LodSection> sections = new ArrayList<>();
        for (LodSection s : this.cache.values()) {
            if (s.evictable()) {
                candidates.add(new long[]{s.lastAccess, sections.size()});
                sections.add(s);
            }
        }
        candidates.sort(Comparator.comparingLong(c -> c[0]));
        long target = this.memoryBudget * 3 / 4;
        for (long[] c : candidates) {
            LodSection s = sections.get((int) c[1]);
            if (bytes <= target) {
                break;
            }
            long size = s.memoryBytes();
            boolean[] removed = {false};
            this.cache.computeIfPresent(s.key, (k, v) -> {
                if (v == s && v.evictable()) {
                    removed[0] = true;
                    return null;
                }
                return v;
            });
            if (removed[0]) {
                bytes -= size;
            }
        }
    }

    /** Saves everything and closes files. Blocks until done. */
    @Override
    public void close() {
        this.running = false;
        LockSupport.unpark(this.saver);
        try {
            this.saver.join(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            this.processPendingMips();
            this.saveDirty();
        } catch (Throwable t) {
            Vantage.LOGGER.error("Failed to flush LOD data", t);
        }
        this.store.close();
        this.cache.clear();
    }
}
