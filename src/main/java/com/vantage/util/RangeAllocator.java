package com.vantage.util;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Best-fit allocator over {@code [0, capacity)} with coalescing of freed neighbours.
 * Units are arbitrary (Vantage uses quads). Offsets and sizes must fit in 32 bits.
 * Not thread-safe.
 */
public final class RangeAllocator {
    private final TreeMap<Long, Long> byOffset = new TreeMap<>();
    private final TreeSet<Long> bySize = new TreeSet<>();
    private long capacity;
    private long used;

    public RangeAllocator(long capacity) {
        this.capacity = capacity;
        if (capacity > 0) {
            this.insert(0, capacity);
        }
    }

    public long capacity() {
        return this.capacity;
    }

    public long used() {
        return this.used;
    }

    /** Returns the offset of a free range of {@code size}, or -1 if none is large enough. */
    public long allocate(long size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size " + size);
        }
        Long k = this.bySize.ceiling(size << 32);
        if (k == null) {
            return -1;
        }
        long blockSize = k >>> 32;
        long offset = k & 0xFFFFFFFFL;
        this.remove(offset, blockSize);
        if (blockSize > size) {
            this.insert(offset + size, blockSize - size);
        }
        this.used += size;
        return offset;
    }

    public void free(long offset, long size) {
        this.used -= size;
        long start = offset;
        long end = offset + size;
        Map.Entry<Long, Long> prev = this.byOffset.floorEntry(start);
        if (prev != null && prev.getKey() + prev.getValue() == start) {
            this.remove(prev.getKey(), prev.getValue());
            start = prev.getKey();
        }
        Long nextSize = this.byOffset.get(end);
        if (nextSize != null) {
            this.remove(end, nextSize);
            end += nextSize;
        }
        this.insert(start, end - start);
    }

    /** Extends the managed range; the new space becomes free. */
    public void grow(long newCapacity) {
        if (newCapacity <= this.capacity) {
            return;
        }
        long old = this.capacity;
        this.capacity = newCapacity;
        this.used += newCapacity - old;
        this.free(old, newCapacity - old);
    }

    /** Largest single free range. */
    public long largestFree() {
        return this.bySize.isEmpty() ? 0 : this.bySize.last() >>> 32;
    }

    private void insert(long offset, long size) {
        this.byOffset.put(offset, size);
        this.bySize.add((size << 32) | offset);
    }

    private void remove(long offset, long size) {
        this.byOffset.remove(offset);
        this.bySize.remove((size << 32) | offset);
    }
}
