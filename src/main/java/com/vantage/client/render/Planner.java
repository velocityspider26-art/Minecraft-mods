package com.vantage.client.render;

import com.vantage.Vantage;
import com.vantage.core.Lod;
import com.vantage.core.SectionKey;
import com.vantage.world.LodWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * Chooses which LOD section to draw at each place, on its own thread.
 *
 * <p>Starting from the coarsest level it walks down the octree, refining a section while one of
 * its voxels would cover more than the configured number of pixels at its distance. A section is
 * only replaced by its children once all of them have meshes, so refinement never opens holes.
 * Sections that vanilla fully covers are skipped; ones it partly covers are flagged so the
 * renderer discards fragments inside vanilla's area.
 *
 * <p>The plan depends on camera position only (never rotation), so it stays valid while looking
 * around; frustum culling happens per frame on the render thread.
 */
public final class Planner implements AutoCloseable {
    /** Result of one planning pass. Entries are sorted front to back. */
    public static final class Plan {
        public static final Plan EMPTY = new Plan(0, new MeshManager.Entry[0], new boolean[0], 0, 0);

        public final long id;
        public final MeshManager.Entry[] entries;
        public final boolean[] masked;
        public final int visited;
        public final long nanos;

        Plan(long id, MeshManager.Entry[] entries, boolean[] masked, int visited, long nanos) {
            this.id = id;
            this.entries = entries;
            this.masked = masked;
            this.visited = visited;
            this.nanos = nanos;
        }
    }

    /** Planner inputs, published by the render thread each frame. */
    public record View(double x, double y, double z, double renderDistance, double radiansPerPixel, double pixelsPerVoxel,
                       CoverageMap.Snapshot coverage) {
    }

    private final LodWorld world;
    private final MeshManager meshes;
    private final Thread thread;
    private volatile boolean running = true;
    private volatile View view;
    private volatile Plan plan = Plan.EMPTY;
    private long nextId = 1;

    // per pass
    private final List<MeshManager.Entry> drawn = new ArrayList<>();
    private final List<Boolean> drawnMasked = new ArrayList<>();
    private final List<Double> drawnDist = new ArrayList<>();
    private int visited;
    private View v;
    private long planId;

    public Planner(LodWorld world, MeshManager meshes) {
        this.world = world;
        this.meshes = meshes;
        this.thread = new Thread(this::loop, "Vantage planner");
        this.thread.setDaemon(true);
        this.thread.setPriority(Thread.NORM_PRIORITY - 1);
        this.thread.start();
    }

    public Plan plan() {
        return this.plan;
    }

    public void setView(View view) {
        this.view = view;
    }

    private void loop() {
        View last = null;
        long lastRun = 0;
        while (this.running) {
            LockSupport.parkNanos(30_000_000L);
            View now = this.view;
            if (now == null) {
                continue;
            }
            boolean meshesChanged = this.meshes.consumeChanged();
            long t = System.nanoTime();
            boolean due = last == null
                    || moved(last, now) > 4.0
                    || last.coverage().version() != now.coverage().version()
                    || last.renderDistance() != now.renderDistance()
                    || last.pixelsPerVoxel() != now.pixelsPerVoxel()
                    || Math.abs(last.radiansPerPixel() - now.radiansPerPixel()) > 1e-6
                    || meshesChanged
                    || t - lastRun > 1_000_000_000L;
            if (!due) {
                continue;
            }
            try {
                this.run(now);
            } catch (Throwable e) {
                Vantage.LOGGER.error("Vantage planner failed", e);
            }
            last = now;
            lastRun = t;
        }
    }

