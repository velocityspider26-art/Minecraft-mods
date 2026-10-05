package com.vantage.worldgen;

import com.mojang.serialization.MapCodec;
import com.vantage.Vantage;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the planet generator and its density function type (world type "Vantage Planet"). */
public final class VantageWorldgen {
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Vantage.MODID);
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Vantage.MODID);

    static {
        GENERATORS.register("planet", () -> PlanetChunkGenerator.CODEC);
        DENSITY_FUNCTIONS.register("planet_climate", () -> PlanetClimate.MAP_CODEC);
    }

    private VantageWorldgen() {
    }

    public static void register(IEventBus modBus) {
        GENERATORS.register(modBus);
        DENSITY_FUNCTIONS.register(modBus);
    }
}
