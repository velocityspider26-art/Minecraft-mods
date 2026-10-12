package com.velocityspider.crashphysics.sable;

import com.velocityspider.crashphysics.config.CrashConfig;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Ships float because their hulls keep the water out.
 * <p>
 * Sable lets each block float by its own volume. Here the air sealed inside a hull counts too: every cell of air in a
 * compartment that sits below the water outside pushes the vehicle up with the weight of the water it keeps out
 * (Archimedes), at the point where it is, so a lopsided hull lists. A compartment is space walled in by watertight
 * blocks below and on all four sides, so an open boat counts as well as a closed hull.
 * <p>
 * Water only gets in through openings below the waterline, at the rate the pressure drives it through them
 * (Torricelli: Q = Cd·A·√(2gΔh) for each square metre of opening). An open boat stays dry until it heels over or rides
 * so low that its rim goes under; a hole knocked in the bottom floods it, faster the deeper it is. The water inside
 * weighs the vehicle down where it collects, at the lowest point, and drains out again through any opening below its
 * level once the hull is lifted out. Heave, roll and pitch are damped the way the water around a hull damps them.
 */
final class HullBuoyancy {

    /**
     * Discharge coefficient of a sharp-edged hole
     */
    private static final double DISCHARGE = 0.6;

    /**
     * How quickly the water around a hull damps its bobbing and rolling [1/s]
     */
    private static final double DAMPING_RATE = 1.5;

    private static final double WATER_DENSITY = 1000.0;
    private static final double LAVA_DENSITY = 3100.0;
    private static final double TICK = 0.05;
    private static final int MAX_BOX_CELLS = 250_000;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final ServerLevel level;
    private final LevelCrashState state;
    private final Reference2ObjectOpenHashMap<ServerSubLevel, Hull> hulls = new Reference2ObjectOpenHashMap<>();

    // Water surface height and density of the liquid in each world column, worked out once per tick
    private final Long2DoubleOpenHashMap surfaces = new Long2DoubleOpenHashMap();
    private final Long2DoubleOpenHashMap densities = new Long2DoubleOpenHashMap();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final Vector3d point = new Vector3d();
    private final Vector3d impulse = new Vector3d();
    /**
     * The level of the open water around the vehicle being measured, or NaN
     */
    private double waterline = Double.NaN;
    private double[] worldX = new double[256];
    private double[] worldY = new double[256];
    private double[] worldZ = new double[256];

    /**
     * Space inside a hull: cells walled in below and on all sides by watertight blocks, connected to each other.
     */
    static final class Compartment {
        final long[] cells;
        /**
         * Cells with a block that isn't watertight in them (a fence, a slab): water gets through, but Sable already
         * floats the block itself
         */
        final boolean[] occupied;
        /**
         * Where water can get in or out: a cell of the compartment, and the face of it that opens to the outside
         */
        final long[] openingCells;
        final byte[] openingFaces;
        final double capacity;
        /**
         * Water inside [m³]
         */
        double water;

        Compartment(final long[] cells, final boolean[] occupied, final long[] openingCells, final byte[] openingFaces) {
            this.cells = cells;
            this.occupied = occupied;
            this.openingCells = openingCells;
            this.openingFaces = openingFaces;
            int free = 0;
            for (final boolean taken : occupied) {
                if (!taken) {
                    free++;
                }
            }
            this.capacity = free;
        }
    }

    static final class Hull {
        int version = Integer.MIN_VALUE;
        Compartment[] compartments = new Compartment[0];
        final Long2IntOpenHashMap owner = new Long2IntOpenHashMap();
        boolean active;

        // This tick's forces, in the world: the lift of the air below the waterline, the weight of water above it
        double lift, liftX, liftY, liftZ;
        double weight, weightX, weightY, weightZ;
        // Submerged volume and its moments about the centre of mass, for damping
        double area, m1x, m1z, sxx, szz, sxz;
        double inflow;

        double water() {
            double total = 0.0;
            for (final Compartment compartment : this.compartments) {
                total += compartment.water;
            }
            return total;
        }
    }

