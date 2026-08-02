package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;

public record SyncTimeOffsetPacket(long timeOffset) implements CustomPacketPayload {
    public static final Type<SyncTimeOffsetPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "sync_time_offset"));

    public static final StreamCodec<ByteBuf, SyncTimeOffsetPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_LONG, SyncTimeOffsetPacket::timeOffset, SyncTimeOffsetPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTimeOffsetPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> GenesisMod.clientTimeOffset = packet.timeOffset());
    }
}
