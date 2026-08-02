package shipwrights.genesis.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import shipwrights.genesis.GenesisMod;

/** Registers Genesis key bindings. */
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GenesisKeyMappings {
    public static KeyMapping ORBIT_MAP;
    public static KeyMapping ORBITAL_SURVEY;

    private GenesisKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        ORBIT_MAP = new KeyMapping(
                "key.genesis.orbit_map",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "key.categories.genesis");
        event.register(ORBIT_MAP);
        ORBITAL_SURVEY = new KeyMapping(
                "key.genesis.orbital_survey",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.genesis");
        event.register(ORBITAL_SURVEY);
    }
}
