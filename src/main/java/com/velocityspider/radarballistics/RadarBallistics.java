package com.velocityspider.radarballistics;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.velocityspider.radarballistics.command.RadarBallisticsCommand;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Create Radar Ballistics &mdash; a fire-control add-on that makes auto-tracking cannons lead
 * their targets instead of aiming where the target already was.
 *
 * <p>The heavy lifting lives in {@link com.velocityspider.radarballistics.ballistics}, a
 * pure-Java engine that solves the intercept problem (target velocity + acceleration, shell
 * muzzle speed, gravity and drag). This class just wires the engine into the game: it loads
 * the config and registers the {@code /radarballistics} command. See {@code docs/INTEGRATION.md}
 * for hooking the engine into Create Radar's auto-tracking and Create: Big Cannons projectiles.</p>
 */
@Mod(RadarBallistics.MODID)
public class RadarBallistics {
    public static final String MODID = "radarballistics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RadarBallistics(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register for game events (command registration lives on the game bus).
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("Create Radar Ballistics loaded — fire-control lead correction active.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RadarBallisticsCommand.register(event.getDispatcher());
    }
}
