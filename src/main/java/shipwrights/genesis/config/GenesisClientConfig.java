package shipwrights.genesis.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class GenesisClientConfig {

    private static ModConfigSpec.ConfigValue<Boolean> spaceShaderEnable;
    private static final boolean defaultSpaceShaderEnable = true;

    private static ModConfigSpec.ConfigValue<Boolean> renderCurrentPlanet;
    private static final boolean defaultRenderCurrentPlanet = true;

    private static ModConfigSpec.ConfigValue<Boolean> proceduralShaders;
    private static final boolean defaultProceduralShaders = true;

    /**
     * Whether to draw celestials with VS Genesis' procedural cube shaders (sun / planet). When off,
     * or when a shader fails to load, the vanilla-pipeline billboard renderer is used instead.
     */
    public static boolean useProceduralShaders() {
        boolean result = defaultProceduralShaders;
        try {
            result = proceduralShaders.get();
        } catch (Exception ignored) { }
        return result;
    }

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
        builder.comment("Draw the Sun and planets with VS Genesis' procedural cube shaders.",
                "Turn off to force the vanilla-pipeline billboard renderer (also the automatic fallback",
                "if the shaders can't compile on your hardware).");
        proceduralShaders = builder.define("UseProceduralCelestialShaders", defaultProceduralShaders);
        return builder.build();
    }
}