    HullBuoyancy(final ServerLevel level, final LevelCrashState state) {
        this.level = level;
        this.state = state;
    }

    /**
     * @return the water inside a vehicle's hull [m³]
     */
    double water(final ServerSubLevel vehicle) {
        final Hull hull = this.hulls.get(vehicle);
        return hull != null ? hull.water() : 0.0;
    }

    /**
     * Works out this tick's buoyancy and flooding for every vehicle in or near a liquid. Called before the first
     * physics sub-step of a tick, after the motions were sampled.
     */
    void evaluate(final Iterable<BodyMotion> motions) {
        this.surfaces.clear();
        this.densities.clear();
        if (!CrashConfig.HULL_BUOYANCY.getAsBoolean()) {
            this.hulls.clear();
            return;
        }

        final double gravity = DimensionPhysicsData.getGravity(this.level).length();
        for (final BodyMotion motion : motions) {
            final ServerSubLevel vehicle = motion.subLevel;
            Hull hull = this.hulls.get(vehicle);
            if (vehicle.isRemoved() || motion.lastSeenTick != this.state.tick()) {
                if (hull != null) {
                    hull.active = false;
                }
                continue;
            }
            final VehicleShape shape = this.state.shapes().get(vehicle);
            if (shape == null) {
                continue;
            }
            final boolean holdsWater = hull != null && hull.water() > 1.0e-3;
            if (!holdsWater && !this.nearLiquid(motion.pose, shape)) {
                if (hull != null) {
                    hull.active = false;
                }
                continue;
            }

            if (hull == null) {
                hull = new Hull();
                this.hulls.put(vehicle, hull);
            }
            if (hull.version != shape.version) {
                this.rebuild(hull, shape);
            }
            hull.active = hull.compartments.length > 0;
            if (hull.active) {
                this.waterline = this.waterline(motion.pose, shape);
                this.measure(vehicle, hull, motion, gravity);
                if (CrashConfig.DEBUG_LOGGING.getAsBoolean() && this.state.tick() % 20 == 0) {
                    int cells = 0;
                    int openings = 0;
                    for (final Compartment compartment : hull.compartments) {
                        cells += compartment.cells.length;
                        openings += compartment.openingCells.length;
                    }
                    com.velocityspider.crashphysics.CrashPhysics.LOGGER.info(
                            "[crashphysics] hull of {}: {} compartments, {} cells, {} openings; lift {} kN, inside water {} m³ weighing {} kN, inflow {} m³/s, y {}",
                            vehicle, hull.compartments.length, cells, openings, String.format("%.0f", hull.lift / 1000.0),
                            String.format("%.1f", hull.water()), String.format("%.0f", hull.weight / 1000.0), String.format("%.2f", hull.inflow),
                            String.format("%.2f", motion.pose.position().y()));
                }
            }
        }

        if (this.state.tick() % 200 == 0) {
            this.hulls.keySet().removeIf(ServerSubLevel::isRemoved);
        }
    }

