package com.velocityspider.radarballistics.mixin;

// -----------------------------------------------------------------------------------------
// INTEGRATION TEMPLATE — NOT COMPILED BY THE DEFAULT BUILD.
//
// This file lives outside src/main/java on purpose: it references Create Radar and
// Create: Big Cannons classes that are only present once you add those mods as
// dependencies (see docs/INTEGRATION.md). To use it:
//   1. Add the CBC / Create Radar / Create dependencies in build.gradle.
//   2. Move this file to src/main/java/com/velocityspider/radarballistics/mixin/.
//   3. Replace the CONFIRM-ME class/method names below with the real ones from the
//      versions you build against (they change between releases), then register the
//      mixin config as described in docs/INTEGRATION.md.
//
// The point of this template is that all the hard part — the actual ballistics — is done
// and verified; this is just the thin adapter that feeds the solver the live target and
// applies the resulting angles to the cannon mount.
// -----------------------------------------------------------------------------------------

import com.velocityspider.radarballistics.Config;
import com.velocityspider.radarballistics.FireControl;
import com.velocityspider.radarballistics.ballistics.FireSolution;
import com.velocityspider.radarballistics.ballistics.TargetState;

import net.minecraft.world.phys.Vec3;

// CONFIRM-ME: import the real Create Radar auto-aim controller class.
// import com.your.createradar.AutoAimController;

/*
 * @Mixin(AutoAimController.class)  // CONFIRM-ME: the Create Radar class that computes the aim
 */
public abstract class AutoAimMixin {

    /*
     * Hook A — replace the vanilla "aim at current position" with a lead intercept.
     *
     * Inject at the point where the controller has resolved the muzzle position and the
     * tracked target, and is about to compute the yaw/pitch to command the mount. Overwrite
     * (or @ModifyVariable) the commanded angles with the solver's output.
     *
     * @Inject(method = "computeAim", at = @At("RETURN"), cancellable = true)  // CONFIRM-ME
     * private void radarballistics$leadTarget(CallbackInfoReturnable<AimAngles> cir) {
     *     if (!Config.ENABLE_AUTO_TRACK_LEAD.get()) return;
     *
     *     Vec3 muzzle       = this.getMuzzleWorldPosition();     // CONFIRM-ME
     *     Vec3 targetPos    = this.getTrackedTargetPosition();   // CONFIRM-ME
     *     Vec3 targetVel    = this.getTrackedTargetVelocity();   // CONFIRM-ME (per-tick delta)
     *
     *     FireSolution sol = FireControl.solve(
     *             FireControl.toEngine(muzzle),
     *             new TargetState(FireControl.toEngine(targetPos), FireControl.toEngine(targetVel)));
     *
     *     if (sol.converged()) {
     *         cir.setReturnValue(new AimAngles(sol.yawDegrees(), sol.pitchDegrees())); // CONFIRM-ME
     *     }
     *     // If it doesn't converge (out of range) leave the vanilla aim untouched.
     * }
     */

    // Hook B (projectile model) is usually done outside a mixin: when CBC fires the shell,
    // read its muzzle speed/gravity/drag and push them into Config (or pass a per-shot
    // ProjectileProfile). See docs/INTEGRATION.md section 3.

    // Referenced so an IDE flags these as the pieces to wire up, not unused imports.
    static {
        assert Config.class != null && FireControl.class != null
                && FireSolution.class != null && TargetState.class != null && Vec3.class != null;
    }
}
