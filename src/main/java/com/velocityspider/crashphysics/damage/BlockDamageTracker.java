package com.velocityspider.crashphysics.damage;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Iterator;

/**
 * Remembers how far each block has been crushed by impacts that didn't break it yet, so damage builds up over
 * several hits (and several physics sub-steps of one long impact). Damaged terrain shows the block-breaking cracks.
 * <p>
 * Keys are block positions packed with {@link BlockPos#asLong}; vehicle blocks live in Sable's plot area, so their
 * keys never collide with terrain.
 */
public final class BlockDamageTracker {

    private final ServerLevel level;
    private final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap<>();
    private int nextCrackId = -0x10000000;

    private static final class Entry {
        double crushed;
        long lastHit;
        final int crackId;
        int crackStage = -1;
        final boolean showCracks;

        Entry(final int crackId, final boolean showCracks) {
            this.crackId = crackId;
            this.showCracks = showCracks;
        }
    }

    public BlockDamageTracker(final ServerLevel level) {
        this.level = level;
    }

    /**
     * Adds crushed depth to a block.
     *
     * @param key        packed block position
     * @param depth      newly crushed depth [m]
     * @param capacity   crushed depth at which the block breaks [m]
     * @param showCracks if the block-breaking overlay should be shown (terrain only; vehicle plots have no viewers)
     * @param tick       current tick
     * @return true if the block should break now
     */
    public boolean addErosion(final long key, final double depth, final double capacity, final boolean showCracks, final long tick) {
        Entry entry = this.entries.get(key);
        if (entry == null) {
            entry = new Entry(this.nextCrackId++, showCracks);
            this.entries.put(key, entry);
        }

        entry.crushed += depth;
        entry.lastHit = tick;

        if (entry.crushed >= capacity) {
            this.entries.remove(key);
            this.clearCracks(key, entry);
            return true;
        }

        if (entry.showCracks) {
            final int stage = Math.min(9, (int) (entry.crushed / capacity * 10.0));
            if (stage != entry.crackStage) {
                entry.crackStage = stage;
                this.level.destroyBlockProgress(entry.crackId, BlockPos.of(key), stage);
            }
        }
        return false;
    }

    /**
     * Forgets the damage of a block, e.g. because it was destroyed by something else.
     */
    public void forget(final long key) {
        final Entry entry = this.entries.remove(key);
        if (entry != null) {
            this.clearCracks(key, entry);
        }
    }

    /**
     * @return how far a block has been crushed so far [m]
     */
    public double crushed(final long key) {
        final Entry entry = this.entries.get(key);
        return entry != null ? entry.crushed : 0.0;
    }

    /**
     * Forgets damage that hasn't been added to for a while.
     */
    public void expire(final long tick, final long memoryTicks) {
        if (this.entries.isEmpty() || tick % 20 != 0) {
            return;
        }

        final Iterator<Long2ObjectMap.Entry<Entry>> iterator = this.entries.long2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            final Long2ObjectMap.Entry<Entry> entry = iterator.next();
            if (tick - entry.getValue().lastHit > memoryTicks) {
                this.clearCracks(entry.getLongKey(), entry.getValue());
                iterator.remove();
            }
        }
    }

    private void clearCracks(final long key, final Entry entry) {
        if (entry.showCracks && entry.crackStage >= 0) {
            this.level.destroyBlockProgress(entry.crackId, BlockPos.of(key), -1);
        }
    }
}
