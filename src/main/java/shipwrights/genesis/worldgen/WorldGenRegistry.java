package shipwrights.genesis.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import shipwrights.genesis.GenesisMod;

import static shipwrights.genesis.GenesisMod.*;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class WorldGenRegistry {
    @SubscribeEvent
    public static void onRegisterEvent(RegisterEvent event) {
        event.register(Registries.DENSITY_FUNCTION_TYPE, helper -> {
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "random_noise"),
                    RandomNoise.MAP_CODEC
            );
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "asteroid_belt"),
                    AsteroidBelt.MAP_CODEC
            );
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "radial_gradient"),
                    RadialGradientDensity.MAP_CODEC
            );
            helper.register(
                    CraterNoise.resourceLocation,
                    CraterNoise.MAP_CODEC
            );

            helper.register(
                    MultiCraterNoise.resourceLocation,
                    MultiCraterNoise.MAP_CODEC
            );
        });

        event.register(Registries.MATERIAL_RULE, helper -> {
            helper.register(ASTEROID_RULE_ID, AsteroidBlockSurfaceRule.MAP_CODEC);
        });
    }
}
