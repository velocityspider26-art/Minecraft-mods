package com.vantage.net;

import com.vantage.world.ColumnSource;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The part of a chunk that can be seen from afar, compactly, for sending to players: for each
 * column the blocks from its top down to where nothing more shows (solid ground that the
 * neighbouring columns do not uncover), as runs; deeper down counts as the dimension's stone.
 * Typically a few hundred bytes to a few kilobytes a chunk, against tens for the whole chunk.
 *
 * <p>Layout: position in its unit and flags; block state palette (state ids); biome palette
 * (registry ids); the biome at the surface of each 4×4-column cell; then per column its top
 * (blocks above the dimension floor, 0 if empty) and its runs (palette index, length).
 */
public final class ChunkSkin {
    /** Flag: the skin is of the real chunk (explored, or saved), not one made for Vantage. */
    public static final int REAL = 1;
    /** Two solid blocks in a row seal a column: nothing under them shows from above. */
    private static final int SEAL = 2;
    /** At chunk edges the neighbouring column is unknown: assume it this much lower. */
    private static final int EDGE_DROP = 12;
    /** Most blocks sent per column. */
    private static final int MAX_DEPTH = 96;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private ChunkSkin() {
    }

    /** Where a skin is read from: blocks by chunk-relative x/z and absolute y, and biomes. */
    public interface Source {
        BlockState get(int x, int y, int z);

        /** Highest non-air block of the column, or {@code Integer.MIN_VALUE}. */
        int top(int x, int z);

        /** Biome at quart position (chunk-relative x/z, absolute y). */
        Holder<Biome> biome(int qx, int qy, int qz);
    }

    /** Reads a chunk (live, or generated) directly. */
    public static Source of(ChunkAccess chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        int minSection = chunk.getMinSection();
        int qx0 = QuartPos.fromBlock(chunk.getPos().getMinBlockX());
        int qz0 = QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
        int highest = sections.length - 1;
        while (highest >= 0 && sections[highest].hasOnlyAir()) {
            highest--;
        }
        int topSection = highest;
        return new Source() {
            @Override
            public BlockState get(int x, int y, int z) {
                int i = (y >> 4) - minSection;
                return i < 0 || i >= sections.length ? AIR : sections[i].getBlockState(x, y & 15, z);
            }

            @Override
            public int top(int x, int z) {
                for (int i = topSection; i >= 0; i--) {
                    LevelChunkSection s = sections[i];
                    if (s.hasOnlyAir()) {
                        continue;
                    }
                    for (int y = 15; y >= 0; y--) {
                        if (!s.getBlockState(x, y, z).isAir()) {
                            return (minSection + i) * 16 + y;
                        }
                    }
                }
                return Integer.MIN_VALUE;
            }

            @Override
            public Holder<Biome> biome(int qx, int qy, int qz) {
                return chunk.getNoiseBiome(qx0 + qx, qy, qz0 + qz);
            }
        };
    }

    /** Reads a decoded chunk (from the save). */
    public static Source of(ColumnSource src, int minY, Holder<Biome> fallback) {
        return new Source() {
            @Override
            public BlockState get(int x, int y, int z) {
                int i = (y - minY) >> 4;
                if (i < 0 || i >= src.sections.length || src.sections[i] == null) {
                    return AIR;
                }
                ColumnSource.Section s = src.sections[i];
                return s.indices() == null ? s.palette()[0] : s.palette()[s.indices()[((y - minY) & 15) << 8 | z << 4 | x]];
            }

            @Override
            public int top(int x, int z) {
                for (int i = src.sections.length - 1; i >= 0; i--) {
                    ColumnSource.Section s = src.sections[i];
                    if (s == null || (s.indices() == null && s.palette()[0].isAir())) {
                        continue;
                    }
                    for (int y = 15; y >= 0; y--) {
                        int by = minY + i * 16 + y;
                        if (!this.get(x, by, z).isAir()) {
                            return by;
                        }
                    }
                }
                return Integer.MIN_VALUE;
            }

            @Override
            public Holder<Biome> biome(int qx, int qy, int qz) {
                int i = (QuartPos.toBlock(qy) - minY) >> 4;
                if (i < 0 || i >= src.sections.length || src.sections[i] == null) {
                    return fallback;
                }
                return src.sections[i].biomes()[(qy & 3) << 4 | qz << 2 | qx];
            }
        };
    }

