package shipwrights.genesis.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import shipwrights.genesis.GenesisMod;

/** Opens the orbit map when its key is pressed in-world. */
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public final class GenesisKeyHandler {
    private GenesisKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (GenesisKeyMappings.ORBIT_MAP == null || GenesisKeyMappings.ORBITAL_SURVEY == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !GenesisMod.isSpaceDimension(minecraft.level)) {
            OrbitalSurveyController.reset();
        }
        while (GenesisKeyMappings.ORBIT_MAP.consumeClick()) {
            // The map is the goggles' instrument, so it needs them worn or held,
            // just like the in-world orbit lines.
            if (minecraft.screen == null && minecraft.player != null
                    && OrbitVisualizerRenderer.holdingGoggles(minecraft.player)) {
                minecraft.setScreen(new OrbitMapScreen());
            }
        }

        while (GenesisKeyMappings.ORBITAL_SURVEY.consumeClick()) {
            if (minecraft.screen == null && minecraft.player != null) {
                OrbitalSurveyController.toggle();
            }
        }
    }
}
