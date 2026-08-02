package shipwrights.genesis.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class GenesisCommonConfig {
    private static ModConfigSpec.ConfigValue<String> planetPreparationQuality;
    private static ModConfigSpec.ConfigValue<Integer> planetPreparationRadius;
    private static ModConfigSpec.ConfigValue<Integer> atmosphereExitHeight;
    private static ModConfigSpec.ConfigValue<Integer> atmosphereEntryHeight;
    private static ModConfigSpec.ConfigValue<Boolean> planetSurfaceSampling;

    public static final ModConfigSpec CONFIG_SPEC = buildConfig();

    private static ModConfigSpec buildConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        // Entities can fly above the build cap, so the boundary sits high: the
        // long climb through the upper sky IS space, and reentry is a real fall
        // from atmosphereEntryHeight down to the surface. 20k keeps the ascent a
        // genuine climb without being an impractical slog to reach on a ship.
        planetPreparationQuality = builder
                .comment("Planet voxel preparation: FAST, BALANCED, HIGH or EXTREME.",
                        "FAST     - coarse complete cube only (fastest world creation)",
                        "BALANCED - coarse cube plus a detailed spawn region",
                        "HIGH     - a larger detailed region",
                        "EXTREME  - uses PlanetPreparationRadius and the larger background budget")
                .define("PlanetPreparationQuality", "BALANCED");
        planetPreparationRadius = builder
                .comment("EXTREME only: radius in blocks around spawn prepared at fine detail.")
                .defineInRange("PlanetPreparationRadius", 2048, 0, 16384);
        atmosphereExitHeight = builder.define("atmosphereExitHeight", 20000);
        atmosphereEntryHeight = builder.define("atmosphereEntryHeight", 19000);
        // Build a six-face world LOD from each planet dimension's generated
        // block columns instead of treating the painted atlas as the surface.
        //
        // Enabled for the overworld only. Sampling is isolated on a dedicated
        // minimum-priority thread, pauses during crossings, and never touches
        // Minecraft's worldgen executor. Slow planetary generators are skipped.
        // Off by default now. This sampler exists to paint the six-face Earth
        // texture, and Earth is no longer painted — the voxel pyramid draws it
        // from real block data. Its only remaining effect is to spend a couple
        // of minutes of CPU on six 8192-block regions at world join, competing
        // with the terrain prediction that does feed the visible planet, and
        // then discard the result.
        planetSurfaceSampling = builder
                .comment("Legacy: build Earth's six-face painted world texture.",
                        "Superseded by the planet voxel engine; leave off unless debugging.")
                .define("planetSurfaceSampling", false);
        return builder.build();
    }

    /**
     * How much of the planet is prepared up front.
     *
     * <p>A complete cube at the coarsest level always exists, because a planet
     * with holes in it is not a planet. The presets choose how much finer
     * detail is generated ahead of the player rather than on demand.</p>
     */
    public enum PreparationQuality {
        /** Coarse complete cube only. */
        FAST(7, 0, 2),
        /** Coarse cube plus a detailed spawn region. */
        BALANCED(6, 512, 4),
        /** A larger detailed region. */
        HIGH(6, 1536, 6),
        /** Configurable radius and the largest background budget. */
        EXTREME(5, 2048, 8);

        private final int coverageLod;
        private final int detailRadius;
        private final int predictionsPerTick;

        PreparationQuality(int coverageLod, int detailRadius, int predictionsPerTick) {
            this.coverageLod = coverageLod;
            this.detailRadius = detailRadius;
            this.predictionsPerTick = predictionsPerTick;
        }

        public int coverageLod() {
            return coverageLod;
        }

        public int detailRadius() {
            return detailRadius;
        }

        public int predictionsPerTick() {
            return predictionsPerTick;
        }
    }

    public static PreparationQuality getPlanetPreparationQuality() {
        try {
            return PreparationQuality.valueOf(planetPreparationQuality.get()
                    .trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception ignored) {
            return PreparationQuality.BALANCED;
        }
    }

    /** Fine-detail radius around spawn. EXTREME uses the configured value. */
    public static int getPlanetPreparationRadius() {
        PreparationQuality quality = getPlanetPreparationQuality();
        if (quality != PreparationQuality.EXTREME) return quality.detailRadius();
        try {
            return Math.max(0, Math.min(16_384, planetPreparationRadius.get()));
        } catch (Exception ignored) {
            return quality.detailRadius();
        }
    }

    public static int getAtmosphereExitHeight() {
        return atmosphereExitHeight.get();
    }

    public static int getAtmosphereEntryHeight() {
        return atmosphereEntryHeight.get();
    }

    public static boolean isPlanetSurfaceSamplingEnabled() {
        return planetSurfaceSampling.get();
    }
}
