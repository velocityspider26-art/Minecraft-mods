package com.velocityspider.crashphysics.sable;

import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.material.CrashMaterial;
import com.velocityspider.crashphysics.material.CrashMaterials;
import com.velocityspider.crashphysics.physics.ContactMechanics;
import com.velocityspider.crashphysics.physics.MaterialProfile;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides, while Sable's physics engine is stepping, which contacts are crushing material.
 * <p>
 * Sable calls block collision callbacks from inside the Rapier solver, once for each side of every contact point. Each
 * hard contact is stepped through the sub-step with the material model. If the material can't bring the vehicle to rest
 * within the sub-step, or a block is crushed through, the callback tells Rapier to drop the contact so the vehicle keeps
 * moving into the material; the resisting force is then applied by {@link ImpactResolver} once the step is over. If
 * the material does stop it, Rapier keeps the contact and stops the vehicle itself, and only the crushing damage is
 * applied. Nothing in here may call back into the physics engine.
 */
final class ImpactCollector {

    /**
     * Contacts at least this hard are recorded for sounds, crew injuries and structural checks even if nothing crushes
     */
    private static final double RECORD_SPEED = 3.0;

    /**
     * Contact area of two block faces [m²]
     */
    static final double CONTACT_AREA = 1.0;

    /**
     * Below this sliding speed nothing leaves skid marks [m/s]
     */
    private static final double SKID_SPEED = 6.0;

    private static final MaterialProfile UNKNOWN = new MaterialProfile(2500.0, 2.0e7, 2.0e6, false);

    private final ServerLevel level;
    private final LevelCrashState state;

    private final Long2ObjectOpenHashMap<ContactRecord> pairs = new Long2ObjectOpenHashMap<>();
    private List<ContactRecord> records = new ArrayList<>();
    private Long2IntOpenHashMap bodyPairsPrevious = new Long2IntOpenHashMap();
    private Long2IntOpenHashMap bodyPairsCurrent = new Long2IntOpenHashMap();
    private final LongOpenHashSet skidPositions = new LongOpenHashSet();
    private final List<SkidMark> skids = new ArrayList<>();

    // Settings, read once per sub-step
    private boolean active;
    private double dt;
    private double minImpactSpeed;
    private double kgPerSableMass;
    private double strengthMultiplier;
    private double vehicleStrengthMultiplier;
    private double elasticTravel;
    private double breakDepth;
    private boolean worldDamage;
    private boolean vehicleDamage;
    private boolean skidMarks;

    // Diagnostics: callbacks seen, contacts fast enough to matter, contacts evaluated, contacts crushing material, and
    // crushing contacts the vehicle carried on through
    long statCallbacks;
    long statFast;
    long statEvaluated;
    long statCrushing;
    long statThrough;

    // Scratch space, only touched while holding the lock
    private final Vector3d scratchA = new Vector3d();
    private final Vector3d scratchB = new Vector3d();
    private final Vector3d scratchC = new Vector3d();
    private final Vector3d scratchD = new Vector3d();
    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    record SkidMark(long pos, BlockState state, BlockState into, double speed) {
    }

    ImpactCollector(final ServerLevel level, final LevelCrashState state) {
        this.level = level;
        this.state = state;
    }

    synchronized void beginSubstep(final double timeStep) {
        this.active = CrashConfig.isLoaded() && CrashConfig.ENABLED.getAsBoolean();
        this.dt = timeStep;

        if (this.active) {
            this.minImpactSpeed = CrashConfig.MIN_IMPACT_SPEED.getAsDouble();
            this.kgPerSableMass = CrashConfig.KG_PER_SABLE_MASS.getAsDouble();
            this.strengthMultiplier = CrashConfig.STRENGTH_MULTIPLIER.getAsDouble();
            this.vehicleStrengthMultiplier = CrashConfig.VEHICLE_STRENGTH_MULTIPLIER.getAsDouble();
            this.elasticTravel = CrashConfig.ELASTIC_TRAVEL.getAsDouble();
            this.breakDepth = CrashConfig.BREAK_DEPTH.getAsDouble();
            this.worldDamage = CrashConfig.WORLD_DAMAGE.getAsBoolean();
            this.vehicleDamage = CrashConfig.VEHICLE_DAMAGE.getAsBoolean();
            this.skidMarks = CrashConfig.SKID_MARKS.getAsBoolean();
        }

        this.pairs.clear();
        final Long2IntOpenHashMap previous = this.bodyPairsPrevious;
        this.bodyPairsPrevious = this.bodyPairsCurrent;
        this.bodyPairsCurrent = previous;
        this.bodyPairsCurrent.clear();
    }

