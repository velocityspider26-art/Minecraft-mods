package shipwrights.genesis.space.physics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import shipwrights.genesis.GenesisMod;

/**
 * Mercury's killer temperature swing — the largest in the solar system. The
 * sun-facing surface reaches ~430°C and the night side ~-180°C, and with no
 * atmosphere to move heat around, standing in the open is lethal either way.
 *
 * <p>Exposure is what matters: a player under the open sky bakes by day and
 * freezes by night, while anyone underground or in shade is insulated — so the
 * planet is survivable only by sheltering, which is exactly how you would live
 * on the real Mercury (and why its ice survives in permanently-shadowed
 * craters). Day/night here is the real 176-day solar cycle, so whichever side
 * you land on, you had better get out of the open.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class MercuryEnvironment {
    private static final ResourceLocation MERCURY =
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "mercury");
    /** How often exposure is checked, in ticks. */
    private static final int INTERVAL = 20;

    private MercuryEnvironment() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().location().equals(MERCURY)) {
            return;
        }
        if (level.getGameTime() % INTERVAL != 0) {
            return;
        }

        boolean day = level.isDay();
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            // Insulated underground or in shade — the only way to survive.
            if (!level.canSeeSky(player.blockPosition())) {
                continue;
            }

            if (day) {
                // Scorching day side: heat builds and burns.
                player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 80));
                player.hurt(level.damageSources().onFire(), 1.0f);
            } else {
                // Frozen night side: the powder-snow freeze plus a chilling toll.
                player.setTicksFrozen(Math.min(player.getTicksFrozen() + INTERVAL + 20, 300));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
                player.hurt(level.damageSources().freeze(), 1.0f);
            }
        }
    }
}