    /**
     * Applies the buoyancy, the weight of water inside and the damping to every floating vehicle. Called before each
     * physics sub-step.
     */
    void apply(final SubLevelPhysicsSystem system, final double dt) {
        if (this.hulls.isEmpty()) {
            return;
        }
        final double kgPerSableMass = this.state.collector.kgPerSableMass();
        for (final var entry : this.hulls.reference2ObjectEntrySet()) {
            final Hull hull = entry.getValue();
            if (!hull.active) {
                continue;
            }
            final ServerSubLevel vehicle = entry.getKey();
            final BodyMotion motion = this.state.motion(vehicle);
            final RigidBodyHandle handle = system.getPhysicsHandle(vehicle);
            final MassData massData = vehicle.getMassTracker();
            if (motion == null || handle == null || !handle.isValid() || massData == null || massData.isInvalid()) {
                continue;
            }
            final Pose3dc pose = motion.pose;

            if (hull.lift > 0.0) {
                this.push(handle, pose, hull.liftX / hull.lift, hull.liftY / hull.lift, hull.liftZ / hull.lift, 0.0, hull.lift * dt, 0.0, kgPerSableMass);
            }
            if (hull.weight > 0.0) {
                this.push(handle, pose, hull.weightX / hull.weight, hull.weightY / hull.weight, hull.weightZ / hull.weight,
                        0.0, -hull.weight * dt, 0.0, kgPerSableMass);
            }

            // The water around the hull resists it moving up and down and rolling, not sailing along
            if (hull.area > 0.0) {
                final double b = WATER_DENSITY * DAMPING_RATE;
                final double vy = motion.linear.y;
                final double wx = motion.angular.x;
                final double wz = motion.angular.z;
                double force = -b * (hull.area * vy + wz * hull.m1x - wx * hull.m1z);
                final double torqueX = b * (hull.m1z * vy + wz * hull.sxz - wx * hull.szz);
                final double torqueZ = -b * (hull.m1x * vy + wz * hull.sxx - wx * hull.sxz);

                // Never more than stops the bobbing in one step
                final double massKg = massData.getMass() * kgPerSableMass;
                final double limit = 0.8 * massKg * Math.abs(vy) / dt;
                force = Mth.clamp(force, -limit, limit);

                final double comX = pose.position().x();
                final double comY = pose.position().y();
                final double comZ = pose.position().z();
                this.push(handle, pose, comX, comY, comZ, 0.0, force * dt, 0.0, kgPerSableMass);
                // A torque as a couple: equal and opposite pushes a metre above and below the centre of mass
                this.push(handle, pose, comX, comY + 1.0, comZ, -torqueZ * 0.5 * dt, 0.0, torqueX * 0.5 * dt, kgPerSableMass);
                this.push(handle, pose, comX, comY - 1.0, comZ, torqueZ * 0.5 * dt, 0.0, -torqueX * 0.5 * dt, kgPerSableMass);
            }
        }
    }

    /**
     * Applies an impulse given in the world [N·s] at a point in the world.
     */
    private void push(final RigidBodyHandle handle, final Pose3dc pose, final double x, final double y, final double z,
                      final double ix, final double iy, final double iz, final double kgPerSableMass) {
        if (!Double.isFinite(ix + iy + iz) || ix * ix + iy * iy + iz * iz < 1.0e-12) {
            return;
        }
        final Vector3d plot = pose.transformPositionInverse(this.point.set(x, y, z));
        final Vector3d local = pose.transformNormalInverse(this.impulse.set(ix, iy, iz)).div(kgPerSableMass);
        handle.applyImpulseAtPoint(plot, local);
    }

