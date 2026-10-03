package dev.velocityspider.caw;

import com.mojang.logging.LogUtils;
import dev.velocityspider.caw.registry.ModBlockEntities;
import dev.velocityspider.caw.registry.ModBlocks;
import dev.velocityspider.caw.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CreateAerialWarfare.MOD_ID)
public final class CreateAerialWarfare {
    public static final String MOD_ID = "create_aerial_warfare";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateAerialWarfare(IEventBus modBus) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
    }
}
