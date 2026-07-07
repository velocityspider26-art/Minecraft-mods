package shipwrights.genesis.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class GenesisCommonConfig {
    private static ModConfigSpec.ConfigValue<Integer> atmosphereExitHeight;
    private static ModConfigSpec.ConfigValue<Integer> atmosphereEntryHeight;

    // Travel-loop tuning (block distances in the space dimension).
    private static ModConfigSpec.ConfigValue<Integer> earthApproachRadius;
    private static ModConfigSpec.ConfigValue<Integer> moonApproachRadius;
    private static ModConfigSpec.ConfigValue<Integer> deepSpaceRadius;
    private static ModConfigSpec.ConfigValue<Integer> cubeFaceRegionSpacing;

    public static final ModConfigSpec CONFIG_SPEC = buildConfig();

    private static ModConfigSpec buildConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Atmosphere heights (blocks). Fly above the exit height to reach space; re-entry drops you here.");
        atmosphereExitHeight = builder.define("atmosphereExitHeight", 2048);
        atmosphereEntryHeight = builder.define("atmosphereEntryHeight", 1440);

        builder.push("travel");
        builder.comment("How close (blocks, beyond a body's own radius) a player/craft must get before that",
                "body's atmosphere captures them and the (screen-hidden) dimension transition happens.");
        earthApproachRadius = builder.defineInRange("earthApproachRadius", 200, 16, 100000);
        moonApproachRadius = builder.defineInRange("moonApproachRadius", 160, 16, 100000);
        builder.comment("Distance from Earth (blocks) at which the Great Unknown gives way to deep space.",
                "Should sit well past the Moon so you clearly fly past it first (Moon orbit is ~10000).");
        deepSpaceRadius = builder.defineInRange("deepSpaceRadius", 30000, 1000, 10000000);
        builder.comment("Spacing (blocks) between the six overworld landing regions the cubed Earth's faces map to.");
        cubeFaceRegionSpacing = builder.defineInRange("cubeFaceRegionSpacing", 20000, 1000, 1000000);
        builder.pop();

        return builder.build();
    }

    public static int getAtmosphereExitHeight() {
        return atmosphereExitHeight.get();
    }

    public static int getAtmosphereEntryHeight() {
        return atmosphereEntryHeight.get();
    }

    public static int getEarthApproachRadius() {
        return earthApproachRadius.get();
    }

    public static int getMoonApproachRadius() {
        return moonApproachRadius.get();
    }

    public static int getDeepSpaceRadius() {
        return deepSpaceRadius.get();
    }

    public static int getCubeFaceRegionSpacing() {
        return cubeFaceRegionSpacing.get();
    }
}
