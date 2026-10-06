package com.vantage.net;

import com.vantage.Vantage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Server to client: an answer to a {@link DetailRequest}. {@code answered} marks the chunks the
 * server dealt with, {@code skins} holds {@code count} {@link ChunkSkin}s one after another.
 * Chunks asked for but not answered can be asked for again later. A big answer may come in
 * several parts ({@code last} false on all but the final one).
 *
 * @param status {@link #OK}, {@link #BUSY} (ask again later) or {@link #REFUSED}
 */
public record DetailChunks(ResourceKey<Level> dimension, int ux, int uz, int status, long answered, boolean last, int count, byte[] skins)
        implements CustomPacketPayload {
    public static final int OK = 0;
    public static final int BUSY = 1;
    public static final int REFUSED = 2;
    /** Largest skin data sent in one message; the game allows a little under 1 MiB. */
    public static final int MAX_BYTES = 512 * 1024;

    public static final Type<DetailChunks> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vantage.MODID, "detail_chunks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DetailChunks> CODEC = CustomPacketPayload.codec(DetailChunks::write, DetailChunks::read);

    public static DetailChunks refusal(DetailRequest r, int status) {
        return new DetailChunks(r.dimension(), r.ux(), r.uz(), status, 0L, true, 0, new byte[0]);
    }

    private static DetailChunks read(RegistryFriendlyByteBuf buf) {
        ResourceKey<Level> dim = buf.readResourceKey(Registries.DIMENSION);
        int ux = buf.readVarInt();
        int uz = buf.readVarInt();
        int status = buf.readByte();
        long answered = buf.readLong();
        boolean last = buf.readBoolean();
        int count = buf.readVarInt();
        if (count < 0 || count > 64) {
            throw new IllegalArgumentException("bad chunk count " + count);
        }
        return new DetailChunks(dim, ux, uz, status, answered, last, count, buf.readByteArray(MAX_BYTES + 4096));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceKey(this.dimension);
        buf.writeVarInt(this.ux);
        buf.writeVarInt(this.uz);
        buf.writeByte(this.status);
        buf.writeLong(this.answered);
        buf.writeBoolean(this.last);
        buf.writeVarInt(this.count);
        buf.writeByteArray(this.skins);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
