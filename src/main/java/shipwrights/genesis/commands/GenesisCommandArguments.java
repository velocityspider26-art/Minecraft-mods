package shipwrights.genesis.commands;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import shipwrights.genesis.GenesisMod;

public final class GenesisCommandArguments {
    private GenesisCommandArguments() {}

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, GenesisMod.MOD_ID);

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<?, ?>> CELESTIAL =
            ARGUMENT_TYPES.register("celestial", () -> SingletonArgumentInfo.contextFree(CelestialArgument::celestial));

    public static void register(IEventBus bus) {
        ARGUMENT_TYPES.register(bus);
    }
}