    /**
     * Encodes a chunk's skin. {@code bottom} is the lowest y the source knows (blocks under it
     * are taken to be stone); {@code slot} is the chunk's position in its unit.
     */
    public static byte[] encode(Source blocks, int minY, int bottom, int slot, int flags, Registry<Biome> biomes) {
        int[] tops = new int[256];
        // Per column, the highest block that light cannot pass: the neighbours show down to it.
        int[] floors = new int[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int top = blocks.top(x, z);
                tops[z << 4 | x] = top;
                int floor = Integer.MIN_VALUE + 1;
                if (top != Integer.MIN_VALUE) {
                    for (int y = top; y >= bottom && top - y < MAX_DEPTH; y--) {
                        if (blocks.get(x, y, z).isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                            floor = y;
                            break;
                        }
                    }
                }
                floors[z << 4 | x] = floor;
            }
        }
        Reference2IntOpenHashMap<BlockState> stateIndex = new Reference2IntOpenHashMap<>();
        stateIndex.defaultReturnValue(-1);
        List<BlockState> states = new ArrayList<>();
        // Columns first, into a scratch buffer, to learn the palette.
        FriendlyByteBuf columns = new FriendlyByteBuf(Unpooled.buffer(2048));
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int top = tops[z << 4 | x];
                if (top == Integer.MIN_VALUE || top < bottom) {
                    // Nothing known above the floor of the data: solid up to it (none at all if the
                    // data goes all the way down, as in a void).
                    columns.writeVarInt(bottom > minY ? bottom - minY : 0);
                    if (bottom > minY) {
                        columns.writeByte(0);
                    }
                    continue;
                }
                int self = floors[z << 4 | x];
                int low = Math.min(Math.min(neighbour(floors, x - 1, z, self), neighbour(floors, x + 1, z, self)),
                        Math.min(neighbour(floors, x, z - 1, self), neighbour(floors, x, z + 1, self)));
                columns.writeVarInt(top - minY + 1);
                int countAt = columns.writerIndex();
                columns.writeByte(0); // run count, patched below (at most MAX_DEPTH < 128 runs)
                int runs = 0;
                BlockState run = null;
                int length = 0;
                int solid = 0;
                for (int y = top; y >= bottom && top - y < MAX_DEPTH; y--) {
                    if (y < low && solid >= SEAL) {
                        break;
                    }
                    BlockState state = blocks.get(x, y, z);
                    solid = state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) ? solid + 1 : 0;
                    if (state == run) {
                        length++;
                        continue;
                    }
                    if (run != null) {
                        writeRun(columns, stateIndex, states, run, length);
                        runs++;
                    }
                    run = state;
                    length = 1;
                }
                if (run != null) {
                    writeRun(columns, stateIndex, states, run, length);
                    runs++;
                }
                columns.setByte(countAt, runs);
            }
        }

        Reference2IntOpenHashMap<Holder<Biome>> biomeIndex = new Reference2IntOpenHashMap<>();
        biomeIndex.defaultReturnValue(-1);
        List<Holder<Biome>> biomeList = new ArrayList<>();
        byte[] cells = new byte[16];
        for (int qz = 0; qz < 4; qz++) {
            for (int qx = 0; qx < 4; qx++) {
                int surface = Integer.MIN_VALUE;
                for (int z = 0; z < 4; z++) {
                    for (int x = 0; x < 4; x++) {
                        surface = Math.max(surface, tops[(qz * 4 + z) << 4 | qx * 4 + x]);
                    }
                }
                Holder<Biome> b = blocks.biome(qx, QuartPos.fromBlock(Math.max(surface, bottom)), qz);
                int i = biomeIndex.getInt(b);
                if (i < 0) {
                    i = biomeList.size();
                    biomeList.add(b);
                    biomeIndex.put(b, i);
                }
                cells[qz << 2 | qx] = (byte) i;
            }
        }

        FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer(columns.readableBytes() + 64 + states.size() * 3));
        out.writeByte(slot);
        out.writeByte(flags);
        out.writeVarInt(states.size());
        for (BlockState s : states) {
            out.writeVarInt(Block.getId(s));
        }
        out.writeVarInt(biomeList.size());
        for (Holder<Biome> b : biomeList) {
            out.writeVarInt(biomes.getId(b.value()));
        }
        out.writeBytes(cells);
        out.writeBytes(columns);
        byte[] bytes = new byte[out.readableBytes()];
        out.readBytes(bytes);
        columns.release();
        out.release();
        return bytes;
    }

    private static int neighbour(int[] floors, int x, int z, int self) {
        if (x < 0 || x > 15 || z < 0 || z > 15) {
            return self == Integer.MIN_VALUE + 1 ? self : self - EDGE_DROP;
        }
        return floors[z << 4 | x];
    }

    private static void writeRun(FriendlyByteBuf buf, Reference2IntOpenHashMap<BlockState> index, List<BlockState> states,
                                 BlockState state, int length) {
        int i = index.getInt(state);
        if (i < 0) {
            i = states.size();
            states.add(state);
            index.put(state, i);
        }
        buf.writeVarInt(i);
        buf.writeVarInt(length);
    }

    /** A decoded skin: where it goes, whether it is real, and the column it makes. */
    public record Decoded(int slot, boolean real, ColumnSource source) {
    }

    /**
     * Reads one skin from {@code buf} into a full column: the skin's blocks, {@code stone} under
     * them, air above, biomes from the surface all the way up and down, and sky light straight
     * down from the sky ({@code sky}: whether the dimension has any).
     */
    @SuppressWarnings("unchecked")
    public static Decoded decode(ByteBuf in, int unitX, int unitZ, int unitChunks, int minY, int sectionCount, BlockState stone,
                                 Registry<Biome> biomes, Holder<Biome> fallback, boolean sky) {
        FriendlyByteBuf buf = new FriendlyByteBuf(in);
        int slot = buf.readUnsignedByte();
        int flags = buf.readUnsignedByte();
        int n = buf.readVarInt();
        if (n < 0 || n > 4096) {
            throw new IllegalArgumentException("bad palette size " + n);
        }
        // Palette: air, stone, then the skin's states.
        BlockState[] palette = new BlockState[n + 2];
        palette[0] = AIR;
        palette[1] = stone;
        for (int i = 0; i < n; i++) {
            palette[i + 2] = Block.stateById(buf.readVarInt());
        }
        int nb = buf.readVarInt();
        if (nb < 1 || nb > 16) {
            throw new IllegalArgumentException("bad biome count " + nb);
        }
        Holder<Biome>[] biomePalette = new Holder[nb];
        for (int i = 0; i < nb; i++) {
            biomePalette[i] = biomes.getHolder(buf.readVarInt()).<Holder<Biome>>map(h -> h).orElse(fallback);
        }
        Holder<Biome>[] cells = new Holder[64];
        for (int qz = 0; qz < 4; qz++) {
            for (int qx = 0; qx < 4; qx++) {
                int b = buf.readUnsignedByte();
                Holder<Biome> biome = b < nb ? biomePalette[b] : fallback;
                for (int qy = 0; qy < 4; qy++) {
                    cells[qy << 4 | qz << 2 | qx] = biome;
                }
            }
        }

        int height = sectionCount * 16;
        short[][] indices = new short[sectionCount][];
        // Per column: the first y (from the floor) under the skin, which is stone from there down.
        int[] ends = new int[256];
        int stoneBelow = height;
        for (int c = 0; c < 256; c++) {
            int x = c & 15;
            int z = c >> 4;
            int top = buf.readVarInt() - 1;
            if (top < 0) {
                ends[c] = 0;
                stoneBelow = 0;
                continue;
            }
            if (top >= height) {
                throw new IllegalArgumentException("bad top " + top);
            }
            int runs = buf.readUnsignedByte();
            int y = top;
            for (int r = 0; r < runs; r++) {
                int p = buf.readVarInt();
                int len = buf.readVarInt();
                if (p < 0 || p >= n || len <= 0 || len > height) {
                    throw new IllegalArgumentException("bad run");
                }
                short v = (short) (p + 2);
                for (int k = 0; k < len && y >= 0; k++, y--) {
                    set(indices, y, x, z, v);
                }
            }
            ends[c] = y + 1;
            stoneBelow = Math.min(stoneBelow, y + 1);
        }
        // Sections wholly under every column's skin are plain stone; fill the rest column by column.
        int stoneSections = stoneBelow >> 4;
        for (int c = 0; c < 256; c++) {
            for (int y = ends[c] - 1; y >= stoneSections * 16; y--) {
                set(indices, y, c & 15, c >> 4, (short) 1);
            }
        }

        ColumnSource src = new ColumnSource(unitX * unitChunks + (slot & 15), unitZ * unitChunks + (slot >> 4), sectionCount, sky);
        byte[][] light = sky ? skyLight(indices, palette, stoneSections, sectionCount) : null;
        for (int i = 0; i < sectionCount; i++) {
            short[] idx = indices[i];
            byte[] l = light == null ? null : light[i];
            if (i < stoneSections) {
                src.sections[i] = new ColumnSource.Section(new BlockState[]{stone}, null, cells, l, null);
            } else if (idx == null) {
                src.sections[i] = new ColumnSource.Section(new BlockState[]{AIR}, null, cells, l, null);
            } else {
                src.sections[i] = new ColumnSource.Section(palette, idx, cells, l, null);
            }
        }
        return new Decoded(slot, (flags & REAL) != 0, src);
    }

    private static void set(short[][] indices, int y, int x, int z, short v) {
        int s = y >> 4;
        short[] idx = indices[s];
        if (idx == null) {
            if (v == 0) {
                return;
            }
            idx = indices[s] = new short[4096];
        }
        idx[(y & 15) << 8 | z << 4 | x] = v;
    }

    private static boolean uniform(short[] idx, short v) {
        for (short s : idx) {
            if (s != v) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sky light straight down: full above each column's top, less through what lets light
     * through. Sections left null take their light from above (air) or are dark (under it all).
     */
    private static byte[][] skyLight(short[][] indices, BlockState[] palette, int stoneSections, int sectionCount) {
        byte[] opacity = new byte[palette.length];
        for (int i = 0; i < palette.length; i++) {
            opacity[i] = (byte) Math.min(15, palette[i].getLightBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        }
        byte[][] out = new byte[sectionCount][];
        int[] light = new int[256];
        Arrays.fill(light, 15);
        for (int s = sectionCount - 1; s >= stoneSections; s--) {
            short[] idx = indices[s];
            if (idx == null) {
                // Air: light passes unchanged; leaving the section out says just that.
                continue;
            }
            byte[] data = new byte[2048];
            boolean dark = true;
            for (int y = 15; y >= 0; y--) {
                for (int c = 0; c < 256; c++) {
                    int l = light[c];
                    if (l > 0) {
                        l = Math.max(0, l - opacity[idx[y << 8 | c]]);
                        light[c] = l;
                    }
                    if (l != 0) {
                        dark = false;
                        int index = y << 8 | c;
                        data[index >> 1] |= (byte) (l << ((index & 1) << 2));
                    }
                }
            }
            out[s] = data;
            if (dark) {
                // Everything further down inherits this section's dark bottom.
                break;
            }
        }
        return out;
    }
}
