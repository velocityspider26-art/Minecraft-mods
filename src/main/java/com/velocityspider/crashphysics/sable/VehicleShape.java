package com.velocityspider.crashphysics.sable;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The blocks a vehicle is made of, in its plot, kept up to date block by block as they change.
 */
final class VehicleShape {

    static final int DOWN = 1 << Direction.DOWN.ordinal();
    static final int UP = 1 << Direction.UP.ordinal();
    static final int NORTH = 1 << Direction.NORTH.ordinal();
    static final int SOUTH = 1 << Direction.SOUTH.ordinal();
    static final int WEST = 1 << Direction.WEST.ordinal();
    static final int EAST = 1 << Direction.EAST.ordinal();

    private static final Direction[] DIRECTIONS = Direction.values();

    /**
     * Every block with a collision shape
     */
    final LongOpenHashSet solid = new LongOpenHashSet();
    /**
     * The blocks water can't get through: full cubes
     */
    final LongOpenHashSet tight = new LongOpenHashSet();
    /**
     * The blocks on the outside, each with the directions it is the outermost block of its row in (one bit per
     * {@link Direction}): the faces that meet whatever the vehicle runs into
     */
    final Long2ByteOpenHashMap outer = new Long2ByteOpenHashMap();
    boolean outerStale = true;
    /**
     * Goes up whenever a block changes, so anything worked out from the shape knows to work it out again
     */
    int version;
    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

    void add(final int x, final int y, final int z, final boolean watertight) {
        final long block = BlockPos.asLong(x, y, z);
        if (watertight ? this.tight.add(block) : this.tight.remove(block)) {
            this.version++;
        }
        if (!this.solid.add(block)) {
            return;
        }
        this.version++;
        this.minX = Math.min(this.minX, x);
        this.minY = Math.min(this.minY, y);
        this.minZ = Math.min(this.minZ, z);
        this.maxX = Math.max(this.maxX, x);
        this.maxY = Math.max(this.maxY, y);
        this.maxZ = Math.max(this.maxZ, z);
        this.outerStale = true;
    }

    void remove(final long block) {
        this.tight.remove(block);
        if (!this.solid.remove(block)) {
            return;
        }
        this.version++;
        if (this.outerStale) {
            return;
        }
        final byte flags = this.outer.remove(block);
        if (flags == 0) {
            return;
        }
        // In each row it was the outermost block of, the next one behind it now is
        for (final Direction direction : DIRECTIONS) {
            final int bit = 1 << direction.ordinal();
            if ((flags & bit) == 0) {
                continue;
            }
            long next = block;
            while (true) {
                next = BlockPos.offset(next, direction.getOpposite());
                if (!this.contains(next)) {
                    break;
                }
                if (this.solid.contains(next)) {
                    this.outer.put(next, (byte) (this.outer.get(next) | bit));
                    break;
                }
            }
        }
    }

    boolean contains(final long block) {
        final int x = BlockPos.getX(block);
        final int y = BlockPos.getY(block);
        final int z = BlockPos.getZ(block);
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }

    /**
     * Works out which blocks are the outermost of their rows along each axis.
     */
    void rebuildOuter() {
        this.outer.clear();
        final Long2IntOpenHashMap lowX = new Long2IntOpenHashMap(), highX = new Long2IntOpenHashMap();
        final Long2IntOpenHashMap lowY = new Long2IntOpenHashMap(), highY = new Long2IntOpenHashMap();
        final Long2IntOpenHashMap lowZ = new Long2IntOpenHashMap(), highZ = new Long2IntOpenHashMap();
        lowX.defaultReturnValue(Integer.MAX_VALUE);
        lowY.defaultReturnValue(Integer.MAX_VALUE);
        lowZ.defaultReturnValue(Integer.MAX_VALUE);
        highX.defaultReturnValue(Integer.MIN_VALUE);
        highY.defaultReturnValue(Integer.MIN_VALUE);
        highZ.defaultReturnValue(Integer.MIN_VALUE);

        for (final LongIterator iterator = this.solid.iterator(); iterator.hasNext(); ) {
            final long block = iterator.nextLong();
            final int x = BlockPos.getX(block);
            final int y = BlockPos.getY(block);
            final int z = BlockPos.getZ(block);
            final long rowX = BlockPos.asLong(0, y, z);
            final long rowY = BlockPos.asLong(x, 0, z);
            final long rowZ = BlockPos.asLong(x, y, 0);
            if (x < lowX.get(rowX)) lowX.put(rowX, x);
            if (x > highX.get(rowX)) highX.put(rowX, x);
            if (y < lowY.get(rowY)) lowY.put(rowY, y);
            if (y > highY.get(rowY)) highY.put(rowY, y);
            if (z < lowZ.get(rowZ)) lowZ.put(rowZ, z);
            if (z > highZ.get(rowZ)) highZ.put(rowZ, z);
        }

        for (final LongIterator iterator = this.solid.iterator(); iterator.hasNext(); ) {
            final long block = iterator.nextLong();
            final int x = BlockPos.getX(block);
            final int y = BlockPos.getY(block);
            final int z = BlockPos.getZ(block);
            final long rowX = BlockPos.asLong(0, y, z);
            final long rowY = BlockPos.asLong(x, 0, z);
            final long rowZ = BlockPos.asLong(x, y, 0);
            int flags = 0;
            if (x == lowX.get(rowX)) flags |= WEST;
            if (x == highX.get(rowX)) flags |= EAST;
            if (y == lowY.get(rowY)) flags |= DOWN;
            if (y == highY.get(rowY)) flags |= UP;
            if (z == lowZ.get(rowZ)) flags |= NORTH;
            if (z == highZ.get(rowZ)) flags |= SOUTH;
            if (flags != 0) {
                this.outer.put(block, (byte) flags);
            }
        }
        this.outerStale = false;
    }
}