    synchronized List<ContactRecord> drainRecords() {
        final List<ContactRecord> drained = this.records;
        this.records = new ArrayList<>();
        return drained;
    }

    synchronized List<SkidMark> drainSkids() {
        final List<SkidMark> drained = new ArrayList<>(this.skids);
        this.skids.clear();
        this.skidPositions.clear();
        return drained;
    }

    /**
     * Handles one side of one contact point.
     *
     * @param selfState    the state of the block this callback belongs to
     * @param x            the block this callback belongs to (plot coordinates if it is on a vehicle)
     * @param ox           the other block (plot coordinates if it is on a vehicle)
     * @param ix           the contact point, in the frame of this block
     * @param closingSpeed speed along the contact normal [m/s]
     * @param hasOther     if the other collider is made of blocks
     * @return if Rapier should drop this contact because the material is giving way under it
     */
    synchronized boolean onContact(final BlockState selfState, final int x, final int y, final int z,
                                   final int ox, final int oy, final int oz,
                                   final double ix, final double iy, final double iz,
                                   final double closingSpeed, final boolean hasOther) {
        this.statCallbacks++;
        if (!this.active || !hasOther) {
            return false;
        }

        if (closingSpeed < this.minImpactSpeed) {
            if (this.skidMarks) {
                this.checkSkid(selfState, x, y, z, ox, oz, ix, iy, iz);
            }
            return false;
        }

        this.statFast++;
        final long selfKey = BlockPos.asLong(x, y, z);
        final long otherKey = BlockPos.asLong(ox, oy, oz);
        final long pairKey = pairKey(selfKey, otherKey);

        final ContactRecord cached = this.pairs.get(pairKey);
        if (cached != null && cached.matches(selfKey, otherKey)) {
            return cached.passThrough;
        }

        final ContactRecord record = this.evaluate(selfState, x, y, z, ox, oy, oz, ix, iy, iz, closingSpeed);
        if (record == null) {
            return false;
        }
        this.statEvaluated++;
        if (record.crushing) {
            this.statCrushing++;
        }
        if (record.passThrough) {
            this.statThrough++;
        }

        this.pairs.put(pairKey, record);
        if (record.crushing || closingSpeed >= RECORD_SPEED) {
            this.records.add(record);
        }
        return record.passThrough;
    }

