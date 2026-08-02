package shipwrights.genesis.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import shipwrights.genesis.GenesisMod;

/** Custom worldgen placement modifiers. */
public final class GenesisPlacementModifiers {
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, GenesisMod.MOD_ID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<PolarPlacement>> POLAR =
            PLACEMENT_MODIFIERS.register("polar", () -> () -> PolarPlacement.CODEC);

    private GenesisPlacementModifiers() {
    }

    public static void register(IEventBus modBus) {
        PLACEMENT_MODIFIERS.register(modBus);
    }
}
