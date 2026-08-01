package com.velocityspider.createjetengines;

import com.mojang.logging.LogUtils;
import com.velocityspider.createjetengines.config.JetEngineConfig;
import com.velocityspider.createjetengines.registry.JetBlockEntities;
import com.velocityspider.createjetengines.registry.JetBlocks;
import com.velocityspider.createjetengines.registry.JetCreativeTabs;
import com.velocityspider.createjetengines.registry.JetItems;
import com.velocityspider.createjetengines.registry.JetParticles;
import com.velocityspider.createjetengines.registry.JetSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Create: Jet Engines — modular turbofan propulsion for Create Aeronautics / Sable sublevels.
 *
 * <p>Registration order matters. Every {@code DeferredRegister} is attached to the mod event bus
 * here, in dependency order (blocks before block items before block entities before creative tabs),
 * and no {@code DeferredHolder} is dereferenced during class initialisation.
 */
@Mod(CreateJetEngines.MODID)
public class CreateJetEngines {

    public static final String MODID = "create_jet_engines";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateJetEngines(IEventBus modBus, ModContainer container) {
        // Blocks first: JetItems reads the block holders when it builds its own suppliers,
        // but only lazily, inside the item supplier lambdas.
        JetBlocks.register(modBus);
        JetItems.register(modBus);
        JetBlockEntities.register(modBus);
        JetSounds.register(modBus);
        JetParticles.register(modBus);
        JetCreativeTabs.register(modBus);

        container.registerConfig(ModConfig.Type.SERVER, JetEngineConfig.SPEC);
    }
}
