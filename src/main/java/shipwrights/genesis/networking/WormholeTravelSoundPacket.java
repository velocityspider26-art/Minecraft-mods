package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.WormholeAmbianceHandler;

public record WormholeTravelSoundPacket(BlockPos enginePos) implements CustomPacketPayload {
    public static final Type<WormholeTravelSoundPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "wormhole_travel_sound"));

    public static final StreamCodec<ByteBuf, WormholeTravelSoundPacket> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, WormholeTravelSoundPacket::enginePos, WormholeTravelSoundPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WormholeTravelSoundPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientHandler.handle(packet));
    }

    private static class ClientHandler {

        public static void handle(WormholeTravelSoundPacket packet) {
            WormholeAmbianceHandler.wormholeTravelPos = packet.enginePos;
        }
    }
}
