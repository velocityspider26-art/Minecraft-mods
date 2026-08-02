package shipwrights.genesis.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.dimension.SubDimensions;

/**
 * Registers the invisible receiving screen only for travel between Genesis
 * rooms. Conditional effects have priority over ordinary incoming/outgoing
 * effects, so another mod cannot accidentally replace the room-to-room path
 * with a generic dimension screen.
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TransitionScreenRegistration {
    private TransitionScreenRegistration() {
    }

    @SubscribeEvent
    public static void onRegisterTransitionScreens(RegisterDimensionTransitionScreenEvent event) {
        int registered = 0;
        int conflicts = 0;

        for (ResourceKey<Level> from : SubDimensions.builtInRooms()) {
            for (ResourceKey<Level> to : SubDimensions.builtInRooms()) {
                if (from.equals(to)) {
                    continue;
                }
                if (event.registerConditionalEffect(to, from, SeamlessTransitionScreen::new)) {
                    registered++;
                } else {
                    conflicts++;
                }
            }
        }

        GenesisMod.LOGGER.info(
                "[TRANSITION] registered {} seamless room-to-room screen routes{}",
                registered,
                conflicts == 0 ? "" : " (" + conflicts + " already claimed)");
    }
}