    /**
     * Finds the compartments of a hull, carrying the water already in them over to the new layout.
     */
    private void rebuild(final Hull hull, final VehicleShape shape) {
        hull.version = shape.version;
        final Compartment[] previous = hull.compartments;
        final Long2IntOpenHashMap previousOwner = new Long2IntOpenHashMap(hull.owner);
        previousOwner.defaultReturnValue(-1);
        hull.owner.clear();
        hull.owner.defaultReturnValue(-1);

        final long volume = (long) (shape.maxX - shape.minX + 1) * (shape.maxY - shape.minY + 1) * (shape.maxZ - shape.minZ + 1);
        if (shape.tight.isEmpty() || volume > MAX_BOX_CELLS) {
            hull.compartments = new Compartment[0];
            return;
        }

        // Watertight walls along each row and column
        final Long2IntOpenHashMap lowX = new Long2IntOpenHashMap(), highX = new Long2IntOpenHashMap();
        final Long2IntOpenHashMap lowZ = new Long2IntOpenHashMap(), highZ = new Long2IntOpenHashMap();
        final Long2IntOpenHashMap lowY = new Long2IntOpenHashMap();
        lowX.defaultReturnValue(Integer.MAX_VALUE);
        lowZ.defaultReturnValue(Integer.MAX_VALUE);
        lowY.defaultReturnValue(Integer.MAX_VALUE);
        highX.defaultReturnValue(Integer.MIN_VALUE);
        highZ.defaultReturnValue(Integer.MIN_VALUE);
        for (final long block : shape.tight) {
            final int x = BlockPos.getX(block);
            final int y = BlockPos.getY(block);
            final int z = BlockPos.getZ(block);
            final long rowX = BlockPos.asLong(0, y, z);
            final long rowZ = BlockPos.asLong(x, y, 0);
            final long column = BlockPos.asLong(x, 0, z);
            if (x < lowX.get(rowX)) lowX.put(rowX, x);
            if (x > highX.get(rowX)) highX.put(rowX, x);
            if (z < lowZ.get(rowZ)) lowZ.put(rowZ, z);
            if (z > highZ.get(rowZ)) highZ.put(rowZ, z);
            if (y < lowY.get(column)) lowY.put(column, y);
        }

        // Inside: not watertight itself, and walled in below and on all four sides
        final LongOpenHashSet inside = new LongOpenHashSet();
        for (int x = shape.minX; x <= shape.maxX; x++) {
            for (int z = shape.minZ; z <= shape.maxZ; z++) {
                final int floor = lowY.get(BlockPos.asLong(x, 0, z));
                if (floor == Integer.MAX_VALUE) {
                    continue;
                }
                for (int y = floor + 1; y <= shape.maxY; y++) {
                    final long cell = BlockPos.asLong(x, y, z);
                    if (shape.tight.contains(cell)) {
                        continue;
                    }
                    final long rowX = BlockPos.asLong(0, y, z);
                    final long rowZ = BlockPos.asLong(x, y, 0);
                    if (lowX.get(rowX) < x && x < highX.get(rowX) && lowZ.get(rowZ) < z && z < highZ.get(rowZ)) {
                        inside.add(cell);
                    }
                }
            }
        }

        // Split it into connected compartments, each with its openings to the outside
        final List<Compartment> compartments = new ArrayList<>();
        final LongOpenHashSet seen = new LongOpenHashSet();
        final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        for (final long start : inside) {
            if (!seen.add(start)) {
                continue;
            }
            final LongArrayList cells = new LongArrayList();
            final LongArrayList openingCells = new LongArrayList();
            final List<Byte> openingFaces = new ArrayList<>();
            queue.enqueue(start);
            while (!queue.isEmpty()) {
                final long cell = queue.dequeueLong();
                cells.add(cell);
                for (final Direction direction : DIRECTIONS) {
                    final long next = BlockPos.offset(cell, direction);
                    if (inside.contains(next)) {
                        if (seen.add(next)) {
                            queue.enqueue(next);
                        }
                    } else if (!shape.tight.contains(next)) {
                        openingCells.add(cell);
                        openingFaces.add((byte) direction.ordinal());
                    }
                }
            }
            final long[] cellArray = cells.toLongArray();
            final boolean[] occupied = new boolean[cellArray.length];
            for (int i = 0; i < cellArray.length; i++) {
                occupied[i] = shape.solid.contains(cellArray[i]);
            }
            final byte[] faces = new byte[openingFaces.size()];
            for (int i = 0; i < faces.length; i++) {
                faces[i] = openingFaces.get(i);
            }
            compartments.add(new Compartment(cellArray, occupied, openingCells.toLongArray(), faces));
        }

        // Water already inside stays where it was, shared out by how the old compartments overlap the new ones
        for (int index = 0; index < compartments.size(); index++) {
            final Compartment compartment = compartments.get(index);
            final Int2DoubleOpenHashMap overlap = new Int2DoubleOpenHashMap();
            for (final long cell : compartment.cells) {
                hull.owner.put(cell, index);
                final int old = previousOwner.get(cell);
                if (old >= 0 && old < previous.length) {
                    overlap.addTo(old, 1.0);
                }
            }
            double water = 0.0;
            for (final var share : overlap.int2DoubleEntrySet()) {
                final Compartment old = previous[share.getIntKey()];
                water += old.water * share.getDoubleValue() / Math.max(1, old.cells.length);
            }
            compartment.water = Math.min(water, compartment.capacity);
        }
        hull.compartments = compartments.toArray(new Compartment[0]);
    }

