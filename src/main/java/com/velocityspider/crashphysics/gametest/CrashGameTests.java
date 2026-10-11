package com.velocityspider.crashphysics.gametest;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.damage.Destruction;
import com.velocityspider.crashphysics.sable.LevelCrashState;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * In-game tests with the real Sable physics engine: build a small vehicle, assemble it, throw it at the ground and
 * check what the crash did. Each test has its own batch so they run one after another and the destruction counters
 * can be attributed to them.
 * <p>
 * Sable simulates in 32-bit floats, which fall apart millions of blocks from the origin, where the game test framework
 * puts its test areas. So every scenario is built at its own spot near the world origin, in force-loaded chunks.
 * <p>
 * Run with {@code ./gradlew runGameTestServer}.
 */
@GameTestHolder(CrashPhysics.MODID)
@PrefixGameTestTemplate(false)
public final class CrashGameTests {

    private static final String EMPTY = "empty";
    private static final int GROUND_TOP = 5;

    /**
     * Ticks to let force-loaded chunks start ticking before building
     */
    private static final int SETTLE_TICKS = 10;

    private CrashGameTests() {
    }

    /**
     * A 3×3×3 wooden block hitting dirt at over 30 m/s ploughs into it: dirt is dug out, the hull survives.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_dirt")
    public static void diveIntoDirtDigsACrater(final GameTestHelper helper) {
        scenario(helper, 0, 120, area -> {
            area.fill(0, 0, 0, 31, GROUND_TOP, 31, Blocks.DIRT.defaultBlockState());
            final ServerSubLevel vehicle = area.assemble(area.fill(14, 16, 14, 16, 18, 16, Blocks.OAK_PLANKS.defaultBlockState()));
            launch(vehicle, 0.0, -30.0, 0.0);
            return new SoilRun(Counters.of(area.level), area.countSolid(0, 31, 0, GROUND_TOP + 6, 0, 31));
        }, (area, run) -> {
            final Counters after = Counters.of(area.level);
            final long dug = after.terrain - run.before.terrain;
            final long hull = after.vehicle - run.before.vehicle;
            final int rim = area.countAbove(0, 31, 0, 31, GROUND_TOP);
            final int lost = run.soil - area.countSolid(0, 31, 0, GROUND_TOP + 6, 0, 31);
            CrashPhysics.LOGGER.info("[gametest] dirt dive: {} ground blocks dug out, {} hull blocks lost, {} piled up into a rim, {} gone for good",
                    dug, hull, rim, lost);
            helper.assertTrue(dug >= 4, "expected a crater, but only " + dug + " ground blocks were broken");
            helper.assertTrue(hull <= 4, "planks are stronger than dirt, but " + hull + " hull blocks broke");
            helper.assertTrue(rim >= 2, "the dug-out soil should pile up into a rim, but only " + rim + " blocks did");
            helper.assertTrue(lost * 2 <= dug, "soil should be shoved aside, not destroyed, but " + lost + " of " + dug + " blocks vanished");
        });
    }

    private record SoilRun(Counters before, int soil) {
    }

    /**
     * The same block hitting rock is crushed instead, and the rock is barely marked.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_stone")
    public static void diveIntoStoneCrumplesTheHull(final GameTestHelper helper) {
        scenario(helper, 1, 120, area -> {
            area.fill(0, 0, 0, 15, GROUND_TOP, 15, Blocks.STONE.defaultBlockState());
            final ServerSubLevel vehicle = area.assemble(area.fill(6, 16, 6, 8, 18, 8, Blocks.OAK_PLANKS.defaultBlockState()));
            launch(vehicle, 0.0, -35.0, 0.0);
            return Counters.of(area.level);
        }, (area, before) -> {
            final Counters after = Counters.of(area.level);
            final long rock = after.terrain - before.terrain;
            final long hull = after.vehicle - before.vehicle;
            CrashPhysics.LOGGER.info("[gametest] stone dive: {} hull blocks crushed, {} rock blocks broken", hull, rock);
            helper.assertTrue(rock == 0, "wood shouldn't break solid rock, but " + rock + " stone blocks broke");
            helper.assertTrue(hull >= 3, "expected the hull to crumple, but only " + hull + " blocks broke");
        });
    }

    /**
     * Setting down gently on grass must not break anything.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_landing")
    public static void gentleLandingLeavesNoDamage(final GameTestHelper helper) {
        scenario(helper, 2, 100, area -> {
            area.fill(0, 0, 0, 15, GROUND_TOP - 1, 15, Blocks.DIRT.defaultBlockState());
            area.fill(0, GROUND_TOP, 0, 15, GROUND_TOP, 15, Blocks.GRASS_BLOCK.defaultBlockState());
            area.assemble(area.fill(6, GROUND_TOP + 2, 6, 8, GROUND_TOP + 4, 8, Blocks.OAK_PLANKS.defaultBlockState()));
            return Counters.of(area.level);
        }, (area, before) -> {
            final Counters after = Counters.of(area.level);
            helper.assertTrue(after.terrain == before.terrain, "a gentle landing broke " + (after.terrain - before.terrain) + " ground blocks");
            helper.assertTrue(after.vehicle == before.vehicle, "a gentle landing broke " + (after.vehicle - before.vehicle) + " hull blocks");
        });
    }

    /**
     * A thin column carrying a long arm hits rock end first. Decelerating the arm loads its root in shear far beyond
     * what a single plank joint can carry, so the arm has to tear off.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_structure")
    public static void hardImpactTearsOffAnOverhangingArm(final GameTestHelper helper) {
        scenario(helper, 3, 120, area -> {
            area.fill(0, 0, 0, 23, GROUND_TOP, 15, Blocks.STONE.defaultBlockState());
            final List<BlockPos> blocks = new ArrayList<>(area.fill(7, 15, 7, 7, 20, 7, Blocks.OAK_PLANKS.defaultBlockState()));
            blocks.addAll(area.fill(8, 20, 7, 15, 20, 7, Blocks.SPRUCE_PLANKS.defaultBlockState()));
            final ServerSubLevel vehicle = area.assemble(blocks);
            launch(vehicle, 0.0, -45.0, 0.0);
            return Counters.of(area.level);
        }, (area, before) -> {
            final Counters after = Counters.of(area.level);
            final long torn = after.torn - before.torn;
            CrashPhysics.LOGGER.info("[gametest] structure: {} joints torn, {} blocks broken", torn, after.vehicle - before.vehicle);
            helper.assertTrue(torn >= 1, "expected crash loads to tear the arm's joints, but none failed");
        });
    }

    /**
     * A heavy iron block rammed sideways into a plank wall punches straight through it.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_ram")
    public static void rammingAWallPunchesThrough(final GameTestHelper helper) {
        scenario(helper, 4, 100, area -> {
            area.fill(0, 0, 0, 23, GROUND_TOP, 15, Blocks.STONE.defaultBlockState());
            area.fill(14, GROUND_TOP + 1, 5, 14, GROUND_TOP + 6, 10, Blocks.OAK_PLANKS.defaultBlockState());
            final ServerSubLevel vehicle = area.assemble(area.fill(4, GROUND_TOP + 2, 6, 6, GROUND_TOP + 4, 8, Blocks.IRON_BLOCK.defaultBlockState()));
            launch(vehicle, 40.0, 0.0, 0.0);
            return Counters.of(area.level);
        }, (area, before) -> {
            final Counters after = Counters.of(area.level);
            final long wall = after.terrain - before.terrain;
            CrashPhysics.LOGGER.info("[gametest] ram: {} wall blocks broken, {} hull blocks lost", wall, after.vehicle - before.vehicle);
            helper.assertTrue(wall >= 3, "a 108 t iron block at 40 m/s should punch through a plank wall, but only " + wall + " blocks broke");
        });
    }

    /**
     * A sled sliding fast over grass tears up the turf behind it.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_skid")
    public static void slidingOverGrassLeavesTracks(final GameTestHelper helper) {
        scenario(helper, 5, 60, area -> {
            area.fill(0, 0, 0, 47, GROUND_TOP - 1, 15, Blocks.DIRT.defaultBlockState());
            area.fill(0, GROUND_TOP, 0, 47, GROUND_TOP, 15, Blocks.GRASS_BLOCK.defaultBlockState());
            final ServerSubLevel vehicle = area.assemble(area.fill(2, GROUND_TOP + 1, 6, 4, GROUND_TOP + 1, 8, Blocks.OAK_PLANKS.defaultBlockState()));
            launch(vehicle, 25.0, -1.0, 0.0);
            return null;
        }, (area, unused) -> {
            int torn = 0;
            for (int x = 0; x <= 47; x++) {
                for (int z = 0; z <= 15; z++) {
                    if (area.level.getBlockState(area.origin.offset(x, GROUND_TOP, z)).is(Blocks.COARSE_DIRT)) {
                        torn++;
                    }
                }
            }
            CrashPhysics.LOGGER.info("[gametest] skid: {} grass blocks torn up", torn);
            helper.assertTrue(torn >= 3, "expected skid marks, but only " + torn + " grass blocks were torn up");
        });
    }

    /**
     * Ramming through the middle of a tower cuts its top loose, which then comes down.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_collapse")
    public static void cuttingThroughATowerBringsItsTopDown(final GameTestHelper helper) {
        scenario(helper, 6, 60, area -> {
            area.fill(0, 0, 0, 23, GROUND_TOP, 15, Blocks.STONE.defaultBlockState());
            area.fill(12, GROUND_TOP + 1, 8, 12, GROUND_TOP + 11, 8, Blocks.STONE_BRICKS.defaultBlockState());
            final ServerSubLevel ram = area.assemble(area.fill(2, GROUND_TOP + 4, 7, 3, GROUND_TOP + 5, 8, Blocks.IRON_BLOCK.defaultBlockState()));
            launch(ram, 35.0, 0.0, 0.0);
            return Counters.of(area.level);
        }, (area, before) -> {
            final Counters after = Counters.of(area.level);
            final BlockState top = area.level.getBlockState(area.origin.offset(12, GROUND_TOP + 11, 8));
            CrashPhysics.LOGGER.info("[gametest] collapse: {} tower blocks broken, top of the tower is now {}", after.terrain - before.terrain, top);
            helper.assertTrue(after.terrain - before.terrain >= 1, "the ram should have broken through the tower");
            helper.assertTrue(!top.is(Blocks.STONE_BRICKS), "the top of the tower should have come down, but it is still floating there");
        });
    }

    /**
     * An iron ram flying at a one block thick brick wall from well away covers most of a block per physics step, which
     * on its own Rapier would let it skip straight through. The wall has to stop it or break: at this speed the bricks
     * in its way are knocked out and it carries on through the hole, slower.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_wall")
    public static void fastRamBreaksThroughAThinWall(final GameTestHelper helper) {
        scenario(helper, 7, 30, area -> {
            area.fill(0, 0, 0, 31, GROUND_TOP, 15, Blocks.STONE.defaultBlockState());
            area.fill(12, GROUND_TOP + 1, 5, 12, GROUND_TOP + 11, 10, Blocks.STONE_BRICKS.defaultBlockState());
            final ServerSubLevel ram = area.assemble(area.fill(2, GROUND_TOP + 6, 7, 3, GROUND_TOP + 7, 8, Blocks.IRON_BLOCK.defaultBlockState()));
            launch(ram, 35.0, 0.0, 0.0);
            return new Run(Counters.of(area.level), ram);
        }, (area, run) -> {
            final long broken = Counters.of(area.level).terrain - run.before.terrain;
            final double x = run.vehicle.logicalPose().position().x() - area.origin.getX();
            final double speed = velocity(run.vehicle).x;
            CrashPhysics.LOGGER.info("[gametest] thin wall: {} bricks knocked out, ram now at x = {} doing {} m/s", broken,
                    String.format("%.1f", x), String.format("%.1f", speed));
            helper.assertTrue(broken >= 4, "the ram should have knocked a hole in the wall, but only " + broken + " bricks went");
            helper.assertTrue(x > 14.0, "the ram should be through the wall, but it is at x = " + x);
            helper.assertTrue(speed > 5.0 && speed < 34.0, "the ram should carry on through, slowed by the wall, but it is doing " + speed + " m/s");
        });
    }

    /**
     * The same ram at a walking pace against a thick wall: nothing breaks, and it comes to rest against the wall
     * instead of bouncing back off it.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_thick_wall")
    public static void slowRamIsStoppedByAThickWall(final GameTestHelper helper) {
        scenario(helper, 8, 40, area -> {
            area.fill(0, 0, 0, 23, GROUND_TOP, 15, Blocks.STONE.defaultBlockState());
            area.fill(12, GROUND_TOP + 1, 4, 14, GROUND_TOP + 8, 11, Blocks.STONE_BRICKS.defaultBlockState());
            final ServerSubLevel ram = area.assemble(area.fill(8, GROUND_TOP + 1, 7, 9, GROUND_TOP + 2, 8, Blocks.IRON_BLOCK.defaultBlockState()));
            launch(ram, 6.0, 0.0, 0.0);
            return new Run(Counters.of(area.level), ram);
        }, (area, run) -> {
            final long broken = Counters.of(area.level).terrain - run.before.terrain;
            final double x = run.vehicle.logicalPose().position().x() - area.origin.getX();
            final double speed = velocity(run.vehicle).x;
            CrashPhysics.LOGGER.info("[gametest] thick wall: {} bricks broken, ram at x = {} doing {} m/s", broken,
                    String.format("%.2f", x), String.format("%.2f", speed));
            helper.assertTrue(broken == 0, "a slow ram shouldn't break a thick wall, but " + broken + " bricks broke");
            helper.assertTrue(x > 10.0 && x < 11.2, "the ram should rest against the wall, but it is at x = " + x);
            helper.assertTrue(Math.abs(speed) < 1.0, "the ram should have stopped, but it is doing " + speed + " m/s");
        });
    }

    /**
     * Coming in shallow, a heavy vehicle ploughs a trench and shoves the soil out into berms along both sides.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_trench")
    public static void shallowCrashPloughsATrench(final GameTestHelper helper) {
        scenario(helper, 9, 80, area -> {
            area.fill(0, 0, 0, 47, GROUND_TOP, 15, Blocks.DIRT.defaultBlockState());
            // A heavy iron sled, 72 t, coming in at about 15 degrees
            final ServerSubLevel vehicle = area.assemble(area.fill(2, GROUND_TOP + 3, 6, 4, GROUND_TOP + 4, 8, Blocks.IRON_BLOCK.defaultBlockState()));
            launch(vehicle, 30.0, -8.0, 0.0);
            return new SoilRun(Counters.of(area.level), area.countSolid(0, 47, 0, GROUND_TOP + 6, 0, 15));
        }, (area, run) -> {
            final long dug = Counters.of(area.level).terrain - run.before.terrain;
            final int berms = area.countAbove(0, 47, 0, 15, GROUND_TOP);
            final int lost = run.soil - area.countSolid(0, 47, 0, GROUND_TOP + 6, 0, 15);
            CrashPhysics.LOGGER.info("[gametest] trench: {} ground blocks dug out, {} shoved up alongside, {} gone for good", dug, berms, lost);
            helper.assertTrue(dug >= 3, "a shallow crash into dirt should plough a trench, but only " + dug + " blocks were dug");
            helper.assertTrue(berms >= 2, "the ploughed soil should be shoved up beside the trench, but only " + berms + " blocks were");
            helper.assertTrue(lost * 2 <= dug, "soil should be shoved aside, not destroyed, but " + lost + " of " + dug + " blocks vanished");
        });
    }

    /**
     * Water has no strength, only inertia: hit at speed it has to be shoved aside so fast that it hits like a wall.
     * A wooden hull plunging in at 65 m/s is smashed and brought up short.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_water_fast")
    public static void fastPlungeIntoWaterWrecksTheHull(final GameTestHelper helper) {
        scenario(helper, 10, 30, area -> {
            area.pool();
            final ServerSubLevel vehicle = area.assemble(area.fill(6, 14, 6, 8, 16, 8, Blocks.OAK_PLANKS.defaultBlockState()));
            launch(vehicle, 0.0, -65.0, 0.0);
            return new Run(Counters.of(area.level), vehicle);
        }, (area, run) -> {
            final long hull = Counters.of(area.level).vehicle - run.before.vehicle;
            final double sinking = run.vehicle.isRemoved() ? 0.0 : -velocity(run.vehicle).y;
            CrashPhysics.LOGGER.info("[gametest] fast water: {} hull blocks smashed, now sinking at {} m/s", hull, String.format("%.1f", sinking));
            helper.assertTrue(hull >= 2, "hitting water at 65 m/s should smash a wooden hull, but only " + hull + " blocks broke");
            helper.assertTrue(sinking < 20.0, "the water should have braked it hard, but it is still going down at " + sinking + " m/s");
        });
    }

    /**
     * Dropping into water slowly does no harm, and a wooden hull stays afloat.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_water_slow")
    public static void slowDropIntoWaterIsHarmlessAndItFloats(final GameTestHelper helper) {
        scenario(helper, 11, 20, area -> {
            area.pool();
            final ServerSubLevel vehicle = area.assemble(area.fill(6, 12, 6, 8, 13, 8, Blocks.OAK_PLANKS.defaultBlockState()));
            launch(vehicle, 0.0, -6.0, 0.0);
            return new Run(Counters.of(area.level), vehicle);
        }, (area, run) -> {
            final long hull = Counters.of(area.level).vehicle - run.before.vehicle;
            final double sinking = -velocity(run.vehicle).y;
            final double depth = 11.0 - (run.vehicle.logicalPose().position().y() - area.origin.getY());
            CrashPhysics.LOGGER.info("[gametest] slow water: {} hull blocks broken, sinking at {} m/s, {} m below the surface", hull,
                    String.format("%.1f", sinking), String.format("%.1f", depth));
            helper.assertTrue(hull == 0, "a gentle splashdown shouldn't break anything, but " + hull + " blocks broke");
            helper.assertTrue(depth < 3.0, "a wooden hull should stay afloat, but it is " + depth + " m under");
        });
    }

    /**
     * A drone (a plank body with wool wings mounted high, like sails) set down from one block up. A drop like that
     * is nothing: its wings have to stay on.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_drone_drop")
    public static void droneDroppedOneBlockKeepsItsWings(final GameTestHelper helper) {
        scenario(helper, 12, 60, area -> {
            area.fill(0, 0, 0, 23, GROUND_TOP, 15, Blocks.DIRT.defaultBlockState());
            final ServerSubLevel drone = area.assemble(drone(area, GROUND_TOP + 2));
            return new Run(Counters.of(area.level), drone);
        }, (area, run) -> {
            final Counters after = Counters.of(area.level);
            CrashPhysics.LOGGER.info("[gametest] drone drop: {} blocks broken, {} joints torn", after.vehicle - run.before.vehicle, after.torn - run.before.torn);
            helper.assertTrue(after.torn == run.before.torn, "a one block drop tore " + (after.torn - run.before.torn) + " joints");
            helper.assertTrue(after.vehicle == run.before.vehicle, "a one block drop broke " + (after.vehicle - run.before.vehicle) + " blocks");
        });
    }

    /**
     * The same drone slamming into the ground at 35 m/s: its wings are wrecked, torn off by the deceleration or
     * crushed as the body ploughs in and they hit the ground too.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_drone_crash")
    public static void droneCrashingHardWrecksItsWings(final GameTestHelper helper) {
        scenario(helper, 13, 60, area -> {
            area.fill(0, 0, 0, 23, GROUND_TOP, 15, Blocks.DIRT.defaultBlockState());
            final ServerSubLevel drone = area.assemble(drone(area, GROUND_TOP + 14));
            launch(drone, 0.0, -35.0, 0.0);
            return new Run(Counters.of(area.level), drone);
        }, (area, run) -> {
            final Counters after = Counters.of(area.level);
            CrashPhysics.LOGGER.info("[gametest] drone crash: {} blocks broken, {} joints torn", after.vehicle - run.before.vehicle, after.torn - run.before.torn);
            final long wrecked = (after.torn - run.before.torn) + (after.vehicle - run.before.vehicle);
            helper.assertTrue(wrecked >= 4, "a 35 m/s crash should wreck the wings (torn off or crushed), but only " + wrecked + " blocks went");
        });
    }

    /**
     * A 3 × 3 × 3 plank body with five-block wool wings on both sides of its top layer.
     */
    private static List<BlockPos> drone(final Area area, final int bottom) {
        final List<BlockPos> blocks = new ArrayList<>(area.fill(10, bottom, 6, 12, bottom + 2, 8, Blocks.OAK_PLANKS.defaultBlockState()));
        blocks.addAll(area.fill(5, bottom + 2, 7, 9, bottom + 2, 7, Blocks.WHITE_WOOL.defaultBlockState()));
        blocks.addAll(area.fill(13, bottom + 2, 7, 17, bottom + 2, 7, Blocks.WHITE_WOOL.defaultBlockState()));
        return blocks;
    }

