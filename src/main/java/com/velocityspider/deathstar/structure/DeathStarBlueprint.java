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
 * <i>The Rise of Skywalker</i>: a scattered field of separate broken pieces — a big curved hull
 * shard, the torn superlaser-dish section, medium shards and twisted debris.
 *
 * <p>The big "hero" shard carries a cutaway of the <b>second Death Star</b>'s interior, laid out to
 * match <i>Return of the Jedi</i>: the Emperor's throne room at the top, a turbolift shaft dropping
 * down to the enormous main reactor chamber, the exposed reactor core suspended on struts and ringed
 * by catwalks, the reactor-shaft tunnel the Falcon flew through, and the skeletal under-construction
 * superstructure on the unfinished part of the hull.
 *
 * <p>{@link #buildWreckField} returns independent {@link Fragment}s; the assembler assembles each
 * into its own Sable physics body. Everything is a pure function of the seed (server-side only).
 */
public final class DeathStarBlueprint {

    public record Voxel(int x, int y, int z, BlockState state) {}

    /** A sign to place after the blocks are down (its text is applied via its block entity). */
    public record SignFeature(int x, int y, int z, Direction facing, List<String> lines) {}

    /** One broken piece, centred on its own origin; {@code offset*} says where to drop that origin. */
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
        final BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
        final BlockState glass = Blocks.GRAY_STAINED_GLASS.defaultBlockState();
    }

    private static final double[] DISH = unit(0.15, 0.72, 0.68);      // superlaser "eye" direction
    private static final double[] HERO = unit(-0.2, 0.15, -0.97);     // big shard's outward normal
    private static final double[] SCAFFOLD = unit(0.1, 0.6, -0.79);   // unfinished sector on the hero shard

    private DeathStarBlueprint() {}

    public static List<Fragment> buildWreckField(long seed, int radius, int thickness) {
        RandomSource rnd = RandomSource.create(seed);
        Palette p = new Palette();
        List<Fragment> frags = new ArrayList<>();

        // 1) Hero hull shard with the full Death Star II interior cutaway.
        {
            Map<Long, BlockState> b = new LinkedHashMap<>();
            List<SignFeature> signs = new ArrayList<>();
            shardInto(b, radius, thickness, HERO, 58.0, true, SCAFFOLD, p, rnd);
            buildInteriorII(b, signs, radius, HERO, p, rnd);
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
            shardInto(b, radius, thickness, randomUnit(rnd), 22.0 + rnd.nextDouble() * 12.0, rnd.nextBoolean(), null, p, rnd);
            frags.add(finish("hull_shard_" + i, b, new ArrayList<>(), offset(2 + i, radius, rnd)));
        }

        // 4) Small twisted debris chunks.
        int debris = 4;
        for (int i = 0; i < debris; i++) {
            Map<Long, BlockState> b = new LinkedHashMap<>();
            debrisInto(b, 3 + rnd.nextInt(3), p, rnd);
            frags.add(finish("debris_" + i, b, new ArrayList<>(), offset(2 + medium + i, radius, rnd)));
        }

        return frags;
    }

    // ------------------------------------------------------------------------------------------
    // Hull pieces
    // ------------------------------------------------------------------------------------------

    /** A curved spherical-cap slice of the two-layer hull with a ragged torn edge and optional scaffolding. */
    private static void shardInto(Map<Long, BlockState> b, int radius, int thickness, double[] dir,
                                  double halfAngleDeg, boolean allowTrench, double[] scaffoldDir,
                                  Palette p, RandomSource rnd) {
        double R = radius;
        double cosA = Math.cos(Math.toRadians(halfAngleDeg));
        double scaffoldCos = Math.cos(Math.toRadians(22.0));
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

                    double t = (dot - cosA) / (1.0 - cosA);
                    double jag = 0.06 + 0.16 * rnd.nextDouble();
                    if (t < jag) continue;
                    boolean rim = t < jag + 0.12;

                    // Unfinished superstructure: skeletal ribs instead of a skin.
                    if (scaffoldDir != null && outer
                            && nx * scaffoldDir[0] + ny * scaffoldDir[1] + nz * scaffoldDir[2] > scaffoldCos) {
                        boolean rib = Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0 || Math.floorMod(y, 4) == 0;
                        if (rib) set(b, x, y, z, p.frame);
                        continue;
                    }

                    if (outer) {
                        if (rim) set(b, x, y, z, rnd.nextInt(3) == 0 ? p.frame : p.scorched);
                        else if (allowTrench && Math.abs(y) <= 2 && dist <= R - 1) set(b, x, y, z, p.hullDark);
                        else set(b, x, y, z, panelFor(x, y, z, rnd, p));
                    } else {
                        set(b, x, y, z, rim ? p.frame : (rnd.nextInt(5) == 0 ? p.wall : p.frame));
                    }
                }
            }
        }
    }

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
                    if (te < 0.06 + 0.12 * rnd.nextDouble()) continue;

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

    private static void debrisInto(Map<Long, BlockState> b, int size, Palette p, RandomSource rnd) {
        BlockState[] mix = {p.frame, p.scorched, p.hull, p.hullDark, p.greeble};
        for (int x = -size; x <= size; x++)
            for (int y = -size; y <= size; y++)
                for (int z = -size; z <= size; z++) {
                    if (Math.sqrt((double) x * x + y * y + z * z) > size + 0.3) continue;
                    if (rnd.nextDouble() < 0.35) continue;
                    set(b, x, y, z, mix[rnd.nextInt(mix.length)]);
                }
    }

    // ------------------------------------------------------------------------------------------
    // Death Star II interior cutaway
    // ------------------------------------------------------------------------------------------

    private static void buildInteriorII(Map<Long, BlockState> b, List<SignFeature> signs, int radius,
                                        double[] dir, Palette p, RandomSource rnd) {
        int[] base = at(dir, (int) (radius * 0.40)); // reactor chamber centre, on the concave side
        int bx = base[0], by = base[1], bz = base[2];
        int chamberR = Math.max(6, radius / 4);

        reactorChamber(b, bx, by, bz, chamberR, dir, p);
        reactorCoreAssembly(b, signs, bx, by, bz, chamberR, p);
        reactorTunnel(b, bx, by, bz, chamberR, p);            // the shaft the Falcon flew down

        int shaftTop = by + chamberR + Math.max(9, radius / 3);
        turbolift(b, bx, by + chamberR - 1, bz, shaftTop, p); // vertical shaft up to the throne

        throneRoom(b, signs, bx, shaftTop, bz, p);

        // Loose Battle-of-Endor quips further out in the wreck.
        addSign(signs, bx + chamberR + 2, by + 2, bz, Direction.EAST,
                List.of("", "IT'S A TRAP!", "- Ackbar", ""));
        addSign(signs, bx - 3, by - chamberR - 1, bz + 3, Direction.NORTH,
                List.of("MANY BOTHANS", "died to bring", "us this", "sign. 1138"));
    }

    /** A hollow spherical reactor chamber, cut open on the side facing the wreck's exposed face. */
    private static void reactorChamber(Map<Long, BlockState> b, int cx, int cy, int cz, int cr,
                                       double[] dir, Palette p) {
        for (int x = -cr; x <= cr; x++)
            for (int y = -cr; y <= cr; y++)
                for (int z = -cr; z <= cr; z++) {
                    double d = Math.sqrt((double) x * x + y * y + z * z);
                    if (Math.abs(d - cr) > 0.8) continue;
                    double nd = (x * dir[0] + y * dir[1] + z * dir[2]) / cr;
                    if (nd < -0.15) continue; // open the viewer-facing side into a cutaway
                    set(b, cx + x, cy + y, cz + z, ((x + y + z) & 3) == 0 ? p.frame : p.casing);
                }
    }

    /** The suspended, glowing reactor core with support struts and two catwalk rings. */
    private static void reactorCoreAssembly(Map<Long, BlockState> b, List<SignFeature> signs,
                                            int cx, int cy, int cz, int cr, Palette p) {
        int coreR = Math.max(2, cr / 3);
        for (int x = -coreR - 1; x <= coreR + 1; x++)
            for (int y = -coreR - 1; y <= coreR + 1; y++)
                for (int z = -coreR - 1; z <= coreR + 1; z++) {
                    double d = Math.sqrt((double) x * x + y * y + z * z);
                    if (d <= coreR) set(b, cx + x, cy + y, cz + z, p.reactor);
                    else if (d <= coreR + 1.2) set(b, cx + x, cy + y, cz + z, p.casing);
                }
        // Six struts from the core out to the chamber shell.
        int[][] axes = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] a : axes)
            for (int t = coreR; t <= cr; t++)
                set(b, cx + a[0] * t, cy + a[1] * t, cz + a[2] * t, p.frame);
        // Two catwalk rings around the inside of the chamber.
        for (int ring = -1; ring <= 1; ring += 2) {
            int ry = (int) (cr * 0.45) * ring;
            int rr = (int) Math.sqrt(Math.max(1, cr * cr - ry * ry)) - 1;
            for (int deg = 0; deg < 360; deg += 8) {
                int x = (int) Math.round(Math.cos(Math.toRadians(deg)) * rr);
                int z = (int) Math.round(Math.sin(Math.toRadians(deg)) * rr);
                set(b, cx + x, cy + ry, cz + z, p.frame);
            }
        }
        addSign(signs, cx + coreR + 2, cy, cz, Direction.EAST,
                List.of("MAIN REACTOR", "", "aim for", "the core"));
    }

    /** The reactor-shaft tunnel bored out through the chamber wall (Falcon's run). */
    private static void reactorTunnel(Map<Long, BlockState> b, int cx, int cy, int cz, int cr, Palette p) {
        for (int t = 0; t <= cr + 6; t++) {
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    boolean wallRing = Math.max(Math.abs(dy), Math.abs(dz)) == 2;
                    int x = cx - t; // bore out along -X
                    if (wallRing) set(b, x, cy + dy, cz + dz, ((t + dy + dz) & 1) == 0 ? p.casing : p.conduit);
                    else b.remove(pack(x, cy + dy, cz + dz)); // hollow bore
                }
        }
    }

    /** Vertical 3x3 turbolift shaft (casing walls, glowing conduit core). */
    private static void turbolift(Map<Long, BlockState> b, int cx, int y0, int cz, int y1, Palette p) {
        for (int y = y0; y <= y1; y++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) set(b, cx, y, cz, p.conduit);
                    else set(b, cx + dx, y, cz + dz, p.casing);
                }
    }

    /** The Emperor's throne room: circular room, panoramic viewport, throne dais, turbolift door. */
    private static void throneRoom(Map<Long, BlockState> b, List<SignFeature> signs, int cx, int cy, int cz, Palette p) {
        int r = 7, h = 5;
        int floorY = cy, ceilY = cy + h;
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++) {
                double d = Math.hypot(x, z);
                if (d > r + 0.5) continue;
                if (d > r - 0.5) {
                    // Wall ring: the front arc (+Z) is the big circular viewport.
                    for (int y = floorY; y <= ceilY; y++) {
                        boolean viewport = z > r * 0.35 && y > floorY && y < ceilY;
                        set(b, cx + x, y, cz + z, viewport ? p.glass : p.wall);
                    }
                } else {
                    set(b, cx + x, floorY, cz + z, p.floor);   // deck
                    set(b, cx + x, ceilY, cz + z, p.wall);     // ceiling
                }
            }
        // Turbolift door: hole in the floor at the back.
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                b.remove(pack(cx + dx, floorY, cz - r + 2 + dz));
        // Throne dais at the back, facing the viewport.
        int daisZ = cz - r + 3;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = 0; dz <= 2; dz++)
                set(b, cx + dx, floorY + 1, daisZ + dz, p.hullDark);
        set(b, cx, floorY + 2, daisZ, p.wall);                 // throne back
        set(b, cx - 1, floorY + 2, daisZ, p.panel);            // arm consoles
        set(b, cx + 1, floorY + 2, daisZ, p.panel);
        set(b, cx, floorY + 3, daisZ, p.wall);
        set(b, cx, ceilY - 1, cz, p.lamp);

        addSign(signs, cx, floorY + 2, daisZ - 1, Direction.SOUTH,
                List.of("THRONE ROOM", "\"Now, young", "Skywalker,", "you will die\""));
        addSign(signs, cx - 4, floorY + 2, cz + r - 1, Direction.NORTH,
                List.of("DEATH STAR II", "", "shield still", "operational"));
    }

    // ------------------------------------------------------------------------------------------
    // Fragment finishing + offsets
    // ------------------------------------------------------------------------------------------

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

    private static int[] offset(int i, int radius, RandomSource rnd) {
        if (i == 0) return new int[] {0, 6, 0};
        double sp = radius * 1.5;
        double angle = i * 2.399963;
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