    /**
     * This tick's lift, inside water weight, damping moments and flooding for one hull.
     */
    private void measure(final ServerSubLevel vehicle, final Hull hull, final BodyMotion motion, final double gravity) {
        final Pose3dc pose = motion.pose;
        final double comX = pose.position().x();
        final double comZ = pose.position().z();
        hull.lift = hull.liftX = hull.liftY = hull.liftZ = 0.0;
        hull.weight = hull.weightX = hull.weightY = hull.weightZ = 0.0;
        hull.area = hull.m1x = hull.m1z = hull.sxx = hull.szz = hull.sxz = 0.0;
        hull.inflow = 0.0;
        final boolean flooding = CrashConfig.FLOODING.getAsBoolean();

        for (final Compartment compartment : hull.compartments) {
            final int n = compartment.cells.length;
            this.ensureScratch(n);
            for (int i = 0; i < n; i++) {
                final long cell = compartment.cells[i];
                final Vector3d world = pose.transformPosition(this.point.set(BlockPos.getX(cell) + 0.5, BlockPos.getY(cell) + 0.5, BlockPos.getZ(cell) + 0.5));
                this.worldX[i] = world.x;
                this.worldY[i] = world.y;
                this.worldZ[i] = world.z;
            }

            final double level = compartment.water > 1.0e-6 ? this.insideLevel(compartment, n) : Double.NEGATIVE_INFINITY;
            double insideArea = 0.0;
            for (int i = 0; i < n; i++) {
                final double x = this.worldX[i];
                final double y = this.worldY[i];
                final double z = this.worldZ[i];
                final double low = y - 0.5;
                final double high = y + 0.5;
                final double surface = this.outside(Mth.floor(x), Mth.floor(z), y);
                final boolean wet = !Double.isNaN(surface);
                final double density = wet ? this.densities.get(ChunkPos.asLong(Mth.floor(x), Mth.floor(z))) : WATER_DENSITY;

                if (level > low && level < high) {
                    insideArea += 1.0;
                }
                if (compartment.occupied[i]) {
                    continue;
                }

                // Air below the waterline outside keeps the water out, and lifts
                final double air = wet ? Math.max(0.0, Math.min(high, surface) - Math.max(low, level)) : 0.0;
                // Water inside above the waterline outside isn't held up by anything
                final double heavy = wet ? Math.max(0.0, Math.min(high, level) - Math.max(low, surface)) : Math.max(0.0, Math.min(high, level) - low);
                if (air > 0.0) {
                    final double force = density * gravity * air;
                    hull.lift += force;
                    hull.liftX += force * x;
                    hull.liftY += force * y;
                    hull.liftZ += force * z;
                }
                if (heavy > 0.0) {
                    final double force = WATER_DENSITY * gravity * heavy;
                    hull.weight += force;
                    hull.weightX += force * x;
                    hull.weightY += force * y;
                    hull.weightZ += force * z;
                }

                // How much of the cell is under water, for damping
                final double submerged = wet ? Mth.clamp(surface - low, 0.0, 1.0) : 0.0;
                if (submerged > 0.0) {
                    final double rx = x - comX;
                    final double rz = z - comZ;
                    hull.area += submerged;
                    hull.m1x += submerged * rx;
                    hull.m1z += submerged * rz;
                    hull.sxx += submerged * rx * rx;
                    hull.szz += submerged * rz * rz;
                    hull.sxz += submerged * rx * rz;
                }
            }

            if (flooding) {
                this.flow(vehicle, hull, compartment, pose, level, Math.max(1.0, insideArea), gravity);
            }
        }
    }

