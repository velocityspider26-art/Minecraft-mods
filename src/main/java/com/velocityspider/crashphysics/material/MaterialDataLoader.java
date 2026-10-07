package com.velocityspider.crashphysics.material;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.velocityspider.crashphysics.CrashPhysics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Loads crash materials from {@code data/<namespace>/crashphysics/materials/*.json}.
 * <pre>
 * {
 *   "selector": "#minecraft:logs",            // block ID, #tag, or a list of them
 *   "priority": 1000,                         // higher priorities are applied later and win
 *   "density": 700,                           // kg/m³
 *   "strength": 4e6,                          // crush strength [Pa]
 *   "joint_strength": 1e6,                    // load a full face connection carries [Pa]
 *   "fracture": "fibrous",                    // granular, brittle, fibrous, ductile, soft, glass, plant
 *   "unbreakable": false,
 *   "ejecta": "self",                         // thrown out of craters as: "none", "self" or a block ID
 *   "scar": "minecraft:coarse_dirt",          // what the floor of a crater turns into
 *   "skid": "minecraft:coarse_dirt",          // what it turns into when a vehicle slides over it
 *   "volatility": 2.5,                        // explosion power when destroyed in a crash
 *   "ignites": true                           // starts fires when destroyed
 * }
 * </pre>
 */
public class MaterialDataLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();

    public MaterialDataLoader() {
        super(GSON, "crashphysics/materials");
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> entries, final ResourceManager resourceManager, final ProfilerFiller profiler) {
        final List<MaterialOverride> overrides = new ArrayList<>();

        for (final Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                overrides.add(parse(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(), "material")));
            } catch (final RuntimeException e) {
                CrashPhysics.LOGGER.error("Skipping invalid crash material {}: {}", entry.getKey(), e.getMessage());
            }
        }

        overrides.sort(Comparator.comparingInt(MaterialOverride::priority).thenComparing(override -> override.source().toString()));
        CrashMaterials.setOverrides(overrides);
        CrashPhysics.LOGGER.info("Loaded {} crash material definitions", overrides.size());
    }

    static MaterialOverride parse(final ResourceLocation source, final JsonObject json) {
        final List<MaterialOverride.Selector> selectors = new ArrayList<>();
        final JsonElement selectorJson = json.get("selector");
        if (selectorJson == null) {
            throw new JsonParseException("missing \"selector\"");
        }
        if (selectorJson.isJsonArray()) {
            final JsonArray array = selectorJson.getAsJsonArray();
            for (final JsonElement element : array) {
                selectors.add(MaterialOverride.Selector.parse(element.getAsString()));
            }
        } else {
            selectors.add(MaterialOverride.Selector.parse(selectorJson.getAsString()));
        }

        final Optional<FractureMode> fracture = json.has("fracture")
                ? Optional.of(FractureMode.valueOf(GsonHelper.getAsString(json, "fracture").toUpperCase(Locale.ROOT)))
                : Optional.empty();

        return new MaterialOverride(
                source,
                selectors,
                GsonHelper.getAsInt(json, "priority", 1000),
                positive(json, "density"),
                positive(json, "strength"),
                positive(json, "joint_strength"),
                fracture,
                json.has("unbreakable") ? Optional.of(GsonHelper.getAsBoolean(json, "unbreakable")) : Optional.empty(),
                state(json, "ejecta"),
                state(json, "scar"),
                state(json, "skid"),
                json.has("volatility") ? OptionalDouble.of(Math.max(0.0, GsonHelper.getAsDouble(json, "volatility"))) : OptionalDouble.empty(),
                json.has("ignites") ? Optional.of(GsonHelper.getAsBoolean(json, "ignites")) : Optional.empty()
        );
    }

    private static OptionalDouble positive(final JsonObject json, final String key) {
        if (!json.has(key)) {
            return OptionalDouble.empty();
        }
        final double value = GsonHelper.getAsDouble(json, key);
        if (!(value > 0.0)) {
            throw new JsonParseException("\"" + key + "\" must be positive");
        }
        return OptionalDouble.of(value);
    }

    private static Optional<MaterialOverride.StateReference> state(final JsonObject json, final String key) {
        return json.has(key) ? Optional.of(MaterialOverride.StateReference.parse(GsonHelper.getAsString(json, key))) : Optional.empty();
    }
}
