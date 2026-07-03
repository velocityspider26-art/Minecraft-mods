package com.example.examplemod.content.thruster;

import net.minecraft.resources.ResourceLocation;

/**
 * A data-driven fuel definition. Loaded from JSON (see {@link ThrusterFuelManager}) so new fuels can
 * be added, and their thrust/burn/visuals tuned, without code changes.
 *
 * @param id               the fuel's id (its json file location)
 * @param thrustMultiplier scales the thrust produced while burning this fuel
 * @param burnRate         scales how fast the fuel is consumed
 * @param tier             the plume/visual tier this fuel drives
 * @param heat             0..1, hint used for plume brightness/colour bias
 * @param smoke            0..1, how much smoke this (dirty) fuel produces
 * @param instability      0..1, how much the plume/thrust jitters
 */
public record ThrusterFuelType(ResourceLocation id, float thrustMultiplier, float burnRate,
                               PlumeType tier, float heat, float smoke, float instability) {

    /** Fallback used when a fluid has no matching fuel definition but is still allowed to burn. */
    public static ThrusterFuelType defaultFor(ResourceLocation id, PlumeType tier) {
        return new ThrusterFuelType(id, 1.0f, 1.0f, tier, 0.5f, 0.1f, 0.05f);
    }
}
