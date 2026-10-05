package com.vantage.worldgen;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.vantage.gen.NoiseSurfaceSampler;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.worldgen.NoiseData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The planet generator end to end: its noise settings, as Minecraft wires and runs them. */
class PlanetRouterTest {
    private static final long SEED = 8678942899319966093L;
    private static final String[] NOISES = {"continents", "warp", "ranges", "erosion", "ridges", "temperature", "vegetation"};

    static PlanetChunkGenerator generator;
    static RandomState random;
    private static final LevelHeightAccessor HEIGHT = LevelHeightAccessor.create(-64, 384);

    @BeforeAll
    static void setUp() {
        // Vanilla noises and density functions plus the planet noises from the mod's data files.
        HolderLookup.Provider lookup = new RegistrySetBuilder()
                .add(Registries.NOISE, ctx -> {
                    NoiseData.bootstrap(ctx);
                    for (String name : NOISES) {
                        ctx.register(ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath("vantage", "planet_" + name)),
                                load(name));
                    }
                })
                .add(Registries.DENSITY_FUNCTION, NoiseRouterData::bootstrap)
                .build(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        HolderLookup.Provider vanilla = VanillaRegistries.createLookup();
        MultiNoiseBiomeSource biomes = MultiNoiseBiomeSource.createFromPreset(vanilla.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        generator = new PlanetChunkGenerator(biomes, PlanetSettings.DEFAULT, lookup.lookupOrThrow(Registries.DENSITY_FUNCTION),
                lookup.lookupOrThrow(Registries.NOISE));
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        random = RandomState.create(settings, lookup.lookupOrThrow(Registries.NOISE), SEED);
    }

    private static NormalNoise.NoiseParameters load(String name) {
        String path = "data/vantage/worldgen/noise/planet_" + name + ".json";
        try (InputStream in = open(path)) {
            return NormalNoise.NoiseParameters.DIRECT_CODEC
                    .parse(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                    .getOrThrow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream open(String path) throws IOException {
        InputStream in = PlanetRouterTest.class.getResourceAsStream("/" + path);
        if (in != null) {
            return in;
        }
        // Tests may run outside the mod's module; read the source tree instead.
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path file = dir.resolve("src/main/resources").resolve(path);
            if (Files.exists(file)) {
                return Files.newInputStream(file);
            }
        }
        throw new IOException("missing " + path);
    }

    private static int height(int x, int z) {
        return generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, HEIGHT, random);
    }

    @Test
    void hasOceansContinentsAndMountains() {
        int sea = generator.getSeaLevel();
        int n = 0;
        int underwater = 0;
        int high = 0;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < 48; i++) {
            for (int j = 0; j < 48; j++) {
                int h = height(i * 1250 - 30_000, j * 1250 - 30_000);
                n++;
                if (h < sea) {
                    underwater++;
                }
                if (h > 150) {
                    high++;
                }
                max = Math.max(max, h);
            }
        }
        double water = underwater / (double) n;
        System.out.printf("[planet] terrain: %.2f under the sea, %.3f above y=150, highest %d%n", water, high / (double) n, max);
        double wanted = PlanetSettings.DEFAULT.oceanFraction();
        assertTrue(Math.abs(water - wanted) < 0.06, "share of terrain under the sea: " + water + ", wanted " + wanted);
        assertTrue(high > 0, "the planet should have high mountains");
        assertTrue(max < 320, "terrain should stay below the build limit: " + max);
    }

    @Test
    void distantTerrainSamplerAgreesWithTheGenerator() {
        NoiseSurfaceSampler sampler = new NoiseSurfaceSampler(random, generator.generatorSettings().value().noiseSettings());
        int n = 300;
        int close = 0;
        for (int i = 0; i < n; i++) {
            int x = (int) (Math.sin(i * 1.7) * 40_000);
            int z = (int) (Math.cos(i * 2.3) * 40_000);
            if (Math.abs(height(x, z) - sampler.surfaceY(x, z, 8, 1)) <= 3) {
                close++;
            }
        }
        System.out.printf("[planet] distant sampler within 3 blocks on %d/%d columns%n", close, n);
        assertTrue(close >= n * 9 / 10, "distant sampler should match the generator, got " + close + "/" + n);
    }

    @Test
    void biomesFollowLatitude() {
        Map<String, Double> equator = landBiomes(2);
        Map<String, Double> subtropics = landBiomes(22);
        Map<String, Double> temperate = landBiomes(45);
        Map<String, Double> pole = landBiomes(85);
        double jungle = share(equator, "jungle", "sparse_jungle", "bamboo_jungle");
        double dry = share(subtropics, "desert", "badlands", "eroded_badlands", "wooded_badlands", "savanna", "savanna_plateau",
                "windswept_savanna");
        double frozen = share(pole, "snowy_plains", "ice_spikes", "snowy_taiga", "snowy_slopes", "frozen_peaks", "jagged_peaks", "grove",
                "snowy_beach", "frozen_river");
        double equatorSnow = share(equator, "snowy_plains", "ice_spikes", "snowy_taiga", "snowy_slopes", "frozen_peaks", "snowy_beach");
        System.out.printf("[planet] equator %s%n[planet] subtropics %s%n[planet] mid-latitudes %s%n[planet] pole %s%n",
                top(equator), top(subtropics), top(temperate), top(pole));
        assertTrue(jungle > 0.2, "the equator should have jungle: " + jungle);
        assertTrue(equatorSnow < 0.05, "no snow at the equator: " + equatorSnow);
        assertTrue(dry > 0.4, "the subtropics should be desert, badlands and savanna: " + dry);
        assertTrue(frozen > 0.75, "the poles should be frozen: " + frozen);
    }

    /** Share of each land biome along a line of latitude (degrees north). */
    private static Map<String, Double> landBiomes(double latitude) {
        PlanetSettings p = PlanetSettings.DEFAULT;
        int z = (int) (Math.toRadians(p.spawnLatitude() - latitude) * p.radius());
        Map<String, Double> counts = new TreeMap<>();
        int n = 0;
        for (int i = 0; i < 240; i++) {
            int x = i * 937;
            int h = height(x, z);
            if (h >= generator.getSeaLevel()) {
                String name = generator.getBiomeSource().getNoiseBiome(x >> 2, h >> 2, z >> 2, random.sampler())
                        .unwrapKey().orElseThrow().location().getPath();
                counts.merge(name, 1.0, Double::sum);
                n++;
            }
        }
        assertTrue(n >= 30, "not enough land at latitude " + latitude + ": " + n);
        for (Map.Entry<String, Double> e : counts.entrySet()) {
            e.setValue(e.getValue() / n);
        }
        return counts;
    }

    private static double share(Map<String, Double> biomes, String... names) {
        double s = 0;
        for (String name : names) {
            s += biomes.getOrDefault(name, 0.0);
        }
        return s;
    }

    private static String top(Map<String, Double> biomes) {
        StringBuilder b = new StringBuilder();
        biomes.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(6)
                .forEach(e -> b.append(String.format("%s %.2f, ", e.getKey(), e.getValue())));
        return b.toString();
    }

    @Test
    void climateWrapsAroundThePlanet() {
        int c = (int) Math.round(PlanetSettings.DEFAULT.circumference());
        int same = 0;
        int n = 40;
        for (int i = 0; i < n; i++) {
            int x = i * 3111 - 50_000;
            int z = i * 977 - 15_000;
            Holder<Biome> a = generator.getBiomeSource().getNoiseBiome(x >> 2, 16, z >> 2, random.sampler());
            Holder<Biome> b = generator.getBiomeSource().getNoiseBiome((x + c) >> 2, 16, z >> 2, random.sampler());
            if (a.equals(b)) {
                same++;
            }
        }
        // Rounding the circumference to whole quarts can flip biomes right on a border.
        assertTrue(same >= n - 2, "biomes should repeat after one trip around the planet: " + same + "/" + n);
        assertEquals(PlanetSettings.DEFAULT, PlanetSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow());
    }
}