    /**
     * Water running in or out through a compartment's openings over one tick.
     */
    private void flow(final ServerSubLevel vehicle, final Hull hull, final Compartment compartment, final Pose3dc pose, final double level,
                      final double insideArea, final double gravity) {
        double total = 0.0;
        double strongest = 0.0;
        double largestHead = 0.0;
        double jetX = 0.0, jetY = 0.0, jetZ = 0.0, jetDX = 0.0, jetDY = 0.0, jetDZ = 0.0;
        for (int j = 0; j < compartment.openingCells.length; j++) {
            final long cell = compartment.openingCells[j];
            final Direction face = DIRECTIONS[compartment.openingFaces[j]];
            final Vector3d world = pose.transformPosition(this.point.set(
                    BlockPos.getX(cell) + 0.5 + face.getStepX() * 0.5,
                    BlockPos.getY(cell) + 0.5 + face.getStepY() * 0.5,
                    BlockPos.getZ(cell) + 0.5 + face.getStepZ() * 0.5));
            final double surface = this.outside(Mth.floor(world.x), Mth.floor(world.z), world.y);
            final double outside = !Double.isNaN(surface) && surface > world.y ? surface - world.y : 0.0;
            final double inside = level > world.y ? level - world.y : 0.0;
            final double head = outside - inside;
            if (head == 0.0) {
                continue;
            }
            // One square metre of opening per face
            final double rate = Math.signum(head) * DISCHARGE * Math.sqrt(2.0 * gravity * Math.abs(head));
            total += rate;
            largestHead = Math.max(largestHead, Math.abs(head));
            if (rate > strongest) {
                strongest = rate;
                jetX = world.x;
                jetY = world.y;
                jetZ = world.z;
                final Vector3d inward = pose.transformNormal(this.impulse.set(-face.getStepX(), -face.getStepY(), -face.getStepZ()));
                jetDX = inward.x;
                jetDY = inward.y;
                jetDZ = inward.z;
            }
        }
        if (total == 0.0) {
            return;
        }

        // Never more in one tick than would bring the levels inside and out together
        final double limit = Math.max(0.05, largestHead * insideArea);
        final double change = Mth.clamp(total * TICK, -limit, limit);
        compartment.water = Mth.clamp(compartment.water + change, 0.0, compartment.capacity);
        hull.inflow += Math.max(0.0, total);
        if (strongest > 0.5 && compartment.water < compartment.capacity) {
            this.state.effects().breach(vehicle, jetX, jetY, jetZ, jetDX, jetDY, jetDZ, strongest);
        }
    }

    /**
     * The height of the water surface inside a compartment: its water settles into the lowest cells.
     */
    private double insideLevel(final Compartment compartment, final int n) {
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            if (!compartment.occupied[i]) {
                low = Math.min(low, this.worldY[i] - 0.5);
                high = Math.max(high, this.worldY[i] + 0.5);
            }
        }
        if (low > high) {
            return Double.NEGATIVE_INFINITY;
        }
        for (int iteration = 0; iteration < 30; iteration++) {
            final double middle = 0.5 * (low + high);
            double volume = 0.0;
            for (int i = 0; i < n; i++) {
                if (!compartment.occupied[i]) {
                    volume += Mth.clamp(middle - (this.worldY[i] - 0.5), 0.0, 1.0);
                }
            }
            if (volume < compartment.water) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return 0.5 * (low + high);
    }

    private void ensureScratch(final int n) {
        if (this.worldX.length < n) {
            final int size = Math.max(n, this.worldX.length * 2);
            this.worldX = new double[size];
            this.worldY = new double[size];
            this.worldZ = new double[size];
        }
    }

    /**
     * The water level outside the hull at a column. Under a hull the world's own water can be lower than the water
     * around it (pockets of air left where the hull used to be, water still flowing back in); the hull doesn't care,
     * the water around it is what presses on it.
     */
    private double outside(final int x, final int z, final double nearY) {
        final double local = this.surface(x, z, nearY);
        if (Double.isNaN(local) || Double.isNaN(this.waterline)) {
            return local;
        }
        return Math.max(local, this.waterline);
    }

