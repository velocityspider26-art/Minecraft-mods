package shipwrights.genesis.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.teleportation.SpaceTravelManager;

/**
 * Sent after vanilla has dismissed the destination receiving screen and the
 * destination has rendered a few frames. The server then asks the client which
 * arriving Sable craft, if any, still need a visibility repair.
 */
public record TransitionReadyPacket() implements CustomPacketPayload {
    public static final Type<TransitionReadyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "transition_ready"));
    public static final StreamCodec<ByteBuf, TransitionReadyPacket> STREAM_CODEC =
            StreamCodec.unit(new TransitionReadyPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TransitionReadyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpaceTravelManager.onClientTransitionReady(player);
            }
        });
    }
}
