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
 * Procedural generator for the Death Star <em>wreckage</em>, styled after the Kef Bir sea-wreck in
 * <i>The Rise of Skywalker</i>: not one sphere with a hole in it, but a scattered field of separate
 * broken pieces — huge curved hull shards, a torn superlaser-dish section, and smaller twisted
 * debris.
 *
 * <p>{@link #buildWreckField} returns a list of independent {@link Fragment}s. The assembler places
 * and assembles each one into its <em>own</em> Sable physics body, so the ruins really are "in
 * pieces": each shard falls, collides and settles on its own.
 *
 * <p>Everything is a pure function of the seed. Generation runs server-side only.
 */
public final class DeathStarBlueprint {

    public record Voxel(int x, int y, int z, BlockState state) {}

    /** A sign to place after the blocks are down (its text is applied via its block entity). */
    public record SignFeature(int x, int y, int z, Direction facing, List<String> lines) {}

    /**
     * One broken piece. {@code voxels}/{@code signs} are centred on the piece's own origin; the
     * {@code offset*} fields say where to drop that origin relative to the summon centre.
     */
    public record Fragment(String name, int offsetX, int offsetY, int offsetZ,
                           List<Voxel> voxels, List<SignFeature> signs) {}

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

    private static final double[] DISH = unit(0.15, 0.72, 0.68); // superlaser "eye" direction

    private DeathStarBlueprint() {}

    /**
     * Builds the whole scattered wreck as a list of independent pieces.
     *
     * @param radius    the radius of the intact station the shards were cut from
     * @param thickness hull skin thickness
     */
    public static List<Fragment> buildWreckField(long seed, int radius, int thickness) {
        RandomSource rnd = RandomSource.create(seed);
        Palette p = new Palette();
        List<Fragment> frags = new ArrayList<>();

        // 1) The big hero shard: a great curved slab of hull with a chunk of interior + rooms.
        {
            Map<Long, BlockState> b = new LinkedHashMap<>();
            List<SignFeature> signs = new ArrayList<>();
            double[] dir = unit(-0.2, 0.15, -0.97);
            shardInto(b, radius, thickness, dir, 55.0, true, p, rnd);
            addInteriorChunk(b, signs, radius, dir, p, rnd);
            frags.add(finish("hero_hull", b, signs, offset(0, radius, rnd)));
        }

        // 2) The superlaser-dish section, torn free.
        {
            Map<Long, BlockState> b = new LinkedHashMap<>();
            dishShardInto(b, radius, thickness, p, rnd);
            frags.add(finish("superlaser_section", b, new ArrayList<>(), offset(1, radius, rnd)));
        }

        // 3) A few medium curved hull shards.
        int medium = 3;
        for (int i = 0; i < medium; i++) {
            Map<Long, BlockState> b = new LinkedHashMap<>();
            double[] dir = randomUnit(rnd);
            shardInto(b, radius, thickness, dir, 24.0 + rnd.nextDouble() * 12.0, rnd.nextBoolean(), p, rnd);
            frags.add(finish("hull_shard_" + i, b, new ArrayList<>(), offset(2 + i, radius, rnd)));
        }

        // 4) Small twisted debris chunks.
        int debris = 4;
        for (int i = 0; i < debris; i++) {
            Map<Long, BlockState> b = new LinkedHashMap<>();
            debrisInto(b, 3 + rnd.nextInt(4), p, rnd);
            frags.add(finish("debris_" + i, b, new ArrayList<>(), offset(2 + medium + i, radius, rnd)));
        }

        return frags;
    }

    // ------------------------------------------------------------------------------------------
    // Piece builders
    // ------------------------------------------------------------------------------------------

    /** A curved spherical-cap slice of the two-layer hull, with a ragged torn boundary. */
    private static void shardInto(Map<Long, BlockState> b, int radius, int thickness, double[] dir,
                                  double halfAngleDeg, boolean allowTrench, Palette p, RandomSource rnd) {
        double R = radius;
        double cosA = Math.cos(Math.toRadians(halfAngleDeg));
        for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int y = -radius - 1; y <= radius + 1; y++) {
                for (int z = -radius - 1; z <= radius + 1; z++) {
                    double dist = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
                    if (dist > R + 0.5) continue;
                    boolean outer = dist >= R - 1.5 && dist <= R + 0.5;
                    boolean inner = dist >= R - 4.5 && dist <= R - 3.5;
                    if (!outer && !inner) continue;

                    double nx = x / dist, ny = y / dist, nz = z / dist;
                    double dot = nx * dir[0] + ny * dir[1] + nz * dir[2];
                    if (dot < cosA) continue;

                    double t = (dot - cosA) / (1.0 - cosA); // 0 at cap edge, 1 at cap centre
                    double jag = 0.06 + 0.16 * rnd.nextDouble();
                    if (t < jag) continue; // torn, ragged edge
                    boolean rim = t < jag + 0.12;

                    if (outer) {
                        if (rim) {
                            set(b, x, y, z, rnd.nextInt(3) == 0 ? p.frame : p.scorched);
                        } else if (allowTrench && Math.abs(y) <= 2 && dist <= R - 1) {
                            set(b, x, y, z, p.hullDark); // a slice of the equatorial trench
                        } else {
                            set(b, x, y, z, panelFor(x, y, z, rnd, p));
                        }
                    } else {
                        set(b, x, y, z, rim ? p.frame : (rnd.nextInt(5) == 0 ? p.wall : p.frame));
                    }
                }
            }
        }
    }

    /** The concave superlaser dish plus a torn ring of hull around it. */
    private static void dishShardInto(Map<Long, BlockState> b, int radius, int thickness, Palette p, RandomSource rnd) {
        double R = radius;
        int dishDepth = Math.max(3, radius / 6);
        double dishCos = Math.cos(Math.toRadians(24.0));
        double rimCos = Math.cos(Math.toRadians(34.0));
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
                    if (dist < R - dishDepth - thickness - 1 || dist > R + 0.5) continue;
                    double nx = x / dist, ny = y / dist, nz = z / dist;
                    double dot = nx * DISH[0] + ny * DISH[1] + nz * DISH[2];
                    if (dot < rimCos) continue;

                    double te = (dot - rimCos) / (1.0 - rimCos);
                    if (te < 0.06 + 0.12 * rnd.nextDouble()) continue; // ragged edge

                    if (dot >= dishCos) {
                        double tt = (dot - dishCos) / (1.0 - dishCos);
                        double bowl = (R + 0.5) - dishDepth * smooth(tt);
                        if (Math.abs(dist - bowl) <= thickness * 0.6 + 0.5) {
                            set(b, x, y, z, tt > 0.86 ? p.lens : (tt > 0.5 ? p.conduit : p.hullDark));
                        }
                    } else {
                        boolean outer = dist >= R - 1.5 && dist <= R + 0.5;
                        boolean inner = dist >= R - 4.5 && dist <= R - 3.5;
                        if (outer) set(b, x, y, z, te < 0.2 ? p.scorched : panelFor(x, y, z, rnd, p));
                        else if (inner) set(b, x, y, z, p.frame);
                    }
                }
            }
        }
    }

    /** A small blob of twisted metal. */
    private static void debrisInto(Map<Long, BlockState> b, int size, Palette p, RandomSource rnd) {
        BlockState[] mix = {p.frame, p.scorched, p.hull, p.hullDark, p.greeble};
        for (int x = -size; x <= size; x++) {
            for (int y = -size; y <= size; y++) {
                for (int z = -size; z <= size; z++) {
                    double d = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
                    if (d > size + 0.3) continue;
                    if (rnd.nextDouble() < 0.35) continue; // jagged / hollow
                    set(b, x, y, z, mix[rnd.nextInt(mix.length)]);
                }
            }
        }
    }

    /** Hangs a chunk of interior (reactor stub, girder, decks, Easter-egg rooms) under the hero shard. */
    private static void addInteriorChunk(Map<Long, BlockState> b, List<SignFeature> signs, int radius,
                                         double[] dir, Palette p, RandomSource rnd) {
        // Perpendicular basis for laying things out under the shard.
        double[] up = Math.abs(dir[1]) > 0.9 ? new double[] {1, 0, 0} : new double[] {0, 1, 0};
        double[] u = unit(cross(up, dir));
        double[] v = unit(cross(dir, u));

        // A structural girder reaching in from the hull toward the old core.
        for (int t = radius - 4; t >= radius / 3; t--) {
            int x = (int) Math.round(dir[0] * t);
            int y = (int) Math.round(dir[1] * t);
            int z = (int) Math.round(dir[2] * t);
            set(b, x, y, z, p.frame);
            set(b, x, y + 1, z, p.frame);
        }
        // A reactor stub still clinging to the piece.
        int cr = radius / 3;
        int[] core = at(dir, cr);
        for (int dx = -3; dx <= 3; dx++)
            for (int dy = -3; dy <= 3; dy++)
                for (int dz = -3; dz <= 3; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d <= 2) set(b, core[0] + dx, core[1] + dy, core[2] + dz, p.reactor);
                    else if (d <= 3.3) set(b, core[0] + dx, core[1] + dy, core[2] + dz, p.casing);
                }

        // Two partial decks and the Easter-egg rooms, laid on the concave side.
        int[] base = at(dir, radius - 12);
        deck(b, base, u, v, p);
        int[] base2 = at(dir, radius - 18);
        deck(b, base2, u, v, p);

        buildRooms(b, signs, base, u, v, p);
    }

    private static void deck(Map<Long, BlockState> b, int[] c, double[] u, double[] v, Palette p) {
        for (int a = -7; a <= 7; a++) {
            for (int e = -7; e <= 7; e++) {
                if (a * a + e * e > 49) continue;
                int x = (int) Math.round(c[0] + u[0] * a + v[0] * e);
                int y = (int) Math.round(c[1] + u[1] * a + v[1] * e);
                int z = (int) Math.round(c[2] + u[2] * a + v[2] * e);
                set(b, x, y, z, p.floor);
            }
        }
    }

    /** The Star Wars Easter-egg rooms, placed around a base point using the shard's local axes. */
    private static void buildRooms(Map<Long, BlockState> b, List<SignFeature> signs, int[] base,
                                   double[] u, double[] v, Palette p) {
        // Detention Block AA-23 / cell 1138.
        Room det = room(b, offAt(base, u, v, -10, 4), 5, 3, 4, p);
        for (int y = det.y0 + 1; y <= det.y0 + 3; y++) {
            set(b, det.cx - 1, y, det.cz, p.bars);
            set(b, det.cx + 1, y, det.cz, p.bars);
        }
        addSign(signs, det.cx, det.cy, det.cz + det.hz, Direction.NORTH,
                List.of("DETENTION", "BLOCK AA-23", "", "cell 1138"));

        // Garbage masher 3263827.
        Room tc = room(b, offAt(base, u, v, 10, -4), 4, 3, 4, p);
        set(b, tc.cx - tc.hx + 1, tc.cy, tc.cz, p.hullDark);
        set(b, tc.cx + tc.hx - 1, tc.cy, tc.cz, p.hullDark);
        addSign(signs, tc.cx, tc.cy, tc.cz + tc.hz, Direction.NORTH,
                List.of("GARBAGE", "MASHER", "3263827", "!!!"));

        // Tractor beam control.
        Room tb = room(b, offAt(base, u, v, -14, -8), 3, 3, 3, p);
        set(b, tb.cx, tb.cy, tb.cz, p.panel);
        set(b, tb.cx, tb.cy + 1, tb.cz, p.conduit);
        addSign(signs, tb.cx, tb.cy, tb.cz + tb.hz, Direction.NORTH,
                List.of("TRACTOR BEAM", "control", "1 of 7", "> power <"));

        // Emperor's throne room.
        Room th = room(b, offAt(base, u, v, 6, 10), 5, 4, 4, p);
        set(b, th.cx, th.y0 + 1, th.cz + th.hz - 1, p.hullDark);
        set(b, th.cx, th.y0 + 2, th.cz + th.hz - 1, p.panel);
        addSign(signs, th.cx, th.cy + 1, th.cz + th.hz, Direction.NORTH,
                List.of("THRONE ROOM", "", "\"a fully armed", "battle station\""));

        // Loose quips.
        int[] q1 = offAt(base, u, v, 0, 14);
        addSign(signs, q1[0], q1[1], q1[2], Direction.NORTH, List.of("", "IT'S A TRAP!", "", ""));
        int[] q2 = offAt(base, u, v, -18, 2);
        addSign(signs, q2[0], q2[1], q2[2], Direction.NORTH,
                List.of("I have a bad", "feeling about", "this...", ""));
        int[] q3 = offAt(base, u, v, 16, 6);
        addSign(signs, q3[0], q3[1], q3[2], Direction.NORTH, List.of("THAT'S", "NO MOON.", "", "- ruins -"));
    }

    // ------------------------------------------------------------------------------------------
    // Room helper
    // ------------------------------------------------------------------------------------------

    private record Room(int cx, int cy, int cz, int hx, int hy, int hz, int y0) {}

    private static Room room(Map<Long, BlockState> b, int[] c, int hx, int hy, int hz, Palette p) {
        int cx = c[0], cy = c[1], cz = c[2];
        int y0 = cy - hy;
        for (int x = cx - hx; x <= cx + hx; x++) {
            for (int y = cy - hy; y <= cy + hy; y++) {
                for (int z = cz - hz; z <= cz + hz; z++) {
                    boolean shell = x == cx - hx || x == cx + hx || y == cy - hy || y == cy + hy
                            || z == cz - hz || z == cz + hz;
                    if (shell) set(b, x, y, z, (y == cy - hy) ? p.floor : p.wall);
                    else b.remove(pack(x, y, z));
                }
            }
        }
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int y = y0 + 1; y <= y0 + 2; y++) {
                b.remove(pack(x, y, cz - hz));
            }
        }
        set(b, cx, cy + hy, cz, p.lamp);
        return new Room(cx, cy, cz, hx, hy, hz, y0);
    }

    // ------------------------------------------------------------------------------------------
    // Fragment finishing + offsets
    // ------------------------------------------------------------------------------------------

    /** Re-centres a piece on its own bounding-box centre and packages it as a {@link Fragment}. */
    private static Fragment finish(String name, Map<Long, BlockState> b, List<SignFeature> signs, int[] off) {
        if (b.isEmpty()) return new Fragment(name, off[0], off[1], off[2], List.of(), List.of());
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (long k : b.keySet()) {
            int x = unpackX(k), y = unpackY(k), z = unpackZ(k);
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
        }
        int cx = (minX + maxX) / 2, cy = (minY + maxY) / 2, cz = (minZ + maxZ) / 2;
        List<Voxel> vox = new ArrayList<>(b.size());
        for (Map.Entry<Long, BlockState> e : b.entrySet()) {
            long k = e.getKey();
            vox.add(new Voxel(unpackX(k) - cx, unpackY(k) - cy, unpackZ(k) - cz, e.getValue()));
        }
        List<SignFeature> sh = new ArrayList<>(signs.size());
        for (SignFeature s : signs) {
            sh.add(new SignFeature(s.x() - cx, s.y() - cy, s.z() - cz, s.facing(), s.lines()));
        }
        return new Fragment(name, off[0], off[1], off[2], vox, sh);
    }

    /** Scatters fragment {@code i} across a rough field so the pieces do not spawn intersecting. */
    private static int[] offset(int i, int radius, RandomSource rnd) {
        if (i == 0) return new int[] {0, 6, 0};
        double sp = radius * 1.5;
        double angle = i * 2.399963; // golden angle, spreads pieces evenly
        double rad = sp * (0.8 + 0.28 * i);
        int x = (int) Math.round(Math.cos(angle) * rad);
        int z = (int) Math.round(Math.sin(angle) * rad);
        int y = 4 + (i % 3) * 4 + rnd.nextInt(3);
        return new int[] {x, y, z};
    }

    // ------------------------------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------------------------------

    private static int[] at(double[] dir, int t) {
        return new int[] {(int) Math.round(dir[0] * t), (int) Math.round(dir[1] * t), (int) Math.round(dir[2] * t)};
    }

    private static int[] offAt(int[] base, double[] u, double[] v, int a, int e) {
        return new int[] {
                (int) Math.round(base[0] + u[0] * a + v[0] * e),
                (int) Math.round(base[1] + u[1] * a + v[1] * e),
                (int) Math.round(base[2] + u[2] * a + v[2] * e)};
    }

    private static void addSign(List<SignFeature> signs, int x, int y, int z, Direction facing, List<String> lines) {
        signs.add(new SignFeature(x, y, z, facing, lines));
    }

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

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] unit(double x, double y, double z) {
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len < 1.0e-9) return new double[] {0, 1, 0};
        return new double[] {x / len, y / len, z / len};
    }

    private static double[] unit(double[] a) { return unit(a[0], a[1], a[2]); }

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