    @Nullable
    private ContactRecord evaluate(final BlockState selfState, final int x, final int y, final int z,
                                   final int ox, final int oy, final int oz,
                                   final double ix, final double iy, final double iz, final double closingSpeed) {
        final ServerSubLevel selfVehicle = this.vehicleAt(x, z);
        final ServerSubLevel otherVehicle = this.vehicleAt(ox, oz);

        if (selfVehicle == null && otherVehicle == null) {
            return null;
        }

        // Side A is a vehicle block, side B is whatever it hit
        final boolean selfIsA = selfVehicle != null;
        final ServerSubLevel vehicleA = selfIsA ? selfVehicle : otherVehicle;
        final ServerSubLevel vehicleB = selfIsA ? otherVehicle : null;

        final BodyMotion motionA = this.state.motion(vehicleA);
        final BodyMotion motionB = vehicleB != null ? this.state.motion(vehicleB) : null;
        if (motionA == null || (vehicleB != null && motionB == null)) {
            return null;
        }

        final int ax = selfIsA ? x : ox;
        final int ay = selfIsA ? y : oy;
        final int az = selfIsA ? z : oz;
        final int bx = selfIsA ? ox : x;
        final int by = selfIsA ? oy : y;
        final int bz = selfIsA ? oz : z;

        final BlockState stateA = selfIsA ? selfState : this.vehicleBlock(vehicleA, ax, ay, az);
        if (stateA == null || stateA.isAir()) {
            return null;
        }

        // Contact point in world space (the callback's point is in the frame of its own block, which is A if A called)
        final Vector3d world = this.scratchA.set(ix, iy, iz);
        if (selfIsA) {
            motionA.pose.transformPosition(world);
        }
        final double px = world.x;
        final double py = world.y;
        final double pz = world.z;

        // Side B: a vehicle block, a terrain block, or something we can't identify (e.g. a Create contraption)
        BlockState stateB = selfIsA ? null : selfState;
        if (selfIsA) {
            stateB = vehicleB != null ? this.vehicleBlock(vehicleB, bx, by, bz) : this.terrainBlock(bx, by, bz);
        }

        boolean unknownB = stateB == null || stateB.isAir();
        if (!unknownB) {
            // The other block has to actually be at the contact, or the coordinates belong to something else
            final Vector3d centerB = this.scratchB.set(bx + 0.5, by + 0.5, bz + 0.5);
            if (vehicleB != null) {
                motionB.pose.transformPosition(centerB);
            }
            if (centerB.distanceSquared(px, py, pz) > 2.5 * 2.5) {
                unknownB = true;
            }
        }

        // Relative velocity of A against B at the contact
        final Vector3d velocity = motionA.pointVelocity(px, py, pz, this.scratchB);
        if (motionB != null) {
            final Vector3d velocityB = motionB.pointVelocity(px, py, pz, this.scratchC);
            velocity.sub(velocityB);
        }
        final double relativeSpeed = velocity.length();
        if (relativeSpeed < 1.0e-3) {
            return null;
        }
        final double vx = velocity.x;
        final double vy = velocity.y;
        final double vz = velocity.z;

        // Contact normal from B into A, from the face of B nearest to the contact point
        final Vector3d normal = this.estimateNormal(unknownB, vehicleB != null ? motionB.pose : null, bx, by, bz, px, py, pz, vx, vy, vz, relativeSpeed, closingSpeed, this.scratchC);
        final double nx = normal.x;
        final double ny = normal.y;
        final double nz = normal.z;

        // Plot-space contact points
        final Vector3d plotA = this.scratchD.set(px, py, pz);
        motionA.pose.transformPositionInverse(plotA);
        final double plotAx = plotA.x;
        final double plotAy = plotA.y;
        final double plotAz = plotA.z;
        double plotBx = px;
        double plotBy = py;
        double plotBz = pz;
        if (vehicleB != null) {
            final Vector3d plotB = this.scratchD.set(px, py, pz);
            motionB.pose.transformPositionInverse(plotB);
            plotBx = plotB.x;
            plotBy = plotB.y;
            plotBz = plotB.z;
        }

        // Effective (reduced) mass of the two bodies at the contact along the normal
        final double inverseA = this.inverseNormalMass(vehicleA, motionA, plotAx, plotAy, plotAz, nx, ny, nz);
        final double inverseB = vehicleB != null ? this.inverseNormalMass(vehicleB, motionB, plotBx, plotBy, plotBz, nx, ny, nz) : 0.0;
        if (!(inverseA + inverseB > 0.0) || Double.isNaN(inverseA + inverseB)) {
            return null;
        }
        final double effectiveMassKg = this.kgPerSableMass / (inverseA + inverseB);

        // Several blocks touching at once share the load, so each takes a fraction of the energy
        final long bodyPair = bodyPairKey(vehicleA, vehicleB);
        final int sharing = Math.max(1, Math.max(this.bodyPairsPrevious.get(bodyPair), this.bodyPairsCurrent.addTo(bodyPair, 1) + 1));

        // Materials
        final BlockState resolvedB = unknownB ? Blocks.STONE.defaultBlockState() : stateB;
        final CrashMaterial materialA = CrashMaterials.get(stateA);
        final CrashMaterial materialB = CrashMaterials.get(resolvedB);
        final MaterialProfile baseA = this.vehicleProfile(stateA, materialA);
        final MaterialProfile baseB = unknownB ? UNKNOWN : vehicleB != null ? this.vehicleProfile(resolvedB, materialB) : this.terrainProfile(materialB);

        final boolean damageA = this.vehicleDamage && !baseA.unbreakable();
        final boolean damageB = !unknownB && !baseB.unbreakable() && (vehicleB != null ? this.vehicleDamage : this.worldDamage);

        final MaterialProfile profileA = damageA ? baseA : asRigid(baseA);
        final MaterialProfile profileB = damageB ? baseB : asRigid(baseB);

        final double shareMassKg = effectiveMassKg / sharing;
        final double weakerStrength = Math.min(profileA.effectiveStrength(), profileB.effectiveStrength());
        final boolean plastic = ContactMechanics.exceedsElasticLimit(shareMassKg, closingSpeed, weakerStrength, CONTACT_AREA, this.elasticTravel);

        // Step the contact through the sub-step. Each block can only be crushed so far before it fails.
        ContactMechanics.Step step = ContactMechanics.Step.NONE;
        if (plastic && this.state.canBreakMoreBlocks()) {
            final long keyA = BlockPos.asLong(ax, ay, az);
            final long keyB = BlockPos.asLong(bx, by, bz);
            final double limitB = damageB ? this.remainingDepth(keyB, resolvedB) : Double.POSITIVE_INFINITY;
            final double limitA = damageA ? this.remainingDepth(keyA, stateA) : Double.POSITIVE_INFINITY;
            step = ContactMechanics.integrate(profileA, profileB, CONTACT_AREA, this.dt, shareMassKg, closingSpeed, limitB, limitA);
        }

        // Only let the vehicle carry on into the material if it really gets through it this sub-step. If the material
        // stops it, Rapier keeps the contact: dropping it would let the vehicle sink a whole sub-step's travel into a
        // block that is still there, and Rapier would then throw it back out.
        final boolean passThrough = step != ContactMechanics.Step.NONE && step.goesThrough();

        motionA.contactThisSubstep = true;
        if (motionB != null) {
            motionB.contactThisSubstep = true;
        }

        return new ContactRecord(
                BlockPos.asLong(ax, ay, az), BlockPos.asLong(bx, by, bz), vehicleA, vehicleB,
                stateA, resolvedB, materialA, materialB, profileA, profileB, damageA, damageB,
                px, py, pz, plotAx, plotAy, plotAz, plotBx, plotBy, plotBz, nx, ny, nz, vx, vy, vz,
                closingSpeed, effectiveMassKg, step, passThrough
        );
    }

