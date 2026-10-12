package com.velocityspider.crashphysics.gametest;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.damage.Destruction;
import com.velocityspider.crashphysics.sable.CrashTimings;
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
import org.jetbrains.annotations.Nullable;
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

    /**
     * Profiling runs ({@code ./gradlew runGameTestServer -Pbench}) only run the benchmarks, normal runs skip them
     */
    private static final boolean BENCH = Boolean.getBoolean("crashphysics.bench");

    /**
     * Started by hand with /test run on a dev server, to watch: every scenario runs
     */
    private static final boolean DEMO = Boolean.getBoolean("crashphysics.demo");
    private static final Vector3d NO_THRUST = new Vector3d();

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
        // Checked while it is still flying clear of the wall, before it lands among the bricks it knocked out
        scenario(helper, 7, 14, area -> {
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
     * An open wooden boat carrying 27 iron blocks. Its blocks alone weigh more than the water they displace, so it only
     * floats because its hull keeps the water out of the space inside it.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "crashphysics_boat")
    public static void loadedBoatFloatsOnItsHull(final GameTestHelper helper) {
        scenario(helper, 18, 200, area -> {
            area.pool();
            final ServerSubLevel boat = area.assemble(boat(area, false));
            return new Run(Counters.of(area.level), boat);
        }, (area, run) -> {
            final double floor = boatFloor(area, run.vehicle);
            final double water = LevelCrashState.get(area.level).floodWater(run.vehicle);
            CrashPhysics.LOGGER.info("[gametest] loaded boat: floor bottom {} m below the surface, {} m³ of water inside",
                    String.format("%.2f", WATER_SURFACE - floor), String.format("%.1f", water));
            helper.assertTrue(floor > WATER_SURFACE - 5.0, "the loaded boat should float on its hull, but its floor is " + (WATER_SURFACE - floor) + " m down");
            helper.assertTrue(water < 2.0, "an intact boat floating level shouldn't take on water, but has " + water + " m³ inside");
        });
    }

    /**
     * The same boat with a hole in its bottom: water pours in, and it goes down.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "crashphysics_boat_holed")
    public static void holedBoatFloodsAndSinks(final GameTestHelper helper) {
        scenario(helper, 19, 200, area -> {
            area.pool();
            final ServerSubLevel boat = area.assemble(boat(area, true));
            return new Run(Counters.of(area.level), boat);
        }, (area, run) -> {
            final double floor = boatFloor(area, run.vehicle);
            final double water = LevelCrashState.get(area.level).floodWater(run.vehicle);
            CrashPhysics.LOGGER.info("[gametest] holed boat: floor bottom {} m below the surface, {} m³ of water inside",
                    String.format("%.2f", WATER_SURFACE - floor), String.format("%.1f", water));
            helper.assertTrue(water > 20.0, "water should pour in through the hole, but only " + water + " m³ got in");
            helper.assertTrue(floor < WATER_SURFACE - 5.0, "the flooded boat should sink, but its floor is only " + (WATER_SURFACE - floor) + " m down");
        });
    }

    /**
     * Height of the water surface in {@link Area#pool()}, relative to the area
     */
    private static final double WATER_SURFACE = 10.0 + 8.0 / 9.0;

    /**
     * A 7 × 7 open boat of planks, four blocks deep, with a 3 × 3 × 3 cube of iron in the middle, built above the pool.
     *
     * @param holed with a two-block hole in the bottom
     */
    private static List<BlockPos> boat(final Area area, final boolean holed) {
        // Built where it floats, its floor three blocks under water, so it settles in gently
        final int floor = 8;
        final List<BlockPos> blocks = new ArrayList<>(area.fill(4, floor, 4, 10, floor, 10, Blocks.OAK_PLANKS.defaultBlockState()));
        for (int y = floor + 1; y <= floor + 3; y++) {
            blocks.addAll(area.fill(4, y, 4, 10, y, 4, Blocks.OAK_PLANKS.defaultBlockState()));
            blocks.addAll(area.fill(4, y, 10, 10, y, 10, Blocks.OAK_PLANKS.defaultBlockState()));
            blocks.addAll(area.fill(4, y, 5, 4, y, 9, Blocks.OAK_PLANKS.defaultBlockState()));
            blocks.addAll(area.fill(10, y, 5, 10, y, 9, Blocks.OAK_PLANKS.defaultBlockState()));
        }
        blocks.addAll(area.fill(6, floor + 1, 6, 8, floor + 3, 8, Blocks.IRON_BLOCK.defaultBlockState()));
        // The space inside is open air, not water
        area.fill(5, floor + 1, 5, 9, floor + 3, 5, Blocks.AIR.defaultBlockState());
        area.fill(5, floor + 1, 9, 9, floor + 3, 9, Blocks.AIR.defaultBlockState());
        area.fill(5, floor + 1, 6, 5, floor + 3, 8, Blocks.AIR.defaultBlockState());
        area.fill(9, floor + 1, 6, 9, floor + 3, 8, Blocks.AIR.defaultBlockState());
        if (holed) {
            for (final BlockPos hole : List.of(area.origin.offset(5, floor, 5), area.origin.offset(5, floor, 6))) {
                area.level.setBlock(hole, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                blocks.remove(hole);
            }
        }
        return blocks;
    }

    /**
     * @return the height of the bottom of the boat's floor, relative to the area
     */
    private static double boatFloor(final Area area, final ServerSubLevel boat) {
        // The floor's centre block, at the boat's local (7, 12, 7) from the area's corner
        final Vector3d floor = boat.logicalPose().transformPosition(new Vector3d(boatOrigin(boat).x + 3.5, boatOrigin(boat).y, boatOrigin(boat).z + 3.5));
        return floor.y - area.origin.getY();
    }

    private static Vector3d boatOrigin(final ServerSubLevel boat) {
        final BoundingBox3ic bounds = boat.getPlot().getBoundingBox();
        return new Vector3d(bounds.minX() + 1, bounds.minY() + 1, bounds.minZ() + 1);
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
     * The same drone slamming into rock at 35 m/s: it is wrecked, the body crushed and the wings torn off or smashed
     * as they hit too.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "crashphysics_drone_crash")
    public static void droneCrashingHardIsWrecked(final GameTestHelper helper) {
        scenario(helper, 13, 60, area -> {
            area.fill(0, 0, 0, 23, GROUND_TOP, 15, Blocks.STONE.defaultBlockState());
            final ServerSubLevel drone = area.assemble(drone(area, GROUND_TOP + 14));
            launch(drone, 0.0, -35.0, 0.0);
            return new Run(Counters.of(area.level), drone);
        }, (area, run) -> {
            final Counters after = Counters.of(area.level);
            CrashPhysics.LOGGER.info("[gametest] drone crash: {} blocks broken, {} joints torn", after.vehicle - run.before.vehicle, after.torn - run.before.torn);
            final long wrecked = (after.torn - run.before.torn) + (after.vehicle - run.before.vehicle);
            helper.assertTrue(wrecked >= 6, "a 35 m/s crash into rock should wreck the drone, but only " + wrecked + " blocks went");
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

    /**
     * Benchmark: a 1,300 block wooden ship sitting on the ground with its engines idling, so it never falls asleep.
     * Every block on its bottom rests on the ground, so this is what idle contacts cost.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "crashphysics_bench_ground")
    public static void benchmarkShipOnTheGround(final GameTestHelper helper) {
        benchmark(helper, 14, "ship idling on the ground", 40, 200, null, area -> {
            area.fill(0, 0, 0, 63, GROUND_TOP, 31, Blocks.DIRT.defaultBlockState());
            return area.assemble(hull(area, 4, GROUND_TOP + 1, 4, 40, 4, 20));
        });
    }

    /**
     * Benchmark: a ship cruising through water at 3 m/s.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "crashphysics_bench_water")
    public static void benchmarkShipCruisingThroughWater(final GameTestHelper helper) {
        benchmark(helper, 15, "ship cruising through water", 40, 200, new Vector3d(3.0, 0.0, 0.0), area -> {
            area.fill(0, 0, 0, 63, 10, 31, Blocks.STONE.defaultBlockState());
            area.fill(1, 2, 1, 62, 9, 30, Blocks.WATER.defaultBlockState());
            area.fill(1, 10, 1, 62, 10, 30, Blocks.AIR.defaultBlockState());
            return area.assemble(hull(area, 3, 9, 6, 28, 4, 16));
        });
    }

    /**
     * Benchmark: a ship flying low and fast over the ground, a few blocks up.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "crashphysics_bench_flight")
    public static void benchmarkShipFlyingLow(final GameTestHelper helper) {
        benchmark(helper, 16, "ship flying low over the ground", 60, 36, NO_THRUST, area -> {
            area.fill(0, 0, 0, 63, GROUND_TOP, 31, Blocks.DIRT.defaultBlockState());
            final ServerSubLevel ship = area.assemble(hull(area, 2, GROUND_TOP + 5, 8, 24, 3, 14));
            launch(ship, 15.0, 10.0, 0.0);
            return ship;
        });
    }

    /**
     * Benchmark: the big ship crashing into a field at 25 m/s, ploughing a trench and breaking up.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "crashphysics_bench_crash")
    public static void benchmarkShipCrashing(final GameTestHelper helper) {
        // The first crash warms up the JVM (class loading, JIT), like any crash after the first in a real game
        benchmark(helper, 17, "big ship crashing into a field", 150, 100, NO_THRUST, area -> {
            area.fill(0, 0, 0, 63, GROUND_TOP, 31, Blocks.DIRT.defaultBlockState());
            area.fill(0, GROUND_TOP, 0, 63, GROUND_TOP, 31, Blocks.GRASS_BLOCK.defaultBlockState());
            final ServerSubLevel ship = area.assemble(hull(area, 2, GROUND_TOP + 8, 8, 24, 4, 14));
            launch(ship, 15.0, -20.0, 0.0);
            return ship;
        });
    }

    /**
     * Only for watching on a dev server: an iron sled scraping along stone, throwing sparks.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "crashphysics_demo_sparks")
    public static void demoIronSledScrapingStone(final GameTestHelper helper) {
        if (!DEMO) {
            helper.succeed();
            return;
        }
        scenario(helper, 20, 100, area -> {
            area.fill(0, 0, 0, 63, GROUND_TOP, 15, Blocks.STONE.defaultBlockState());
            final ServerSubLevel sled = area.assemble(area.fill(2, GROUND_TOP + 1, 7, 4, GROUND_TOP + 1, 8, Blocks.IRON_BLOCK.defaultBlockState()));
            launch(sled, 18.0, -3.0, 0.0);
            return Counters.of(area.level);
        }, (area, before) -> {
        });
    }

    /**
     * A box-shaped hull: a solid bottom and deck with walls between them.
     */
    private static List<BlockPos> hull(final Area area, final int x0, final int y0, final int z0, final int length, final int height, final int width) {
        final List<BlockPos> blocks = new ArrayList<>();
        final int x1 = x0 + length - 1;
        final int y1 = y0 + height - 1;
        final int z1 = z0 + width - 1;
        blocks.addAll(area.fill(x0, y0, z0, x1, y0, z1, Blocks.OAK_PLANKS.defaultBlockState()));
        blocks.addAll(area.fill(x0, y1, z0, x1, y1, z1, Blocks.SPRUCE_PLANKS.defaultBlockState()));
        for (int y = y0 + 1; y < y1; y++) {
            blocks.addAll(area.fill(x0, y, z0, x1, y, z0, Blocks.OAK_PLANKS.defaultBlockState()));
            blocks.addAll(area.fill(x0, y, z1, x1, y, z1, Blocks.OAK_PLANKS.defaultBlockState()));
            blocks.addAll(area.fill(x0, y, z0 + 1, x0, y, z1 - 1, Blocks.OAK_PLANKS.defaultBlockState()));
            blocks.addAll(area.fill(x1, y, z0 + 1, x1, y, z1 - 1, Blocks.OAK_PLANKS.defaultBlockState()));
        }
        return blocks;
    }

    /**
     * Builds a scene, lets it run for a while, then measures what crash physics costs per tick.
     *
     * @param cruise if not null, the ship is held at this velocity like an engine would; otherwise its engines just
     *               idle, which keeps it awake. {@link #NO_THRUST} leaves it alone.
     */
    private static void benchmark(final GameTestHelper helper, final int index, final String label, final int warmupTicks, final int measureTicks,
                                  @Nullable final Vector3d cruise, final Function<Area, ServerSubLevel> setup) {
        if (!BENCH && !DEMO) {
            helper.succeed();
            return;
        }
        final Area area = new Area(helper.getLevel(), new BlockPos(index * 128, 96, 64));
        final Area next = new Area(helper.getLevel(), new BlockPos(index * 128, 96, 192));
        area.forceChunks(true);
        next.forceChunks(true);

        final int[] tick = {0};
        final long[] worst = {0L};
        final long[] last = {0L};
        final List<ServerSubLevel> ship = new ArrayList<>(1);
        helper.onEachTick(() -> {
            final int now = ++tick[0];
            if (now == SETTLE_TICKS) {
                ship.add(setup.apply(area));
                return;
            }
            if (now > SETTLE_TICKS && cruise != NO_THRUST && !ship.getFirst().isRemoved()) {
                final RigidBodyHandle handle = RigidBodyHandle.of(ship.getFirst());
                if (handle != null && handle.isValid()) {
                    if (cruise != null) {
                        final Vector3d velocity = handle.getLinearVelocity(new Vector3d());
                        handle.addLinearAndAngularVelocity(new Vector3d(cruise.x - velocity.x, 0.0, cruise.z - velocity.z), new Vector3d());
                    } else {
                        handle.applyLinearAndAngularImpulse(new Vector3d(0.0, 1.0e-4, 0.0), new Vector3d(), true);
                    }
                }
            }
            if (now == SETTLE_TICKS + warmupTicks) {
                if (cruise == NO_THRUST) {
                    // Do it again next door, this time measured
                    ship.set(0, setup.apply(next));
                }
                CrashTimings.reset();
                worst[0] = 0L;
                last[0] = 0L;
            } else if (now > SETTLE_TICKS + warmupTicks) {
                final long total = CrashTimings.total();
                worst[0] = Math.max(worst[0], total - last[0]);
                last[0] = total;
            }
            if (now == SETTLE_TICKS + warmupTicks + measureTicks) {
                CrashTimings.report(label, measureTicks);
                final int debris = area.level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,
                                new net.minecraft.world.phys.AABB(next.origin).inflate(96)).size();
                CrashPhysics.LOGGER.info("[timings] {}: worst tick {} ms, {} falling blocks in the air", label,
                        String.format("%.3f", worst[0] / 1.0e6), debris);
                CrashPhysics.LOGGER.info("[timings] {}: ship at {}, moving at {} m/s", label, ship.getFirst().logicalPose().position(),
                        String.format("%.2f", velocity(ship.getFirst()).length()));
                area.forceChunks(false);
                next.forceChunks(false);
                helper.succeed();
            }
        });
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
        if (BENCH && !DEMO) {
            helper.succeed();
            return;
        }
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
