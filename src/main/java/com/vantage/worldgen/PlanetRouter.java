package com.vantage.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Builds a planet's noise settings: Minecraft's overworld terrain recipe (caves, aquifers, ore
 * veins, terrain splines, surface rules) driven by the planet's own climate instead of the vanilla
 * noises. Everything that reads climate — terrain height, biomes, structures — therefore follows
 * the planet's continents, mountain ranges and latitude bands.
 */
public final class PlanetRouter {
    private static final DensityFunction BLENDING_FACTOR = DensityFunctions.constant(10.0);
    private static final DensityFunction BLENDING_JAGGEDNESS = DensityFunctions.zero();
    // Lowest and highest y of ore veins (copper 0..50, iron -60..-8).
    private static final int VEIN_MIN_Y = -60;
    private static final int VEIN_MAX_Y = 50;

    private PlanetRouter() {
    }

    public static NoiseGeneratorSettings settings(PlanetSettings planet, HolderGetter<DensityFunction> functions,
                                                  HolderGetter<NormalNoise.NoiseParameters> noises) {
        return new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2),
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                router(planet, functions, noises),
                SurfaceRuleData.overworld(),
                new OverworldBiomeBuilder().spawnTarget(),
                63,
                false,
                true,
                true,
                false);
    }

    static NoiseRouter router(PlanetSettings planet, HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises) {
        PlanetNoises pn = PlanetNoises.of(noises);
        DensityFunction continents = DensityFunctions.flatCache(new PlanetClimate(PlanetClimate.Kind.CONTINENTS, planet, pn));
        DensityFunction erosion = DensityFunctions.flatCache(new PlanetClimate(PlanetClimate.Kind.EROSION, planet, pn));
        DensityFunction ridges = DensityFunctions.flatCache(new PlanetClimate(PlanetClimate.Kind.RIDGES, planet, pn));
        DensityFunction temperature = DensityFunctions.flatCache(new PlanetClimate(PlanetClimate.Kind.TEMPERATURE, planet, pn));
        DensityFunction vegetation = DensityFunctions.flatCache(new PlanetClimate(PlanetClimate.Kind.VEGETATION, planet, pn));
        DensityFunction ridgesFolded = peaksAndValleys(ridges);

        // Terrain shape: vanilla's splines, fed with the planet's climate.
        DensityFunctions.Spline.Coordinate c = new DensityFunctions.Spline.Coordinate(Holder.direct(continents));
        DensityFunctions.Spline.Coordinate e = new DensityFunctions.Spline.Coordinate(Holder.direct(erosion));
        DensityFunctions.Spline.Coordinate w = new DensityFunctions.Spline.Coordinate(Holder.direct(ridges));
        DensityFunctions.Spline.Coordinate pv = new DensityFunctions.Spline.Coordinate(Holder.direct(ridgesFolded));
        DensityFunction offset = splineWithBlending(
                DensityFunctions.add(DensityFunctions.constant(-0.50375F), DensityFunctions.spline(TerrainProvider.overworldOffset(c, e, pv, false))),
                DensityFunctions.blendOffset());
        DensityFunction factor = splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldFactor(c, e, w, pv, false)), BLENDING_FACTOR);
        DensityFunction depth = DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5), offset);
        DensityFunction jaggedness = splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldJaggedness(c, e, w, pv, false)),
                BLENDING_JAGGEDNESS);
        DensityFunction jaggedNoise = DensityFunctions.noise(noises.getOrThrow(Noises.JAGGED), 1500.0, 0.0);
        DensityFunction slopedCheese = DensityFunctions.add(
                noiseGradientDensity(factor, DensityFunctions.add(depth, DensityFunctions.mul(jaggedness, jaggedNoise.halfNegative()))),
                function(functions, "overworld/base_3d_noise"));

        // From here on: vanilla's overworld router, unchanged.
        DensityFunction entrances = function(functions, "overworld/caves/entrances");
        DensityFunction surface = DensityFunctions.min(slopedCheese, DensityFunctions.mul(DensityFunctions.constant(5.0), entrances));
        DensityFunction caves = DensityFunctions.rangeChoice(slopedCheese, -1000000.0, 1.5625, surface, underground(functions, noises, slopedCheese));
        DensityFunction finalDensity = DensityFunctions.min(postProcess(slideOverworld(caves)), function(functions, "overworld/caves/noodle"));
        DensityFunction initialDensity = slideOverworld(DensityFunctions.add(
                noiseGradientDensity(DensityFunctions.cache2d(factor), depth), DensityFunctions.constant(-0.703125)).clamp(-64.0, 64.0));

        DensityFunction y = function(functions, "y");
        DensityFunction veininess = yLimitedInterpolatable(y, DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEININESS), 1.5, 1.5), 0);
        DensityFunction veinA = yLimitedInterpolatable(y, DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEIN_A), 4.0, 4.0), 0).abs();
        DensityFunction veinB = yLimitedInterpolatable(y, DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEIN_B), 4.0, 4.0), 0).abs();
        DensityFunction veinRidged = DensityFunctions.add(DensityFunctions.constant(-0.08F), DensityFunctions.max(veinA, veinB));

        return new NoiseRouter(
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_BARRIER), 0.5),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_LAVA)),
                temperature,
                vegetation,
                continents,
                erosion,
                depth,
                ridges,
                initialDensity,
                finalDensity,
                veininess,
                veinRidged,
                DensityFunctions.noise(noises.getOrThrow(Noises.ORE_GAP)));
    }

    private static DensityFunction function(HolderGetter<DensityFunction> functions, String path) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(
                ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(path))));
    }

    private static DensityFunction peaksAndValleys(DensityFunction weirdness) {
        return DensityFunctions.mul(
                DensityFunctions.add(DensityFunctions.add(weirdness.abs(), DensityFunctions.constant(-0.6666666666666666)).abs(),
                        DensityFunctions.constant(-0.3333333333333333)),
                DensityFunctions.constant(-3.0));
    }

    private static DensityFunction splineWithBlending(DensityFunction spline, DensityFunction blended) {
        return DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.lerp(DensityFunctions.blendAlpha(), blended, spline)));
    }

    private static DensityFunction noiseGradientDensity(DensityFunction factor, DensityFunction depth) {
        return DensityFunctions.mul(DensityFunctions.constant(4.0), DensityFunctions.mul(depth, factor).quarterNegative());
    }

    private static DensityFunction postProcess(DensityFunction density) {
        return DensityFunctions.mul(DensityFunctions.interpolated(DensityFunctions.blendDensity(density)), DensityFunctions.constant(0.64)).squeeze();
    }

    private static DensityFunction slideOverworld(DensityFunction density) {
        DensityFunction top = DensityFunctions.lerp(DensityFunctions.yClampedGradient(-64 + 384 - 80, -64 + 384 - 64, 1.0, 0.0), -0.078125, density);
        return DensityFunctions.lerp(DensityFunctions.yClampedGradient(-64, -64 + 24, 0.0, 1.0), 0.1171875, top);
    }

    private static DensityFunction yLimitedInterpolatable(DensityFunction y, DensityFunction inRange, int outOfRange) {
        return DensityFunctions.interpolated(DensityFunctions.rangeChoice(y, VEIN_MIN_Y, VEIN_MAX_Y + 1, inRange,
                DensityFunctions.constant(outOfRange)));
    }

    /** Cheese caves, spaghetti and pillars, as vanilla builds them. */
    private static DensityFunction underground(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises,
                                               DensityFunction slopedCheese) {
        DensityFunction spaghetti = function(functions, "overworld/caves/spaghetti_2d");
        DensityFunction roughness = function(functions, "overworld/caves/spaghetti_roughness_function");
        DensityFunction layer = DensityFunctions.mul(DensityFunctions.constant(4.0),
                DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_LAYER), 8.0).square());
        DensityFunction cheese = DensityFunctions.add(
                DensityFunctions.add(DensityFunctions.constant(0.27), DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666))
                        .clamp(-1.0, 1.0),
                DensityFunctions.add(DensityFunctions.constant(1.5), DensityFunctions.mul(DensityFunctions.constant(-0.64), slopedCheese))
                        .clamp(0.0, 0.5));
        DensityFunction open = DensityFunctions.min(
                DensityFunctions.min(DensityFunctions.add(layer, cheese), function(functions, "overworld/caves/entrances")),
                DensityFunctions.add(spaghetti, roughness));
        DensityFunction pillars = function(functions, "overworld/caves/pillars");
        DensityFunction pillarsIfAny = DensityFunctions.rangeChoice(pillars, -1000000.0, 0.03, DensityFunctions.constant(-1000000.0), pillars);
        return DensityFunctions.max(open, pillarsIfAny);
    }
}
