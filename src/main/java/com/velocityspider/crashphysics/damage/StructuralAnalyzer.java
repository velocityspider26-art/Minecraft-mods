package com.velocityspider.crashphysics.damage;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashConfig;
import com.velocityspider.crashphysics.material.CrashMaterials;
import com.velocityspider.crashphysics.physics.StructuralSolver;
import com.velocityspider.crashphysics.sable.LevelCrashState;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the joints of a crashing vehicle that can't carry the crash loads, and tears them apart.
 * <p>
 * The vehicle's blocks become a graph (face neighbours fully connected, edge neighbours weakly, matching how Sable
 * decides which blocks hold together). Every block is given the acceleration it is experiencing (linear deceleration
 * plus the effects of the vehicle spinning up or tumbling), and {@link StructuralSolver} finds which connections fail.
 * Once those blocks break, Sable splits the loose part off into its own physics object.
 */
public final class StructuralAnalyzer {

    private static final int[][] NEIGHBORS;
    private static final float FACE_WEIGHT = 1.0f;
    private static final float EDGE_WEIGHT = 0.25f;

    /**
     * Don't re-analyse the same vehicle every tick of a long crash
     */
    private static final long COOLDOWN_TICKS = 4;

    private static final Reference2LongOpenHashMap<ServerSubLevel> LAST_ANALYSIS = new Reference2LongOpenHashMap<>();

