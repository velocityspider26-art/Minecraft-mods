package shipwrights.genesis.time;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.networking.SyncTimeOffsetPacket;

public class TimeTracker {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) return;
        if (serverLevel.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) return;

        // doDaylightCycle is false: decrement offset each tick to cancel out gameTime++,
        // keeping genesisTime frozen. Sync every tick so clients stay in step.
        GenesisTimeData data = GenesisTimeData.getOrCreate(serverLevel.getServer());
        data.addOffset(-1);
        GenesisNetworking.sendToAll(new SyncTimeOffsetPacket(data.getTimeOffset()));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        long offset = GenesisTimeData.getOrCreate(server).getTimeOffset();
        PacketDistributor.sendToPlayer(player, new SyncTimeOffsetPacket(offset));
    }
}
