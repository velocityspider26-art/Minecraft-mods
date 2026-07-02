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
 * Procedural generator for the ruined Death Star, built as <b>one whole connected body</b>: a
 * battle-scarred sphere with a huge section of the top torn open, revealing a cutaway of the
 * <b>second Death Star</b>'s interior (Emperor's throne room, turbolift shaft, and the main reactor
 * chamber with the exposed core), plus the superlaser dish, the equatorial trench, scattered
 * craters and a patch of skeletal under-construction superstructure.
 *
 * <p>{@link #build} returns a single {@link Fragment}; the assembler assembles it into one Sable
 * physics body. Everything is a pure function of the seed (server-side only).
 */
public final class DeathStarBlueprint {

    public record Voxel(int x, int y, int z, BlockState state) {}

    /** A sign to place after the blocks are down (its text is applied via its block entity). */
    public record SignFeature(int x, int y, int z, Direction facing, List<String> lines) {}

    /** The finished build, centred on its own origin. */
    public record Fragment(List<Voxel> voxels, List<SignFeature> signs) {}

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
        final BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
        final BlockState glass = Blocks.GRAY_STAINED_GLASS.defaultBlockState();
    }

    private static final double[] GASH = unit(0.0, 1.0, 0.25);      // blown-open section: the top, tilted forward
    private static final double[] DISH = unit(0.80, 0.05, 0.55);    // superlaser "eye" on the side
    private static final double[] SCAFFOLD = unit(-0.55, -0.35, 0.6); // unfinished superstructure patch

    private DeathStarBlueprint() {}

    public static Fragment build(long seed, int radius, int thickness) {
        RandomSource rnd = RandomSource.create(seed);
        Palette p = new Palette();
        Map<Long, BlockState> b = new LinkedHashMap<>(60_000);
        List<SignFeature> signs = new ArrayList<>();

        hullScan(b, radius, thickness, p, rnd);
        buildDish(b, radius, thickness, p, rnd);
        buildInteriorII(b, signs, radius, p, rnd);

        return finish(b, signs);
    }

    // ------------------------------------------------------------------------------------------
    // Hull
    // ------------------------------------------------------------------------------------------

    private static void hullScan(Map<Long, BlockState> b, int radius, int thickness, Palette p, RandomSource rnd) {
        double R = radius;
        double gashCos = Math.cos(Math.toRadians(46.0));
        double dishCos = Math.cos(Math.toRadians(22.0));
        double scaffoldCos = Math.cos(Math.toRadians(24.0));
        int trenchHalf = 2;

        int craterCount = 6 + rnd.nextInt(5);
        double[][] craters = new double[craterCount][4];
        for (int i = 0; i < craterCount; i++) {
            double[] d = randomUnit(rnd);
            craters[i][0] = d[0]; craters[i][1] = d[1]; craters[i][2] = d[2];
            craters[i][3] = Math.cos(Math.toRadians(7.0 + rnd.nextDouble() * 11.0));
        }

        for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int y = -radius - 1; y <= radius + 1; y++) {
                for (int z = -radius - 1; z <= radius + 1; z++) {
                    double dist = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
                    if (dist > R + 0.5) continue;
                    boolean outer = dist >= R - 1.5 && dist <= R + 0.5;
                    boolean inner = dist >= R - 4.5 && dist <= R - 3.5;
                    if (!outer && !inner) continue;

                    double nx = x / dist, ny = y / dist, nz = z / dist;

                    // The dish cone is filled by buildDish; leave its outer skin out here.
                    double dishDot = nx * DISH[0] + ny * DISH[1] + nz * DISH[2];
                    if (outer && dishDot >= dishCos) continue;

                    // Blown-open top: tear the hull away inside the gash cone, with a jagged rim.
                    double gashDot = nx * GASH[0] + ny * GASH[1] + nz * GASH[2];
                    if (gashDot >= gashCos) {
                        double t = (gashDot - gashCos) / (1.0 - gashCos);
                        if (t > 0.10 + 0.22 * rnd.nextDouble()) continue;
                        set(b, x, y, z, rnd.nextInt(3) == 0 ? p.frame : p.scorched);
                        continue;
                    }

                    // Unfinished skeletal superstructure patch.
                    if (outer && nx * SCAFFOLD[0] + ny * SCAFFOLD[1] + nz * SCAFFOLD[2] > scaffoldCos) {
                        boolean rib = Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0 || Math.floorMod(y, 4) == 0;
                        if (rib) set(b, x, y, z, p.frame);
                        continue;
                    }

                    // Equatorial trench.
                    if (outer && Math.abs(y) <= trenchHalf) {
                        if (dist <= R - 1) {
                            boolean tower = Math.floorMod(x * 31 + z * 17, 23) == 0;
                            set(b, x, y, z, tower ? p.panel : p.hullDark);
                        }
                        continue;
                    }

                    if (outer) {
                        double worst = -1.0;
                        for (double[] c : craters) {
                            double dot = nx * c[0] + ny * c[1] + nz * c[2];
                            if (dot > c[3]) worst = Math.max(worst, (dot - c[3]) / (1.0 - c[3]));
                        }
                        if (worst >= 0.0) {
                            if (worst > 0.4) continue;
                            set(b, x, y, z, p.scorched);
                            continue;
                        }
                        set(b, x, y, z, panelFor(x, y, z, rnd, p));
                    } else {
                        set(b, x, y, z, rnd.nextInt(6) == 0 ? p.wall : p.frame);
                    }
                }
            }
        }
    }

    /** Fills the superlaser dish cone with a concave green-lensed bowl. */
    private static void buildDish(Map<Long, BlockState> b, int radius, int thickness, Palette p, RandomSource rnd) {
        double R = radius;
        int dishDepth = Math.max(3, radius / 6);
        double dishCos = Math.cos(Math.toRadians(22.0));
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
                    if (dist < R - dishDepth - thickness || dist > R + 0.5) continue;
                    double nx = x / dist, ny = y / dist, nz = z / dist;
                    double dot = nx * DISH[0] + ny * DISH[1] + nz * DISH[2];
                    if (dot < dishCos) continue;
                    double t = (dot - dishCos) / (1.0 - dishCos);
                    double bowl = (R + 0.5) - dishDepth * smooth(t);
                    if (Math.abs(dist - bowl) <= thickness * 0.6 + 0.5) {
                        set(b, x, y, z, t > 0.86 ? p.lens : (t > 0.5 ? p.conduit : p.hullDark));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Death Star II interior (centred; throne room up top, reactor at the core)
    // ------------------------------------------------------------------------------------------

    private static void buildInteriorII(Map<Long, BlockState> b, List<SignFeature> signs, int radius, Palette p, RandomSource rnd) {
        int cr = Math.max(6, radius / 3);           // reactor chamber radius (at the core)
        reactorChamber(b, cr, p);
        reactorCoreAssembly(b, signs, cr, p);
        reactorTunnel(b, cr, radius, p);            // the shaft the Falcon flew down (bored toward +Z)

        int throneY = radius - 8;
        turbolift(b, cr - 1, throneY, p);           // vertical shaft up to the throne
        throneRoom(b, signs, throneY, p);

        addSign(signs, cr + 2, 1, 0, Direction.EAST, List.of("", "IT'S A TRAP!", "- Ackbar", ""));
        addSign(signs, -3, -cr - 1, 3, Direction.NORTH, List.of("MANY BOTHANS", "died to bring", "us this sign", "1138"));
    }

    /** Hollow reactor chamber at the core, open at the top (toward the gash) so the core is visible. */
    private static void reactorChamber(Map<Long, BlockState> b, int cr, Palette p) {
        for (int x = -cr; x <= cr; x++)
            for (int y = -cr; y <= cr; y++)
                for (int z = -cr; z <= cr; z++) {
                    double d = Math.sqrt((double) x * x + y * y + z * z);
                    if (Math.abs(d - cr) > 0.8) continue;
                    if ((double) y / cr > 0.35) continue; // open the top of the chamber
                    set(b, x, y, z, ((x + y + z) & 3) == 0 ? p.frame : p.casing);
                }
    }

    /** The suspended glowing reactor core, support struts and two catwalk rings. */
    private static void reactorCoreAssembly(Map<Long, BlockState> b, List<SignFeature> signs, int cr, Palette p) {
        int coreR = Math.max(2, cr / 3);
        for (int x = -coreR - 1; x <= coreR + 1; x++)
            for (int y = -coreR - 1; y <= coreR + 1; y++)
                for (int z = -coreR - 1; z <= coreR + 1; z++) {
                    double d = Math.sqrt((double) x * x + y * y + z * z);
                    if (d <= coreR) set(b, x, y, z, p.reactor);
                    else if (d <= coreR + 1.2) set(b, x, y, z, p.casing);
                }
        int[][] axes = {{1, 0, 0}, {-1, 0, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] a : axes)
            for (int t = coreR; t <= cr; t++)
                set(b, a[0] * t, a[1] * t, a[2] * t, p.frame);
        for (int ring = -1; ring <= 1; ring += 2) {
            int ry = (int) (cr * 0.45) * ring;
            int rr = (int) Math.sqrt(Math.max(1, cr * cr - ry * ry)) - 1;
            for (int deg = 0; deg < 360; deg += 8) {
                int x = (int) Math.round(Math.cos(Math.toRadians(deg)) * rr);
                int z = (int) Math.round(Math.sin(Math.toRadians(deg)) * rr);
                set(b, x, ry, z, p.frame);
            }
        }
        addSign(signs, coreR + 2, 0, 0, Direction.EAST, List.of("MAIN REACTOR", "", "aim for", "the core"));
    }

    /** Reactor-shaft tunnel bored out through the chamber wall (Falcon's run), toward +Z. */
    private static void reactorTunnel(Map<Long, BlockState> b, int cr, int radius, Palette p) {
        for (int t = 0; t <= radius - 3; t++) {
            for (int dx = -2; dx <= 2; dx++)
                for (int dy = -2; dy <= 2; dy++) {
                    boolean wallRing = Math.max(Math.abs(dx), Math.abs(dy)) == 2;
                    int z = t;
                    if (z < cr - 1) continue; // start at the chamber wall
                    if (wallRing) set(b, dx, dy, z, ((t + dx + dy) & 1) == 0 ? p.casing : p.conduit);
                    else b.remove(pack(dx, dy, z));
                }
        }
    }

    private static void turbolift(Map<Long, BlockState> b, int y0, int y1, Palette p) {
        for (int y = y0; y <= y1; y++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    set(b, dx, y, dz, (dx == 0 && dz == 0) ? p.conduit : p.casing);
    }

    /** The Emperor's throne room near the top: circular room, viewport, dais, turbolift door. */
    private static void throneRoom(Map<Long, BlockState> b, List<SignFeature> signs, int floorY, Palette p) {
        int r = 7, h = 5;
        int ceilY = floorY + h;
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++) {
                double d = Math.hypot(x, z);
                if (d > r + 0.5) continue;
                if (d > r - 0.5) {
                    for (int y = floorY; y <= ceilY; y++) {
                        boolean viewport = z > r * 0.35 && y > floorY && y < ceilY;
                        set(b, x, y, z, viewport ? p.glass : p.wall);
                    }
                } else {
                    set(b, x, floorY, z, p.floor);
                    set(b, x, ceilY, z, p.wall);
                }
            }
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                b.remove(pack(dx, floorY, -r + 2 + dz)); // turbolift door in the floor
        int daisZ = -r + 3;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = 0; dz <= 2; dz++)
                set(b, dx, floorY + 1, daisZ + dz, p.hullDark);
        set(b, 0, floorY + 2, daisZ, p.wall);
        set(b, -1, floorY + 2, daisZ, p.panel);
        set(b, 1, floorY + 2, daisZ, p.panel);
        set(b, 0, floorY + 3, daisZ, p.wall);
        set(b, 0, ceilY - 1, 0, p.lamp);

        addSign(signs, 0, floorY + 2, daisZ - 1, Direction.SOUTH,
                List.of("THRONE ROOM", "\"Now, young", "Skywalker,", "you will die\""));
        addSign(signs, -4, floorY + 2, r - 1, Direction.NORTH,
                List.of("DEATH STAR II", "", "shield still", "operational"));
    }

    // ------------------------------------------------------------------------------------------
    // Finishing
    // ------------------------------------------------------------------------------------------

    private static Fragment finish(Map<Long, BlockState> b, List<SignFeature> signs) {
        if (b.isEmpty()) return new Fragment(List.of(), List.of());
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
        return new Fragment(vox, sh);
    }

    // ------------------------------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------------------------------

    private static void addSign(List<SignFeature> signs, int x, int y, int z, Direction facing, List<String> lines) {
        signs.add(new SignFeature(x, y, z, facing, lines));
    }

    private static BlockState panelFor(int x, int y, int z, RandomSource rnd, Palette p) {
        boolean seam = (Math.floorMod(x, 6) == 0) || (Math.floorMod(y, 6) == 0) || (Math.floorMod(z, 6) == 0);
        if (seam) return p.hullDark;
        int r = rnd.nextInt(20);
        if (r == 0) return p.greeble;
        if (r <= 2) return p.hullDark;
        return p.hull; // mostly light plating, so it reads grey like the Death Star
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
