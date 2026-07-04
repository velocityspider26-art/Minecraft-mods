package shipwrights.genesis.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.NetworkEvent;
import shipwrights.genesis.client.WormholeAmbianceHandler;

import java.util.function.Supplier;

public record StopVoidEngineStartSoundPacket() {

    public static void encode(StopVoidEngineStartSoundPacket packet, FriendlyByteBuf buf) {
    }

    public static StopVoidEngineStartSoundPacket decode(FriendlyByteBuf buf) {
        return new StopVoidEngineStartSoundPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientHandler.handle(this));
        }
        context.setPacketHandled(true);
    }

    private static class ClientHandler {

        public static void handle(StopVoidEngineStartSoundPacket packet) {
            WormholeAmbianceHandler.stopVoidEngineStart();
        }
    }
}