    private static double moved(View a, View b) {
        double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void run(View view) {
        long start = System.nanoTime();
        this.v = view;
        this.planId = this.nextId++;
        this.drawn.clear();
        this.drawnMasked.clear();
        this.drawnDist.clear();
        this.visited = 0;

        int top = Lod.MAX_LEVEL;
        int span = Lod.sectionBlocks(top);
        double r = view.renderDistance();
        int x0 = (int) Math.floor((view.x() - r) / span);
        int x1 = (int) Math.floor((view.x() + r) / span);
        int z0 = (int) Math.floor((view.z() - r) / span);
        int z1 = (int) Math.floor((view.z() + r) / span);
        int ys = this.world.verticalSections(top);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = 0; y < ys; y++) {
                    this.visit(SectionKey.of(top, x, y, z));
                }
            }
        }

        int n = this.drawn.size();
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(this.drawnDist.get(a), this.drawnDist.get(b)));
        MeshManager.Entry[] entries = new MeshManager.Entry[n];
        boolean[] masked = new boolean[n];
        for (int i = 0; i < n; i++) {
            entries[i] = this.drawn.get(order[i]);
            masked[i] = this.drawnMasked.get(order[i]);
        }
        this.plan = new Plan(this.planId, entries, masked, this.visited, System.nanoTime() - start);
    }

    /** Distance at which voxels of {@code level} first look small enough. */
    private double refineDistance(int level) {
        return Lod.voxelBlocks(level) / (this.v.pixelsPerVoxel() * this.v.radiansPerPixel());
    }

    private void visit(long key) {
        this.visited++;
        int level = SectionKey.level(key);
        int span = Lod.sectionBlocks(level);
        double bx0 = (double) SectionKey.x(key) * span;
        double bz0 = (double) SectionKey.z(key) * span;
        double by0 = this.world.minY + (double) SectionKey.y(key) * span;
        double dxz = axisDistance(this.v.x(), bx0, bx0 + span);
        double dz = axisDistance(this.v.z(), bz0, bz0 + span);
        double horizontal = Math.sqrt(dxz * dxz + dz * dz);
        if (horizontal > this.v.renderDistance()) {
            return;
        }
        if (this.world.content(key) != LodWorld.PRESENT) {
            return;
        }
        CoverageMap.Snapshot cov = this.v.coverage();
        int cx0 = SectionKey.x(key) * (span >> 4);
        int cz0 = SectionKey.z(key) * (span >> 4);
        int cx1 = cx0 + (span >> 4) - 1;
        int cz1 = cz0 + (span >> 4) - 1;
        if (cov.allCovered(cx0, cz0, cx1, cz1)) {
            return;
        }
        double dy = axisDistance(this.v.y(), by0, by0 + span);
        double dist = Math.sqrt(horizontal * horizontal + dy * dy);

        MeshManager.Entry self = this.meshes.entry(key);
        self.lastPlanned = this.planId;

        if (level > 0 && dist < this.refineDistance(level)) {
            long[] kids = new long[8];
            int count = 0;
            boolean allReady = true;
            int ys = this.world.verticalSections(level - 1);
            for (int i = 0; i < 8; i++) {
                long c = SectionKey.child(key, i);
                if (SectionKey.y(c) >= ys || !this.childWorthVisiting(c)) {
                    continue;
                }
                kids[count++] = c;
                MeshManager.Entry ce = this.meshes.entry(c);
                ce.lastPlanned = this.planId;
                if (!ce.ready()) {
                    allReady = false;
                    this.meshes.request(ce, dist);
                }
            }
            if (allReady || !self.ready()) {
                for (int i = 0; i < count; i++) {
                    if (allReady || this.meshes.entry(kids[i]).ready()) {
                        this.visit(kids[i]);
                    }
                }
                if (allReady) {
                    return;
                }
            }
        }

        if (self.ready()) {
            if (self.drawable()) {
                this.drawn.add(self);
                this.drawnMasked.add(cov.anyCovered(cx0, cz0, cx1, cz1));
                this.drawnDist.add(dist);
            }
            if (self.stale()) {
                this.meshes.request(self, dist);
            }
        } else {
            this.meshes.request(self, dist);
        }
    }

    private boolean childWorthVisiting(long key) {
        int level = SectionKey.level(key);
        int span = Lod.sectionBlocks(level);
        double bx0 = (double) SectionKey.x(key) * span;
        double bz0 = (double) SectionKey.z(key) * span;
        double dx = axisDistance(this.v.x(), bx0, bx0 + span);
        double dz = axisDistance(this.v.z(), bz0, bz0 + span);
        if (Math.sqrt(dx * dx + dz * dz) > this.v.renderDistance()) {
            return false;
        }
        if (this.world.content(key) != LodWorld.PRESENT) {
            return false;
        }
        int cx0 = SectionKey.x(key) * (span >> 4);
        int cz0 = SectionKey.z(key) * (span >> 4);
        return !this.v.coverage().allCovered(cx0, cz0, cx0 + (span >> 4) - 1, cz0 + (span >> 4) - 1);
    }

    private static double axisDistance(double p, double min, double max) {
        return p < min ? min - p : p > max ? p - max : 0;
    }

    @Override
    public void close() {
        this.running = false;
        LockSupport.unpark(this.thread);
        try {
            this.thread.join(2_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
