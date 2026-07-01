package com.velocityspider.radarballistics.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.happysg.radar.compat.cbc.CannonTargeting;
import com.velocityspider.radarballistics.integration.cbc.CbcBridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

/**
 * Replaces Create Radar's barrel-elevation solver with the drag-accurate one from this mod.
 *
 * <p>The original {@code CannonTargeting.calculatePitch} uses a continuous-time closed form
 * that treats CBC's per-tick drag as a continuous coefficient and ignores quadratic drag,
 * so the elevation is systematically off (worse at longer range / higher drag). We intercept
 * the call and return a pitch found by simulating CBC's exact discrete flight; if our solver
 * can't produce one (out of range, laser cannon, or the feature is disabled in config) we do
 * nothing and let the original run.</p>
 */
@Mixin(value = CannonTargeting.class, remap = false)
public abstract class CannonTargetingMixin {

    @Inject(
            method = "calculatePitch(Lrbasamoyai/createbigcannons/cannon_control/cannon_mount/CannonMountBlockEntity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/server/level/ServerLevel;)Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void radarballistics$accuratePitch(CannonMountBlockEntity mount, Vec3 originPos,
                                                      Vec3 targetPos, ServerLevel level,
                                                      CallbackInfoReturnable<List<Double>> cir) {
        List<Double> solution = CbcBridge.solvePitch(mount, originPos, targetPos, level);
        if (solution != null) {
            cir.setReturnValue(solution);
        }
    }
}
