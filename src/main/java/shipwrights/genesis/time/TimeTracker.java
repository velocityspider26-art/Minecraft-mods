package shipwrights.genesis.time;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.networking.SyncTimeOffsetPacket;

public class TimeTracker {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        long offset = GenesisTimeData.getOrCreate(server).getTimeOffset();
        GenesisNetworking.sendToPlayer(player, new SyncTimeOffsetPacket(offset));
    }
}
