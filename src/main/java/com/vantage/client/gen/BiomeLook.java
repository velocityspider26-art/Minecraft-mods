package com.vantage.client.gen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomBooleanFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the surface of a biome looks like from far away, for generated terrain: the top block,
 * what lies under it, the sea floor, and tree cover.
 *
 * <p>Ground blocks come from biome tags (vanilla and NeoForge's common tags, which mods use too).
 * Trees come from the biome's own vegetation features: their leaves block, their average height,
 * and how many are placed per chunk, so modded biomes get their own trees.
 */
public record BiomeLook(BlockState top, BlockState under, BlockState seabed, @Nullable BlockState leaves,
                        float canopy, int treeHeight) {
    /** Leaf area of one tree crown seen from above, in blocks. */
    private static final double CROWN_AREA = 22.0;

    public static BiomeLook resolve(Holder<Biome> biome, BlockState defaultBlock) {
        BlockState top;
        BlockState under;
        BlockState seabed;
        if (!biome.is(BiomeTags.IS_OVERWORLD) && !biome.is(Tags.Biomes.IS_OVERWORLD)) {
            // Other worlds (the End, modded planets): their base rock is the best guess.
            top = defaultBlock;
            under = defaultBlock;
            seabed = defaultBlock;
        } else if (is(biome, Tags.Biomes.IS_BADLANDS, BiomeTags.IS_BADLANDS)) {
            top = Blocks.RED_SAND.defaultBlockState();
            under = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            seabed = Blocks.RED_SAND.defaultBlockState();
        } else if (is(biome, Tags.Biomes.IS_DESERT, null) || is(biome, Tags.Biomes.IS_BEACH, BiomeTags.IS_BEACH)) {
            top = Blocks.SAND.defaultBlockState();
            under = Blocks.SANDSTONE.defaultBlockState();
            seabed = Blocks.SAND.defaultBlockState();
        } else if (is(biome, Tags.Biomes.IS_MUSHROOM, null)) {
            top = Blocks.MYCELIUM.defaultBlockState();
            under = Blocks.DIRT.defaultBlockState();
            seabed = Blocks.DIRT.defaultBlockState();
        } else if (is(biome, Tags.Biomes.IS_ICY, null)) {
            top = Blocks.SNOW_BLOCK.defaultBlockState();
            under = Blocks.PACKED_ICE.defaultBlockState();
            seabed = Blocks.GRAVEL.defaultBlockState();
        } else if (is(biome, Tags.Biomes.IS_STONY_SHORES, null) || is(biome, Tags.Biomes.IS_MOUNTAIN_PEAK, null)) {
            top = Blocks.STONE.defaultBlockState();
            under = Blocks.STONE.defaultBlockState();
            seabed = Blocks.GRAVEL.defaultBlockState();
        } else if (is(biome, Tags.Biomes.IS_OCEAN, BiomeTags.IS_OCEAN)) {
            boolean warm = biome.value().getBaseTemperature() >= 0.5f;
            top = Blocks.SAND.defaultBlockState();
            under = Blocks.SAND.defaultBlockState();
            seabed = warm ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
        } else if (is(biome, Tags.Biomes.IS_SWAMP, null)) {
            top = Blocks.GRASS_BLOCK.defaultBlockState();
            under = Blocks.DIRT.defaultBlockState();
            seabed = Blocks.MUD.defaultBlockState();
        } else if (is(biome, Tags.Biomes.IS_WASTELAND, null) || is(biome, Tags.Biomes.IS_DEAD, null)) {
            top = Blocks.COARSE_DIRT.defaultBlockState();
            under = Blocks.DIRT.defaultBlockState();
            seabed = Blocks.GRAVEL.defaultBlockState();
        } else {
            top = Blocks.GRASS_BLOCK.defaultBlockState();
            under = Blocks.DIRT.defaultBlockState();
            seabed = is(biome, Tags.Biomes.IS_RIVER, BiomeTags.IS_RIVER) ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
        }

        Trees trees = new Trees();
        try {
            List<HolderSet<PlacedFeature>> steps = biome.value().getGenerationSettings().features();
            int vegetal = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
            if (vegetal < steps.size()) {
                for (Holder<PlacedFeature> f : steps.get(vegetal)) {
                    trees.collect(f, 1.0, 0);
                }
            }
        } catch (RuntimeException ignored) {
            // Unusual modded features: no trees then.
        }
        BlockState leaves = trees.leaves();
        float canopy = leaves == null ? 0f : (float) Math.min(1.0, trees.perChunk * CROWN_AREA / 256.0);
        int height = trees.height();
        return new BiomeLook(top, under, seabed, canopy < 0.02f ? null : leaves, canopy, height);
    }

    private static boolean is(Holder<Biome> biome, TagKey<Biome> common, @Nullable TagKey<Biome> vanilla) {
        return biome.is(common) || (vanilla != null && biome.is(vanilla));
    }

    /** Tree statistics gathered from a biome's features. */
    private static final class Trees {
        // Per instance: Minecraft's random sources refuse to be shared between threads.
        private final RandomSource random = RandomSource.create(42L);
        double perChunk;
        private final Map<Block, Double> leafWeights = new HashMap<>();
        private double heightSum;
        private double heightWeight;

        void collect(Holder<PlacedFeature> placed, double weight, int depth) {
            if (depth > 6 || weight <= 0) {
                return;
            }
            PlacedFeature p = placed.value();
            this.collectConfigured(p.feature().value(), weight * expectedCount(p.placement()), depth);
        }

        private void collectConfigured(ConfiguredFeature<?, ?> feature, double count, int depth) {
            FeatureConfiguration config = feature.config();
            if (config instanceof TreeConfiguration tree) {
                this.perChunk += count;
                BlockState leaves = tree.foliageProvider.getState(this.random, BlockPos.ZERO);
                int samples = 8;
                double h = 0;
                for (int i = 0; i < samples; i++) {
                    h += tree.trunkPlacer.getTreeHeight(this.random);
                }
                h /= samples;
                // Tall crowns cover the low ones when seen from above (jungle trees over bushes).
                this.leafWeights.merge(leaves.getBlock(), count * h * h, Double::sum);
                this.heightSum += h * count;
                this.heightWeight += count;
            } else if (config instanceof RandomFeatureConfiguration random) {
                double rest = 1.0;
                for (WeightedPlacedFeature w : random.features) {
                    this.collect(w.feature, count * rest * w.chance, depth + 1);
                    rest *= 1.0 - w.chance;
                }
                this.collect(random.defaultFeature, count * rest, depth + 1);
            } else if (config instanceof SimpleRandomFeatureConfiguration simple) {
                int n = simple.features.size();
                for (Holder<PlacedFeature> f : simple.features) {
                    this.collect(f, count / n, depth + 1);
                }
            } else if (config instanceof RandomBooleanFeatureConfiguration bool) {
                this.collect(bool.featureTrue, count / 2, depth + 1);
                this.collect(bool.featureFalse, count / 2, depth + 1);
            }
        }

        @Nullable BlockState leaves() {
            Block best = null;
            double bestWeight = 0;
            for (Map.Entry<Block, Double> e : this.leafWeights.entrySet()) {
                if (e.getValue() > bestWeight) {
                    bestWeight = e.getValue();
                    best = e.getKey();
                }
            }
            return best == null ? null : best.defaultBlockState();
        }

        int height() {
            return this.heightWeight <= 0 ? 6 : (int) Math.round(this.heightSum / this.heightWeight);
        }
    }

    /** Average number of placements a modifier list yields per chunk (filters assumed to pass). */
    static double expectedCount(List<PlacementModifier> modifiers) {
        double count = 1.0;
        for (PlacementModifier m : modifiers) {
            ResourceLocation type = BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.getKey(m.type());
            if (type == null) {
                continue;
            }
            String path = type.getPath();
            if (!path.equals("count") && !path.equals("count_on_every_layer") && !path.equals("rarity_filter")
                    && !path.equals("noise_threshold_count") && !path.equals("noise_based_count")) {
                continue;
            }
            // The fields are private; the codec is the public way to read them.
            JsonObject json = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, m).result()
                    .filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElse(null);
            if (json == null) {
                continue;
            }
            switch (path) {
                case "count", "count_on_every_layer" -> count *= intMean(json.get("count"));
                case "rarity_filter" -> count /= Math.max(1, json.get("chance").getAsInt());
                case "noise_threshold_count" -> count *= (json.get("below_noise").getAsInt() + json.get("above_noise").getAsInt()) / 2.0;
                case "noise_based_count" -> count *= json.get("noise_to_count_ratio").getAsInt() / 2.0;
                default -> {
                }
            }
        }
        return count;
    }

    /** Mean of an encoded int provider. */
    static double intMean(@Nullable JsonElement e) {
        if (e == null) {
            return 1.0;
        }
        if (e.isJsonPrimitive()) {
            return e.getAsDouble();
        }
        if (!e.isJsonObject()) {
            return 1.0;
        }
        JsonObject o = e.getAsJsonObject();
        String type = o.has("type") ? o.get("type").getAsString() : "";
        switch (type) {
            case "minecraft:constant":
                return o.get("value").getAsDouble();
            case "minecraft:uniform":
                return (o.get("min_inclusive").getAsDouble() + o.get("max_inclusive").getAsDouble()) / 2.0;
            case "minecraft:weighted_list": {
                double sum = 0;
                double weights = 0;
                for (JsonElement entry : o.getAsJsonArray("distribution")) {
                    JsonObject eo = entry.getAsJsonObject();
                    double w = eo.get("weight").getAsDouble();
                    sum += intMean(eo.get("data")) * w;
                    weights += w;
                }
                return weights > 0 ? sum / weights : 0;
            }
            case "minecraft:clamped": {
                double mean = intMean(o.get("source"));
                return Math.max(o.get("min_inclusive").getAsDouble(), Math.min(o.get("max_inclusive").getAsDouble(), mean));
            }
            default:
                return IntProvider.CODEC.parse(JsonOps.INSTANCE, e).result()
                        .map(p -> (p.getMinValue() + p.getMaxValue()) / 2.0).orElse(1.0);
        }
    }
}
