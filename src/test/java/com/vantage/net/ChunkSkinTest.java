package com.vantage.net;

import com.mojang.serialization.Lifecycle;
import com.vantage.world.ColumnSource;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Skins keep every block that can be seen from outside the ground, and nothing else matters. */
class ChunkSkinTest {
    private static final int MIN_Y = -64;
    private static final int SECTIONS = 24;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();

    private static Registry<Biome> biomes;
    private static Holder<Biome> plains;
    private static Holder<Biome> forest;

    @BeforeAll
    static void setUp() {
        HolderLookup.Provider lookup = VanillaRegistries.createLookup();
        HolderLookup.RegistryLookup<Biome> vanilla = lookup.lookupOrThrow(Registries.BIOME);
        MappedRegistry<Biome> registry = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        plains = Registry.registerForHolder(registry, Biomes.PLAINS, vanilla.getOrThrow(Biomes.PLAINS).value());
        forest = Registry.registerForHolder(registry, Biomes.FOREST, vanilla.getOrThrow(Biomes.FOREST).value());
        biomes = registry;
    }

    /** A small world: rolling ground, a cliff, a tree and a pond, forest on the east half. */
    private static BlockState block(int x, int y, int z) {
        boolean pond = x >= 2 && x <= 4 && z >= 10 && z <= 12;
        int ground = x >= 8 ? 80 : 63 + (z % 3 == 0 ? 1 : 0);
        if (pond) {
            ground = 55;
        }
        // Tree at (3, 3): trunk up to 68, a leaf blob around 67-70.
        if (x == 3 && z == 3 && y > ground && y <= ground + 5) {
            return Blocks.OAK_LOG.defaultBlockState();
        }
        if (Math.abs(x - 3) <= 1 && Math.abs(z - 3) <= 1 && y >= ground + 4 && y <= ground + 7) {
            return Blocks.OAK_LEAVES.defaultBlockState();
        }
        if (y > ground) {
            return pond && y <= 62 ? Blocks.WATER.defaultBlockState() : AIR;
        }
        if (y == ground) {
            return pond ? Blocks.SAND.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (y >= ground - 2) {
            return Blocks.DIRT.defaultBlockState();
        }
        return y % 7 == 0 ? Blocks.ANDESITE.defaultBlockState() : STONE;
    }

    private static int top(int x, int z) {
        for (int y = MIN_Y + SECTIONS * 16 - 1; y >= MIN_Y; y--) {
            if (!block(x, y, z).isAir()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static final ChunkSkin.Source SOURCE = new ChunkSkin.Source() {
        @Override
        public BlockState get(int x, int y, int z) {
            return block(x, y, z);
        }

        @Override
        public int top(int x, int z) {
            return ChunkSkinTest.top(x, z);
        }

        @Override
        public Holder<Biome> biome(int qx, int qy, int qz) {
            return qx >= 2 ? forest : plains;
        }
    };

    private static BlockState decoded(ColumnSource src, int x, int y, int z) {
        ColumnSource.Section s = src.sections[(y - MIN_Y) >> 4];
        return s.indices() == null ? s.palette()[0] : s.palette()[s.indices()[((y - MIN_Y) & 15) << 8 | z << 4 | x]];
    }

    /** Whether the block can be seen: it is not air and something next to it lets light through. */
    private static boolean visible(int x, int y, int z) {
        if (block(x, y, z).isAir()) {
            return false;
        }
        int[][] around = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] d : around) {
            int nx = x + d[0];
            int nz = z + d[2];
            if (nx < 0 || nx > 15 || nz < 0 || nz > 15) {
                continue;
            }
            if (!block(nx, y + d[1], nz).isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                return true;
            }
        }
        return false;
    }

    private static ChunkSkin.Decoded roundTrip(int bottom, int slot, int flags) {
        byte[] bytes = ChunkSkin.encode(SOURCE, MIN_Y, bottom, slot, flags, biomes);
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        ChunkSkin.Decoded d = ChunkSkin.decode(buf, -3, 5, DetailRequest.UNIT, MIN_Y, SECTIONS, STONE, biomes, plains, true);
        assertEquals(0, buf.readableBytes(), "every byte should be read");
        return d;
    }

    @Test
    void keepsWhatCanBeSeen() {
        ChunkSkin.Decoded d = roundTrip(MIN_Y, 7 | 2 << 4, ChunkSkin.REAL);
        assertTrue(d.real());
        assertEquals(-3 * DetailRequest.UNIT + 7, d.source().chunkX);
        assertEquals(5 * DetailRequest.UNIT + 2, d.source().chunkZ);
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int top = top(x, z);
                for (int y = MIN_Y; y < MIN_Y + SECTIONS * 16; y++) {
                    BlockState got = decoded(d.source(), x, y, z);
                    if (y > top) {
                        assertSame(AIR, got, "air above the top at " + x + "," + y + "," + z);
                    } else if (visible(x, y, z)) {
                        assertSame(block(x, y, z), got, "visible block at " + x + "," + y + "," + z);
                    } else {
                        assertTrue(got.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) || got == block(x, y, z),
                                "hidden blocks stay solid at " + x + "," + y + "," + z);
                    }
                }
            }
        }
    }

    @Test
    void stoneUnderGeneratedBand() {
        ChunkSkin.Decoded d = roundTrip(58, 0, 0);
        assertFalse(d.real());
        // The pond floor (55) is under the band: the column reads as stone up to the band.
        assertSame(STONE, decoded(d.source(), 3, 57, 11));
        assertSame(AIR, decoded(d.source(), 3, 70, 11));
        assertSame(Blocks.GRASS_BLOCK.defaultBlockState(), decoded(d.source(), 10, 80, 10));
        assertSame(STONE, decoded(d.source(), 10, 0, 10));
    }

    @Test
    void biomesAndLight() {
        ColumnSource src = roundTrip(MIN_Y, 0, ChunkSkin.REAL).source();
        ColumnSource.Section s = src.sections[(64 - MIN_Y) >> 4];
        assertSame(plains, s.biomes()[0]);
        assertSame(forest, s.biomes()[3]);
        assertSame(forest, src.sections[0].biomes()[3 << 4 | 3 << 2 | 3]);
        // Sky light: full on the grass's top neighbour, none inside the ground, less under water.
        assertEquals(15, light(src, 10, 81, 10));
        assertEquals(0, light(src, 10, 79, 10));
        assertTrue(light(src, 3, 60, 11) < 15 && light(src, 3, 60, 11) > 0, "water dims the light");
    }

    private static int light(ColumnSource src, int x, int y, int z) {
        int i = (y - MIN_Y) >> 4;
        for (int k = i; k < src.sections.length; k++) {
            byte[] data = src.sections[k].skyLight();
            if (data != null) {
                // Sections without light take the bottom layer of the next one up (as the voxelizer does).
                int index = (k == i ? (y - MIN_Y) & 15 : 0) << 8 | z << 4 | x;
                return ColumnSource.Section.nibble(data, index);
            }
        }
        return 15;
    }

    @Test
    void answersSurviveTheTrip() {
        byte[] a = ChunkSkin.encode(SOURCE, MIN_Y, MIN_Y, 0, 0, biomes);
        byte[] b = ChunkSkin.encode(SOURCE, MIN_Y, 50, 9, ChunkSkin.REAL, biomes);
        byte[] both = new byte[a.length + b.length];
        System.arraycopy(a, 0, both, 0, a.length);
        System.arraycopy(b, 0, both, a.length, b.length);
        DetailChunks chunks = new DetailChunks(net.minecraft.world.level.Level.OVERWORLD, 4, -9, DetailChunks.OK, 0b11L, true, 2, both);
        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        DetailChunks.CODEC.encode(buf, chunks);
        DetailChunks back = DetailChunks.CODEC.decode(buf);
        assertEquals(2, back.count());
        ByteBuf in = Unpooled.wrappedBuffer(back.skins());
        assertEquals(0, ChunkSkin.decode(in, 4, -9, 8, MIN_Y, SECTIONS, STONE, biomes, plains, true).slot());
        assertEquals(9, ChunkSkin.decode(in, 4, -9, 8, MIN_Y, SECTIONS, STONE, biomes, plains, true).slot());
        assertEquals(0, in.readableBytes());
        System.out.println("[skin] bytes per chunk: real " + b.length + ", full depth " + a.length);
    }
}
