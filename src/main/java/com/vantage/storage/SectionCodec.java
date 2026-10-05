package com.vantage.storage;

import com.vantage.core.Lod;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Serialises a non-uniform 32³ section.
 *
 * <p>Layout before compression: palette size, palette ({@code int}s), then one index per voxel
 * as a byte (palette ≤ 256) or a short. The voxel order is y-major, so the long runs of identical
 * layers (sky, deep stone) compress to almost nothing. Compression is LZ4, which Minecraft
 * already ships.
 *
 * <p>Uniform sections never reach this codec: the region index stores them inline.
 */
public final class SectionCodec {
    private static final int FORMAT = 1;
    private static final LZ4Factory LZ4 = LZ4Factory.fastestJavaInstance();

    private SectionCodec() {
    }

    /** Returns the single value of a uniform section, or {@code null} if it is mixed. */
    public static Integer uniformValue(int[] voxels) {
        int first = voxels[0];
        for (int i = 1; i < voxels.length; i++) {
            if (voxels[i] != first) {
                return null;
            }
        }
        return first;
    }

    public static byte[] encode(int[] voxels) {
        Int2IntOpenHashMap lookup = new Int2IntOpenHashMap();
        lookup.defaultReturnValue(-1);
        int[] palette = new int[64];
        int paletteSize = 0;
        int[] indices = new int[Lod.VOLUME];
        int last = 0;
        int lastIndex = -1;
        for (int i = 0; i < Lod.VOLUME; i++) {
            int v = voxels[i];
            int idx;
            if (v == last && lastIndex >= 0) {
                idx = lastIndex;
            } else {
                idx = lookup.get(v);
                if (idx < 0) {
                    idx = paletteSize++;
                    if (idx == palette.length) {
                        palette = java.util.Arrays.copyOf(palette, palette.length * 2);
                    }
                    palette[idx] = v;
                    lookup.put(v, idx);
                }
                last = v;
                lastIndex = idx;
            }
            indices[i] = idx;
        }

        boolean wide = paletteSize > 256;
        int rawLength = 8 + paletteSize * 4 + Lod.VOLUME * (wide ? 2 : 1);
        ByteBuffer raw = ByteBuffer.allocate(rawLength).order(ByteOrder.LITTLE_ENDIAN);
        raw.putInt(FORMAT);
        raw.putInt(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            raw.putInt(palette[i]);
        }
        if (wide) {
            for (int i = 0; i < Lod.VOLUME; i++) {
                raw.putShort((short) indices[i]);
            }
        } else {
            for (int i = 0; i < Lod.VOLUME; i++) {
                raw.put((byte) indices[i]);
            }
        }

        LZ4Compressor compressor = LZ4.fastCompressor();
        byte[] out = new byte[4 + compressor.maxCompressedLength(rawLength)];
        ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).putInt(rawLength);
        int compressed = compressor.compress(raw.array(), 0, rawLength, out, 4, out.length - 4);
        return java.util.Arrays.copyOf(out, 4 + compressed);
    }

    public static int[] decode(byte[] blob, int[] dst) {
        int rawLength = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN).getInt();
        byte[] rawBytes = new byte[rawLength];
        LZ4FastDecompressor decompressor = LZ4.fastDecompressor();
        decompressor.decompress(blob, 4, rawBytes, 0, rawLength);
        ByteBuffer raw = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN);
        int format = raw.getInt();
        if (format != FORMAT) {
            throw new IllegalStateException("Unknown section format " + format);
        }
        int paletteSize = raw.getInt();
        int[] palette = new int[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            palette[i] = raw.getInt();
        }
        if (dst == null) {
            dst = new int[Lod.VOLUME];
        }
        if (paletteSize > 256) {
            for (int i = 0; i < Lod.VOLUME; i++) {
                dst[i] = palette[raw.getShort() & 0xFFFF];
            }
        } else {
            for (int i = 0; i < Lod.VOLUME; i++) {
                dst[i] = palette[raw.get() & 0xFF];
            }
        }
        return dst;
    }
}
