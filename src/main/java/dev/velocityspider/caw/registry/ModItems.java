package dev.velocityspider.caw.registry;

import dev.velocityspider.caw.CreateAerialWarfare;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateAerialWarfare.MOD_ID);

    static {
        ITEMS.registerSimpleBlockItem(ModBlocks.ENGINE_INLET);
        ITEMS.registerSimpleBlockItem(ModBlocks.ENGINE_FAN);
        ITEMS.registerSimpleBlockItem(ModBlocks.ENGINE_COMPRESSOR);
        ITEMS.registerSimpleBlockItem(ModBlocks.ENGINE_COMBUSTOR);
        ITEMS.registerSimpleBlockItem(ModBlocks.ENGINE_TURBINE);
        ITEMS.registerSimpleBlockItem(ModBlocks.ENGINE_NOZZLE);
    }

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
