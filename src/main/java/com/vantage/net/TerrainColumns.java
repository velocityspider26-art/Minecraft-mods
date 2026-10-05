package com.vantage.net;

import com.vantage.Vantage;
import com.vantage.core.Lod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Server to client: sampled terrain for a {@link TerrainRequest}. For each sampled column, the
 * surface height and an index into a small biome palette; for each palette biome, how it looks
 * from afar (clients cannot work that out: biome features are not sent to them).
 *
 * @param status  {@link #OK}, {@link #BUSY} (ask again later) or {@link #REFUSED}
 * @param columns which columns were sampled (bit per column, z * 32 + x)
 * @param heights surface height per sampled column, in column order
 * @param biomes  palette index per sampled column
 * @param palette biome registry ids
 * @param looks   per palette entry: top, under, seabed and leaves block state ids (+1, 0 = none)
 * @param canopy  per palette entry: tree cover 0..1
 * @param treeHeight per palette entry: typical tree height in blocks
 */
public record TerrainColumns(ResourceKey<Level> dimension, int level, int sx, int sz, int status, long[] columns,
                             short[] heights, byte[] biomes, int[] palette, int[] looks, float[] canopy, byte[] treeHeight)
        implements CustomPacketPayload {
    public static final int OK = 0;
    public static final int BUSY = 1;
    public static final int REFUSED = 2;

    public static final Type<TerrainColumns> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vantage.MODID, "terrain_columns"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerrainColumns> CODEC = CustomPacketPayload.codec(TerrainColumns::write, TerrainColumns::read);

    public static TerrainColumns refusal(TerrainRequest r, int status) {
        return new TerrainColumns(r.dimension(), r.level(), r.sx(), r.sz(), status, new long[TerrainRequest.MASK_LONGS],
                new short[0], new byte[0], new int[0], new int[0], new float[0], new byte[0]);
    }

    private static TerrainColumns read(RegistryFriendlyByteBuf buf) {
        ResourceKey<Level> dim = buf.readResourceKey(Registries.DIMENSION);
        int level = buf.readByte();
        int sx = buf.readVarInt();
        int sz = buf.readVarInt();
        int status = buf.readByte();
        long[] mask = new long[TerrainRequest.MASK_LONGS];
        for (int i = 0; i < mask.length; i++) {
            mask[i] = buf.readLong();
        }
        int n = buf.readVarInt();
        if (n < 0 || n > Lod.AREA) {
            throw new IllegalArgumentException("bad column count " + n);
        }
        short[] heights = new short[n];
        byte[] biomes = new byte[n];
        for (int i = 0; i < n; i++) {
            heights[i] = buf.readShort();
            biomes[i] = buf.readByte();
        }
        int p = buf.readVarInt();
        if (p < 0 || p > 256) {
            throw new IllegalArgumentException("bad palette size " + p);
        }
        int[] palette = new int[p];
        int[] looks = new int[p * 4];
        float[] canopy = new float[p];
        byte[] treeHeight = new byte[p];
        for (int i = 0; i < p; i++) {
            palette[i] = buf.readVarInt();
            for (int k = 0; k < 4; k++) {
                looks[i * 4 + k] = buf.readVarInt();
            }
            canopy[i] = buf.readFloat();
            treeHeight[i] = buf.readByte();
        }
        return new TerrainColumns(dim, level, sx, sz, status, mask, heights, biomes, palette, looks, canopy, treeHeight);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceKey(this.dimension);
        buf.writeByte(this.level);
        buf.writeVarInt(this.sx);
        buf.writeVarInt(this.sz);
        buf.writeByte(this.status);
        for (long l : this.columns) {
            buf.writeLong(l);
        }
        buf.writeVarInt(this.heights.length);
        for (int i = 0; i < this.heights.length; i++) {
            buf.writeShort(this.heights[i]);
            buf.writeByte(this.biomes[i]);
        }
        buf.writeVarInt(this.palette.length);
        for (int i = 0; i < this.palette.length; i++) {
            buf.writeVarInt(this.palette[i]);
            for (int k = 0; k < 4; k++) {
                buf.writeVarInt(this.looks[i * 4 + k]);
            }
            buf.writeFloat(this.canopy[i]);
            buf.writeByte(this.treeHeight[i]);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
