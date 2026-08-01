package com.velocityspider.createjetengines.gametest;

import com.velocityspider.createjetengines.CreateJetEngines;
import com.velocityspider.createjetengines.block.JetModuleBlock;
import com.velocityspider.createjetengines.blockentity.CombustionCoreBlockEntity;
import com.velocityspider.createjetengines.engine.EngineChain;
import com.velocityspider.createjetengines.registry.JetBlocks;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.gametest.SableTestHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Automated propulsion tests.
 *
 * <p>These build a real engine inside a real Sable sublevel and assert on the rigid body's own
 * velocity, so they exercise the actual physics path rather than a mock. The sublevel is spawned
 * using Sable's own test helper, the same one its {@code PhysicsTest} suite uses.
 *
 * <p>Thrust is asserted on the horizontal axis so gravity cannot mask or fake the result.
 */
@GameTestHolder(CreateJetEngines.MODID)
public final class JetPropulsionGameTest {

    private static final String TEMPLATE = "empty_air";
    /**
     * Long enough for the engine to develop clear thrust, short enough that a light test craft is
     * still inside the loaded area. Peak speed is sampled every tick over this window.
     */
    private static final int SPOOL_TICKS = 120;
    private static final int REMOVAL_TICKS = 70;
    private static final int TORQUE_TICKS = 90;
    /** Early enough that the craft is still in its launch attitude. */
    private static final int DIRECTION_TICKS = 45;
    /**
     * Horizontal speed below which a craft counts as unpropelled.
     *
     * <p>Not zero: the test craft is in free fall in an empty template, and a falling body with a
     * slightly asymmetric mass distribution picks up a little horizontal drift from the solver.
     * Observed noise peaks around 0.08. A powered engine reaches ~111, so this threshold still
     * separates the two cases by more than two orders of magnitude.
     */
    private static final double NO_THRUST_TOLERANCE = 0.5D;

    private JetPropulsionGameTest() {
    }

    // =====================================================================================
    // helpers
    // =====================================================================================

