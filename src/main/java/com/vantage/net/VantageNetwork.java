package com.vantage.net;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * Vantage's network messages. All optional: players and servers without Vantage connect as
 * usual, they just do not exchange distant terrain.
 */
public final class VantageNetwork {
    /** Receives the client-bound messages; set by the client side, absent on dedicated servers. */
    public interface ClientHandler {
        void columns(TerrainColumns columns);

        void planet(PlanetInfo info);

        void detail(DetailChunks chunks);
    }

    private static volatile @Nullable ClientHandler client;

    private VantageNetwork() {
    }

    public static void setClientHandler(ClientHandler handler) {
        client = handler;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(VantageNetwork::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2").optional().executesOn(HandlerThread.NETWORK);
        registrar.playToServer(TerrainRequest.TYPE, TerrainRequest.CODEC, ServerTerrain::handle);
        registrar.playToServer(DetailRequest.TYPE, DetailRequest.CODEC, DetailService::handle);
        registrar.playToClient(DetailChunks.TYPE, DetailChunks.CODEC, (payload, context) -> {
            ClientHandler h = client;
            if (h != null) {
                h.detail(payload);
            }
        });
        registrar.playToClient(TerrainColumns.TYPE, TerrainColumns.CODEC, (payload, context) -> {
            ClientHandler h = client;
            if (h != null) {
                h.columns(payload);
            }
        });
        registrar.playToClient(PlanetInfo.TYPE, PlanetInfo.CODEC, (payload, context) -> {
            ClientHandler h = client;
            if (h != null) {
                h.planet(payload);
            }
        });
    }
}