    private record Run(Counters before, ServerSubLevel vehicle) {
    }

    private static Vector3d velocity(final ServerSubLevel vehicle) {
        final RigidBodyHandle handle = RigidBodyHandle.of(vehicle);
        return handle != null ? handle.getLinearVelocity(new Vector3d()) : new Vector3d();
    }

    private record Counters(long terrain, long vehicle, long torn) {
        static Counters of(final ServerLevel level) {
            final Destruction destruction = LevelCrashState.get(level).destruction();
            return new Counters(destruction.terrainBroken(), destruction.vehicleBlocksBroken(), destruction.jointsTorn());
        }
    }

    /**
     * Runs a scenario at its own spot near the world origin: wait for the force-loaded chunks there to tick, build and
     * launch, then check the outcome after a while. Driven from a single per-tick listener, because the game test
     * framework doesn't allow scheduling new callbacks from inside its callbacks.
     */
    private static <T> void scenario(final GameTestHelper helper, final int index, final int runTicks,
                                     final Function<Area, T> setup, final BiConsumer<Area, T> check) {
        final Area area = new Area(helper.getLevel(), new BlockPos(index * 128, 96, 64));
        area.forceChunks(true);

        final int[] tick = {0};
        final List<T> context = new ArrayList<>(1);
        helper.onEachTick(() -> {
            final int now = ++tick[0];
            if (now == SETTLE_TICKS) {
                context.add(setup.apply(area));
            } else if (now == SETTLE_TICKS + runTicks) {
                try {
                    check.accept(area, context.getFirst());
                } finally {
                    area.forceChunks(false);
                }
                helper.succeed();
            }
        });
    }

