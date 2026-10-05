package com.vantage.gen;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.util.KeyDispatchDataCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * Finds the terrain surface of a noise-based world straight from its density function, without
 * generating chunks.
 *
 * <p>The router's final density is re-wired once per instance: holder indirections and markers
 * are removed, and functions the generator marks as two-dimensional are cached per column, so a
 * vertical search computes the expensive terrain-shape splines once. Interpolation is skipped (the
 * function is evaluated exactly), which moves the surface by at most a block or two.
 *
 * <p>Not thread-safe (the column caches are per instance); use one per worker.
 */
public final class NoiseSurfaceSampler {
    private final DensityFunction density;
    private final int minY;
    private final int maxY;
    private final Context ctx = new Context();
    private int evaluations;

    public NoiseSurfaceSampler(RandomState random, NoiseSettings noise) {
        this.minY = noise.minY();
        this.maxY = noise.minY() + noise.height();
        Map<DensityFunction, DensityFunction> memo = new HashMap<>();
        this.density = random.router().finalDensity().mapAll(f -> memo.computeIfAbsent(f, NoiseSurfaceSampler::rewire));
    }

    private static DensityFunction rewire(DensityFunction f) {
        if (f instanceof DensityFunctions.HolderHolder holder) {
            return holder.function().value();
        }
        if (f instanceof DensityFunctions.MarkerOrMarked marker) {
            String type = ((StringRepresentable) marker.type()).getSerializedName();
            if (type.equals("flat_cache")) {
                return new ColumnCache(marker.wrapped(), true);
            }
            if (type.equals("cache_2d")) {
                return new ColumnCache(marker.wrapped(), false);
            }
            return marker.wrapped();
        }
        return f;
    }

    /** Lowest block y of the generator's range. */
    public int minY() {
        return this.minY;
    }

    /** Density evaluations so far (for statistics). */
    public int evaluations() {
        return this.evaluations;
    }

    public boolean solid(int x, int y, int z) {
        this.evaluations++;
        return this.density.compute(this.ctx.set(x, y, z)) > 0.0;
    }

    /**
     * Height of the column's surface: one above its highest solid block, or {@link #minY()} if the
     * column is empty. Searches down from the top in steps of {@code step} blocks, then narrows
     * the hit down until it is known which {@code cell}-block slot (aligned to {@link #minY()})
     * holds the top block; the result is exact to that slot.
     */
    public int surfaceY(int x, int z, int step, int cell) {
        return this.surfaceY(x, z, step, cell, Integer.MIN_VALUE);
    }

    /**
     * As {@link #surfaceY(int, int, int, int)}, starting the search {@code margin} blocks above
     * {@code hint} (a nearby column's surface) instead of at the top of the world. If that start
     * point is already solid the full search runs, so only a solid overhang floating more than
     * {@code margin} blocks above open air can be missed.
     */
    public int surfaceY(int x, int z, int step, int cell, int hint, int margin) {
        if (hint == Integer.MIN_VALUE) {
            return this.surfaceY(x, z, step, cell, Integer.MIN_VALUE);
        }
        int start = Math.min(this.maxY - 1, Math.max(this.minY, hint + margin));
        if (start >= this.maxY - 1 || this.solid(x, start, z)) {
            return this.surfaceY(x, z, step, cell, Integer.MIN_VALUE);
        }
        return this.surfaceY(x, z, step, cell, start);
    }

    /** Searches down from {@code from}, which must be known not solid, or from the top if unset. */
    private int surfaceY(int x, int z, int step, int cell, int from) {
        int top = this.maxY - 1;
        if (from == Integer.MIN_VALUE) {
            if (this.solid(x, top, z)) {
                return this.maxY;
            }
            from = top;
        }
        int hi = from; // known not solid
        if (hi == this.minY) {
            return this.minY;
        }
        for (int y = hi - step; ; y -= step) {
            if (y < this.minY) {
                y = this.minY;
            }
            if (this.solid(x, y, z)) {
                int lo = y; // known solid
                while (hi - lo > 1 && Math.floorDiv(lo - this.minY, cell) != Math.floorDiv(hi - 1 - this.minY, cell)) {
                    int mid = (lo + hi) >>> 1;
                    if (this.solid(x, mid, z)) {
                        lo = mid;
                    } else {
                        hi = mid;
                    }
                }
                return lo + 1;
            }
            if (y == this.minY) {
                return this.minY;
            }
            hi = y;
        }
    }

    private static final class Context implements DensityFunction.FunctionContext {
        int x, y, z;

        Context set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override
        public int blockX() {
            return this.x;
        }

        @Override
        public int blockY() {
            return this.y;
        }

        @Override
        public int blockZ() {
            return this.z;
        }
    }

    /**
     * Remembers the value for the last column asked; for functions that ignore y. Flat caches
     * behave like the generator's: sampled at y = 0 on a 4-block grid.
     */
    private static final class ColumnCache implements DensityFunction.SimpleFunction {
        private final DensityFunction wrapped;
        private final boolean flat;
        private int lastX = Integer.MIN_VALUE;
        private int lastZ = Integer.MIN_VALUE;
        private double value;

        ColumnCache(DensityFunction wrapped, boolean flat) {
            this.wrapped = wrapped;
            this.flat = flat;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            int x = context.blockX();
            int z = context.blockZ();
            if (this.flat) {
                x &= ~3;
                z &= ~3;
            }
            if (x != this.lastX || z != this.lastZ) {
                this.value = this.wrapped.compute(this.flat ? new DensityFunction.SinglePointContext(x, 0, z) : context);
                this.lastX = x;
                this.lastZ = z;
            }
            return this.value;
        }

        @Override
        public double minValue() {
            return this.wrapped.minValue();
        }

        @Override
        public double maxValue() {
            return this.wrapped.maxValue();
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException("not serialisable");
        }
    }
}
