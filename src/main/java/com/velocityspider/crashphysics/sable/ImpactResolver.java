package com.velocityspider.crashphysics.sable;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.damage.BlockDamageTracker;
import com.velocityspider.crashphysics.damage.Destruction;
import com.velocityspider.crashphysics.physics.ContactMechanics;
import com.velocityspider.crashphysics.physics.PenetrationSolver;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

/**
 * Applies the outcome of the contacts {@link ImpactCollector} recorded, once Rapier has finished a sub-step.
 * <p>
 * Contacts the vehicle carried on through were dropped from the solver, so their resisting force is applied here: an
 * impulse at the contact, plus friction from ploughing through the material. Contacts that held were stopped by Rapier
 * itself. Either way the crushed depth is added to each block's damage, and blocks crushed through break, which is
 * what carves craters and trenches and crumples hulls.
 */
final class ImpactResolver {

    private final ServerLevel level;
    private final LevelCrashState state;

    private final Vector3d impulse = new Vector3d();
    private final Vector3d local = new Vector3d();
    private final Vector3d point = new Vector3d();
    private final Vector3d velocity = new Vector3d();
    private final Vector3d velocityB = new Vector3d();
    private final Vector3d angular = new Vector3d();

    ImpactResolver(final ServerLevel level, final LevelCrashState state) {
        this.level = level;
        this.state = state;
    }

    void resolve(final SubLevelPhysicsSystem system, final List<ContactRecord> records, final List<ImpactCollector.SkidMark> skids) {
        if (!records.isEmpty()) {
            this.resolveContacts(system, records);
        }
        if (!skids.isEmpty()) {
            this.state.destruction().skid(skids.stream().map(skid -> new Destruction.Skid(BlockPos.of(skid.pos()), skid.state(), skid.into(), skid.speed())).toList());
        }

        // Break blocks now, so the next sub-step already sees the holes
        this.state.destruction().process();
    }

    private void resolveContacts(final SubLevelPhysicsSystem system, final List<ContactRecord> records) {
        final double kgPerSableMass = CrashConfig.KG_PER_SABLE_MASS.getAsDouble();
        final double plowFriction = CrashConfig.PLOW_FRICTION.getAsDouble();
        final double breakDepth = CrashConfig.BREAK_DEPTH.getAsDouble();
        final boolean debug = CrashConfig.DEBUG_LOGGING.getAsBoolean();
        final BlockDamageTracker damage = this.state.damage();
        final Destruction destruction = this.state.destruction();

        for (final ContactRecord record : records) {
            this.state.recordImpact(record.vehicleA, record.keyA);
            if (record.vehicleB != null) {
                this.state.recordImpact(record.vehicleB, record.keyB);
            }

            final ContactMechanics.Step step = record.step;
            if (!record.crushing) {
                // Hard, but nothing gave way: Rapier stopped it. Still worth a sound.
                final double energy = ContactMechanics.kineticEnergy(record.effectiveMassKg, record.closingSpeed);
                this.state.effects().recordImpact(record.vehicleA, record.px, record.py, record.pz, energy * 0.5, record.materialA.fracture(), record.materialB.fracture(), record.stateB);
                continue;
            }

            if (record.passThrough) {
                this.applyResistance(system, record, step, kgPerSableMass, plowFriction);
            }

            this.state.effects().recordImpact(record.vehicleA, record.px, record.py, record.pz, step.dissipatedEnergy(), record.materialA.fracture(), record.materialB.fracture(), record.stateB);

            if (debug) {
                CrashPhysics.LOGGER.info("Impact {} -> {} at {} m/s: {} kJ, crushed {} m into B and {} m of A, {}",
                        record.stateA, record.stateB, String.format("%.1f", record.closingSpeed), String.format("%.1f", step.dissipatedEnergy() / 1000.0),
                        String.format("%.3f", step.targetErosion()), String.format("%.3f", step.projectileErosion()),
                        step.targetFailed() ? "B gave way" : step.projectileFailed() ? "A gave way" : record.passThrough ? "carried on" : "held");
            }

            // Damage. A block the step crushed through breaks even if rounding leaves it a hair short.
            if (record.damageB && (step.targetErosion() > 0.0 || step.targetFailed())) {
                final boolean breaks = damage.addErosion(record.keyB, step.targetErosion(), breakDepth * ImpactCollector.blockDepth(record.stateB),
                        record.vehicleB == null, this.state.tick()) || step.targetFailed();
                if (breaks) {
                    damage.forget(record.keyB);
                    final BlockPos posB = BlockPos.of(record.keyB);
                    if (record.vehicleB == null) {
                        final double penetrationSpeed = PenetrationSolver.solve(record.closingSpeed, record.profileA, record.profileB).targetErosionSpeed();
                        destruction.breakTerrain(posB, record.stateB, record.materialB, record.vx, record.vy, record.vz, penetrationSpeed);
                    } else {
                        destruction.breakVehicleBlock(record.vehicleB, posB, record.stateB, record.materialB, record.closingSpeed > 35.0);
                    }
                }
            }

            if (record.damageA && (step.projectileErosion() > 0.0 || step.projectileFailed())) {
                final boolean breaks = damage.addErosion(record.keyA, step.projectileErosion(), breakDepth * ImpactCollector.blockDepth(record.stateA),
                        false, this.state.tick()) || step.projectileFailed();
                if (breaks) {
                    damage.forget(record.keyA);
                    // A block crushed at high speed is pulverised; slower crushing tears it off as wreckage
                    destruction.breakVehicleBlock(record.vehicleA, BlockPos.of(record.keyA), record.stateA, record.materialA, record.closingSpeed > 35.0);
                }
            }
        }
    }

