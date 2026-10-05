package com.vantage.mixin;

import com.vantage.client.VantageClient;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Light changes alter LOD data (face lighting and cave detection), so they count as changes too. */
@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {
    @Inject(method = "onLightUpdate", at = @At("HEAD"))
    private void vantage$lightUpdated(LightLayer layer, SectionPos pos, CallbackInfo ci) {
        VantageClient.onSectionDirty(pos.x(), pos.z());
    }
}
