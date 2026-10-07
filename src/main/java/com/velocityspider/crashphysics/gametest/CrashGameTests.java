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
            area.fill(0, 0, 0, 15, GROUND_TOP, 15, Blocks.DIRT.defaultBlockState());
            final ServerSubLevel vehicle = area.assemble(area.fill(6, 16, 6, 8, 18, 8, Blocks.OAK_PLANKS.defaultBlockState()));
            launch(vehicle, 0.0, -30.0, 0.0);
            return Counters.of(area.level);
        }, (area, before) -> {
            final Counters after = Counters.of(area.level);
            final long dug = after.terrain - before.terrain;
            final long hull = after.vehicle - before.vehicle;
            CrashPhysics.LOGGER.info("[gametest] dirt dive: {} ground blocks dug out, {} hull blocks lost", dug, hull);
            helper.assertTrue(dug >= 4, "expected a crater, but only " + dug + " ground blocks were broken");
            helper.assertTrue(hull <= 4, "planks are stronger than dirt, but " + hull + " hull blocks broke");
        });
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
            launch(vehicle, 0.0, -35.0, 0.0);
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

        ServerSubLevel assemble(final List<BlockPos> blocks) {
            final BoundingBox3ic bounds = BoundingBox3i.from(blocks).expand(1, 1, 1);
            return SubLevelAssemblyHelper.assembleBlocks(this.level, blocks.getFirst(), blocks, bounds);
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
