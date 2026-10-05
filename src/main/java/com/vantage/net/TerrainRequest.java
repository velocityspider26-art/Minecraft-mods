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
 * Client to server: "sample the terrain of these voxel columns of section column (sx, sz) at this
 * LOD level", for players whose game cannot run the world generator itself.
 */
public record TerrainRequest(ResourceKey<Level> dimension, int level, int sx, int sz, long[] columns) implements CustomPacketPayload {
    public static final Type<TerrainRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vantage.MODID, "terrain_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerrainRequest> CODEC = CustomPacketPayload.codec(TerrainRequest::write, TerrainRequest::read);

    static final int MASK_LONGS = Lod.AREA / 64;

    private static TerrainRequest read(RegistryFriendlyByteBuf buf) {
        ResourceKey<Level> dim = buf.readResourceKey(Registries.DIMENSION);
        int level = buf.readByte();
        int sx = buf.readVarInt();
        int sz = buf.readVarInt();
        long[] mask = new long[MASK_LONGS];
        for (int i = 0; i < MASK_LONGS; i++) {
            mask[i] = buf.readLong();
        }
        return new TerrainRequest(dim, level, sx, sz, mask);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceKey(this.dimension);
        buf.writeByte(this.level);
        buf.writeVarInt(this.sx);
        buf.writeVarInt(this.sz);
        for (long l : this.columns) {
            buf.writeLong(l);
        }
    }

    public static long[] mask(boolean[] columns) {
        long[] m = new long[MASK_LONGS];
        for (int i = 0; i < columns.length; i++) {
            if (columns[i]) {
                m[i >> 6] |= 1L << (i & 63);
            }
        }
        return m;
    }

    public static boolean[] unmask(long[] m) {
        boolean[] columns = new boolean[Lod.AREA];
        for (int i = 0; i < Lod.AREA; i++) {
            columns[i] = (m[i >> 6] & (1L << (i & 63))) != 0;
        }
        return columns;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
