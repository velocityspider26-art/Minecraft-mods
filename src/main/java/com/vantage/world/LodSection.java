package com.vantage.world;

import com.vantage.core.Lod;

import java.util.Arrays;

/**
 * Voxel data of one LOD section held in memory. Uniform sections keep no array.
 *
 * <p>All access is synchronised on the instance; sections are small enough that contention is
 * rare (a level-4 section covers 32×32 chunks, so two ingest workers can meet there).
 */
public final class LodSection {
    public final long key;
    private int[] data;
    private int uniform;
    private boolean dirty;
    private int pins;
    volatile long lastAccess;

    LodSection(long key, int[] data, int uniform) {
        this.key = key;
        this.data = data;
        this.uniform = uniform;
    }

    /** Immutable copy of a section's voxels; {@code data} is null when uniform. */
    public record Snapshot(int[] data, int uniform) {
        public int get(int index) {
            return this.data == null ? this.uniform : this.data[index];
        }
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(this.data == null ? null : this.data.clone(), this.uniform);
    }

    public synchronized int get(int index) {
        return this.data == null ? this.uniform : this.data[index];
    }

    /**
     * Writes a {@code size}³ cube (y-major) at the given voxel offset.
     *
     * @return {@code true} if any voxel changed
     */
    public synchronized boolean writeCube(int[] src, int srcOffset, int size, int ox, int oy, int oz) {
        boolean changed = false;
        if (this.data == null) {
            for (int i = 0; i < size * size * size; i++) {
                if (src[srcOffset + i] != this.uniform) {
                    changed = true;
                    break;
                }
            }
            if (!changed) {
                return false;
            }
            this.data = new int[Lod.VOLUME];
            Arrays.fill(this.data, this.uniform);
        }
        int[] d = this.data;
        int i = srcOffset;
        for (int y = 0; y < size; y++) {
            for (int z = 0; z < size; z++) {
                int base = Lod.index(ox, oy + y, oz + z);
                for (int x = 0; x < size; x++, i++) {
                    int v = src[i];
                    if (d[base + x] != v) {
                        d[base + x] = v;
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            this.dirty = true;
        }
        return changed;
    }

    /**
     * Copies one boundary layer. {@code axis} 0 = x, 1 = y, 2 = z; the destination is indexed
     * {@code z*32+x} for y layers, {@code y*32+x} for z layers and {@code y*32+z} for x layers.
     */
    public synchronized void copyLayer(int axis, int layer, int[] dst) {
        if (this.data == null) {
            Arrays.fill(dst, 0, Lod.AREA, this.uniform);
            return;
        }
        int[] d = this.data;
        for (int a = 0; a < Lod.SIZE; a++) {
            for (int b = 0; b < Lod.SIZE; b++) {
                int idx = switch (axis) {
                    case 1 -> Lod.index(b, layer, a);
                    case 2 -> Lod.index(b, a, layer);
                    default -> Lod.index(layer, a, b);
                };
                dst[a * Lod.SIZE + b] = d[idx];
            }
        }
    }

    /** Copies the voxels of this section into a 32³ array. */
    public synchronized void copyTo(int[] dst) {
        if (this.data == null) {
            Arrays.fill(dst, this.uniform);
        } else {
            System.arraycopy(this.data, 0, dst, 0, Lod.VOLUME);
        }
    }

    /**
     * Takes the dirty flag together with a snapshot to save. If the section turned uniform the
     * array is dropped here, which keeps memory down for sky and deep-underground sections.
     */
    synchronized Snapshot takeForSave() {
        if (!this.dirty) {
            return null;
        }
        this.dirty = false;
        if (this.data != null) {
            int first = this.data[0];
            boolean uniform = true;
            for (int i = 1; i < Lod.VOLUME; i++) {
                if (this.data[i] != first) {
                    uniform = false;
                    break;
                }
            }
            if (uniform) {
                this.data = null;
                this.uniform = first;
            }
        }
        return new Snapshot(this.data == null ? null : this.data.clone(), this.uniform);
    }

    synchronized void markDirty() {
        this.dirty = true;
    }

    synchronized boolean isDirty() {
        return this.dirty;
    }

    synchronized void pin() {
        this.pins++;
    }

    synchronized void unpin() {
        this.pins--;
    }

    synchronized boolean evictable() {
        return this.pins == 0 && !this.dirty;
    }

    synchronized boolean isUniformAir() {
        return this.data == null && (this.uniform & 0xFFFFF) == 0;
    }

    synchronized long memoryBytes() {
        return this.data == null ? 48 : 48 + 4L * Lod.VOLUME;
    }
}