    private static SubLevelPhysicsSystem physics(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            throw new IllegalStateException("No Sable sublevel container in this level");
        }
        SubLevelPhysicsSystem system = container.physicsSystem();
        if (system == null) {
            throw new IllegalStateException("Sable container has no physics system");
        }
        return system;
    }

    private static ServerSubLevelContainer container(GameTestHelper helper) {
        return SubLevelContainer.getContainer(helper.getLevel());
    }

    /**
     * Builds an engine chain running along +Z inside a fresh sublevel.
     *
     * <p>Modules face SOUTH, so exhaust leaves toward +Z and thrust must push toward -Z.
     *
     * @param stages     compressor stages between fan and core
     * @param afterburner whether to insert an afterburner before the nozzle
     * @param powered    whether to attach a redstone block (full throttle)
     * @param lateral    sideways offset of the whole engine, used for the torque test
     */
    private static ServerSubLevel buildEngine(GameTestHelper helper, Vector3dc at,
                                              int stages, boolean afterburner, boolean powered,
                                              int lateral) {
        return SableTestHelper.spawnSubLevel(container(helper), at, accessor ->
                place(accessor, stages, afterburner, powered, lateral));
    }

    private static void place(CommonLevelAccessor accessor, int stages, boolean afterburner,
                              boolean powered, int lateral) {
        int z = 0;
        // fan -> compressors -> core -> [afterburner] -> nozzle, all facing SOUTH (+Z)
        set(accessor, lateral, 0, z++, JetBlocks.FAN_MODULE.get().defaultBlockState());
        for (int i = 0; i < stages; i++) {
            set(accessor, lateral, 0, z++, JetBlocks.COMPRESSOR_MODULE.get().defaultBlockState());
        }
        int coreZ = z;
        set(accessor, lateral, 0, z++, JetBlocks.COMBUSTION_CORE.get().defaultBlockState());
        if (afterburner) {
            set(accessor, lateral, 0, z++, JetBlocks.AFTERBURNER_MODULE.get().defaultBlockState());
        }
        set(accessor, lateral, 0, z, JetBlocks.NOZZLE_MODULE.get().defaultBlockState());

        if (powered) {
            // Redstone blocks drive throttle to 15/15. They are placed symmetrically on both
            // sides: a single block would shift the centre of mass off the engine axis, the
            // craft would yaw, and — because thrust correctly rotates with the airframe — the
            // velocity would swing off the axis the test measures.
            accessor.setBlock(new BlockPos(lateral + 1, 0, coreZ), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            accessor.setBlock(new BlockPos(lateral - 1, 0, coreZ), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        }
    }

    private static void set(CommonLevelAccessor accessor, int x, int y, int z,
                            net.minecraft.world.level.block.state.BlockState state) {
        accessor.setBlock(new BlockPos(x, y, z),
                state.setValue(JetModuleBlock.FACING, Direction.SOUTH), 3);
    }

    private static double thrustAxisVelocity(RigidBodyHandle handle) {
        // exhaust is +Z, so acceleration must appear as negative Z
        return handle.getLinearVelocity().z();
    }

    /**
     * Horizontal speed. Gravity acts on Y, so the horizontal magnitude isolates the engine's
     * contribution without assuming the craft has not rotated.
     *
     * <p>Returns -1 once the body is gone. A light test craft under full thrust leaves the loaded
     * area within a few seconds and Sable then destroys its body, so every read has to be guarded.
     */
    private static double horizontalSpeed(RigidBodyHandle handle) {
        if (!handle.isValid()) {
            return -1.0D;
        }
        Vector3dc v = handle.getLinearVelocity();
        return Math.hypot(v.x(), v.z());
    }

    // =====================================================================================
    // tests
    // =====================================================================================

    /** A complete, powered chain must accelerate the body against its exhaust direction. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void validEngineProducesThrust(GameTestHelper helper) {
        SubLevelPhysicsSystem system = physics(helper);
        Vector3dc at = SableTestHelper.absolutePosition(helper, new Vector3d(8.0, 10.0, 4.0));
        ServerSubLevel sub = buildEngine(helper, at, 2, false, true, 0);
        RigidBodyHandle handle = system.getPhysicsHandle(sub);

        double[] peak = {0.0D};
        double[] earlyAxis = {0.0D};
        double[] earlySpeed = {0.0D};

        helper.startSequence()
                // Direction is checked early. Over a long window an unconstrained craft can pitch
                // or flip, and once it has, thrust correctly follows the airframe — so a late
                // sample legitimately reads the opposite sign. Sampling while the craft is still
                // in its launch attitude tests what this assertion is actually about.
                .thenExecuteAfter(DIRECTION_TICKS, () -> {
                    earlyAxis[0] = handle.isValid() ? thrustAxisVelocity(handle) : 0.0D;
                    earlySpeed[0] = Math.max(0.0D, horizontalSpeed(handle));
                })
                .thenExecuteFor(SPOOL_TICKS - DIRECTION_TICKS, () -> {
                    double s = horizontalSpeed(handle);
                    if (s > peak[0]) {
                        peak[0] = s;
                    }
                })
                .thenExecute(() -> {
                    CreateJetEngines.LOGGER.info(
                            "[gametest] valid engine peak speed={} early vz={} (early speed {})",
                            peak[0], earlyAxis[0], earlySpeed[0]);
                    if (peak[0] < 0.5D) {
                        helper.fail("Engine produced no thrust (peak horizontal speed=" + peak[0] + ")");
                    }
                    if (earlyAxis[0] > -0.5D * earlySpeed[0] || earlySpeed[0] < 0.05D) {
                        helper.fail("Thrust is not opposing the exhaust direction (early vz="
                                + earlyAxis[0] + ", early speed=" + earlySpeed[0] + ")");
                    }
                })
                .thenSucceed();
    }

    /** The chain must be detected, with the right stage count and end positions. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void chainIsDetected(GameTestHelper helper) {
        Vector3dc at = SableTestHelper.absolutePosition(helper, new Vector3d(8.0, 10.0, 4.0));
        ServerSubLevel sub = buildEngine(helper, at, 3, true, true, 0);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    CombustionCoreBlockEntity core = findCore(helper, sub);
                    if (core == null) {
                        helper.fail("Combustion core was not present in the sublevel plot");
                        return;
                    }
                    EngineChain chain = core.getChain();
                    if (!chain.valid()) {
                        helper.fail("Chain not detected: " + chain.reason());
                    }
                    if (chain.compressorStages() != 3) {
                        helper.fail("Expected 3 compressor stages, got " + chain.compressorStages());
                    }
                    if (!chain.hasAfterburner()) {
                        helper.fail("Afterburner not detected");
                    }
                    if (chain.exhaustDirection() != Direction.SOUTH) {
                        helper.fail("Wrong exhaust direction: " + chain.exhaustDirection());
                    }
                })
                .thenSucceed();
    }

    /** A chain with no nozzle is invalid and must stay completely inert. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void invalidEngineProducesNoThrust(GameTestHelper helper) {
        SubLevelPhysicsSystem system = physics(helper);
        Vector3dc at = SableTestHelper.absolutePosition(helper, new Vector3d(8.0, 10.0, 4.0));

        ServerSubLevel sub = SableTestHelper.spawnSubLevel(container(helper), at, accessor -> {
            // fan -> compressor -> core, and deliberately no nozzle
            set(accessor, 0, 0, 0, JetBlocks.FAN_MODULE.get().defaultBlockState());
            set(accessor, 0, 0, 1, JetBlocks.COMPRESSOR_MODULE.get().defaultBlockState());
            set(accessor, 0, 0, 2, JetBlocks.COMBUSTION_CORE.get().defaultBlockState());
            accessor.setBlock(new BlockPos(1, 0, 2), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        });
        RigidBodyHandle handle = system.getPhysicsHandle(sub);

        double[] peak = {0.0D};
        helper.startSequence()
                .thenExecuteFor(SPOOL_TICKS, () -> {
                    double s = horizontalSpeed(handle);
                    if (s > peak[0]) {
                        peak[0] = s;
                    }
                })
                .thenExecute(() -> {
                    CreateJetEngines.LOGGER.info("[gametest] invalid engine peak speed={}", peak[0]);
                    if (peak[0] > NO_THRUST_TOLERANCE) {
                        helper.fail("Invalid engine still produced thrust (peak speed=" + peak[0] + ")");
                    }
                })
                .thenSucceed();
    }

    /** An unpowered but complete chain must not move either. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void unpoweredEngineProducesNoThrust(GameTestHelper helper) {
        SubLevelPhysicsSystem system = physics(helper);
        Vector3dc at = SableTestHelper.absolutePosition(helper, new Vector3d(8.0, 10.0, 4.0));
        ServerSubLevel sub = buildEngine(helper, at, 2, false, false, 0);
        RigidBodyHandle handle = system.getPhysicsHandle(sub);

        double[] peak = {0.0D};
        helper.startSequence()
                .thenExecuteFor(SPOOL_TICKS, () -> {
                    double s = horizontalSpeed(handle);
                    if (s > peak[0]) {
                        peak[0] = s;
                    }
                })
                .thenExecute(() -> {
                    if (peak[0] > NO_THRUST_TOLERANCE) {
                        helper.fail("Unpowered engine produced thrust (peak speed=" + peak[0] + ")");
                    }
                })
                .thenSucceed();
    }

    /**
     * Breaking the combustion core must remove the actor and stop thrust.
     * Sable keys actors by position, so clearing the block is the whole mechanism.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 500)
    public static void removingCoreStopsThrust(GameTestHelper helper) {
        SubLevelPhysicsSystem system = physics(helper);
        Vector3dc at = SableTestHelper.absolutePosition(helper, new Vector3d(8.0, 10.0, 4.0));
        ServerSubLevel sub = buildEngine(helper, at, 2, false, true, 0);
        RigidBodyHandle handle = system.getPhysicsHandle(sub);

        double[] speedAtRemoval = new double[1];

        helper.startSequence()
                .thenExecuteAfter(REMOVAL_TICKS, () -> {
                    CombustionCoreBlockEntity core = findCore(helper, sub);
                    if (core == null) {
                        // Craft already left the loaded area and the sublevel is gone. There is no
                        // actor and no body, so ghost thrust is impossible; nothing left to assert.
                        speedAtRemoval[0] = -1.0D;
                        return;
                    }
                    speedAtRemoval[0] = horizontalSpeed(handle);
                    if (speedAtRemoval[0] < 0.5D) {
                        helper.fail("Engine never spooled up before the removal step (speed="
                                + speedAtRemoval[0] + ")");
                    }
                    helper.getLevel().removeBlock(core.getBlockPos(), false);
                })
                .thenExecuteAfter(60, () -> {
                    if (speedAtRemoval[0] < 0.0D) {
                        return;
                    }
                    double after = horizontalSpeed(handle);
                    CreateJetEngines.LOGGER.info("[gametest] speed at removal={} after={}",
                            speedAtRemoval[0], after);
                    if (after < 0.0D) {
                        // Body already destroyed; a removed body cannot be receiving ghost thrust.
                        return;
                    }
                    // With the actor gone nothing can add energy; it must not keep accelerating.
                    if (after > speedAtRemoval[0] + 0.25D) {
                        helper.fail("Ghost thrust after the core was removed: "
                                + speedAtRemoval[0] + " -> " + after);
                    }
                })
                .thenSucceed();
    }

    /** An engine mounted off the centre line must impart yaw, not just linear motion. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void offCentreEngineCreatesTorque(GameTestHelper helper) {
        SubLevelPhysicsSystem system = physics(helper);
        Vector3dc at = SableTestHelper.absolutePosition(helper, new Vector3d(8.0, 10.0, 4.0));
        // A wide, light hull so the moment arm is meaningful.
        ServerSubLevel sub = SableTestHelper.spawnSubLevel(container(helper), at, accessor -> {
            for (int x = -4; x <= 4; x++) {
                accessor.setBlock(new BlockPos(x, 0, 0), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }
            place(accessor, 1, false, true, 4);
        });
        RigidBodyHandle handle = system.getPhysicsHandle(sub);

        // Angular velocity at a single instant is not a stable measurement: the craft yaws,
        // swings back, and can be passing through zero exactly when sampled. Observed values
        // across runs ranged from 1.7e-3 to 5.5 against a 1e-3 threshold, which made this test
        // flaky. Track the peak over the window instead.
        double[] peak = {0.0D};
        helper.startSequence()
                .thenExecuteFor(TORQUE_TICKS, () -> {
                    if (!handle.isValid()) {
                        return;
                    }
                    double m = handle.getAngularVelocity().length();
                    if (m > peak[0]) {
                        peak[0] = m;
                    }
                })
                .thenExecute(() -> {
                    CreateJetEngines.LOGGER.info("[gametest] off-centre peak angular velocity={}", peak[0]);
                    if (peak[0] < 1.0E-3D) {
                        helper.fail("Off-centre engine produced no torque (peak |w|=" + peak[0] + ")");
                    }
                })
                .thenSucceed();
    }

    /**
     * Locates the combustion core inside a sublevel's plot, or null if it is not there.
     *
     * <p>Returns null rather than throwing: a test craft under full thrust leaves the loaded area
     * within seconds and Sable then unloads the sublevel, taking the plot's blocks with it. Throwing
     * from inside a game-test callback aborts the entire run, which is what made the suite
     * intermittently red with "Combustion core not found inside the sublevel plot".
     */
    @javax.annotation.Nullable
    private static CombustionCoreBlockEntity findCore(GameTestHelper helper, ServerSubLevel sub) {
        BlockPos centre = sub.getPlot().getCenterBlock();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-12, -6, -12), centre.offset(12, 6, 12))) {
            if (helper.getLevel().getBlockEntity(pos) instanceof CombustionCoreBlockEntity core) {
                return core;
            }
        }
        return null;
    }
}
