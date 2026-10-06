package com.vantage.world;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainerRO;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a block-state container into a palette plus one index per voxel ({@code y<<8|z<<4|x}).
 * Note: {@code PalettedContainer.getAll} yields each distinct state once, not every voxel, so
 * the voxels are read one by one. Reusable, not thread-safe.
 */
public final class PaletteDecoder {
    private final Reference2IntOpenHashMap<BlockState> lookup = new Reference2IntOpenHashMap<>();
    private final List<BlockState> palette = new ArrayList<>();
    private short[] indices;

    public PaletteDecoder() {
        this.lookup.defaultReturnValue(-1);
    }

    public void decode(PalettedContainerRO<BlockState> states) {
        this.lookup.clear();
        this.palette.clear();
        short[] out = new short[4096];
        BlockState last = null;
        int lastIndex = 0;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = states.get(x, y, z);
                    int p;
                    if (state == last) {
                        p = lastIndex;
                    } else {
                        p = this.lookup.getInt(state);
                        if (p < 0) {
                            p = this.palette.size();
                            this.palette.add(state);
                            this.lookup.put(state, p);
                        }
                        last = state;
                        lastIndex = p;
                    }
                    out[(y << 8) | (z << 4) | x] = (short) p;
                }
            }
        }
        this.indices = this.palette.size() == 1 ? null : out;
    }

    public BlockState[] palette() {
        return this.palette.toArray(new BlockState[0]);
    }

    public short[] indices() {
        return this.indices;
    }
}
