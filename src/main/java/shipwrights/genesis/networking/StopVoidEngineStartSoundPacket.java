package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.WormholeAmbianceHandler;

public record StopVoidEngineStartSoundPacket() implements CustomPacketPayload {
    public static final Type<StopVoidEngineStartSoundPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "stop_void_engine_start_sound"));

    public static final StreamCodec<ByteBuf, StopVoidEngineStartSoundPacket> STREAM_CODEC =
            StreamCodec.unit(new StopVoidEngineStartSoundPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StopVoidEngineStartSoundPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientHandler.handle(packet));
    }

    private static class ClientHandler {

        public static void handle(StopVoidEngineStartSoundPacket packet) {
            WormholeAmbianceHandler.stopVoidEngineStart();
        }
    }
}
