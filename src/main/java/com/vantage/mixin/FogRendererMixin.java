package com.vantage.mixin;

import com.vantage.client.VantageClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks the fog colour computation, which blends in the sky colour: the space sky darkens the
 * finished fog colour itself, and the ground's haze keeps the undarkened one.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "setupColor", at = @At("HEAD"))
    private static void vantage$fogColorStart(Camera camera, float partialTicks, ClientLevel level, int renderDistanceChunks,
                                              float bossColorModifier, CallbackInfo ci) {
        VantageClient.computingFogColor = true;
    }

    @Inject(method = "setupColor", at = @At("RETURN"))
    private static void vantage$fogColorEnd(Camera camera, float partialTicks, ClientLevel level, int renderDistanceChunks,
                                            float bossColorModifier, CallbackInfo ci) {
        VantageClient.computingFogColor = false;
    }
}
