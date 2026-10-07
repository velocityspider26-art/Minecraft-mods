package com.velocityspider.crashphysics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.velocityspider.crashphysics.sable.CrashCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives every solid block state a collision callback when Sable bakes it into the Rapier physics world, so that every
 * impact between a vehicle and a block reaches crash physics. Whatever callback the block already had keeps working.
 */
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderBakery", remap = false)
public abstract class RapierVoxelColliderBakeryMixin {

    @WrapOperation(
            method = "buildPhysicsDataForBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/api/block/BlockWithSubLevelCollisionCallback;sable$getCallback(Lnet/minecraft/world/level/block/state/BlockState;)Ldev/ryanhcode/sable/api/physics/callback/BlockSubLevelCollisionCallback;"
            ),
            remap = false
    )
    private BlockSubLevelCollisionCallback crashphysics$installCallback(final BlockState state, final Operation<BlockSubLevelCollisionCallback> original) {
        final BlockSubLevelCollisionCallback existing = original.call(state);

        // Air and fluids never take part in solid contacts
        if (state.isAir() || (!state.getFluidState().isEmpty() && state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty())) {
            return existing;
        }

        return new CrashCollisionCallback(state, existing);
    }
}
