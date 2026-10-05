package com.vantage;

import com.mojang.logging.LogUtils;
import com.vantage.net.ServerTerrain;
import com.vantage.net.VantageNetwork;
import com.vantage.worldgen.VantageWorldgen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Common side of Vantage: the Vantage Planet world type and serving distant terrain to players.
 * The renderer lives in {@link com.vantage.client.VantageClientMod}, loaded only in the game client.
 */
@Mod(Vantage.MODID)
public final class Vantage {
    public static final String MODID = "vantage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Vantage(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, VantageServerConfig.SPEC);
        VantageWorldgen.register(modBus);
        VantageNetwork.register(modBus);
        ServerTerrain.init();
    }
}
