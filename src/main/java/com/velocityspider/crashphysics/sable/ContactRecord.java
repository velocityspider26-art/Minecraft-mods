package com.velocityspider.crashphysics.sable;

import com.velocityspider.crashphysics.material.CrashMaterial;
import com.velocityspider.crashphysics.physics.ContactMechanics;
import com.velocityspider.crashphysics.physics.MaterialProfile;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * One block-on-block contact seen during a physics sub-step.
 * <p>
 * Side A is always a block on a vehicle. Side B is either terrain ({@link #vehicleB} is null) or a block on another
 * vehicle.
 */
final class ContactRecord {
    final long keyA;
    final long keyB;

    final ServerSubLevel vehicleA;
    @Nullable
    final ServerSubLevel vehicleB;

    final BlockState stateA;
    final BlockState stateB;
    final CrashMaterial materialA;
    final CrashMaterial materialB;

    /**
     * Profiles used for the solve, including multipliers; unbreakable if that side may not be damaged
     */
    final MaterialProfile profileA;
    final MaterialProfile profileB;
    final boolean damageA;
    final boolean damageB;

    /**
     * Contact point in world space and in each vehicle's plot
     */
    final double px, py, pz;
    final double plotAx, plotAy, plotAz;
    final double plotBx, plotBy, plotBz;

    /**
     * Contact normal pointing from B into A [unit vector]
     */
    final double nx, ny, nz;

    /**
     * Relative velocity of A with respect to B at the contact [m/s]
     */
    final double vx, vy, vz;

    final double closingSpeed;

    /**
     * Reduced effective mass of the two bodies at the contact, along the normal [kg]
     */
    final double effectiveMassKg;

    /**
     * What the contact does over the sub-step, for the share of the vehicle's mass it carries
     */
    final ContactMechanics.Step step;

    /**
     * If the material is giving way, so the contact does damage
     */
    final boolean crushing;

    /**
     * If Rapier was told to drop the contact: the vehicle carries on into the material and the resisting force is
     * applied after the step. Otherwise Rapier brings the contact to rest itself and only the damage is applied.
     */
    final boolean passThrough;

    ContactRecord(final long keyA, final long keyB, final ServerSubLevel vehicleA, @Nullable final ServerSubLevel vehicleB,
                  final BlockState stateA, final BlockState stateB, final CrashMaterial materialA, final CrashMaterial materialB,
                  final MaterialProfile profileA, final MaterialProfile profileB, final boolean damageA, final boolean damageB,
                  final double px, final double py, final double pz,
                  final double plotAx, final double plotAy, final double plotAz,
                  final double plotBx, final double plotBy, final double plotBz,
                  final double nx, final double ny, final double nz,
                  final double vx, final double vy, final double vz,
                  final double closingSpeed, final double effectiveMassKg, final ContactMechanics.Step step, final boolean passThrough) {
        this.keyA = keyA;
        this.keyB = keyB;
        this.vehicleA = vehicleA;
        this.vehicleB = vehicleB;
        this.stateA = stateA;
        this.stateB = stateB;
        this.materialA = materialA;
        this.materialB = materialB;
        this.profileA = profileA;
        this.profileB = profileB;
        this.damageA = damageA;
        this.damageB = damageB;
        this.px = px;
        this.py = py;
        this.pz = pz;
        this.plotAx = plotAx;
        this.plotAy = plotAy;
        this.plotAz = plotAz;
        this.plotBx = plotBx;
        this.plotBy = plotBy;
        this.plotBz = plotBz;
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.closingSpeed = closingSpeed;
        this.effectiveMassKg = effectiveMassKg;
        this.step = step;
        this.crushing = step != ContactMechanics.Step.NONE;
        this.passThrough = passThrough;
    }

    boolean matches(final long a, final long b) {
        return (this.keyA == a && this.keyB == b) || (this.keyA == b && this.keyB == a);
    }

    /**
     * Speed of A sliding along B, perpendicular to the normal [m/s]
     */
    double tangentialSpeed() {
        final double vn = this.vx * this.nx + this.vy * this.ny + this.vz * this.nz;
        final double tx = this.vx - vn * this.nx;
        final double ty = this.vy - vn * this.ny;
        final double tz = this.vz - vn * this.nz;
        return Math.sqrt(tx * tx + ty * ty + tz * tz);
    }
}
