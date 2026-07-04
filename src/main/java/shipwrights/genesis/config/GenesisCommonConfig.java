package shipwrights.genesis.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class GenesisCommonConfig {
    private static ModConfigSpec.ConfigValue<Integer> atmosphereExitHeight;
    private static ModConfigSpec.ConfigValue<Integer> atmosphereEntryHeight;

    public static final ModConfigSpec CONFIG_SPEC = buildConfig();

    private static ModConfigSpec buildConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        atmosphereExitHeight = builder.define("atmosphereExitHeight", 2048);
        atmosphereEntryHeight = builder.define("atmosphereEntryHeight",1440);
        return builder.build();
    }

    public static int getAtmosphereExitHeight() {
        return atmosphereExitHeight.get();
    }

    public static int getAtmosphereEntryHeight() {
        return atmosphereEntryHeight.get();
    }
}
