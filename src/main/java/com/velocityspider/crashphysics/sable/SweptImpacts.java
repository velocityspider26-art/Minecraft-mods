package com.velocityspider.crashphysics.sable;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashConfig;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds the terrain fast vehicles are about to hit during the coming physics sub-step, and deals with it before the
 * step runs.
 * <p>
 * Rapier only notices a contact once two blocks already overlap. A vehicle covering most of a block per sub-step can
 * start a contact half way into a thin wall, be pushed out through its far side, and pass a wall, a tree or a tower as
 * if it weren't there. And during the first sub-step of a crash the contact callbacks arrive one by one, so the first
 * blocks to report can't know how many others share the load.
 * <p>
 * So before each sub-step, every moving vehicle's leading blocks are swept along their motion against the terrain.
 * Every contact that will start within the sub-step is assessed with the material model, with the load shared between
 * all the blocks that hit at the same time. What gives way is crushed and broken now, so the vehicle carries on into the
 * hole. An obstacle that holds is left for Rapier: the vehicle is slowed just enough to arrive at it by the end of the
 * sub-step, and Rapier stops it there.
 */
final class SweptImpacts {

    /**
     * Contacts starting this close together share the load [fraction of the sub-step]
     */
    private static final double SIMULTANEOUS = 0.35;

    /**
     * Blocks overlapping by this much are already touching, and Rapier is handling them [m]
     */
    private static final double TOUCHING = 0.02;

    /**
     * Two blocks have to overlap this much across the direction of travel to hit face on, not just graze [m]
     */
    private static final double MIN_SIDE_OVERLAP = 0.05;

    /**
     * A block leads along a local axis it is moving along at least this fraction of its speed
     */
    private static final double LEADING_AXIS = 0.2;

    private static final long SHAPE_REFRESH_TICKS = 100;
    private static final int MAX_LEADING_BLOCKS = 4096;

    private final ServerLevel level;
    private final LevelCrashState state;
    private final Reference2ObjectOpenHashMap<ServerSubLevel, Shape> shapes = new Reference2ObjectOpenHashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private final LongOpenHashSet blocked = new LongOpenHashSet();
    private final LongOpenHashSet brokenTerrain = new LongOpenHashSet();
    private final LongOpenHashSet moving = new LongOpenHashSet();
    private final List<Contact> contacts = new ArrayList<>();
    private final List<Contact> held = new ArrayList<>();

    private final Matrix3d rotation = new Matrix3d();
    private final Vector3d center = new Vector3d();
    private final Vector3d pointVelocity = new Vector3d();
    private final Vector3d local = new Vector3d();
    private final Vector3d linear = new Vector3d();
    private final Vector3d angular = new Vector3d();
    private final Vector3d plot = new Vector3d();
    private final Vector3d zero = new Vector3d();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

    @Nullable
    private LevelChunk lastChunk;

    /**
     * The blocks a vehicle is made of
     */
    private record Shape(LongOpenHashSet solid, long[] surface, double mass, long gatheredTick) {
    }

    /**
     * One vehicle block about to hit one terrain block. Positions and times are as swept at the start of the sub-step.
     *
     * @param time  when the two boxes start to touch [s]
     * @param axis  the world axis the vehicle block hits the terrain along (0, 1, 2)
     * @param sign  the direction the vehicle block moves along that axis
     * @param gap   how far apart the two are along that axis [m]
     * @param area  how much of the two faces meet [m²]
     */
    private record Hit(long vehicleBlock, long terrainBlock, BlockState terrain, double time, int axis, double sign, double gap, double area,
                       double centerX, double centerY, double centerZ, double extentX, double extentY, double extentZ,
                       double velocityX, double velocityY, double velocityZ, AABB box) {
    }

    SweptImpacts(final ServerLevel level, final LevelCrashState state) {
        this.level = level;
        this.state = state;
    }