    /**
     * How much more of a block can be crushed before it fails [m].
     */
    private double remainingDepth(final long key, final BlockState state) {
        return Math.max(0.0, this.breakDepth * blockDepth(state) - this.state.damage().crushed(key));
    }

    /**
     * How deep a block is along a crash, from Sable's volume for the block (slabs are half as deep) [m].
     */
    static double blockDepth(final BlockState state) {
        return Math.max(0.25, Math.min(1.0, PhysicsBlockPropertyHelper.getVolume(state)));
    }

    private Vector3d estimateNormal(final boolean unknownB, @Nullable final Pose3dc poseB, final int bx, final int by, final int bz,
                                    final double px, final double py, final double pz,
                                    final double vx, final double vy, final double vz, final double relativeSpeed,
                                    final double closingSpeed, final Vector3d dest) {
        if (!unknownB) {
            // Offset of the contact from the centre of B, in B's own frame
            final Vector3d offset = dest.set(px, py, pz);
            if (poseB != null) {
                poseB.transformPositionInverse(offset);
            }
            offset.sub(bx + 0.5, by + 0.5, bz + 0.5);

            final double absX = Math.abs(offset.x);
            final double absY = Math.abs(offset.y);
            final double absZ = Math.abs(offset.z);
            if (absX >= absY && absX >= absZ) {
                offset.set(Math.signum(offset.x), 0.0, 0.0);
            } else if (absY >= absZ) {
                offset.set(0.0, Math.signum(offset.y), 0.0);
            } else {
                offset.set(0.0, 0.0, Math.signum(offset.z));
            }

            if (poseB != null) {
                poseB.transformNormal(offset);
                offset.normalize();
            }

            // A has to be moving into that face, at roughly the speed Rapier reported
            final double approach = -(vx * offset.x + vy * offset.y + vz * offset.z);
            if (approach > 0.35 * closingSpeed) {
                return offset;
            }
        }

        // Fall back to pushing straight back against the motion
        return dest.set(-vx, -vy, -vz).div(relativeSpeed);
    }

