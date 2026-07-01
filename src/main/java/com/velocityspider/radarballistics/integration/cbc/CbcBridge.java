package com.velocityspider.radarballistics.integration.cbc;

import java.util.List;

import com.happysg.radar.compat.cbc.CannonLead;
import com.happysg.radar.compat.cbc.CannonUtil;
import com.velocityspider.radarballistics.Config;
import com.velocityspider.radarballistics.ballistics.BallisticSolver;
import com.velocityspider.radarballistics.ballistics.FireSolution;
import com.velocityspider.radarballistics.ballistics.ProjectileProfile;
import com.velocityspider.radarballistics.ballistics.TargetState;
import com.velocityspider.radarballistics.ballistics.Vec3d;
import com.happysg.radar.compat.vs2.PhysicsHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;

/**
 * Adapts Create: Big Cannons' real cannon/projectile data to this mod's verified fire-control
 * engine. The two mixins ({@link CannonTargetingMixinInfo the pitch solver} and the lead
 * solver) delegate here so the accurate, drag-correct math replaces Create Radar's original
 * calculations.
 *
 * <p>All physics inputs come straight from the loaded cannon via Create Radar's own
 * {@code CannonUtil}, so the muzzle speed (charge count), gravity, drag and quadratic-drag
 * flag are exactly what CBC will fly the shell with.</p>
 */
public final class CbcBridge {

    private static final BallisticSolver SOLVER = BallisticSolver.standard();
    private static final java.util.concurrent.atomic.AtomicBoolean ANNOUNCED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private CbcBridge() {
    }

    /** Logs once, the first time the corrected fire-control actually runs, to confirm it is live. */
    private static void announceOnce() {
        if (ANNOUNCED.compareAndSet(false, true)) {
            com.velocityspider.radarballistics.RadarBallistics.LOGGER.info(
                    "Create Radar Ballistics: accurate lead/elevation fire-control is now driving cannon auto-aim.");
        }
    }

    static Vec3d vec(Vec3 v) {
        return new Vec3d(v.x, v.y, v.z);
    }

