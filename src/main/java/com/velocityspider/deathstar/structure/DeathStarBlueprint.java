package com.velocityspider.deathstar.structure;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Deterministic procedural generator for the ruined Death Star shell.
 *
 * <p>The Death Star is a hollow sphere with a single-block panelled skin. On top of that base
 * shape we carve the three features that make the silhouette recognisable:
 * <ul>
 *     <li>the equatorial trench (the dark groove running all the way around),</li>
 *     <li>the concave superlaser dish (the "eye") on the upper hemisphere, and</li>
 *     <li>battle damage: impact craters that punch holes in the skin and expose the darker
 *         internal structure around their rims, so the thing reads as <em>ruins</em>.</li>
 * </ul>
 *
 * <p>Generation is a pure function of {@code (seed, radius, thickness)} so the client can rebuild
 * the exact same voxel set the server spawned without shipping ~50k block positions over the wire.
 * The result is a flat list of {@link Voxel}s in entity-local coordinates centred on the origin.
 */
public final class DeathStarBlueprint {

    /** A single block of the shell, in entity-local integer coordinates. */
    public record Voxel(int x, int y, int z, BlockState state) {}

    /** Palette of solid, fully-opaque blocks so the whole model bakes into one render layer. */
    private enum Palette {
        HULL(Blocks.IRON_BLOCK.defaultBlockState()),
        PANEL_LIGHT(Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState()),
        PANEL_DARK(Blocks.GRAY_CONCRETE.defaultBlockState()),
        GREEBLE(Blocks.POLISHED_ANDESITE.defaultBlockState()),
        TRENCH(Blocks.DEEPSLATE_TILES.defaultBlockState()),
        DISH(Blocks.BLACK_CONCRETE.defaultBlockState()),
        DISH_FOCUS(Blocks.SEA_LANTERN.defaultBlockState()),
        EXPOSED(Blocks.NETHERITE_BLOCK.defaultBlockState()),
        BURNT(Blocks.BLACKSTONE.defaultBlockState());

        final BlockState state;

        Palette(BlockState state) {
            this.state = state;
        }
    }

    private DeathStarBlueprint() {}

    /**
     * Builds the voxel list for a Death Star of the given radius.
     *
     * @param seed      world-unique seed; identical seeds produce identical ruins
     * @param radius    outer radius in blocks (diameter is roughly {@code 2*radius})
     * @param thickness skin thickness in blocks
     */
    public static List<Voxel> build(long seed, int radius, int thickness) {
        RandomSource rand = RandomSource.create(seed);

        // Superlaser dish: an off-centre bowl on the upper hemisphere. Axis is fixed relative to
        // the model so the eye always points the same way regardless of seed.
        final double laserX = 0.0, laserY = 0.62, laserZ = 0.78; // already ~unit length
        final double dishCos = Math.cos(Math.toRadians(24.0)); // half-angle of the dish cone
        final int dishDepth = Math.max(3, radius / 6);

        // Battle-damage impact points, scattered over the sphere.
        int craterCount = 10 + rand.nextInt(8);
        double[][] craters = new double[craterCount][4]; // nx, ny, nz, cosAngularRadius
        for (int i = 0; i < craterCount; i++) {
            double[] dir = randomUnit(rand);
            craters[i][0] = dir[0];
            craters[i][1] = dir[1];
            craters[i][2] = dir[2];
            double angRad = Math.toRadians(6.0 + rand.nextDouble() * 12.0);
            craters[i][3] = Math.cos(angRad);
        }

        List<Voxel> voxels = new ArrayList<>(60_000);
        double outer = radius + 0.5;
        double inner = radius - thickness + 0.5;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
                    if (dist > outer || dist < inner - dishDepth - 1) {
                        continue; // far outside the skin, or deep inside the hollow interior
                    }

                    // Normalised surface direction for this cell (avoid divide-by-zero at centre).
                    double nx = dist > 0.0001 ? x / dist : 0;
                    double ny = dist > 0.0001 ? y / dist : 0;
                    double nz = dist > 0.0001 ? z / dist : 0;

                    boolean inShell = dist <= outer && dist >= inner;

                    // --- Superlaser dish: recess the skin inside the cone into a concave bowl. ---
                    double dishDot = nx * laserX + ny * laserY + nz * laserZ;
                    if (dishDot >= dishCos) {
                        // How far into the cone (0 at rim, 1 at centre of the eye).
                        double t = (dishDot - dishCos) / (1.0 - dishCos);
                        double bowlRadius = outer - dishDepth * smooth(t);
                        if (Math.abs(dist - bowlRadius) <= thickness * 0.6 + 0.5) {
                            Palette p = t > 0.9 ? Palette.DISH_FOCUS : Palette.DISH;
                            voxels.add(new Voxel(x, y, z, p.state));
                        }
                        continue; // the normal shell is removed where the dish sits
                    }

                    if (!inShell) {
                        continue;
                    }

                    // --- Battle damage: blow holes, expose inner structure around the rim. ---
                    double worstCrater = -1.0;
                    for (double[] c : craters) {
                        double dot = nx * c[0] + ny * c[1] + nz * c[2];
                        if (dot > c[3]) {
                            // Inside this crater. Map to 0 (rim) .. 1 (dead centre).
                            double depth = (dot - c[3]) / (1.0 - c[3]);
                            worstCrater = Math.max(worstCrater, depth);
                        }
                    }
                    if (worstCrater >= 0.0) {
                        if (worstCrater > 0.35) {
                            continue; // hole punched clean through the skin
                        }
                        // Rim: charred and torn, exposing the internal frame.
                        Palette p = (rand.nextInt(3) == 0) ? Palette.EXPOSED : Palette.BURNT;
                        voxels.add(new Voxel(x, y, z, p.state));
                        continue;
                    }

                    // --- Equatorial trench: a recessed dark band around the middle. ---
                    if (Math.abs(y) <= 2) {
                        // Only keep the inner layer here so the groove reads as recessed.
                        if (dist <= outer - 1) {
                            voxels.add(new Voxel(x, y, z, Palette.TRENCH.state));
                        }
                        continue;
                    }

                    // --- Ordinary panelled hull. ---
                    voxels.add(new Voxel(x, y, z, panelFor(x, y, z, rand)));
                }
            }
        }
        return voxels;
    }

    /** Chooses a hull block so the surface reads as riveted panels rather than a flat sphere. */
    private static BlockState panelFor(int x, int y, int z, RandomSource rand) {
        // Panel seams every few blocks give the skin its plated look.
        boolean seam = (Math.floorMod(x, 6) == 0) || (Math.floorMod(y, 6) == 0) || (Math.floorMod(z, 6) == 0);
        if (seam) {
            return Palette.PANEL_DARK.state;
        }
        int r = rand.nextInt(16);
        if (r == 0) {
            return Palette.GREEBLE.state;
        }
        if (r <= 4) {
            return Palette.PANEL_LIGHT.state;
        }
        return Palette.HULL.state;
    }

    /** Smoothstep, used to give the dish a rounded profile instead of a cone. */
    private static double smooth(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double[] randomUnit(RandomSource rand) {
        // Marsaglia's method for a uniform point on the unit sphere.
        double a, b, s;
        do {
            a = rand.nextDouble() * 2.0 - 1.0;
            b = rand.nextDouble() * 2.0 - 1.0;
            s = a * a + b * b;
        } while (s >= 1.0 || s == 0.0);
        double f = 2.0 * Math.sqrt(1.0 - s);
        return new double[] {a * f, b * f, 1.0 - 2.0 * s};
    }
}
