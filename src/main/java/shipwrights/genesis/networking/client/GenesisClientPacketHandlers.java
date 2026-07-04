package shipwrights.genesis.networking.client;

import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.ClientStorage;
import shipwrights.genesis.client.WormholeAmbianceHandler;
import shipwrights.genesis.networking.SyncTimeOffsetPacket;
import shipwrights.genesis.networking.VoidEngineSoundPacket;
import shipwrights.genesis.networking.WormholeTravelSoundPacket;

/**
 * Client-only handlers for Genesis clientbound packets. Only referenced from inside the
 * {@code playToClient} handler lambdas, which never run on a dedicated server, so this class
 * (and the client classes it touches) is never classloaded server-side.
 */
public final class GenesisClientPacketHandlers {

    private GenesisClientPacketHandlers() {}

    public static void handleWormholeTravel(WormholeTravelSoundPacket packet) {
        WormholeAmbianceHandler.wormholeTravelPos = packet.enginePos();
    }

    public static void handleVoidEngineSound(VoidEngineSoundPacket packet) {
        WormholeAmbianceHandler.voidEngineStartPos = packet.enginePos();
        WormholeAmbianceHandler.playVoidEngineStart();
    }

    public static void handleStopVoidEngineStart() {
        WormholeAmbianceHandler.stopVoidEngineStart();
    }

    public static void handleEnteringWarp() {
        ClientStorage.goingToFromWormhole = true;
    }

    public static void handleSyncTimeOffset(SyncTimeOffsetPacket packet) {
        GenesisMod.clientTimeOffset = packet.timeOffset();
    }
}