    static Vec3 mc(Vec3d v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    private static AbstractMountedCannonContraption cannonOf(CannonMountBlockEntity mount) {
        if (mount == null) {
            return null;
        }
        PitchOrientedContraptionEntity poce = mount.getContraption();
        if (poce != null && poce.getContraption() instanceof AbstractMountedCannonContraption c) {
            return c;
        }
        return null;
    }

    private static ProjectileProfile profileOf(AbstractMountedCannonContraption cannon, ServerLevel level) {
        double muzzle = CannonUtil.getInitialVelocity(cannon, level);
        if (muzzle <= 0) {
            return null;
        }
        BallisticPropertiesComponent bp = CannonUtil.getBallistics(cannon, level);
        double gravity = bp != null ? bp.gravity() : CannonUtil.getProjectileGravity(cannon, level);
        double drag = bp != null ? bp.drag() : CannonUtil.getProjectileDrag(cannon, level);
        boolean quad = bp != null && bp.isQuadraticDrag();
        return ProjectileProfile.ofCbc(muzzle, gravity, drag, quad);
    }

    /**
     * Accurate replacement for {@code CannonTargeting.calculatePitch}: the barrel elevation
     * (degrees above horizontal) that lands a shell of this cannon's real muzzle speed, gravity
     * and drag on a static point, found by simulating CBC's exact per-tick flight.
     *
     * @return a single-element list holding the low-arc pitch in degrees, or {@code null} to
     *         let Create Radar's original calculation run (out of range, laser, or disabled)
     */
    public static List<Double> solvePitch(CannonMountBlockEntity mount, Vec3 originPos, Vec3 targetPos, ServerLevel level) {
        if (!Config.ENABLE_AUTO_TRACK_LEAD.get()) {
            return null;
        }
        AbstractMountedCannonContraption cannon = cannonOf(mount);
        if (cannon == null || CannonUtil.isLaserCannon(cannon)) {
            return null;
        }
        ProjectileProfile profile = profileOf(cannon, level);
        if (profile == null) {
            return null;
        }
        FireSolution sol = SOLVER.solve(vec(originPos), TargetState.stationary(vec(targetPos)), profile);
        if (!sol.converged()) {
            return null;
        }
        announceOnce();
        return List.of(sol.elevationDegrees());
    }

    /**
     * Accurate replacement for {@code CannonLead.solveLeadPerTickWithAcceleration}: solves the
     * moving-target intercept (lead point, pitch, yaw, time-of-flight) using the target's
     * velocity and acceleration and the shell's real ballistics, all in tick units.
     *
     * <p>The shooter's own motion (e.g. a Valkyrien Skies ship) is folded in as relative target
     * motion, and the fire delay plus radar latency are applied as an extra lead time on top of
     * the computed flight time.</p>
     *
     * @return a lead solution, or {@code null} to fall back to Create Radar's original solver
     */
    public static CannonLead.LeadSolution solveLead(
            CannonMountBlockEntity mount,
            AbstractMountedCannonContraption cannon,
            ServerLevel level,
            Vec3 shooterVelPerTick,
            Vec3 shooterAccelPerTick2,
            Vec3 targetPosNow,
            Vec3 targetVelPerTick,
            Vec3 targetAccelPerTick2,
            int fireDelayTicks,
            double latencyTicks) {

        if (!Config.ENABLE_AUTO_TRACK_LEAD.get()) {
            return null;
        }
        if (mount == null || cannon == null || level == null
                || targetPosNow == null || targetVelPerTick == null || targetAccelPerTick2 == null) {
            return null;
        }
        if (CannonUtil.isLaserCannon(cannon)) {
            return null;
        }
        ProjectileProfile profile = profileOf(cannon, level);
        if (profile == null) {
            return null;
        }

        Vec3 shooterVel = shooterVelPerTick != null ? shooterVelPerTick : Vec3.ZERO;
        Vec3 shooterAccel = shooterAccelPerTick2 != null ? shooterAccelPerTick2 : Vec3.ZERO;

        // Origin: the mount's controller position lifted to the barrel pivot, VS2-aware.
        Vec3 originNow = PhysicsHandler.getWorldVec(level, mount.getControllerBlockPos().above(2).getCenter());

        // Work in the shooter's frame: relative target motion so a moving mount is handled.
        Vec3d launcher = vec(originNow);
        Vec3d relVel = vec(targetVelPerTick.subtract(shooterVel));
        Vec3d relAccel = vec(targetAccelPerTick2.subtract(shooterAccel));

        // Pre-advance the target by the (constant) fire delay + radar latency; the solver adds
        // the flight-time lead on top of that.
        double pre = fireDelayTicks + latencyTicks;
        Vec3d startPos = vec(targetPosNow)
                .add(relVel.scale(pre))
                .add(relAccel.scale(0.5 * pre * pre));

        TargetState target = new TargetState(startPos, relVel, relAccel);
        FireSolution sol = SOLVER.solve(launcher, target, profile);
        if (!sol.converged()) {
            return null;
        }

        Vec3 aimPoint = mc(sol.aimPoint());
        double pitchDeg = sol.elevationDegrees();
        // yaw in CannonLead's math convention: atan2(dz, dx) from origin to the aim point.
        Vec3d toAim = sol.aimPoint().subtract(launcher);
        double yawRad = Math.atan2(toAim.z(), toAim.x());
        int flightTicks = (int) Math.round(sol.timeToImpactTicks());

        announceOnce();
        return new CannonLead.LeadSolution(aimPoint, pitchDeg, yawRad, flightTicks);
    }

    /** Marker javadoc anchor for the mixin classes; keeps IDEs from flagging the import path. */
    interface CannonTargetingMixinInfo {
    }
}