    /**
     * Sweeps every moving vehicle. Called before each sub-step, after the motions were sampled.
     */
    void run(final SubLevelPhysicsSystem system, final double dt, final Iterable<BodyMotion> motions) {
        final ImpactCollector collector = this.state.collector;
        final double minSpeed = collector.minImpactSpeed();
        this.lastChunk = null;
        this.brokenTerrain.clear();

        boolean anything = false;
        for (final BodyMotion motion : motions) {
            final ServerSubLevel vehicle = motion.subLevel;
            if (vehicle.isRemoved() || motion.lastSeenTick != this.state.tick()) {
                continue;
            }
            final double speed = motion.linear.length();
            final double spin = motion.angular.length();
            if (speed < minSpeed && spin < 0.25) {
                continue;
            }

            final RigidBodyHandle handle = system.getPhysicsHandle(vehicle);
            if (handle == null || !handle.isValid()) {
                continue;
            }

            final Shape shape = this.shape(vehicle);
            if (shape == null || shape.surface.length == 0) {
                continue;
            }

            this.findHits(motion, shape, dt, minSpeed);
            if (!this.hits.isEmpty()) {
                anything |= this.resolveHits(system, motion, handle, dt, minSpeed);
            }
        }

        if (anything) {
            // Break what gave way now, so it is gone before Rapier steps
            this.state.destruction().process();
        }

        if (this.shapes.size() > 64 && this.state.tick() % 200 == 0) {
            this.shapes.keySet().removeIf(ServerSubLevel::isRemoved);
        }
    }

    /**
     * Finds every vehicle block that will hit an exposed terrain face during the sub-step.
     */
    private void findHits(final BodyMotion motion, final Shape shape, final double dt, final double minSpeed) {
        this.hits.clear();

        final Pose3dc pose = motion.pose;
        final Vector3dc com = pose.position();
        this.rotation.set(pose.orientation());

        // World-space half size of a rotated block
        final double extentX = 0.5 * (Math.abs(this.rotation.m00) + Math.abs(this.rotation.m10) + Math.abs(this.rotation.m20));
        final double extentY = 0.5 * (Math.abs(this.rotation.m01) + Math.abs(this.rotation.m11) + Math.abs(this.rotation.m21));
        final double extentZ = 0.5 * (Math.abs(this.rotation.m02) + Math.abs(this.rotation.m12) + Math.abs(this.rotation.m22));

        int leading = 0;
        for (final long block : shape.surface) {
            final int bx = BlockPos.getX(block);
            final int by = BlockPos.getY(block);
            final int bz = BlockPos.getZ(block);

            final Vector3d c = pose.transformPosition(this.center.set(bx + 0.5, by + 0.5, bz + 0.5));
            final double rx = c.x - com.x();
            final double ry = c.y - com.y();
            final double rz = c.z - com.z();
            final Vector3d v = this.pointVelocity.set(
                    motion.linear.x + motion.angular.y * rz - motion.angular.z * ry,
                    motion.linear.y + motion.angular.z * rx - motion.angular.x * rz,
                    motion.linear.z + motion.angular.x * ry - motion.angular.y * rx);
            final double speed = v.length();
            if (speed < minSpeed) {
                continue;
            }

            // Only blocks with an open face towards where they are going can hit anything first
            final Vector3d direction = pose.transformNormalInverse(this.local.set(v).div(speed));
            if (!this.leads(shape, bx, by, bz, direction)) {
                continue;
            }
            if (++leading > MAX_LEADING_BLOCKS) {
                break;
            }

            this.sweepBlock(block, c.x, c.y, c.z, extentX, extentY, extentZ, v.x * dt, v.y * dt, v.z * dt, dt, minSpeed);
        }
    }

    private boolean leads(final Shape shape, final int x, final int y, final int z, final Vector3d direction) {
        return (Math.abs(direction.x) >= LEADING_AXIS && !shape.solid.contains(BlockPos.asLong(x + (direction.x > 0 ? 1 : -1), y, z)))
                || (Math.abs(direction.y) >= LEADING_AXIS && !shape.solid.contains(BlockPos.asLong(x, y + (direction.y > 0 ? 1 : -1), z)))
                || (Math.abs(direction.z) >= LEADING_AXIS && !shape.solid.contains(BlockPos.asLong(x, y, z + (direction.z > 0 ? 1 : -1))));
    }

