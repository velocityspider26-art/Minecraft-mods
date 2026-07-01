package com.velocityspider.radarballistics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.happysg.radar.compat.cbc.CannonLead;
import com.velocityspider.radarballistics.integration.cbc.CbcBridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;

/**
 * Replaces Create Radar's moving-target lead solver with this mod's coupled intercept solver.
 *
 * <p>The original iterates a flight-time estimate against an approximate projectile sim whose
 * drag handling it even flags as possibly wrong. We instead solve the lead point, elevation
 * and time-of-flight together using CBC's exact per-tick physics (target velocity AND
 * acceleration, muzzle speed, gravity, linear/quadratic drag). On any failure we return
 * nothing so the original solver still runs.</p>
 */
@Mixin(value = CannonLead.class, remap = false)
public abstract class CannonLeadMixin {

    @Inject(method = "solveLeadPerTickWithAcceleration", at = @At("HEAD"), cancellable = true, remap = false)
    private static void radarballistics$accurateLead(CannonMountBlockEntity mount,
                                                     AbstractMountedCannonContraption cannon,
                                                     ServerLevel level,
                                                     Vec3 shooterVelPerTick,
                                                     Vec3 shooterAccelPerTick2,
                                                     Vec3 targetPosNow,
                                                     Vec3 targetVelPerTick,
                                                     Vec3 targetAccelPerTick2,
                                                     int fireDelayTicks,
                                                     double latencyTicks,
                                                     double maxSimDistanceBlocks,
                                                     CallbackInfoReturnable<CannonLead.LeadSolution> cir) {
        CannonLead.LeadSolution solution = CbcBridge.solveLead(
                mount, cannon, level,
                shooterVelPerTick, shooterAccelPerTick2,
                targetPosNow, targetVelPerTick, targetAccelPerTick2,
                fireDelayTicks, latencyTicks);
        if (solution != null) {
            cir.setReturnValue(solution);
        }
    }
}
