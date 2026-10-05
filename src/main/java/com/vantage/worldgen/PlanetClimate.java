package com.vantage.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * One climate parameter of a planet, as a density function: the generator's terrain splines and
 * Minecraft's biome table read these exactly as they read the vanilla noises.
 *
 * <p>All five parameters of a column are computed together and remembered per thread, because
 * they share the coordinate transforms and warp noise, and biome lookups ask for all of them at
 * the same column many times.
 */
public record PlanetClimate(Kind kind, PlanetSettings planet, PlanetNoises noises) implements DensityFunction {
    public enum Kind implements StringRepresentable {
        CONTINENTS("continents"), EROSION("erosion"), RIDGES("ridges"), TEMPERATURE("temperature"), VEGETATION("vegetation");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final MapCodec<PlanetClimate> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Kind.CODEC.fieldOf("kind").forGetter(PlanetClimate::kind),
            PlanetSettings.CODEC.fieldOf("planet").forGetter(PlanetClimate::planet),
            PlanetNoises.CODEC.forGetter(PlanetClimate::noises)
    ).apply(i, PlanetClimate::new));
    public static final KeyDispatchDataCodec<PlanetClimate> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return PlanetModel.get(this.planet, this.noises, this.kind.ordinal(), context.blockX(), context.blockZ());
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new PlanetClimate(this.kind, this.planet, this.noises.visit(visitor)));
    }

    @Override
    public double minValue() {
        return -1.2;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
