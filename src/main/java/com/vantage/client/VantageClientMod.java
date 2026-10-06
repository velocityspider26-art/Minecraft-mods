package com.vantage.client;

import com.vantage.Vantage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client side of Vantage: the distant terrain renderer. */
@Mod(value = Vantage.MODID, dist = Dist.CLIENT)
public final class VantageClientMod {
    public VantageClientMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, VantageConfig.SPEC);
        modBus.addListener((ModConfigEvent.Loading e) -> {
            if (e.getConfig().getSpec() == VantageConfig.SPEC) {
                VantageConfig.migrate();
            }
        });
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        VantageClient.init(modBus);
    }
}