    /**
     * Sweeps one vehicle block's box along its motion over the sub-step, against the terrain around it.
     */
    private void sweepBlock(final long block, final double cx, final double cy, final double cz,
                            final double ex, final double ey, final double ez,
                            final double dx, final double dy, final double dz, final double dt, final double minSpeed) {
        final double minX = cx - ex;
        final double minY = cy - ey;
        final double minZ = cz - ez;
        final double maxX = cx + ex;
        final double maxY = cy + ey;
        final double maxZ = cz + ez;

        final int x0 = (int) Math.floor(Math.min(minX, minX + dx) - TOUCHING);
        final int y0 = (int) Math.floor(Math.min(minY, minY + dy) - TOUCHING);
        final int z0 = (int) Math.floor(Math.min(minZ, minZ + dz) - TOUCHING);
        final int x1 = (int) Math.floor(Math.max(maxX, maxX + dx) + TOUCHING);
        final int y1 = (int) Math.floor(Math.max(maxY, maxY + dy) + TOUCHING);
        final int z1 = (int) Math.floor(Math.max(maxZ, maxZ + dz) + TOUCHING);

        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    final BlockState terrain = this.terrainAt(this.cursor.set(x, y, z));
                    if (terrain == null || terrain.isAir()) {
                        continue;
                    }
                    final VoxelShape collision = terrain.getCollisionShape(this.level, this.cursor);
                    if (collision.isEmpty()) {
                        continue;
                    }
                    final AABB box = collision.bounds().move(x, y, z);

                    // Already touching: Rapier has it
                    if (minX + TOUCHING < box.maxX && maxX - TOUCHING > box.minX
                            && minY + TOUCHING < box.maxY && maxY - TOUCHING > box.minY
                            && minZ + TOUCHING < box.maxZ && maxZ - TOUCHING > box.minZ) {
                        continue;
                    }

                    // Slab test: when the two boxes start and stop overlapping, as fractions of the sub-step
                    double entry = Double.NEGATIVE_INFINITY;
                    double exit = Double.POSITIVE_INFINITY;
                    int axis = -1;
                    for (int k = 0; k < 3; k++) {
                        final double lo = k == 0 ? minX : k == 1 ? minY : minZ;
                        final double hi = k == 0 ? maxX : k == 1 ? maxY : maxZ;
                        final double d = k == 0 ? dx : k == 1 ? dy : dz;
                        final double boxLo = k == 0 ? box.minX : k == 1 ? box.minY : box.minZ;
                        final double boxHi = k == 0 ? box.maxX : k == 1 ? box.maxY : box.maxZ;

                        if (Math.abs(d) < 1.0e-9) {
                            if (hi - MIN_SIDE_OVERLAP <= boxLo || lo + MIN_SIDE_OVERLAP >= boxHi) {
                                entry = Double.POSITIVE_INFINITY;
                                break;
                            }
                            continue;
                        }
                        final double enter = d > 0.0 ? (boxLo - hi) / d : (boxHi - lo) / d;
                        final double leave = d > 0.0 ? (boxHi - lo) / d : (boxLo - hi) / d;
                        if (enter > entry) {
                            entry = enter;
                            axis = k;
                        }
                        exit = Math.min(exit, leave);
                    }
                    if (axis < 0 || !(entry >= 0.0) || entry > 1.0 || entry >= exit) {
                        continue;
                    }

                    final double d = axis == 0 ? dx : axis == 1 ? dy : dz;
                    final double sign = Math.signum(d);
                    if (Math.abs(d) / dt < minSpeed) {
                        continue;
                    }

                    // Hitting a face inside solid ground (between two ground blocks) is just sliding over the ground
                    if (this.faceIsInternal(x, y, z, box, axis, sign)) {
                        continue;
                    }

                    // Grazing an edge isn't a hit: the boxes have to overlap across the direction of travel
                    final double area = this.overlapAcross(axis, entry, minX, minY, minZ, maxX, maxY, maxZ, dx, dy, dz, box);
                    if (area <= 0.0) {
                        continue;
                    }

                    final double gap = sign > 0.0
                            ? (axis == 0 ? box.minX - maxX : axis == 1 ? box.minY - maxY : box.minZ - maxZ)
                            : (axis == 0 ? minX - box.maxX : axis == 1 ? minY - box.maxY : minZ - box.maxZ);

                    this.hits.add(new Hit(block, BlockPos.asLong(x, y, z), terrain, entry * dt, axis, sign, Math.max(0.0, gap), area,
                            cx, cy, cz, ex, ey, ez, dx / dt, dy / dt, dz / dt, box));
                }
            }
        }
    }

    private boolean faceIsInternal(final int x, final int y, final int z, final AABB box, final int axis, final double sign) {
        // The face being hit, and the cell on the far side of it
        final boolean onBoundary = sign > 0.0
                ? (axis == 0 ? box.minX == x : axis == 1 ? box.minY == y : box.minZ == z)
                : (axis == 0 ? box.maxX == x + 1 : axis == 1 ? box.maxY == y + 1 : box.maxZ == z + 1);
        if (!onBoundary) {
            return false;
        }

        final int step = sign > 0.0 ? -1 : 1;
        this.neighbor.set(x + (axis == 0 ? step : 0), y + (axis == 1 ? step : 0), z + (axis == 2 ? step : 0));
        final BlockState beside = this.terrainAt(this.neighbor);
        return beside != null && !beside.isAir() && Block.isShapeFullBlock(beside.getCollisionShape(this.level, this.neighbor));
    }

    /**
     * How much the two boxes overlap across the direction of travel when they meet [m²], or 0 if they only graze.
     */
    private double overlapAcross(final int axis, final double entry,
                                 final double minX, final double minY, final double minZ,
                                 final double maxX, final double maxY, final double maxZ,
                                 final double dx, final double dy, final double dz, final AABB box) {
        double area = 1.0;
        for (int k = 0; k < 3; k++) {
            if (k == axis) {
                continue;
            }
            final double shift = (k == 0 ? dx : k == 1 ? dy : dz) * entry;
            final double lo = (k == 0 ? minX : k == 1 ? minY : minZ) + shift;
            final double hi = (k == 0 ? maxX : k == 1 ? maxY : maxZ) + shift;
            final double overlap = Math.min(hi, k == 0 ? box.maxX : k == 1 ? box.maxY : box.maxZ) - Math.max(lo, k == 0 ? box.minX : k == 1 ? box.minY : box.minZ);
            if (overlap < MIN_SIDE_OVERLAP) {
                return 0.0;
            }
            area *= overlap;
        }
        return area;
    }

    /**
     * One hit as it will actually happen: where, how hard, and how much of the vehicle is behind it.
     */
    private static final class Contact {
        final Hit hit;
        final BlockState stateA;
        final double px, py, pz;
        final double plotX, plotY, plotZ;
        final double nx, ny, nz;
        final double vx, vy, vz;
        final double closing;
        final double effectiveMassKg;
        double shareMassKg;

        Contact(final Hit hit, final BlockState stateA, final double px, final double py, final double pz, final Vector3d plot,
                final double nx, final double ny, final double nz, final double vx, final double vy, final double vz,
                final double closing, final double effectiveMassKg) {
            this.hit = hit;
            this.stateA = stateA;
            this.px = px;
            this.py = py;
            this.pz = pz;
            this.plotX = plot.x;
            this.plotY = plot.y;
            this.plotZ = plot.z;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.closing = closing;
            this.effectiveMassKg = effectiveMassKg;
        }

        int direction() {
            return this.hit.axis * 2 + (this.hit.sign > 0.0 ? 1 : 0);
        }
    }

    /**
     * Deals with one vehicle's hits in the order they happen.
     *
     * @return if anything was hit
     */
    private boolean resolveHits(final SubLevelPhysicsSystem system, final BodyMotion motion, final RigidBodyHandle handle,
                                final double dt, final double minSpeed) {
        this.hits.sort(Comparator.comparingDouble(Hit::time));
        this.blocked.clear();

        boolean anything = false;
        final int count = this.hits.size();
        int start = 0;
        while (start < count) {
            // Hits starting at about the same time happen together and share the load
            int end = start + 1;
            final double groupTime = this.hits.get(start).time;
            while (end < count && this.hits.get(end).time <= groupTime + SIMULTANEOUS * dt) {
                end++;
            }
            anything |= this.resolveGroup(system, motion, handle, start, end, dt, minSpeed);
            start = end;
        }

        if (anything) {
            motion.refreshVelocity(handle);
            if (CrashConfig.DEBUG_LOGGING.getAsBoolean()) {
                CrashPhysics.LOGGER.info("[crashphysics] swept {}: {} contacts ahead", motion.subLevel, count);
            }
        }
        return anything;
    }

    private boolean resolveGroup(final SubLevelPhysicsSystem system, final BodyMotion motion, final RigidBodyHandle handle,
                                 final int start, final int end, final double dt, final double minSpeed) {
        final ServerSubLevel vehicle = motion.subLevel;
        final ImpactCollector collector = this.state.collector;
        final ImpactResolver resolver = this.state.resolver;
        final double kgPerSableMass = collector.kgPerSableMass();
        final Pose3dc pose = motion.pose;
        final Vector3dc com = pose.position();

        // Where and how hard each pair meets, all with the vehicle as it is moving before any of them
        handle.getLinearVelocity(this.linear);
        handle.getAngularVelocity(this.angular);
        this.contacts.clear();
        this.moving.clear();
        for (int i = start; i < end; i++) {
            final Hit hit = this.hits.get(i);
            if (this.blocked.contains(hit.vehicleBlock) || this.brokenTerrain.contains(hit.terrainBlock)
                    || collector.wasHeld(hit.vehicleBlock, hit.terrainBlock)) {
                continue;
            }

            final double nx = hit.axis == 0 ? -hit.sign : 0.0;
            final double ny = hit.axis == 1 ? -hit.sign : 0.0;
            final double nz = hit.axis == 2 ? -hit.sign : 0.0;
            final double px = this.contactCoordinate(0, hit, hit.time);
            final double py = this.contactCoordinate(1, hit, hit.time);
            final double pz = this.contactCoordinate(2, hit, hit.time);

            final double rx = px - com.x();
            final double ry = py - com.y();
            final double rz = pz - com.z();
            final double vx = this.linear.x + this.angular.y * rz - this.angular.z * ry;
            final double vy = this.linear.y + this.angular.z * rx - this.angular.x * rz;
            final double vz = this.linear.z + this.angular.x * ry - this.angular.y * rx;
            final double closing = -(vx * nx + vy * ny + vz * nz);

            // Slowed down by earlier hits: it may arrive gently, or not this sub-step at all
            if (closing < minSpeed || hit.gap > closing * dt) {
                this.blocked.add(hit.vehicleBlock);
                continue;
            }

            final BlockState stateA = this.level.getBlockState(this.cursor.set(hit.vehicleBlock));
            if (stateA.isAir()) {
                continue;
            }

            final Vector3d plotPoint = pose.transformPositionInverse(this.plot.set(px, py, pz));
            final double inverse = collector.inverseNormalMass(vehicle, motion, plotPoint.x, plotPoint.y, plotPoint.z, nx, ny, nz);
            if (!(inverse > 0.0) || Double.isNaN(inverse)) {
                continue;
            }

            // A terrain block met by two of the vehicle's blocks is still one obstacle: keep the fuller contact
            final Contact contact = new Contact(hit, stateA, px, py, pz, plotPoint, nx, ny, nz, vx, vy, vz, closing, kgPerSableMass / inverse);
            if (this.moving.add(hit.terrainBlock)) {
                this.contacts.add(contact);
            } else {
                for (int j = 0; j < this.contacts.size(); j++) {
                    final Contact other = this.contacts.get(j);
                    if (other.hit.terrainBlock == hit.terrainBlock) {
                        if (hit.area > other.hit.area) {
                            this.contacts.set(j, contact);
                        }
                        break;
                    }
                }
            }
        }
        if (this.contacts.isEmpty()) {
            return false;
        }

        // Contacts pushing the same way share the load: each carries an equal part of the vehicle's effective mass at
        // their centre. A flat belly landing spreads the whole mass over every block; a lone corner hit gets only what
        // that corner can bring to bear, the rest of the vehicle swinging round it.
        for (int direction = 0; direction < 6; direction++) {
            double cx = 0.0;
            double cy = 0.0;
            double cz = 0.0;
            int n = 0;
            Contact sample = null;
            for (final Contact contact : this.contacts) {
                if (contact.direction() == direction) {
                    cx += contact.px;
                    cy += contact.py;
                    cz += contact.pz;
                    n++;
                    sample = contact;
                }
            }
            if (n == 0) {
                continue;
            }

            double groupMassKg = sample.effectiveMassKg;
            if (n > 1) {
                final Vector3d centre = pose.transformPositionInverse(this.plot.set(cx / n, cy / n, cz / n));
                final double inverse = collector.inverseNormalMass(vehicle, motion, centre.x, centre.y, centre.z, sample.nx, sample.ny, sample.nz);
                if (inverse > 0.0 && !Double.isNaN(inverse)) {
                    groupMassKg = kgPerSableMass / inverse;
                }
            }
            for (final Contact contact : this.contacts) {
                if (contact.direction() == direction) {
                    contact.shareMassKg = groupMassKg / n;
                }
            }
        }

        // What each contact does, then apply it
        this.held.clear();
        for (final Contact contact : this.contacts) {
            final Hit hit = contact.hit;
            final double capacity = collector.knockOutCapacity(BlockPos.getX(hit.terrainBlock), BlockPos.getY(hit.terrainBlock), BlockPos.getZ(hit.terrainBlock),
                    hit.axis, hit.sign > 0.0 ? 1 : -1, hit.terrain, this.moving);

            final ContactRecord record = collector.assess(vehicle, null, hit.vehicleBlock, contact.stateA, hit.terrainBlock, hit.terrain, false,
                    contact.px, contact.py, contact.pz, contact.plotX, contact.plotY, contact.plotZ, contact.px, contact.py, contact.pz,
                    contact.nx, contact.ny, contact.nz, contact.vx, contact.vy, contact.vz, contact.closing,
                    contact.effectiveMassKg, contact.shareMassKg, capacity);

            final boolean holds = !record.crushing || !record.passThrough;
            collector.markHandled(record, holds);
            resolver.apply(system, record, true);
            motion.contactThisSubstep = true;

            if (holds) {
                this.held.add(contact);
                this.blocked.add(hit.vehicleBlock);
            } else if (record.step.targetFailed()) {
                this.brokenTerrain.add(hit.terrainBlock);
            } else {
                // Still pushing into soft ground, or its own block gave way: nothing further for it this sub-step
                this.blocked.add(hit.vehicleBlock);
            }
        }

        // Obstacles that hold: slow the vehicle just enough to arrive at them by the end of the sub-step, instead of
        // sinking into (or through) them, and let Rapier stop it there against all of them at once
        if (!this.held.isEmpty()) {
            handle.getLinearVelocity(this.linear);
            handle.getAngularVelocity(this.angular);
            final double[] cut = new double[6];
            for (final Contact contact : this.held) {
                final double rx = contact.px - com.x();
                final double ry = contact.py - com.y();
                final double rz = contact.pz - com.z();
                final double vx = this.linear.x + this.angular.y * rz - this.angular.z * ry;
                final double vy = this.linear.y + this.angular.z * rx - this.angular.x * rz;
                final double vz = this.linear.z + this.angular.x * ry - this.angular.y * rx;
                final double closingNow = -(vx * contact.nx + vy * contact.ny + vz * contact.nz);
                final int direction = contact.direction();
                cut[direction] = Math.max(cut[direction], closingNow - contact.hit.gap / dt);
            }

            final Vector3d change = this.local.zero();
            for (int direction = 0; direction < 6; direction++) {
                if (cut[direction] > 0.0) {
                    // The normal points out of the obstacle, so moving along it closes in more slowly
                    final double sign = (direction & 1) == 1 ? -1.0 : 1.0;
                    final int axis = direction >> 1;
                    change.add(axis == 0 ? sign * cut[direction] : 0.0, axis == 1 ? sign * cut[direction] : 0.0, axis == 2 ? sign * cut[direction] : 0.0);
                }
            }
            if (change.lengthSquared() > 0.0 && change.isFinite()) {
                handle.addLinearAndAngularVelocity(change, this.zero);
            }
        }
        return true;
    }

    /**
     * Where the two boxes of a hit meet along one world axis: on the terrain face along the hit axis, in the middle of
     * the overlap across it.
     */
    private double contactCoordinate(final int k, final Hit hit, final double time) {
        final AABB box = hit.box;
        final double boxLo = k == 0 ? box.minX : k == 1 ? box.minY : box.minZ;
        final double boxHi = k == 0 ? box.maxX : k == 1 ? box.maxY : box.maxZ;
        if (k == hit.axis) {
            return hit.sign > 0.0 ? boxLo : boxHi;
        }
        final double center = (k == 0 ? hit.centerX : k == 1 ? hit.centerY : hit.centerZ) + (k == 0 ? hit.velocityX : k == 1 ? hit.velocityY : hit.velocityZ) * time;
        final double extent = k == 0 ? hit.extentX : k == 1 ? hit.extentY : hit.extentZ;
        return 0.5 * (Math.max(center - extent, boxLo) + Math.min(center + extent, boxHi));
    }

    @Nullable
    private BlockState terrainAt(final BlockPos pos) {
        // Never load chunks from here
        final int chunkX = pos.getX() >> 4;
        final int chunkZ = pos.getZ() >> 4;
        LevelChunk chunk = this.lastChunk;
        if (chunk == null || chunk.getPos().x != chunkX || chunk.getPos().z != chunkZ) {
            chunk = this.level.getChunkSource().getChunkNow(chunkX, chunkZ);
            if (chunk == null) {
                return null;
            }
            this.lastChunk = chunk;
        }
        return chunk.getBlockState(pos);
    }

    /**
     * The vehicle's blocks, gathered again whenever its mass changes (a block was added or removed).
     */
    @Nullable
    private Shape shape(final ServerSubLevel vehicle) {
        final MassData massData = vehicle.getMassTracker();
        if (massData == null || massData.isInvalid()) {
            return null;
        }
        final double mass = massData.getMass();

        final Shape cached = this.shapes.get(vehicle);
        if (cached != null && cached.mass == mass && this.state.tick() - cached.gatheredTick < SHAPE_REFRESH_TICKS) {
            return cached;
        }

        final BoundingBox3ic bounds = vehicle.getPlot().getBoundingBox();
        if (bounds == null) {
            return null;
        }
        final long volume = (long) (bounds.maxX() - bounds.minX() + 1) * (bounds.maxY() - bounds.minY() + 1) * (bounds.maxZ() - bounds.minZ() + 1);
        final long maxVolume = Math.max(200_000L, CrashConfig.MAX_STRUCTURAL_BLOCKS.getAsInt() * 8L);
        if (volume <= 0 || volume > maxVolume) {
            this.shapes.put(vehicle, new Shape(new LongOpenHashSet(), new long[0], mass, this.state.tick()));
            return null;
        }

        final LongOpenHashSet solid = new LongOpenHashSet();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    final BlockState blockState = this.level.getBlockState(pos.set(x, y, z));
                    if (!blockState.isAir() && !blockState.getCollisionShape(this.level, pos).isEmpty()) {
                        solid.add(pos.asLong());
                    }
                }
            }
        }

        final LongArrayList surface = new LongArrayList();
        for (final long block : solid) {
            final int x = BlockPos.getX(block);
            final int y = BlockPos.getY(block);
            final int z = BlockPos.getZ(block);
            if (!solid.contains(BlockPos.asLong(x + 1, y, z)) || !solid.contains(BlockPos.asLong(x - 1, y, z))
                    || !solid.contains(BlockPos.asLong(x, y + 1, z)) || !solid.contains(BlockPos.asLong(x, y - 1, z))
                    || !solid.contains(BlockPos.asLong(x, y, z + 1)) || !solid.contains(BlockPos.asLong(x, y, z - 1))) {
                surface.add(block);
            }
        }

        final Shape shape = new Shape(solid, surface.toLongArray(), mass, this.state.tick());
        this.shapes.put(vehicle, shape);
        return shape;
    }
}
