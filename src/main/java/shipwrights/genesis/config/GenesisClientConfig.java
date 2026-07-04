package shipwrights.genesis.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class GenesisClientConfig {

    private static ModConfigSpec.ConfigValue<Boolean> spaceShaderEnable;
    private static final boolean defaultSpaceShaderEnable = true;

    private static ModConfigSpec.ConfigValue<Boolean> renderCurrentPlanet;
    private static final boolean defaultRenderCurrentPlanet = true;

    public static boolean enableSpaceLighting() {
        boolean result = defaultSpaceShaderEnable;
        try {
            result = spaceShaderEnable.get();
        } catch (Exception ignored) { }
        return result;
    }

    public static boolean shouldRenderCurrentPlanet() {
        boolean result = defaultRenderCurrentPlanet;
        try {
            result = renderCurrentPlanet.get();
        } catch (Exception ignored) { }
        return result;
    }

    public static final ModConfigSpec CONFIG_SPEC = buildConfig();

    private static ModConfigSpec buildConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        spaceShaderEnable = builder.define("EnableDynamicSpaceLighting", defaultSpaceShaderEnable);
        renderCurrentPlanet = builder.define("ShouldRenderCurrentPlanet", defaultRenderCurrentPlanet);
        return builder.build();
    }
}
