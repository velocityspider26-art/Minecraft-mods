package com.velocityspider.deathstar.structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.velocityspider.deathstar.block.ModBlocks;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Procedural generator for the <em>ruined</em> Death Star.
 *
 * <p>This is deliberately a wreck, not a pristine sphere: a huge section is blown open to reveal a
 * cutaway of the interior (the main reactor, the superlaser shaft, exposed decks and structural
 * girders), the hull is a two-layer skin peppered with craters, and a handful of Star Wars
 * Easter-egg rooms are tucked into the exposed decks. Generation runs on the server; the resulting
 * blocks are then assembled into a Sable physics body, so nothing here needs to be client-safe.
 *
 * <p>Everything is a pure function of the seed for reproducibility.
 */
public final class DeathStarBlueprint {

    public record Voxel(int x, int y, int z, BlockState state) {}

    /** A sign to place after the blocks are down (its text is applied via its block entity). */
    public record SignFeature(int x, int y, int z, Direction facing, List<String> lines) {}

    public record Blueprint(List<Voxel> voxels, List<SignFeature> signs) {}

    // Resolved block states (registration has happened by the time this runs).
    private static final class Palette {
        final BlockState hull = ModBlocks.HULL_PLATING.get().defaultBlockState();
        final BlockState hullDark = ModBlocks.HULL_PLATING_DARK.get().defaultBlockState();
        final BlockState greeble = ModBlocks.HULL_GREEBLE.get().defaultBlockState();
        final BlockState scorched = ModBlocks.SCORCHED_HULL.get().defaultBlockState();
        final BlockState frame = ModBlocks.REINFORCED_FRAME.get().defaultBlockState();
        final BlockState wall = ModBlocks.INTERIOR_WALL.get().defaultBlockState();
        final BlockState floor = ModBlocks.INTERIOR_FLOOR.get().defaultBlockState();
        final BlockState panel = ModBlocks.CONTROL_PANEL.get().defaultBlockState();
        final BlockState reactor = ModBlocks.REACTOR_CORE.get().defaultBlockState();
        final BlockState casing = ModBlocks.REACTOR_CASING.get().defaultBlockState();
        final BlockState lens = ModBlocks.SUPERLASER_LENS.get().defaultBlockState();
        final BlockState conduit = ModBlocks.POWER_CONDUIT.get().defaultBlockState();
        final BlockState bars = Blocks.IRON_BARS.defaultBlockState();
        final BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
    }

    // Fixed axes (unit vectors) so the silhouette is consistent between summons.
    private static final double[] DISH = unit(0.15, 0.72, 0.68);   // superlaser "eye", upper front
    private static final double[] GASH = unit(-0.30, -0.12, -0.95); // blown-open section, toward -Z

    private DeathStarBlueprint() {}

