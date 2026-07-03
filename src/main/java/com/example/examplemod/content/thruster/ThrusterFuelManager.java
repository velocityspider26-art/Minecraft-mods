package com.example.examplemod.content.thruster;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Loads thruster fuel definitions from {@code data/<namespace>/thruster_fuel/*.json} and indexes them
 * by the fluids they accept, so a thruster can look up the fuel for whatever is in its tank.
 *
 * <p>Each json looks like:</p>
 * <pre>{@code
 * {
 *   "fluids": ["minecraft:lava"],
 *   "thrust_multiplier": 0.65,
 *   "burn_rate": 0.8,
 *   "tier": "low_grade",
 *   "heat": 0.4, "smoke": 0.6, "instability": 0.15
 * }
 * }</pre>
 */
public class ThrusterFuelManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private static volatile ThrusterFuelManager instance;

    /** fluid id -> fuel definition. */
    private Map<ResourceLocation, ThrusterFuelType> byFluid = Map.of();

    public ThrusterFuelManager() {
        super(GSON, "thruster_fuel");
        instance = this;
    }

    public static ThrusterFuelManager getInstance() {
        return instance;
    }

    /** Returns the fuel for a fluid, or null if the fluid is not a configured fuel. */
    public ThrusterFuelType getFuel(Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        return key == null ? null : byFluid.get(key);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ThrusterFuelType> map = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "fuel");
                float thrust = GsonHelper.getAsFloat(json, "thrust_multiplier", 1.0f);
                float burn = GsonHelper.getAsFloat(json, "burn_rate", 1.0f);
                PlumeType tier = PlumeType.byName(GsonHelper.getAsString(json, "tier", "standard"), PlumeType.STANDARD);
                float heat = GsonHelper.getAsFloat(json, "heat", 0.5f);
                float smoke = GsonHelper.getAsFloat(json, "smoke", 0.1f);
                float instab = GsonHelper.getAsFloat(json, "instability", 0.05f);
                ThrusterFuelType fuel = new ThrusterFuelType(id, thrust, burn, tier, heat, smoke, instab);

                for (JsonElement fluidEl : GsonHelper.getAsJsonArray(json, "fluids")) {
                    ResourceLocation fluidId = ResourceLocation.parse(fluidEl.getAsString());
                    map.put(fluidId, fuel);
                }
            } catch (Exception e) {
                LOGGER.error("Skipping invalid thruster fuel {}: {}", id, e.getMessage());
            }
        }
        this.byFluid = Map.copyOf(map);
        LOGGER.info("Loaded {} thruster fuel fluid mappings", byFluid.size());
    }
}