    /**
     * @return the level of the open water around a vehicle (the median of a ring of columns just outside it), or NaN
     */
    private double waterline(final Pose3dc pose, final VehicleShape shape) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            final Vector3d world = pose.transformPosition(this.point.set(
                    (corner & 1) == 0 ? shape.minX : shape.maxX + 1,
                    (corner & 2) == 0 ? shape.minY : shape.maxY + 1,
                    (corner & 4) == 0 ? shape.minZ : shape.maxZ + 1));
            minX = Math.min(minX, world.x);
            minY = Math.min(minY, world.y);
            minZ = Math.min(minZ, world.z);
            maxX = Math.max(maxX, world.x);
            maxZ = Math.max(maxZ, world.z);
        }
        final double[] levels = new double[16];
        int count = 0;
        for (int n = 0; n < 16; n++) {
            final double t = (n % 4 + 0.5) / 4.0;
            final double x;
            final double z;
            switch (n / 4) {
                case 0 -> { x = minX + (maxX - minX) * t; z = minZ - 1.5; }
                case 1 -> { x = minX + (maxX - minX) * t; z = maxZ + 1.5; }
                case 2 -> { x = minX - 1.5; z = minZ + (maxZ - minZ) * t; }
                default -> { x = maxX + 1.5; z = minZ + (maxZ - minZ) * t; }
            }
            final double level = this.surface(Mth.floor(x), Mth.floor(z), minY);
            if (!Double.isNaN(level)) {
                levels[count++] = level;
            }
        }
        if (count == 0) {
            return Double.NaN;
        }
        java.util.Arrays.sort(levels, 0, count);
        return levels[count / 2];
    }

    /**
     * @return if any part of the vehicle reaches down into a liquid
     */
    private boolean nearLiquid(final Pose3dc pose, final VehicleShape shape) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            final Vector3d world = pose.transformPosition(this.point.set(
                    (corner & 1) == 0 ? shape.minX : shape.maxX + 1,
                    (corner & 2) == 0 ? shape.minY : shape.maxY + 1,
                    (corner & 4) == 0 ? shape.minZ : shape.maxZ + 1));
            minX = Math.min(minX, world.x);
            minY = Math.min(minY, world.y);
            minZ = Math.min(minZ, world.z);
            maxX = Math.max(maxX, world.x);
            maxZ = Math.max(maxZ, world.z);
        }
        for (int i = 0; i <= 2; i++) {
            for (int k = 0; k <= 2; k++) {
                final double x = minX + (maxX - minX) * (0.1 + 0.4 * i);
                final double z = minZ + (maxZ - minZ) * (0.1 + 0.4 * k);
                final double surface = this.surface(Mth.floor(x), Mth.floor(z), minY);
                if (!Double.isNaN(surface) && surface > minY) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The height of the liquid surface in a world column near a height, or NaN if there's none there.
     */
    private double surface(final int x, final int z, final double nearY) {
        final long key = ChunkPos.asLong(x, z);
        if (this.surfaces.containsKey(key)) {
            return this.surfaces.get(key);
        }
        double surface = Double.NaN;
        double density = WATER_DENSITY;
        final LevelChunk chunk = this.level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk != null) {
            final int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15);
            final int from = Math.min(top, Mth.floor(nearY) + 24);
            final int to = Math.max(this.level.getMinBuildHeight(), Mth.floor(nearY) - 24);
            for (int y = from; y >= to; y--) {
                final FluidState fluid = chunk.getFluidState(x, y, z);
                if (!fluid.isEmpty()) {
                    surface = y + fluid.getHeight(this.level, this.cursor.set(x, y, z));
                    density = fluid.is(FluidTags.LAVA) ? LAVA_DENSITY : WATER_DENSITY;
                    break;
                }
                // Ground under the vehicle: whatever water is below that isn't around the hull. A dock or an overhang
                // above it doesn't count
                if (y < nearY - 1.0 && chunk.getBlockState(this.cursor.set(x, y, z)).blocksMotion()) {
                    break;
                }
            }
        }
        this.surfaces.put(key, surface);
        this.densities.put(key, density);
        return surface;
    }

    @Nullable
    Hull hull(final ServerSubLevel vehicle) {
        return this.hulls.get(vehicle);
    }
}