    public static Blueprint build(long seed, int radius, int thickness) {
        RandomSource rnd = RandomSource.create(seed);
        Palette p = new Palette();

        Map<Long, BlockState> blocks = new LinkedHashMap<>(80_000);
        List<SignFeature> signs = new ArrayList<>();

        final double R = radius;
        final double gashHalf = Math.toRadians(48.0);
        final double gashCos = Math.cos(gashHalf);
        final double dishHalf = Math.toRadians(22.0);
        final double dishCos = Math.cos(dishHalf);
        final int dishDepth = Math.max(3, radius / 6);
        final int trenchHalf = 2;

        // Secondary battle-damage craters (punch through the outer skin only).
        int craterCount = 7 + rnd.nextInt(6);
        double[][] craters = new double[craterCount][4];
        for (int i = 0; i < craterCount; i++) {
            double[] d = randomUnit(rnd);
            craters[i][0] = d[0]; craters[i][1] = d[1]; craters[i][2] = d[2];
            craters[i][3] = Math.cos(Math.toRadians(7.0 + rnd.nextDouble() * 12.0));
        }

        // ---- Hull scan -------------------------------------------------------------------------
        for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int y = -radius - 1; y <= radius + 1; y++) {
                for (int z = -radius - 1; z <= radius + 1; z++) {
                    double dist = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
                    if (dist > R + 0.5) continue;

                    boolean outer = dist >= R - 1.5 && dist <= R + 0.5;
                    boolean inner = dist >= R - 4.5 && dist <= R - 3.5;
                    if (!outer && !inner) continue;

                    double nx = x / dist, ny = y / dist, nz = z / dist;

                    // Superlaser dish is carved later; leave its cone empty for now.
                    double dishDot = nx * DISH[0] + ny * DISH[1] + nz * DISH[2];
                    if (dishDot >= dishCos) continue;

                    // Blown-open ruin: remove hull inside the gash cone with a jagged edge.
                    double gashDot = nx * GASH[0] + ny * GASH[1] + nz * GASH[2];
                    if (gashDot >= gashCos) {
                        double t = (gashDot - gashCos) / (1.0 - gashCos); // 0 at rim, 1 at centre
                        double jag = 0.10 + 0.22 * rnd.nextDouble();
                        if (t > jag) continue; // torn away
                        // Charred, twisted rim exposing the frame.
                        set(blocks, x, y, z, rnd.nextInt(3) == 0 ? p.frame : p.scorched);
                        continue;
                    }

                    // Equatorial trench: recess the outer skin into a dark channel.
                    if (outer && Math.abs(y) <= trenchHalf) {
                        if (dist <= R - 1) {
                            boolean tower = Math.floorMod(x * 31 + z * 17, 23) == 0;
                            set(blocks, x, y, z, tower ? p.panel : p.hullDark);
                        }
                        continue;
                    }

                    if (outer) {
                        // Secondary craters blow holes through the outer skin only.
                        double worst = -1.0;
                        for (double[] c : craters) {
                            double dot = nx * c[0] + ny * c[1] + nz * c[2];
                            if (dot > c[3]) worst = Math.max(worst, (dot - c[3]) / (1.0 - c[3]));
                        }
                        if (worst >= 0.0) {
                            if (worst > 0.4) continue;                // hole
                            set(blocks, x, y, z, p.scorched);         // scorched rim
                            continue;
                        }
                        set(blocks, x, y, z, panelFor(x, y, z, rnd, p));
                    } else { // inner hull
                        set(blocks, x, y, z, rnd.nextInt(5) == 0 ? p.wall : p.frame);
                    }
                }
            }
        }

        // ---- Superlaser dish (concave bowl + green focusing lens) -------------------------------
        buildDish(blocks, radius, dishCos, dishDepth, thickness, p);

        // ---- Interior: reactor, superlaser shaft, decks, girders -------------------------------
        buildReactor(blocks, radius, p);
        buildSuperlaserShaft(blocks, radius, p);
        buildDecks(blocks, radius, p, rnd);
        buildGirders(blocks, radius, p);

        // ---- Thermal exhaust port (the one Luke used) + Easter-egg rooms -----------------------
        buildExhaustPort(blocks, signs, radius, p);
        buildEasterEggRooms(blocks, signs, radius, p);

        // Exterior plaque near the trench.
        addSign(signs, (int) Math.round(GASH[0] * (R - 2)) + 3, trenchHalf + 3,
                (int) Math.round(GASH[2] * 0) - radius / 2, Direction.NORTH,
                List.of("THAT'S", "NO MOON.", "", "- ruins -"));

        // ---- Emit ------------------------------------------------------------------------------
        List<Voxel> voxels = new ArrayList<>(blocks.size());
        for (Map.Entry<Long, BlockState> e : blocks.entrySet()) {
            long k = e.getKey();
            voxels.add(new Voxel(unpackX(k), unpackY(k), unpackZ(k), e.getValue()));
        }
        return new Blueprint(voxels, signs);
    }

    // ------------------------------------------------------------------------------------------
    // Feature builders
    // ------------------------------------------------------------------------------------------

    private static void buildDish(Map<Long, BlockState> b, int radius, double dishCos, int dishDepth,
                                  int thickness, Palette p) {
        double R = radius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
                    if (dist < R - dishDepth - thickness || dist > R + 0.5) continue;
                    double nx = x / dist, ny = y / dist, nz = z / dist;
                    double dot = nx * DISH[0] + ny * DISH[1] + nz * DISH[2];
                    if (dot < dishCos) continue;
                    double t = (dot - dishCos) / (1.0 - dishCos);         // 0 rim, 1 centre
                    // Part of the dish rim is blown away on the ruined station.
                    if (t < 0.25 && (Math.floorMod(x + z, 7) == 0)) continue;
                    double bowl = (R + 0.5) - dishDepth * smooth(t);
                    if (Math.abs(dist - bowl) <= thickness * 0.6 + 0.5) {
                        set(b, x, y, z, t > 0.86 ? p.lens : (t > 0.5 ? p.conduit : p.hullDark));
                    }
                }
            }
        }
    }

    private static void buildReactor(Map<Long, BlockState> b, int radius, Palette p) {
        int coreR = Math.max(3, radius / 10);
        for (int x = -coreR - 2; x <= coreR + 2; x++) {
            for (int y = -coreR - 2; y <= coreR + 2; y++) {
                for (int z = -coreR - 2; z <= coreR + 2; z++) {
                    double d = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
                    if (d <= coreR) set(b, x, y, z, p.reactor);
                    else if (d <= coreR + 1.4) set(b, x, y, z, p.casing);
                }
            }
        }
    }

    /** Vertical reactor / exhaust shaft up the Y axis, plus the superlaser feed tube to the dish. */
    private static void buildSuperlaserShaft(Map<Long, BlockState> b, int radius, Palette p) {
        int half = (int) (radius * 0.55);
        for (int y = -half; y <= half; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    double r = Math.hypot(dx, dz);
                    if (r > 2.2) continue;
                    if (r > 1.2) set(b, dx, y, dz, p.casing);   // shaft wall
                    else set(b, dx, y, dz, p.conduit);          // energy column
                }
            }
        }
        // Superlaser tube from the core out to the dish focus.
        double R = radius;
        for (double t = 0; t <= R; t += 0.5) {
            int x = (int) Math.round(DISH[0] * t);
            int y = (int) Math.round(DISH[1] * t);
            int z = (int) Math.round(DISH[2] * t);
            set(b, x, y, z, t > R - 6 ? p.lens : p.conduit);
        }
    }

    /** Partial decks, only within the exposed gash sector so they read as an exposed cross-section. */
    private static void buildDecks(Map<Long, BlockState> b, int radius, Palette p, RandomSource rnd) {
        double[] gh = unit(GASH[0], 0, GASH[2]); // gash direction, flattened
        double ghCos = Math.cos(Math.toRadians(52.0));
        int inner = radius - 6;
        int[] levels = {-radius / 2, -radius / 4, radius / 4, radius / 2};
        for (int y : levels) {
            double rr = (inner * inner) - (double) y * y;
            if (rr <= 4) continue;
            int r = (int) Math.sqrt(rr);
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double d = Math.hypot(x, z);
                    if (d > r || d < 4) continue;
                    double hx = x / d, hz = z / d;
                    if (hx * gh[0] + hz * gh[2] < ghCos) continue;     // only in the opening
                    set(b, x, y, z, p.floor);
                    if (Math.floorMod(x, 9) == 0) set(b, x, y + 1, z, p.wall); // partition
                    if (rnd.nextInt(40) == 0) set(b, x, y + 1, z, p.panel);    // a console here and there
                }
            }
        }
    }

    private static void buildGirders(Map<Long, BlockState> b, int radius, Palette p) {
        int spokes = 10;
        int from = Math.max(4, radius / 8);
        int to = radius - 3;
        for (int i = 0; i < spokes; i++) {
            double a = (Math.PI * 2 * i) / spokes;
            double tilt = (i % 3 - 1) * 0.4;
            double dx = Math.cos(a), dz = Math.sin(a), dy = tilt;
            double[] n = unit(dx, dy, dz);
            for (int t = from; t <= to; t++) {
                int x = (int) Math.round(n[0] * t);
                int y = (int) Math.round(n[1] * t);
                int z = (int) Math.round(n[2] * t);
                set(b, x, y, z, p.frame);
                set(b, x, y + 1, z, p.frame);
            }
        }
    }

    private static void buildExhaustPort(Map<Long, BlockState> b, List<SignFeature> signs, int radius, Palette p) {
        // Sit the port on the trench, front of the station.
        double[] dir = unit(0.96, 0.05, 0.28);
        int px = (int) Math.round(dir[0] * (radius - 1));
        int py = 0;
        int pz = (int) Math.round(dir[2] * (radius - 1));
        // Recessed 3x3 port ringed with conduit, with a shaft drilled inward toward the reactor.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(b, px + dx, py, pz + dz, (dx == 0 && dz == 0) ? p.conduit : p.casing);
            }
        }
        for (int t = 1; t <= radius - 4; t++) {
            int x = (int) Math.round(px - dir[0] * t);
            int z = (int) Math.round(pz - dir[2] * t);
            set(b, x, py, z, p.conduit);
        }
        addSign(signs, px, py + 2, pz, Direction.EAST,
                List.of("THERMAL", "EXHAUST PORT", "2m wide", "-> reactor"));
    }

    /** The fun part: recognisable rooms tucked into the exposed decks, facing the gash opening. */
    private static void buildEasterEggRooms(Map<Long, BlockState> b, List<SignFeature> signs, int radius, Palette p) {
        int deep = radius - 14;
        // Detention block AA-23 / cell 1138.
        Room det = room(b, -8, radius / 5, -deep, 5, 3, 4, p);
        for (int y = det.y0 + 1; y <= det.y0 + 3; y++) {
            set(b, det.cx - 1, y, det.cz, p.bars);
            set(b, det.cx + 1, y, det.cz, p.bars);
        }
        addSign(signs, det.cx, det.cy, det.cz + det.hz, Direction.NORTH,
                List.of("DETENTION", "BLOCK AA-23", "", "cell 1138"));

        // Trash compactor 3263827 (the numbers Han shouts).
        Room tc = room(b, 9, -radius / 6, -deep + 3, 4, 3, 4, p);
        set(b, tc.cx - tc.hx + 1, tc.cy, tc.cz, p.hullDark); // the closing walls
        set(b, tc.cx + tc.hx - 1, tc.cy, tc.cz, p.hullDark);
        addSign(signs, tc.cx, tc.cy, tc.cz + tc.hz, Direction.NORTH,
                List.of("GARBAGE", "MASHER", "3263827", "!!!"));

        // Tractor beam control (Obi-Wan's target).
        Room tb = room(b, -15, 2, -deep - 2, 3, 3, 3, p);
        set(b, tb.cx, tb.cy, tb.cz, p.panel);
        set(b, tb.cx, tb.cy + 1, tb.cz, p.conduit);
        addSign(signs, tb.cx, tb.cy, tb.cz + tb.hz, Direction.NORTH,
                List.of("TRACTOR BEAM", "control", "1 of 7", "> power <"));

        // Emperor's throne room (Death Star II nod).
        Room th = room(b, 3, radius / 3, -deep + 2, 5, 4, 4, p);
        set(b, th.cx, th.y0 + 1, th.cz + th.hz - 1, p.hullDark); // throne dais
        set(b, th.cx, th.y0 + 2, th.cz + th.hz - 1, p.panel);
        addSign(signs, th.cx, th.cy + 1, th.cz + th.hz, Direction.NORTH,
                List.of("THRONE ROOM", "", "\"a fully armed", "battle station\""));

        // A couple of loose quips.
        addSign(signs, 2, radius / 5 + 4, -deep - 5, Direction.NORTH,
                List.of("", "IT'S A TRAP!", "", ""));
        addSign(signs, -4, -3, -deep - 6, Direction.NORTH,
                List.of("I have a bad", "feeling about", "this...", ""));
    }

    // ------------------------------------------------------------------------------------------
    // Room helper
    // ------------------------------------------------------------------------------------------

    private record Room(int cx, int cy, int cz, int hx, int hy, int hz, int y0) {}

    /**
     * Hollow box of interior walls with a floor, a ceiling lamp and a doorway on the -Z (gash) side.
     * Returns its geometry so callers can furnish it.
     */
    private static Room room(Map<Long, BlockState> b, int cx, int cy, int cz, int hx, int hy, int hz, Palette p) {
        int y0 = cy - hy;
        for (int x = cx - hx; x <= cx + hx; x++) {
            for (int y = cy - hy; y <= cy + hy; y++) {
                for (int z = cz - hz; z <= cz + hz; z++) {
                    boolean shell = x == cx - hx || x == cx + hx || y == cy - hy || y == cy + hy
                            || z == cz - hz || z == cz + hz;
                    if (shell) {
                        set(b, x, y, z, (y == cy - hy) ? p.floor : p.wall);
                    } else {
                        b.remove(pack(x, y, z)); // hollow it out (clear any hull/girder inside)
                    }
                }
            }
        }
        // Doorway on the -Z wall.
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int y = y0 + 1; y <= y0 + 2; y++) {
                b.remove(pack(x, y, cz - hz));
            }
        }
        set(b, cx, cy + hy, cz, p.lamp); // ceiling light
        return new Room(cx, cy, cz, hx, hy, hz, y0);
    }

    private static void addSign(List<SignFeature> signs, int x, int y, int z, Direction facing, List<String> lines) {
        signs.add(new SignFeature(x, y, z, facing, lines));
    }

    // ------------------------------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------------------------------

    private static BlockState panelFor(int x, int y, int z, RandomSource rnd, Palette p) {
        boolean seam = (Math.floorMod(x, 6) == 0) || (Math.floorMod(y, 6) == 0) || (Math.floorMod(z, 6) == 0);
        if (seam) return p.hullDark;
        int r = rnd.nextInt(16);
        if (r == 0) return p.greeble;
        if (r <= 4) return p.hullDark;
        return p.hull;
    }

    private static double smooth(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double[] unit(double x, double y, double z) {
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len < 1.0e-9) return new double[] {0, 1, 0};
        return new double[] {x / len, y / len, z / len};
    }

    private static double[] randomUnit(RandomSource rand) {
        double a, b, s;
        do {
            a = rand.nextDouble() * 2.0 - 1.0;
            b = rand.nextDouble() * 2.0 - 1.0;
            s = a * a + b * b;
        } while (s >= 1.0 || s == 0.0);
        double f = 2.0 * Math.sqrt(1.0 - s);
        return new double[] {a * f, b * f, 1.0 - 2.0 * s};
    }

    // Position packing: each coord offset by 1024 into 12 bits (range -1024..3071 covers any radius).
    private static long pack(int x, int y, int z) {
        return ((long) (x + 1024) << 24) | ((long) (y + 1024) << 12) | (z + 1024);
    }

    private static int unpackX(long k) { return (int) ((k >> 24) & 0xFFF) - 1024; }
    private static int unpackY(long k) { return (int) ((k >> 12) & 0xFFF) - 1024; }
    private static int unpackZ(long k) { return (int) (k & 0xFFF) - 1024; }

    private static void set(Map<Long, BlockState> b, int x, int y, int z, BlockState state) {
        b.put(pack(x, y, z), state);
    }
}
