package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.ClientStorage;

public record EnteringWarpPacket() implements CustomPacketPayload {
    public static final Type<EnteringWarpPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "entering_warp"));

    public static final StreamCodec<ByteBuf, EnteringWarpPacket> STREAM_CODEC =
            StreamCodec.unit(new EnteringWarpPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EnteringWarpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientStorage.goingToFromWormhole = true);
    }
}