    private double inverseNormalMass(final ServerSubLevel vehicle, final BodyMotion motion, final double plotX, final double plotY, final double plotZ,
                                     final double nx, final double ny, final double nz) {
        final MassData mass = vehicle.getMassTracker();
        if (mass == null || mass.isInvalid()) {
            return 0.0;
        }
        final Vector3d localDirection = motion.pose.transformNormalInverse(new Vector3d(nx, ny, nz));
        return mass.getInverseNormalMass(new Vector3d(plotX, plotY, plotZ), localDirection);
    }

    private MaterialProfile vehicleProfile(final BlockState state, final CrashMaterial material) {
        final MaterialProfile base = material.profile();
        if (base.unbreakable()) {
            return base;
        }

        // On a vehicle, the physics engine's own mass for the block wins, so momentum stays consistent
        double density = base.density();
        final Double sableMass = ((BlockStateExtension) state).sable$getProperty(PhysicsBlockPropertyTypes.MASS.get());
        if (sableMass != null && sableMass > 0.0) {
            density = sableMass * this.kgPerSableMass / Math.max(PhysicsBlockPropertyHelper.getVolume(state), 0.1);
        }

        final double scale = this.strengthMultiplier * this.vehicleStrengthMultiplier;
        return base.withDensity(density).scaled(scale, scale);
    }

    private MaterialProfile terrainProfile(final CrashMaterial material) {
        return material.profile().scaled(this.strengthMultiplier, this.strengthMultiplier);
    }

    private static MaterialProfile asRigid(final MaterialProfile profile) {
        return profile.unbreakable() ? profile : new MaterialProfile(profile.density(), profile.strength(), profile.jointStrength(), true);
    }

    private void checkSkid(final BlockState selfState, final int x, final int y, final int z, final int ox, final int oz,
                           final double ix, final double iy, final double iz) {
        // Only terrain under a vehicle gets marked
        final CrashMaterial material = CrashMaterials.get(selfState);
        if (material.skid() == null || this.skidPositions.contains(BlockPos.asLong(x, y, z))) {
            return;
        }
        if (this.vehicleAt(x, z) != null) {
            return;
        }

        final ServerSubLevel vehicle = this.vehicleAt(ox, oz);
        final BodyMotion motion = vehicle != null ? this.state.motion(vehicle) : null;
        if (motion == null || motion.linear.lengthSquared() < SKID_SPEED * SKID_SPEED * 0.25) {
            return;
        }

        final Vector3d velocity = motion.pointVelocity(ix, iy, iz, this.scratchA);
        final double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed >= SKID_SPEED) {
            final long key = BlockPos.asLong(x, y, z);
            this.skidPositions.add(key);
            this.skids.add(new SkidMark(key, selfState, material.skid(), horizontalSpeed));
        }
    }

    @Nullable
    private ServerSubLevel vehicleAt(final int x, final int z) {
        final SubLevel subLevel = Sable.HELPER.getContaining(this.level, x >> 4, z >> 4);
        return subLevel instanceof final ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved() ? serverSubLevel : null;
    }

    @Nullable
    private BlockState vehicleBlock(final ServerSubLevel vehicle, final int x, final int y, final int z) {
        // Callback positions are absolute level coordinates inside the vehicle's plot, which is always loaded
        this.mutablePos.set(x, y, z);
        return vehicle.getPlot().contains(x + 0.5, z + 0.5) ? this.level.getBlockState(this.mutablePos) : null;
    }

    @Nullable
    private BlockState terrainBlock(final int x, final int y, final int z) {
        // Never load or wait for chunks from inside the physics step; getChunkNow returns null off the main thread
        final LevelChunk chunk = this.level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        return chunk != null ? chunk.getBlockState(this.mutablePos.set(x, y, z)) : null;
    }

    private static long pairKey(final long a, final long b) {
        final long low = Math.min(a, b);
        final long high = Math.max(a, b);
        return HashCommon.mix(low) * 31L + HashCommon.mix(high);
    }

    private static long bodyPairKey(final ServerSubLevel a, @Nullable final ServerSubLevel b) {
        final long idA = a.getRuntimeId();
        final long idB = b != null ? b.getRuntimeId() : -1;
        return (Math.min(idA, idB) << 32) ^ (Math.max(idA, idB) & 0xFFFFFFFFL);
    }
}
