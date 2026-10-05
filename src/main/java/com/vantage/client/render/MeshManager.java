package com.vantage.client.render;

import com.vantage.core.Lod;
import com.vantage.core.SectionKey;
import com.vantage.core.VisualClass;
import com.vantage.mesh.MeshData;
import com.vantage.mesh.Mesher;
import com.vantage.util.WorkerPool;
import com.vantage.world.LodSection;
import com.vantage.world.LodWorld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the GPU mesh of every LOD section that has been asked for.
 *
 * <p>Generations make the threads agree without locks on the hot path: a data change bumps
 * {@code wantedGen}; a build captures the generation it started from; the render thread only
 * installs a result newer than what is resident. A stale mesh stays drawable until its replacement
 * arrives, so updates never open holes.
 */
public final class MeshManager implements LodWorld.Listener {
    /** Per-section mesh state. Fields marked render-thread are only touched there. */
    public static final class Entry {
        public final long key;
        private int wantedGen;
        private int queuedGen = -1;
        volatile int builtGen = -1;
        volatile boolean ready;
        volatile boolean empty;
        volatile long lastPlanned;
        // render thread
        long offset = -1;
        int quads;
        final int[] counts = new int[Mesher.GROUPS];
        int minX, minY, minZ, maxX, maxY, maxZ;

        Entry(long key) {
            this.key = key;
        }

        public boolean ready() {
            return this.ready;
        }

        public boolean drawable() {
            return this.ready && !this.empty;
        }

        public synchronized boolean stale() {
            return this.builtGen != this.wantedGen;
        }
    }

    private record Result(Entry entry, int gen, MeshData mesh) {
    }

    private static final int MAX_QUEUED = 512;

    private final LodWorld world;
    private final WorkerPool pool;
    private final ThreadLocal<Mesher> meshers;
    private final ThreadLocal<int[][]> slices = ThreadLocal.withInitial(() -> new int[6][Lod.AREA]);
    private final boolean skyCull;
    private final ConcurrentHashMap<Long, Entry> entries = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Result> results = new ConcurrentLinkedQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger();
    private volatile boolean changed;
    private long residentQuads;
    private int residentMeshes;

    public MeshManager(LodWorld world, WorkerPool pool, VisualClass.Table classes, boolean skyCull) {
        this.world = world;
        this.pool = pool;
        this.skyCull = skyCull;
        this.meshers = ThreadLocal.withInitial(() -> new Mesher(classes));
    }

    public Entry entry(long key) {
        return this.entries.computeIfAbsent(key, Entry::new);
    }

    public Entry peek(long key) {
        return this.entries.get(key);
    }

    /** True once since the last call if any mesh became ready or changed. */
    public boolean consumeChanged() {
        boolean c = this.changed;
        this.changed = false;
        return c;
    }

    @Override
    public void onSectionChanged(long key, int borderMask) {
        this.bump(key);
        for (int face = 0; face < 6; face++) {
            if ((borderMask & (1 << face)) != 0) {
                long n = neighbour(key, face);
                if (n != Long.MIN_VALUE) {
                    this.bump(n);
                }
            }
        }
        this.changed = true;
    }

    private void bump(long key) {
        Entry e = this.entries.get(key);
        if (e != null) {
            synchronized (e) {
                e.wantedGen++;
            }
        }
    }

    /** Neighbour across a face, or {@link Long#MIN_VALUE} below the world floor. */
    static long neighbour(long key, int face) {
        int dx = face == 4 ? -1 : face == 5 ? 1 : 0;
        int dy = face == 0 ? -1 : face == 1 ? 1 : 0;
        int dz = face == 2 ? -1 : face == 3 ? 1 : 0;
        if (SectionKey.y(key) + dy < 0) {
            return Long.MIN_VALUE;
        }
        return SectionKey.offset(key, dx, dy, dz);
    }

    /** Queues a build if the entry has no up-to-date mesh and none is in flight. */
    public void request(Entry e, double priority) {
        int gen;
        synchronized (e) {
            if (e.builtGen == e.wantedGen || e.queuedGen == e.wantedGen) {
                return;
            }
            if (this.inFlight.get() >= MAX_QUEUED) {
                return;
            }
            gen = e.wantedGen;
            e.queuedGen = gen;
        }
        this.inFlight.incrementAndGet();
        this.pool.submit(priority, () -> {
            try {
                this.build(e, gen);
            } finally {
                this.inFlight.decrementAndGet();
            }
        });
    }