    private record Area(ServerLevel level, BlockPos origin) {
        List<BlockPos> fill(final int x0, final int y0, final int z0, final int x1, final int y1, final int z1, final BlockState state) {
            final List<BlockPos> placed = new ArrayList<>();
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        final BlockPos pos = this.origin.offset(x, y, z);
                        this.level.setBlock(pos, state, Block.UPDATE_ALL);
                        placed.add(pos);
                    }
                }
            }
            return placed;
        }

        /**
         * A stone basin 16 × 16, full of water 9 deep (surface at y = 11).
         */
        void pool() {
            this.fill(0, 0, 0, 15, 11, 15, Blocks.STONE.defaultBlockState());
            this.fill(1, 2, 1, 14, 10, 14, Blocks.WATER.defaultBlockState());
            this.fill(1, 11, 1, 14, 11, 14, Blocks.AIR.defaultBlockState());
        }

        ServerSubLevel assemble(final List<BlockPos> blocks) {
            final BoundingBox3ic bounds = BoundingBox3i.from(blocks).expand(1, 1, 1);
            return SubLevelAssemblyHelper.assembleBlocks(this.level, blocks.getFirst(), blocks, bounds);
        }

        int countSolid(final int x0, final int x1, final int y0, final int y1, final int z0, final int z1) {
            int count = 0;
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        if (!this.level.getBlockState(this.origin.offset(x, y, z)).isAir()) {
                            count++;
                        }
                    }
                }
            }
            return count;
        }

        /**
         * Counts world blocks above a height in a box: in a scenario built on flat ground, that's soil piled up by a crash.
         */
        int countAbove(final int x0, final int x1, final int z0, final int z1, final int height) {
            int count = 0;
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int y = height + 1; y <= height + 6; y++) {
                        if (!this.level.getBlockState(this.origin.offset(x, y, z)).isAir()) {
                            count++;
                        }
                    }
                }
            }
            return count;
        }

        void forceChunks(final boolean forced) {
            for (int cx = (this.origin.getX() - 16) >> 4; cx <= (this.origin.getX() + 64) >> 4; cx++) {
                for (int cz = (this.origin.getZ() - 16) >> 4; cz <= (this.origin.getZ() + 32) >> 4; cz++) {
                    this.level.setChunkForced(cx, cz, forced);
                }
            }
        }
    }

    private static void launch(final ServerSubLevel vehicle, final double vx, final double vy, final double vz) {
        final RigidBodyHandle handle = RigidBodyHandle.of(vehicle);
        if (handle == null) {
            throw new IllegalStateException("vehicle has no physics body");
        }
        handle.addLinearAndAngularVelocity(new Vector3d(vx, vy, vz), new Vector3d());
    }
}
