package shipwrights.genesis.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import static shipwrights.genesis.GenesisMod.*;

@EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DimensionEffectsRegistration {

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(SPACE_DIM, new SpaceDimensionEffects());
        event.register(WORMHOLE_DIM, new WormholeDimensionEffects());
        event.register(GENERIC_PLANET_ID, new PlanetDimensionEffects());
    }
}
