package com.velocityspider.crashphysics.mixin;

import com.velocityspider.crashphysics.sable.CrashTickHandler;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells crash physics about every block that changes on a vehicle, so it can keep its picture of each vehicle's shape
 * up to date block by block instead of scanning the whole vehicle again.
 */
@Mixin(value = SubLevelPhysicsSystem.class, remap = false)
public abstract class SubLevelPhysicsSystemMixin {

    @Shadow
    public abstract ServerLevel getLevel();

    @Inject(method = "handleBlockChange", at = @At("HEAD"), remap = false)
    private void crashphysics$blockChanged(final SectionPos section, final LevelChunkSection chunkSection, final int x, final int y, final int z,
                                           final BlockState oldState, final BlockState newState, final CallbackInfo ci) {
        CrashTickHandler.onBlockChanged(this.getLevel(), (section.x() << 4) + x, (section.y() << 4) + y, (section.z() << 4) + z, oldState, newState);
    }
}
