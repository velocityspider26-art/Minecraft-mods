package dev.velocityspider.caw.registry;

import dev.velocityspider.caw.CreateAerialWarfare;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateAerialWarfare.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CAW = TABS.register(
            "caw",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_aerial_warfare.caw"))
                    .icon(() -> new ItemStack(ModBlocks.ENGINE_FAN.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.ENGINE_INLET.get());
                        output.accept(ModBlocks.ENGINE_FAN.get());
                        output.accept(ModBlocks.ENGINE_COMPRESSOR.get());
                        output.accept(ModBlocks.ENGINE_COMBUSTOR.get());
                        output.accept(ModBlocks.ENGINE_TURBINE.get());
                        output.accept(ModBlocks.ENGINE_NOZZLE.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
