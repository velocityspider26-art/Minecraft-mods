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
    /** Long enough for the engine to spool from cold to full authority. */
    private static final int SPOOL_TICKS = 160;

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
     */
    private static double horizontalSpeed(RigidBodyHandle handle) {
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

        helper.startSequence()
                .thenExecuteAfter(SPOOL_TICKS, () -> {
                    double speed = horizontalSpeed(handle);
                    double vz = thrustAxisVelocity(handle);
                    CreateJetEngines.LOGGER.info("[gametest] valid engine speed={} vz={}", speed, vz);
                    if (speed < 0.5D) {
                        helper.fail("Engine produced no thrust (horizontal speed=" + speed + ")");
                    }
                    // A symmetric craft should not have yawed, so thrust must still oppose exhaust.
                    if (vz > -0.5D * speed) {
                        helper.fail("Thrust is not opposing the exhaust direction (vz=" + vz
                                + ", speed=" + speed + ")");
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

        helper.startSequence()
                .thenExecuteAfter(SPOOL_TICKS, () -> {
                    double speed = horizontalSpeed(handle);
                    CreateJetEngines.LOGGER.info("[gametest] invalid engine speed={}", speed);
                    if (speed > 0.05D) {
                        helper.fail("Invalid engine still produced thrust (speed=" + speed + ")");
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

        helper.startSequence()
                .thenExecuteAfter(SPOOL_TICKS, () -> {
                    double speed = horizontalSpeed(handle);
                    if (speed > 0.05D) {
                        helper.fail("Unpowered engine produced thrust (speed=" + speed + ")");
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
                .thenExecuteAfter(SPOOL_TICKS, () -> {
                    CombustionCoreBlockEntity core = findCore(helper, sub);
                    speedAtRemoval[0] = horizontalSpeed(handle);
                    if (speedAtRemoval[0] < 0.5D) {
                        helper.fail("Engine never spooled up before the removal step (speed="
                                + speedAtRemoval[0] + ")");
                    }
                    helper.getLevel().removeBlock(core.getBlockPos(), false);
                })
                .thenExecuteAfter(80, () -> {
                    double after = horizontalSpeed(handle);
                    CreateJetEngines.LOGGER.info("[gametest] speed at removal={} after={}",
                            speedAtRemoval[0], after);
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

        helper.startSequence()
                .thenExecuteAfter(SPOOL_TICKS, () -> {
                    Vector3dc angular = handle.getAngularVelocity();
                    double magnitude = angular.length();
                    CreateJetEngines.LOGGER.info("[gametest] off-centre angular velocity={}", magnitude);
                    if (magnitude < 1.0E-3D) {
                        helper.fail("Off-centre engine produced no torque (|w|=" + magnitude + ")");
                    }
                })
                .thenSucceed();
    }

    private static CombustionCoreBlockEntity findCore(GameTestHelper helper, ServerSubLevel sub) {
        BlockPos centre = sub.getPlot().getCenterBlock();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-12, -6, -12), centre.offset(12, 6, 12))) {
            if (helper.getLevel().getBlockEntity(pos) instanceof CombustionCoreBlockEntity core) {
                return core;
            }
        }
        throw new IllegalStateException("Combustion core not found inside the sublevel plot");
    }
}
