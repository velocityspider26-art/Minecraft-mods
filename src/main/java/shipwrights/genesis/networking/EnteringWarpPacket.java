package shipwrights.genesis.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;

public record EnteringWarpPacket() implements CustomPacketPayload {
    public static final EnteringWarpPacket INSTANCE = new EnteringWarpPacket();

    public static final Type<EnteringWarpPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "entering_warp"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnteringWarpPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
