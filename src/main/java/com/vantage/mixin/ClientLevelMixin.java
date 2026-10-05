package com.vantage.mixin;

import com.vantage.client.VantageClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block and light-enable notifications. Hooked here rather than in LevelRenderer so it keeps
 * working when another renderer (e.g. Sodium) replaces the vanilla section bookkeeping.
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
}
