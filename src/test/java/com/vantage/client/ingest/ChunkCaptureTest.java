package com.vantage.client.ingest;

import com.vantage.world.PaletteDecoder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Runs inside NeoForge's test environment, so real block states and containers are available. */
class ChunkCaptureTest {
    private static PalettedContainer<BlockState> container() {
        return new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
    }

    @Test
    void decodesEveryVoxel() {
        BlockState[] kinds = {Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState(),
                Blocks.OAK_LEAVES.defaultBlockState(), Blocks.WATER.defaultBlockState(), Blocks.AIR.defaultBlockState()};
        Random r = new Random(9);
        PalettedContainer<BlockState> c = container();
        BlockState[] expected = new BlockState[4096];
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState s = kinds[r.nextInt(kinds.length)];
                    c.set(x, y, z, s);
                    expected[(y << 8) | (z << 4) | x] = s;
                }
            }
        }
        PaletteDecoder d = new PaletteDecoder();
        d.decode(c.copy());
        BlockState[] palette = d.palette();
        short[] idx = d.indices();
        BlockState[] actual = new BlockState[4096];
        for (int i = 0; i < 4096; i++) {
            actual[i] = palette[idx[i]];
        }
        assertArrayEquals(expected, actual);
    }

    @Test
    void singleStateHasNoIndices() {
        PalettedContainer<BlockState> c = container();
        for (int i = 0; i < 4096; i++) {
            c.set(i & 15, i >> 8, (i >> 4) & 15, Blocks.STONE.defaultBlockState());
        }
        PaletteDecoder d = new PaletteDecoder();
        d.decode(c);
        assertEquals(1, d.palette().length);
        assertSame(Blocks.STONE.defaultBlockState(), d.palette()[0]);
        assertNull(d.indices());
    }
}
