package com.velocityspider.crashphysics.sable;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.util.BlockScan;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
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

    /**
     * Below this much travel per sub-step a block can't tunnel through anything, and Rapier's own contacts are enough
     * [blocks]
     */
    private static final double CCD_TRAVEL = 0.1;

    /**
     * Space kept above the highest terrain block of a column before it counts as clear, for thin blocks (carpets,
     * snow layers) that the heightmap doesn't count [blocks]
     */
    private static final int CLEARANCE = 2;

    private static final int MAX_LEADING_BLOCKS = 4096;
    private static final int MAX_COLUMNS = 256 * 256;

    private final ServerLevel level;
    private final LevelCrashState state;
    private final Reference2ObjectOpenHashMap<ServerSubLevel, Shape> shapes = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<BlockState, double[]> boundsCache = new Reference2ObjectOpenHashMap<>();
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
    private final Vector3d dragForce = new Vector3d();
    private final Vector3d dragPoint = new Vector3d();
    private double dragWeight;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

    @Nullable
    private LevelChunk lastChunk;
    private final BlockPos.MutableBlockPos changed = new BlockPos.MutableBlockPos();

    /**
     * The top of the terrain (or liquid) in each column around the vehicle being swept, from the world's heightmap
     */
    private int[] columns = new int[1024];
    private int columnsX;
    private int columnsZ;
    private int columnsWidth;
    private int columnsDepth;

    /**
     * The blocks a vehicle is made of, kept up to date block by block as they change
     */
    private static final class Shape {
        final LongOpenHashSet solid = new LongOpenHashSet();
        /**
         * The blocks on the outside, each with the directions it is the outermost block of its row in (one bit per
         * {@link Direction}): the faces that meet whatever the vehicle runs into
         */
        final Long2ByteOpenHashMap outer = new Long2ByteOpenHashMap();
        boolean outerStale = true;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        void add(final int x, final int y, final int z) {
            if (!this.solid.add(BlockPos.asLong(x, y, z))) {
                return;
            }
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
            this.outerStale = true;
        }

        void remove(final long block) {
            if (!this.solid.remove(block) || this.outerStale) {
                return;
            }
            final byte flags = this.outer.remove(block);
            if (flags == 0) {
                return;
            }
            // In each row it was the outermost block of, the next one behind it now is
            for (final Direction direction : DIRECTIONS) {
                final int bit = 1 << direction.ordinal();
                if ((flags & bit) == 0) {
                    continue;
                }
                long next = block;
                while (true) {
                    next = BlockPos.offset(next, direction.getOpposite());
                    if (!this.contains(next)) {
                        break;
                    }
                    if (this.solid.contains(next)) {
                        this.outer.put(next, (byte) (this.outer.get(next) | bit));
                        break;
                    }
                }
            }
        }

        private boolean contains(final long block) {
            final int x = BlockPos.getX(block);
            final int y = BlockPos.getY(block);
            final int z = BlockPos.getZ(block);
            return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
        }

        /**
         * Works out which blocks are the outermost of their rows along each axis.
         */
        void rebuildOuter() {
            this.outer.clear();
            final Long2IntOpenHashMap lowX = new Long2IntOpenHashMap(), highX = new Long2IntOpenHashMap();
            final Long2IntOpenHashMap lowY = new Long2IntOpenHashMap(), highY = new Long2IntOpenHashMap();
            final Long2IntOpenHashMap lowZ = new Long2IntOpenHashMap(), highZ = new Long2IntOpenHashMap();
            lowX.defaultReturnValue(Integer.MAX_VALUE);
            lowY.defaultReturnValue(Integer.MAX_VALUE);
            lowZ.defaultReturnValue(Integer.MAX_VALUE);
            highX.defaultReturnValue(Integer.MIN_VALUE);
            highY.defaultReturnValue(Integer.MIN_VALUE);
            highZ.defaultReturnValue(Integer.MIN_VALUE);

            for (final LongIterator iterator = this.solid.iterator(); iterator.hasNext(); ) {
                final long block = iterator.nextLong();
                final int x = BlockPos.getX(block);
                final int y = BlockPos.getY(block);
                final int z = BlockPos.getZ(block);
                final long rowX = BlockPos.asLong(0, y, z);
                final long rowY = BlockPos.asLong(x, 0, z);
                final long rowZ = BlockPos.asLong(x, y, 0);
                if (x < lowX.get(rowX)) lowX.put(rowX, x);
                if (x > highX.get(rowX)) highX.put(rowX, x);
                if (y < lowY.get(rowY)) lowY.put(rowY, y);
                if (y > highY.get(rowY)) highY.put(rowY, y);
                if (z < lowZ.get(rowZ)) lowZ.put(rowZ, z);
                if (z > highZ.get(rowZ)) highZ.put(rowZ, z);
            }

            for (final LongIterator iterator = this.solid.iterator(); iterator.hasNext(); ) {
                final long block = iterator.nextLong();
                final int x = BlockPos.getX(block);
                final int y = BlockPos.getY(block);
                final int z = BlockPos.getZ(block);
                final long rowX = BlockPos.asLong(0, y, z);
                final long rowY = BlockPos.asLong(x, 0, z);
                final long rowZ = BlockPos.asLong(x, y, 0);
                int flags = 0;
                if (x == lowX.get(rowX)) flags |= WEST;
                if (x == highX.get(rowX)) flags |= EAST;
                if (y == lowY.get(rowY)) flags |= DOWN;
                if (y == highY.get(rowY)) flags |= UP;
                if (z == lowZ.get(rowZ)) flags |= NORTH;
                if (z == highZ.get(rowZ)) flags |= SOUTH;
                if (flags != 0) {
                    this.outer.put(block, (byte) flags);
                }
            }
            this.outerStale = false;
        }
    }

    private static final int DOWN = 1 << Direction.DOWN.ordinal();
    private static final int UP = 1 << Direction.UP.ordinal();
    private static final int NORTH = 1 << Direction.NORTH.ordinal();
    private static final int SOUTH = 1 << Direction.SOUTH.ordinal();
    private static final int WEST = 1 << Direction.WEST.ordinal();
    private static final int EAST = 1 << Direction.EAST.ordinal();

    private static final Direction[] DIRECTIONS = Direction.values();

    /**
     * Below this a vehicle moving through a liquid isn't slowed noticeably [m/s]
     */
    private static final double DRAG_MIN_SPEED = 0.5;

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
            if (speed < DRAG_MIN_SPEED && spin < 0.25) {
                continue;
            }

            final RigidBodyHandle handle = system.getPhysicsHandle(vehicle);
            if (handle == null || !handle.isValid()) {
                continue;
            }

            final long shapeTimer = CrashTimings.begin();
            final Shape shape = this.shape(vehicle);
            CrashTimings.end(CrashTimings.Section.SWEEP_SHAPE, shapeTimer);
            if (shape == null) {
                continue;
            }

            // Nothing but air anywhere near where it is going: nothing to hit, nothing to slow it
            if (!this.anythingNear(motion, shape, speed, spin, dt)) {
                continue;
            }

            this.dragForce.zero();
            this.dragPoint.zero();
            this.dragWeight = 0.0;
            final long findTimer = CrashTimings.begin();
            this.findHits(motion, shape, dt, minSpeed);
            CrashTimings.end(CrashTimings.Section.SWEEP_FIND, findTimer);
            if (!this.hits.isEmpty()) {
                final long resolveTimer = CrashTimings.begin();
                anything |= this.resolveHits(system, motion, handle, dt, minSpeed);
                CrashTimings.end(CrashTimings.Section.SWEEP_RESOLVE, resolveTimer);
            }
            if (this.dragWeight > 0.0) {
                this.applyDrag(motion, handle, dt);
            }
        }

        if (anything) {
            // Break what gave way now, so it is gone before Rapier steps
            final long timer = CrashTimings.begin();
            this.state.destruction().process();
            CrashTimings.end(CrashTimings.Section.SWEEP_BREAKING, timer);
        }

        if (this.state.tick() % 200 == 0 && !this.shapes.isEmpty()) {
            this.shapes.keySet().removeIf(ServerSubLevel::isRemoved);
        }
    }

    /**
     * Keeps the shape of a vehicle up to date when one of its blocks changes: broken off in a crash, built, mined.
     */
    void blockChanged(final int x, final int y, final int z, final BlockState oldState, final BlockState newState) {
        if (this.shapes.isEmpty()) {
            return;
        }
        final SubLevel subLevel = Sable.HELPER.getContaining(this.level, x >> 4, z >> 4);
        if (!(subLevel instanceof final ServerSubLevel vehicle)) {
            return;
        }
        final Shape shape = this.shapes.get(vehicle);
        if (shape == null) {
            return;
        }
        this.changed.set(x, y, z);
        final boolean was = !oldState.isAir() && !oldState.getCollisionShape(this.level, this.changed).isEmpty();
        final boolean is = !newState.isAir() && !newState.getCollisionShape(this.level, this.changed).isEmpty();
        if (is && !was) {
            shape.add(x, y, z);
        } else if (was && !is) {
            shape.remove(this.changed.asLong());
        }
    }

    /**
     * Checks whether there is anything but air (terrain or liquid) around the space the vehicle sweeps this sub-step.
     */
    private boolean anythingNear(final BodyMotion motion, final Shape shape, final double speed, final double spin, final double dt) {
        final Pose3dc pose = motion.pose;
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            final Vector3d world = pose.transformPosition(this.center.set(
                    (corner & 1) == 0 ? shape.minX : shape.maxX + 1,
                    (corner & 2) == 0 ? shape.minY : shape.maxY + 1,
                    (corner & 4) == 0 ? shape.minZ : shape.maxZ + 1));
            minX = Math.min(minX, world.x);
            minY = Math.min(minY, world.y);
            minZ = Math.min(minZ, world.z);
            maxX = Math.max(maxX, world.x);
            maxY = Math.max(maxY, world.y);
            maxZ = Math.max(maxZ, world.z);
        }
        final double radius = 0.5 * Math.sqrt((maxX - minX) * (maxX - minX) + (maxY - minY) * (maxY - minY) + (maxZ - minZ) * (maxZ - minZ));
        final double reach = (speed + spin * radius) * dt + 1.0;
        if (!BlockScan.anythingIn(this.level, minX - reach, minY - reach, minZ - reach, maxX + reach, maxY + reach, maxZ + reach)) {
            return false;
        }

        // Flying over the ground: everything below is further down than it can get this sub-step
        final int highest = this.gatherColumns(Mth.floor(minX - reach), Mth.floor(minZ - reach), Mth.floor(maxX + reach), Mth.floor(maxZ + reach));
        return minY - reach < highest + CLEARANCE;
    }

    /**
     * Reads the heightmap for a rectangle of columns.
     *
     * @return the highest terrain or liquid block in it, or {@link Integer#MAX_VALUE} if it is too big to look at
     */
    private int gatherColumns(final int x0, final int z0, final int x1, final int z1) {
        final int width = x1 - x0 + 1;
        final int depth = z1 - z0 + 1;
        if ((long) width * depth > MAX_COLUMNS) {
            this.columnsWidth = 0;
            return Integer.MAX_VALUE;
        }
        if (this.columns.length < width * depth) {
            this.columns = new int[Math.max(width * depth, this.columns.length * 2)];
        }
        this.columnsX = x0;
        this.columnsZ = z0;
        this.columnsWidth = width;
        this.columnsDepth = depth;

        final int nothing = this.level.getMinBuildHeight() - 64;
        int highest = nothing;
        for (int chunkX = x0 >> 4; chunkX <= x1 >> 4; chunkX++) {
            for (int chunkZ = z0 >> 4; chunkZ <= z1 >> 4; chunkZ++) {
                final LevelChunk chunk = this.level.getChunkSource().getChunkNow(chunkX, chunkZ);
                final Heightmap heightmap = chunk != null ? chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING) : null;
                final int fromX = Math.max(x0, chunkX << 4);
                final int toX = Math.min(x1, (chunkX << 4) + 15);
                final int fromZ = Math.max(z0, chunkZ << 4);
                final int toZ = Math.min(z1, (chunkZ << 4) + 15);
                for (int x = fromX; x <= toX; x++) {
                    for (int z = fromZ; z <= toZ; z++) {
                        // Unloaded chunks hold nothing to hit
                        final int top = heightmap != null ? heightmap.getFirstAvailable(x & 15, z & 15) - 1 : nothing;
                        this.columns[(x - x0) * depth + (z - z0)] = top;
                        highest = Math.max(highest, top);
                    }
                }
            }
        }
        return highest;
    }

    /**
     * @return if the heightmap shows nothing but air in a box of columns from some height up
     */
    private boolean clearAbove(final double minX, final double maxX, final double minZ, final double maxZ, final double bottom) {
        if (this.columnsWidth == 0) {
            return false;
        }
        final int x0 = Mth.floor(minX) - this.columnsX;
        final int x1 = Mth.floor(maxX) - this.columnsX;
        final int z0 = Mth.floor(minZ) - this.columnsZ;
        final int z1 = Mth.floor(maxZ) - this.columnsZ;
        if (x0 < 0 || z0 < 0 || x1 >= this.columnsWidth || z1 >= this.columnsDepth) {
            return false;
        }
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (bottom < this.columns[x * this.columnsDepth + z] + CLEARANCE) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Slows a vehicle moving through water or lava: each leading face pushes liquid out of the way, with a force
     * ½·ρ·Cd·A·v², applied at the centre of pressure so a nose-down plunge also pitches.
     */
    private void applyDrag(final BodyMotion motion, final RigidBodyHandle handle, final double dt) {
        final ServerSubLevel vehicle = motion.subLevel;
        final MassData massData = vehicle.getMassTracker();
        final double kgPerSableMass = this.state.collector.kgPerSableMass();
        if (massData == null || massData.isInvalid() || !(massData.getMass() > 0.0)) {
            return;
        }

        // Never more than half the momentum in one step, so it can't reverse the motion
        final double massKg = massData.getMass() * kgPerSableMass;
        final double speed = handle.getLinearVelocity(this.linear).length();
        final Vector3d impulse = this.local.set(this.dragForce).mul(dt);
        final double limit = 0.5 * massKg * speed;
        if (impulse.length() > limit) {
            impulse.normalize(limit);
        }
        if (!impulse.isFinite() || impulse.lengthSquared() < 1.0e-12) {
            return;
        }

        final Pose3dc pose = motion.pose;
        final Vector3d point = pose.transformPositionInverse(this.plot.set(this.dragPoint).div(this.dragWeight));
        pose.transformNormalInverse(impulse.div(kgPerSableMass));
        handle.applyImpulseAtPoint(point, impulse);
        motion.refreshVelocity(handle);
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

        final boolean drag = CrashConfig.WATER_DRAG.getAsBoolean();
        final double dragCoefficient = CrashConfig.DRAG_COEFFICIENT.getAsDouble();
        if (shape.outerStale) {
            shape.rebuildOuter();
        }
        final boolean pending = this.state.destruction().anyPending();
        int leading = 0;
        for (final ObjectIterator<Long2ByteMap.Entry> iterator = shape.outer.long2ByteEntrySet().fastIterator(); iterator.hasNext(); ) {
            final Long2ByteMap.Entry entry = iterator.next();
            final long block = entry.getLongKey();
            final int flags = entry.getByteValue();
            if (pending && this.state.destruction().isPending(block)) {
                // Already broken, only waiting to be removed
                continue;
            }
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
            if (speed < DRAG_MIN_SPEED) {
                continue;
            }

            // Nothing but air anywhere along its path
            final double dx = v.x * dt;
            final double dy = v.y * dt;
            final double dz = v.z * dt;
            if (this.clearAbove(c.x - extentX + Math.min(dx, 0.0) - TOUCHING, c.x + extentX + Math.max(dx, 0.0) + TOUCHING,
                    c.z - extentZ + Math.min(dz, 0.0) - TOUCHING, c.z + extentZ + Math.max(dz, 0.0) + TOUCHING,
                    c.y - extentY + Math.min(dy, 0.0) - TOUCHING)) {
                continue;
            }

            // How much of its outside faces the way it is going, and whether it leads along an axis: only blocks out in
            // front can run into anything
            final Vector3d direction = pose.transformNormalInverse(this.local.set(v).div(speed));
            double frontArea = 0.0;
            boolean leads = false;
            if ((flags & (direction.x > 0.0 ? EAST : WEST)) != 0) {
                frontArea += Math.abs(direction.x);
                leads = Math.abs(direction.x) >= LEADING_AXIS;
            }
            if ((flags & (direction.y > 0.0 ? UP : DOWN)) != 0) {
                frontArea += Math.abs(direction.y);
                leads |= Math.abs(direction.y) >= LEADING_AXIS;
            }
            if ((flags & (direction.z > 0.0 ? SOUTH : NORTH)) != 0) {
                frontArea += Math.abs(direction.z);
                leads |= Math.abs(direction.z) >= LEADING_AXIS;
            }
            if (frontArea <= 0.0) {
                continue;
            }

            // Under water (or lava), its front faces push the liquid aside
            if (drag) {
                final double frontX = c.x + v.x / speed * 0.45;
                final double frontY = c.y + v.y / speed * 0.45;
                final double frontZ = c.z + v.z / speed * 0.45;
                final BlockState around = this.terrainAt(this.cursor.set(Mth.floor(frontX), Mth.floor(frontY), Mth.floor(frontZ)));
                if (around != null && !around.getFluidState().isEmpty()) {
                    final double density = around.getFluidState().is(FluidTags.LAVA) ? 3100.0 : 1000.0;
                    final double force = 0.5 * density * dragCoefficient * frontArea * speed * speed;
                    this.dragForce.sub(v.x / speed * force, v.y / speed * force, v.z / speed * force);
                    this.dragPoint.add(c.x * force, c.y * force, c.z * force);
                    this.dragWeight += force;
                }
            }

            // Slower than this Rapier's own contacts catch everything
            if (!leads || speed < minSpeed || speed * dt < CCD_TRAVEL) {
                continue;
            }
            if (++leading > MAX_LEADING_BLOCKS) {
                break;
            }

            this.sweepBlock(block, c.x, c.y, c.z, extentX, extentY, extentZ, dx, dy, dz, dt, minSpeed);
        }
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
        final boolean waterImpacts = CrashConfig.WATER_IMPACTS.getAsBoolean();
        final boolean pending = this.state.destruction().anyPending();

        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    final BlockState terrain = this.terrainAt(this.cursor.set(x, y, z));
                    if (terrain == null || terrain.isAir() || (pending && this.state.destruction().isPending(this.cursor.asLong()))) {
                        continue;
                    }
                    final double boxMinX, boxMinY, boxMinZ, boxMaxX, boxMaxY, boxMaxZ;
                    final boolean liquid;
                    final double[] bounds = this.collisionBounds(terrain);
                    if (bounds != null) {
                        boxMinX = x + bounds[0];
                        boxMinY = y + bounds[1];
                        boxMinZ = z + bounds[2];
                        boxMaxX = x + bounds[3];
                        boxMaxY = y + bounds[4];
                        boxMaxZ = z + bounds[5];
                        liquid = false;
                    } else if (waterImpacts && !terrain.getFluidState().isEmpty()) {
                        // The liquid up to its surface
                        boxMinX = x;
                        boxMinY = y;
                        boxMinZ = z;
                        boxMaxX = x + 1;
                        boxMaxY = y + terrain.getFluidState().getHeight(this.level, this.cursor);
                        boxMaxZ = z + 1;
                        liquid = true;
                    } else {
                        continue;
                    }

                    // Already touching: Rapier has it
                    if (minX + TOUCHING < boxMaxX && maxX - TOUCHING > boxMinX
                            && minY + TOUCHING < boxMaxY && maxY - TOUCHING > boxMinY
                            && minZ + TOUCHING < boxMaxZ && maxZ - TOUCHING > boxMinZ) {
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
                        final double boxLo = k == 0 ? boxMinX : k == 1 ? boxMinY : boxMinZ;
                        final double boxHi = k == 0 ? boxMaxX : k == 1 ? boxMaxY : boxMaxZ;

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

                    final AABB box = new AABB(boxMinX, boxMinY, boxMinZ, boxMaxX, boxMaxY, boxMaxZ);

                    // Hitting a face inside solid ground (between two ground blocks) is just sliding over the ground,
                    // and moving from water into more water isn't hitting the surface
                    if (this.faceIsInternal(x, y, z, box, axis, sign, liquid)) {
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

    /**
     * The bounds of a block state's collision shape within its cell, or null if it has none. Cached per state, except
     * for the few blocks whose shape moves around with their position.
     */
    @Nullable
    private double[] collisionBounds(final BlockState state) {
        if (state.hasOffsetFunction()) {
            final VoxelShape shape = state.getCollisionShape(this.level, this.cursor);
            if (shape.isEmpty()) {
                return null;
            }
            final AABB bounds = shape.bounds();
            return new double[]{bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ};
        }
        double[] bounds = this.boundsCache.get(state);
        if (bounds == null) {
            final VoxelShape shape = state.getCollisionShape(this.level, this.cursor);
            if (shape.isEmpty()) {
                bounds = NO_BOUNDS;
            } else {
                final AABB box = shape.bounds();
                bounds = new double[]{box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ};
            }
            this.boundsCache.put(state, bounds);
        }
        return bounds == NO_BOUNDS ? null : bounds;
    }

    private static final double[] NO_BOUNDS = new double[0];

    private boolean faceIsInternal(final int x, final int y, final int z, final AABB box, final int axis, final double sign, final boolean liquid) {
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
        if (beside == null || beside.isAir()) {
            return false;
        }
        return liquid ? !beside.getFluidState().isEmpty() : beside.isCollisionShapeFullBlock(this.level, this.neighbor);
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
                    contact.effectiveMassKg, contact.shareMassKg, capacity, Math.max(dt - hit.time, 0.0));

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
     * The vehicle's blocks: gathered once, then kept up to date as they change.
     */
    @Nullable
    private Shape shape(final ServerSubLevel vehicle) {
        final Shape cached = this.shapes.get(vehicle);
        if (cached != null) {
            return cached.solid.isEmpty() ? null : cached;
        }

        final Shape shape = new Shape();
        this.shapes.put(vehicle, shape);

        final BoundingBox3ic bounds = vehicle.getPlot().getBoundingBox();
        if (bounds == null) {
            return null;
        }
        final long volume = (long) (bounds.maxX() - bounds.minX() + 1) * (bounds.maxY() - bounds.minY() + 1) * (bounds.maxZ() - bounds.minZ() + 1);
        final long maxVolume = Math.max(200_000L, CrashConfig.MAX_STRUCTURAL_BLOCKS.getAsInt() * 8L);
        if (volume <= 0 || volume > maxVolume) {
            return null;
        }

        BlockScan.solidBlocks(this.level, bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ(),
                (x, y, z, blockState) -> shape.add(x, y, z));
        return shape.solid.isEmpty() ? null : shape;
    }
}
