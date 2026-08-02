package shipwrights.genesis.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class GenesisCommonConfig {
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
        atmosphereExitHeight = builder.define("atmosphereExitHeight", 20000);
        atmosphereEntryHeight = builder.define("atmosphereEntryHeight", 19000);
        // Build a six-face world LOD from each planet dimension's generated
        // block columns instead of treating the painted atlas as the surface.
        //
        // Enabled for the overworld only. Sampling is isolated on a dedicated
        // minimum-priority thread, pauses during crossings, and never touches
        // Minecraft's worldgen executor. Slow planetary generators are skipped.
        planetSurfaceSampling = builder
                .comment("Build Earth's six-face world LOD from generated Minecraft world columns.",
                        "Runs on one low-priority thread and pauses during dimension crossings.")
                .define("planetSurfaceSampling", true);
        return builder.build();
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
