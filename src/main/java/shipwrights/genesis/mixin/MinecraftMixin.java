package shipwrights.genesis.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.dimension.SubDimensions;

/**
 * Measures the complete client handoff between Genesis rooms.
 *
 * <p>This mixin intentionally does not replace screens. The old implementation
 * intercepted {@code setScreen}, captured the framebuffer, and replaced
 * NeoForge's registered receiving screen with a legacy {@code TransitionScreen}
 * whose completion supplier never became true. That defeated the seamless
 * screen path and added GPU work directly inside {@code Minecraft.setLevel}.</p>
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Unique
    private long genesis$roomTransitionStartedNanos;

    @Unique
    private ResourceKey<Level> genesis$roomTransitionFrom;

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void genesis$beginRoomTransition(ClientLevel newLevel,
                                             ReceivingLevelScreen.Reason reason,
                                             CallbackInfo callback) {
        ClientLevel oldLevel = Minecraft.getInstance().level;
        if (oldLevel != null && SubDimensions.sameFloorplan(oldLevel, newLevel)) {
            genesis$roomTransitionStartedNanos = System.nanoTime();
            genesis$roomTransitionFrom = oldLevel.dimension();
        } else {
            genesis$roomTransitionStartedNanos = 0L;
            genesis$roomTransitionFrom = null;
        }
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void genesis$finishRoomTransition(ClientLevel newLevel,
                                              ReceivingLevelScreen.Reason reason,
                                              CallbackInfo callback) {
        if (genesis$roomTransitionStartedNanos == 0L) {
            return;
        }

        long elapsedMillis = (System.nanoTime() - genesis$roomTransitionStartedNanos) / 1_000_000L;
        GenesisMod.LOGGER.info(
                "[TRANSITION] client room handoff {} -> {} completed in {} ms",
                genesis$roomTransitionFrom.location(),
                newLevel.dimension().location(),
                elapsedMillis);

        genesis$roomTransitionStartedNanos = 0L;
        genesis$roomTransitionFrom = null;
    }
}
