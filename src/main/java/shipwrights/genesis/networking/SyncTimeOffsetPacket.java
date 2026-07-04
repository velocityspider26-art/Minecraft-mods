package shipwrights.genesis.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;

public record SyncTimeOffsetPacket(long timeOffset) implements CustomPacketPayload {
    public static final Type<SyncTimeOffsetPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "sync_time_offset"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTimeOffsetPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, SyncTimeOffsetPacket::timeOffset,
                    SyncTimeOffsetPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
