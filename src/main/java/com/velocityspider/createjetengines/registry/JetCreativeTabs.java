package com.velocityspider.createjetengines.registry;

import com.velocityspider.createjetengines.CreateJetEngines;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JetCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateJetEngines.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_jet_engines"))
                    .icon(() -> new ItemStack(JetItems.COMBUSTION_CORE.get()))
                    .displayItems((params, output) -> {
                        output.accept(JetItems.FAN_MODULE.get());
                        output.accept(JetItems.COMPRESSOR_MODULE.get());
                        output.accept(JetItems.COMBUSTION_CORE.get());
                        output.accept(JetItems.AFTERBURNER_MODULE.get());
                        output.accept(JetItems.NOZZLE_MODULE.get());
                    })
                    .build());

    private JetCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
