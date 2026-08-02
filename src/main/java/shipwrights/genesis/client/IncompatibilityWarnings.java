package shipwrights.genesis.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class IncompatibilityWarnings {


    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && ModList.get().isLoaded("cosmos")) {
            mc.player.sendSystemMessage(
                    Component.literal("Both Cosmic Horizons and Genesis are installed. These are two different space mods and may cause unpredictable behavior when used together. Remove one of them before reporting any space-related bugs.")
                            .withStyle(ChatFormatting.RED)
            );
        }
    }
}