    /**
     * Applies the resisting force of material the vehicle carried on through. Never more than it takes to stop the
     * contact as it is moving right now: other contacts on the same vehicle may already have slowed it down.
     */
    private void applyResistance(final SubLevelPhysicsSystem system, final ContactRecord record, final ContactMechanics.Step step,
                                 final double kgPerSableMass, final double plowFriction) {
        final Vector3d relative = this.pointVelocityNow(system, record.vehicleA, record.plotAx, record.plotAy, record.plotAz, this.velocity);
        if (relative == null) {
            return;
        }
        if (record.vehicleB != null) {
            final Vector3d velocityOfB = this.pointVelocityNow(system, record.vehicleB, record.plotBx, record.plotBy, record.plotBz, this.velocityB);
            if (velocityOfB != null) {
                relative.sub(velocityOfB);
            }
        }

        final double vn = relative.x * record.nx + relative.y * record.ny + relative.z * record.nz;
        final double closingNow = -vn;
        if (!(closingNow > 0.0)) {
            return;
        }
        final double normalImpulse = Math.min(step.impulse(), record.effectiveMassKg * closingNow);

        // Ploughing through material also drags against the sliding motion
        final double tx = relative.x - vn * record.nx;
        final double ty = relative.y - vn * record.ny;
        final double tz = relative.z - vn * record.nz;
        final double tangentialSpeed = Math.sqrt(tx * tx + ty * ty + tz * tz);
        final double frictionImpulse = tangentialSpeed > 1.0e-6 ? Math.min(plowFriction * normalImpulse, record.effectiveMassKg * tangentialSpeed) : 0.0;

        final Vector3d total = this.impulse.set(record.nx, record.ny, record.nz).mul(normalImpulse);
        if (frictionImpulse > 0.0) {
            total.sub(tx / tangentialSpeed * frictionImpulse, ty / tangentialSpeed * frictionImpulse, tz / tangentialSpeed * frictionImpulse);
        }
        total.div(kgPerSableMass);
        if (!total.isFinite()) {
            return;
        }

        this.applyImpulse(system, record.vehicleA, record.plotAx, record.plotAy, record.plotAz, total);
        if (record.vehicleB != null) {
            total.negate();
            this.applyImpulse(system, record.vehicleB, record.plotBx, record.plotBy, record.plotBz, total);
        }
    }

    /**
     * Current world-space velocity of a point fixed to a vehicle, given in plot coordinates [m/s].
     */
    @Nullable
    private Vector3d pointVelocityNow(final SubLevelPhysicsSystem system, final ServerSubLevel vehicle,
                                      final double plotX, final double plotY, final double plotZ, final Vector3d dest) {
        if (vehicle.isRemoved()) {
            return null;
        }
        final RigidBodyHandle handle = system.getPhysicsHandle(vehicle);
        if (handle == null || !handle.isValid()) {
            return null;
        }

        final Pose3dc pose = vehicle.logicalPose();
        final Vector3d world = pose.transformPosition(this.point.set(plotX, plotY, plotZ));
        final Vector3dc com = pose.position();
        final double rx = world.x - com.x();
        final double ry = world.y - com.y();
        final double rz = world.z - com.z();

        handle.getLinearVelocity(dest);
        final Vector3d omega = handle.getAngularVelocity(this.angular);
        return dest.add(omega.y * rz - omega.z * ry, omega.z * rx - omega.x * rz, omega.x * ry - omega.y * rx);
    }

    private void applyImpulse(final SubLevelPhysicsSystem system, final ServerSubLevel vehicle, final double plotX, final double plotY, final double plotZ,
                              final Vector3d worldImpulse) {
        if (vehicle.isRemoved()) {
            return;
        }

        final RigidBodyHandle handle = system.getPhysicsHandle(vehicle);
        if (handle == null || !handle.isValid()) {
            return;
        }

        vehicle.logicalPose().transformNormalInverse(worldImpulse, this.local);
        handle.applyImpulseAtPoint(this.point.set(plotX, plotY, plotZ), this.local);
    }
}
