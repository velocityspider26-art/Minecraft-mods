package com.velocityspider.createjetengines.registry;

import com.velocityspider.createjetengines.CreateJetEngines;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JetItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateJetEngines.MODID);

    public static final DeferredItem<BlockItem> FAN_MODULE =
            ITEMS.registerSimpleBlockItem(JetBlocks.FAN_MODULE);
    public static final DeferredItem<BlockItem> COMPRESSOR_MODULE =
            ITEMS.registerSimpleBlockItem(JetBlocks.COMPRESSOR_MODULE);
    public static final DeferredItem<BlockItem> COMBUSTION_CORE =
            ITEMS.registerSimpleBlockItem(JetBlocks.COMBUSTION_CORE);
    public static final DeferredItem<BlockItem> AFTERBURNER_MODULE =
            ITEMS.registerSimpleBlockItem(JetBlocks.AFTERBURNER_MODULE);
    public static final DeferredItem<BlockItem> NOZZLE_MODULE =
            ITEMS.registerSimpleBlockItem(JetBlocks.NOZZLE_MODULE);

    private JetItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
