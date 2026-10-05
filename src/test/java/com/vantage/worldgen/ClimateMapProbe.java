package com.vantage.worldgen;

import net.minecraft.world.level.biome.Climate;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Debug aid: map of climate bands over 12 km around spawn ({@code *} frozen, {@code :} cold,
 * {@code .}/{@code f} temperate dry/wet, {@code s}/{@code J} warm dry/wet, {@code D} hot).
 * Runs only with -Dvantage.probe=true.
 */
class ClimateMapProbe {
    @Test
    void probe() {
        Assumptions.assumeTrue(Boolean.getBoolean("vantage.probe"));
        PlanetRouterTest.setUp();
        var random = PlanetRouterTest.random;
        StringBuilder b = new StringBuilder();
        for (int z = -6000; z <= 6000; z += 240) {
            b.append(String.format("%6d ", z));
            for (int x = -6000; x <= 6000; x += 120) {
                Climate.TargetPoint t = random.sampler().sample(x >> 2, 16, z >> 2);
                float temp = Climate.unquantizeCoord(t.temperature());
                float hum = Climate.unquantizeCoord(t.humidity());
                char ch = temp < -0.45f ? '*' : temp < -0.15f ? ':' : temp < 0.2f ? (hum > 0.1f ? 'f' : '.') : temp < 0.55f ? (hum > 0.1f ? 'J' : 's') : 'D';
                b.append(ch);
            }
            b.append('\n');
        }
        System.out.println("[probe]\n" + b);
    }
}