    static {
        final List<int[]> offsets = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    final int nonZero = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (nonZero == 1 || nonZero == 2) {
                        offsets.add(new int[]{dx, dy, dz});
                    }
                }
            }
        }
        NEIGHBORS = offsets.toArray(int[][]::new);
    }

    private StructuralAnalyzer() {
    }

    /**
     * @param linearAcceleration  proper acceleration of the centre of mass [m/s²], world space
     * @param angularAcceleration [rad/s²], world space
     * @param angularVelocity     [rad/s], world space
     * @param impactBlocks        plot positions of the blocks the impact loads came in through
     */
    public static void analyze(final LevelCrashState state, final ServerSubLevel vehicle, final Pose3dc pose,
                               final Vector3dc linearAcceleration, final Vector3dc angularAcceleration, final Vector3dc angularVelocity,
                               final LongSet impactBlocks) {
        final long now = state.tick();
        synchronized (LAST_ANALYSIS) {
            if (LAST_ANALYSIS.containsKey(vehicle)) {
                final long last = LAST_ANALYSIS.getLong(vehicle);
                if (now >= last && now - last < COOLDOWN_TICKS) {
                    return;
                }
            }
            LAST_ANALYSIS.put(vehicle, now);
            if (LAST_ANALYSIS.size() > 256) {
                LAST_ANALYSIS.reference2LongEntrySet().removeIf(entry -> entry.getKey().isRemoved());
            }
        }

        final BoundingBox3ic bounds = vehicle.getPlot().getBoundingBox();
        if (bounds == null) {
            return;
        }

        final int maxBlocks = CrashConfig.MAX_STRUCTURAL_BLOCKS.getAsInt();
        final long volume = (long) (bounds.maxX() - bounds.minX() + 1) * (bounds.maxY() - bounds.minY() + 1) * (bounds.maxZ() - bounds.minZ() + 1);
        if (volume <= 0 || volume > Math.max(200_000L, maxBlocks * 8L)) {
            return;
        }

        final double kgPerSableMass = CrashConfig.KG_PER_SABLE_MASS.getAsDouble();
        final double jointScale = CrashConfig.JOINT_STRENGTH_MULTIPLIER.getAsDouble() * CrashConfig.STRENGTH_MULTIPLIER.getAsDouble()
                * CrashConfig.VEHICLE_STRENGTH_MULTIPLIER.getAsDouble();

        // Gather the blocks (the plot's bounds are in absolute level coordinates)
        final ServerLevel level = vehicle.getLevel();
        final Long2IntOpenHashMap index = new Long2IntOpenHashMap();
        index.defaultReturnValue(-1);
        final LongArrayList positions = new LongArrayList();
        final List<BlockState> states = new ArrayList<>();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    final BlockState blockState = level.getBlockState(pos.set(x, y, z));
                    if (blockState.isAir() || blockState.getCollisionShape(level, pos).isEmpty()) {
                        continue;
                    }
                    if (positions.size() >= maxBlocks) {
                        return;
                    }
                    index.put(pos.asLong(), positions.size());
                    positions.add(pos.asLong());
                    states.add(blockState);
                }
            }
        }

        final int count = positions.size();
        if (count < 2) {
            return;
        }

        // Adjacency, positions, masses, strengths and accelerations, all in the vehicle's own frame
        final int[] start = new int[count + 1];
        final IntArrayList neighbors = new IntArrayList(count * 6);
        final List<Float> weightList = new ArrayList<>(count * 6);
        final double[] position = new double[count * 3];
        final double[] mass = new double[count];
        final double[] jointStrength = new double[count];
        final double[] crushStrength = new double[count];
        final double[] acceleration = new double[count * 3];
        final double crushScale = CrashConfig.STRENGTH_MULTIPLIER.getAsDouble() * CrashConfig.VEHICLE_STRENGTH_MULTIPLIER.getAsDouble();
        final Vector3d local = new Vector3d();

        final Vector3dc com = pose.position();
        final Vector3d offset = new Vector3d();
        final Vector3d alphaCrossR = new Vector3d();
        final Vector3d omegaCrossR = new Vector3d();
        final Vector3d centripetal = new Vector3d();

        for (int i = 0; i < count; i++) {
            final long packed = positions.getLong(i);
            final int x = BlockPos.getX(packed);
            final int y = BlockPos.getY(packed);
            final int z = BlockPos.getZ(packed);

            start[i] = neighbors.size();
            for (final int[] neighbor : NEIGHBORS) {
                final int j = index.get(BlockPos.asLong(x + neighbor[0], y + neighbor[1], z + neighbor[2]));
                if (j >= 0) {
                    neighbors.add(j);
                    weightList.add(Math.abs(neighbor[0]) + Math.abs(neighbor[1]) + Math.abs(neighbor[2]) == 1 ? FACE_WEIGHT : EDGE_WEIGHT);
                }
            }

            final BlockState blockState = states.get(i);
            final Double sableMass = ((BlockStateExtension) blockState).sable$getProperty(PhysicsBlockPropertyTypes.MASS.get());
            mass[i] = (sableMass != null ? sableMass : 1.0) * kgPerSableMass;
            jointStrength[i] = CrashMaterials.get(blockState).profile().effectiveJointStrength() * jointScale;
            crushStrength[i] = CrashMaterials.get(blockState).profile().effectiveStrength() * crushScale;

            position[i * 3] = x + 0.5;
            position[i * 3 + 1] = y + 0.5;
            position[i * 3 + 2] = z + 0.5;

            // Acceleration of this block: a = a_com + α × r + ω × (ω × r), in the world, then into the vehicle's frame
            pose.transformPosition(offset.set(x + 0.5, y + 0.5, z + 0.5)).sub(com);
            angularAcceleration.cross(offset, alphaCrossR);
            angularVelocity.cross(offset, omegaCrossR);
            angularVelocity.cross(omegaCrossR, centripetal);

            local.set(linearAcceleration).add(alphaCrossR).add(centripetal);
            pose.transformNormalInverse(local);
            acceleration[i * 3] = local.x;
            acceleration[i * 3 + 1] = local.y;
            acceleration[i * 3 + 2] = local.z;
        }
        start[count] = neighbors.size();

        final float[] weights = new float[weightList.size()];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = weightList.get(i);
        }

        // The impact loads enter through the blocks that were hit, or what is left next to them
        final IntArrayList sources = new IntArrayList();
        for (final LongIterator iterator = impactBlocks.iterator(); iterator.hasNext(); ) {
            final long impact = iterator.nextLong();
            final int direct = index.get(impact);
            if (direct >= 0) {
                sources.add(direct);
                continue;
            }
            for (final int[] neighbor : NEIGHBORS) {
                final int adjacent = index.get(BlockPos.offset(impact, neighbor[0], neighbor[1], neighbor[2]));
                if (adjacent >= 0) {
                    sources.add(adjacent);
                }
            }
        }
        if (sources.isEmpty()) {
            return;
        }

        final StructuralSolver.Graph graph = new StructuralSolver.Graph(count, start, neighbors.toIntArray(), weights, position, mass, acceleration,
                jointStrength, crushStrength, sources.toIntArray());
        final List<StructuralSolver.Failure> failures = StructuralSolver.solve(graph, 8);

        if (CrashConfig.DEBUG_LOGGING.getAsBoolean()) {
            CrashPhysics.LOGGER.info("Structural check on {}: {} blocks, {} load entry points, a = {} m/s², {} failures",
                    vehicle, count, sources.size(), String.format("%.0f", linearAcceleration.length()), failures.size());
        }

        for (final StructuralSolver.Failure failure : failures) {
            if (CrashConfig.DEBUG_LOGGING.getAsBoolean()) {
                CrashPhysics.LOGGER.info("Structural failure on {}: {} kN load on a {} kN joint, breaking {} blocks",
                        vehicle, Math.round(failure.load() / 1000.0), Math.round(failure.capacity() / 1000.0), failure.brokenNodes().length);
            }
            for (final int node : failure.brokenNodes()) {
                state.destruction().tearOff(vehicle, BlockPos.of(positions.getLong(node)));
            }
        }
    }
}
