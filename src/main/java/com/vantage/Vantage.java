package com.vantage;

import com.mojang.logging.LogUtils;
import com.vantage.client.VantageClient;
import com.vantage.client.VantageConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

/** Vantage is purely client-side; on a dedicated server it does nothing. */
@Mod(value = Vantage.MODID, dist = Dist.CLIENT)
public final class Vantage {
    public static final String MODID = "vantage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Vantage(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, VantageConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        VantageClient.init(modBus);
    }
}
