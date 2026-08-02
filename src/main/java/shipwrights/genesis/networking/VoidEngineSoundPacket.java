package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.WormholeAmbianceHandler;

public record VoidEngineSoundPacket(BlockPos enginePos) implements CustomPacketPayload {
    public static final Type<VoidEngineSoundPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "void_engine_sound"));

    public static final StreamCodec<ByteBuf, VoidEngineSoundPacket> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, VoidEngineSoundPacket::enginePos, VoidEngineSoundPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VoidEngineSoundPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientHandler.handle(packet));
    }

    private static class ClientHandler {

        public static void handle(VoidEngineSoundPacket packet) {
            WormholeAmbianceHandler.voidEngineStartPos = packet.enginePos;
            WormholeAmbianceHandler.playVoidEngineStart();
        }
    }
}
