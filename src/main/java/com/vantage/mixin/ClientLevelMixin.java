package com.vantage.mixin;

import com.vantage.client.VantageClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Block and light-enable notifications, hooked here rather than in LevelRenderer so they keep
 * working when another renderer (e.g. Sodium) replaces the vanilla section bookkeeping; and the
 * space sky: a darker sky and daytime stars high above the atmosphere.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void vantage$blockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        VantageClient.onSectionDirty(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Inject(method = "setBlocksDirty", at = @At("HEAD"))
    private void vantage$blocksDirty(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        VantageClient.onSectionDirty(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Inject(method = "setSectionDirtyWithNeighbors", at = @At("HEAD"))
    private void vantage$sectionDirty(int sectionX, int sectionY, int sectionZ, CallbackInfo ci) {
        VantageClient.onSectionDirty(sectionX, sectionZ);
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void vantage$spaceSky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (VantageClient.computingFogColor) {
            return;
        }
        float d = VantageClient.spaceDarkness(pos.y);
        if (d > 0f) {
            cir.setReturnValue(cir.getReturnValue().scale(1.0 - d));
        }
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
    private void vantage$spaceStars(float partialTick, CallbackInfoReturnable<Float> cir) {
        float d = VantageClient.spaceDarkness(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y);
        if (d > cir.getReturnValueF()) {
            cir.setReturnValue(d);
        }
    }
}
