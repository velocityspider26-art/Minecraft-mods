package shipwrights.genesis.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import shipwrights.genesis.networking.client.GenesisClientPacketHandlers;

/**
 * Genesis networking, ported to the NeoForge 1.21.1 payload system.
 *
 * <p>All Genesis packets are clientbound (server -&gt; client) sound / warp UI cues. The actual
 * physics / construct state is synced by Sable, so Genesis never sends construct data itself.</p>
 */
public class GenesisNetworking {

    private static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();

        registrar.playToClient(WormholeTravelSoundPacket.TYPE, WormholeTravelSoundPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> GenesisClientPacketHandlers.handleWormholeTravel(payload)));

        registrar.playToClient(VoidEngineSoundPacket.TYPE, VoidEngineSoundPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> GenesisClientPacketHandlers.handleVoidEngineSound(payload)));

        registrar.playToClient(StopVoidEngineStartSoundPacket.TYPE, StopVoidEngineStartSoundPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(GenesisClientPacketHandlers::handleStopVoidEngineStart));

        registrar.playToClient(EnteringWarpPacket.TYPE, EnteringWarpPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(GenesisClientPacketHandlers::handleEnteringWarp));

        registrar.playToClient(SyncTimeOffsetPacket.TYPE, SyncTimeOffsetPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> GenesisClientPacketHandlers.handleSyncTimeOffset(payload)));
    }

    /** Send a clientbound payload to every connected player. */
    public static void sendToAll(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }

    /** Send a clientbound payload to all players tracking the given chunk. */
    public static void sendToChunk(LevelChunk chunk, CustomPacketPayload payload) {
        if (chunk.getLevel() instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk.getPos(), payload);
        }
    }

    public static void sendToChunk(ServerLevel level, ChunkPos pos, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingChunk(level, pos, payload);
    }
}