    private void build(Entry e, int gen) {
        LodSection.Snapshot self = this.world.snapshot(e.key);
        MeshData mesh;
        if (self == null) {
            mesh = MeshData.EMPTY;
        } else {
            int[][] scratch = this.slices.get();
            int[][] neighbours = new int[6][];
            int height = this.world.verticalSections(SectionKey.level(e.key));
            for (int face = 0; face < 6; face++) {
                long n = neighbour(e.key, face);
                if (n == Long.MIN_VALUE) {
                    neighbours[face] = Mesher.SOLID_SLICE;
                    continue;
                }
                if (SectionKey.y(n) >= height) {
                    continue;
                }
                int axis = Mesher.NORMAL_AXIS[face];
                int layer = (face & 1) == 1 ? 0 : Lod.SIZE - 1;
                if (this.world.copyLayer(n, axis, layer, scratch[face])) {
                    neighbours[face] = scratch[face];
                }
            }
            mesh = this.meshers.get().build(self.data(), self.uniform(), neighbours, this.skyCull);
        }
        this.results.add(new Result(e, gen, mesh));
    }

    /**
     * Installs finished meshes on the GPU. Render thread only.
     *
     * @param budgetBytes stop after uploading about this much
     * @param planId      id of the plan being drawn; sections in it are never evicted
     */
    void processResults(GeometryArena arena, long budgetBytes, long planId) {
        long uploaded = 0;
        Result r;
        while (uploaded < budgetBytes && (r = this.results.poll()) != null) {
            Entry e = r.entry;
            if (r.gen <= e.builtGen) {
                continue;
            }
            MeshData m = r.mesh;
            if (m.isEmpty()) {
                this.release(arena, e);
                e.empty = true;
            } else {
                int quads = m.quadCount();
                long off = arena.allocate(quads);
                if (off < 0) {
                    this.evict(arena, quads, planId);
                    off = arena.allocate(quads);
                }
                if (off < 0) {
                    synchronized (e) {
                        e.queuedGen = -1;
                    }
                    continue;
                }
                arena.upload(off, m.quads);
                uploaded += (long) quads * GeometryArena.QUAD_BYTES;
                this.release(arena, e);
                e.offset = off;
                e.quads = quads;
                System.arraycopy(m.counts, 0, e.counts, 0, Mesher.GROUPS);
                e.minX = m.minX;
                e.minY = m.minY;
                e.minZ = m.minZ;
                e.maxX = m.maxX;
                e.maxY = m.maxY;
                e.maxZ = m.maxZ;
                e.empty = false;
                this.residentQuads += quads;
                this.residentMeshes++;
            }
            e.builtGen = r.gen;
            e.ready = true;
            this.changed = true;
        }
    }

    private void release(GeometryArena arena, Entry e) {
        if (e.offset >= 0) {
            arena.free(e.offset, e.quads);
            this.residentQuads -= e.quads;
            this.residentMeshes--;
            e.offset = -1;
            e.quads = 0;
        }
    }

    /** Frees meshes that the current plan does not use, least recently planned first. */
    private void evict(GeometryArena arena, int needQuads, long planId) {
        List<Entry> candidates = new ArrayList<>();
        for (Entry e : this.entries.values()) {
            if (e.offset >= 0 && e.lastPlanned < planId) {
                candidates.add(e);
            }
        }
        candidates.sort(Comparator.comparingLong(e -> e.lastPlanned));
        long freed = 0;
        for (Entry e : candidates) {
            if (freed >= needQuads * 4L) {
                break;
            }
            freed += e.quads;
            this.release(arena, e);
            synchronized (e) {
                e.ready = false;
                e.builtGen = -1;
                e.queuedGen = -1;
            }
        }
    }

    /** Drops every mesh; used when GPU resources are recreated. Render thread only. */
    void releaseAll(GeometryArena arena) {
        for (Entry e : this.entries.values()) {
            if (arena != null) {
                this.release(arena, e);
            }
            synchronized (e) {
                e.ready = false;
                e.builtGen = -1;
                e.queuedGen = -1;
            }
        }
        this.results.clear();
        this.residentQuads = 0;
        this.residentMeshes = 0;
    }

    public int inFlight() {
        return this.inFlight.get();
    }

    public int pendingUploads() {
        return this.results.size();
    }

    public long residentQuads() {
        return this.residentQuads;
    }

    public int residentMeshes() {
        return this.residentMeshes;
    }
}
