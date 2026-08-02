package shipwrights.genesis.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class GenesisNetworking {

    private static final String PROTOCOL_VERSION = "5";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(WormholeTravelSoundPacket.TYPE, WormholeTravelSoundPacket.STREAM_CODEC, WormholeTravelSoundPacket::handle);
        registrar.playToClient(VoidEngineSoundPacket.TYPE, VoidEngineSoundPacket.STREAM_CODEC, VoidEngineSoundPacket::handle);
        registrar.playToClient(StopVoidEngineStartSoundPacket.TYPE, StopVoidEngineStartSoundPacket.STREAM_CODEC, StopVoidEngineStartSoundPacket::handle);
        registrar.playToClient(EnteringWarpPacket.TYPE, EnteringWarpPacket.STREAM_CODEC, EnteringWarpPacket::handle);
        registrar.playToClient(HyperspaceStatePacket.TYPE, HyperspaceStatePacket.STREAM_CODEC, HyperspaceStatePacket::handle);
        registrar.playToClient(SyncTimeOffsetPacket.TYPE, SyncTimeOffsetPacket.STREAM_CODEC, SyncTimeOffsetPacket::handle);
        registrar.playToClient(PlanetSurfacePacket.TYPE, PlanetSurfacePacket.STREAM_CODEC, PlanetSurfacePacket::handle);
        registrar.playToClient(PlanetVoxelBrickPacket.TYPE, PlanetVoxelBrickPacket.STREAM_CODEC, PlanetVoxelBrickPacket::handle);
        registrar.playToServer(PlanetVoxelInterestPacket.TYPE, PlanetVoxelInterestPacket.STREAM_CODEC, PlanetVoxelInterestPacket::handle);
    }

    public static void sendToAll(CustomPacketPayload packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToTrackingChunk(ServerLevel level, ChunkPos chunkPos, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, packet);
    }
}
