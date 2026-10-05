package com.vantage.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The noises a planet's climate is made of. Defined as data
 * ({@code data/vantage/worldgen/noise/planet_*.json}) and seeded per world by Minecraft when the
 * density functions are wired up.
 */
public record PlanetNoises(NoiseHolder continents, NoiseHolder warp, NoiseHolder ranges, NoiseHolder erosion,
                           NoiseHolder ridges, NoiseHolder temperature, NoiseHolder vegetation) {
    public static final MapCodec<PlanetNoises> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NoiseHolder.CODEC.fieldOf("continents").forGetter(PlanetNoises::continents),
            NoiseHolder.CODEC.fieldOf("warp").forGetter(PlanetNoises::warp),
            NoiseHolder.CODEC.fieldOf("ranges").forGetter(PlanetNoises::ranges),
            NoiseHolder.CODEC.fieldOf("erosion").forGetter(PlanetNoises::erosion),
            NoiseHolder.CODEC.fieldOf("ridges").forGetter(PlanetNoises::ridges),
            NoiseHolder.CODEC.fieldOf("temperature").forGetter(PlanetNoises::temperature),
            NoiseHolder.CODEC.fieldOf("vegetation").forGetter(PlanetNoises::vegetation)
    ).apply(i, PlanetNoises::new));

    public static PlanetNoises of(HolderGetter<NormalNoise.NoiseParameters> noises) {
        return new PlanetNoises(holder(noises, "continents"), holder(noises, "warp"), holder(noises, "ranges"),
                holder(noises, "erosion"), holder(noises, "ridges"), holder(noises, "temperature"), holder(noises, "vegetation"));
    }

    private static NoiseHolder holder(HolderGetter<NormalNoise.NoiseParameters> noises, String name) {
        return new NoiseHolder(noises.getOrThrow(ResourceKey.create(Registries.NOISE,
                ResourceLocation.fromNamespaceAndPath("vantage", "planet_" + name))));
    }

    /** Seeded copies, as handed out by the world's random state. */
    public PlanetNoises visit(DensityFunction.Visitor visitor) {
        return new PlanetNoises(visitor.visitNoise(this.continents), visitor.visitNoise(this.warp), visitor.visitNoise(this.ranges),
                visitor.visitNoise(this.erosion), visitor.visitNoise(this.ridges), visitor.visitNoise(this.temperature),
                visitor.visitNoise(this.vegetation));
    }
}
