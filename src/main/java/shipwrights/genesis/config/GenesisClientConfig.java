package shipwrights.genesis.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class GenesisClientConfig {

    private static ModConfigSpec.ConfigValue<Boolean> spaceShaderEnable;
    private static final boolean defaultSpaceShaderEnable = true;

    private static ModConfigSpec.ConfigValue<Boolean> renderCurrentPlanet;
    private static final boolean defaultRenderCurrentPlanet = true;

    private static ModConfigSpec.ConfigValue<Double> celestialVisualScale;
    private static final double defaultCelestialVisualScale = 1.5;

    private static ModConfigSpec.ConfigValue<Integer> currentPlanetRevealHeight;
    private static final int defaultCurrentPlanetRevealHeight = 16000;

    private static ModConfigSpec.ConfigValue<Integer> worldLodStartHeight;
    private static final int defaultWorldLodStartHeight = 256;

    private static ModConfigSpec.ConfigValue<Integer> worldLodFullHeight;
    private static final int defaultWorldLodFullHeight = 1536;

    private static ModConfigSpec.ConfigValue<Integer> worldCubeFoldStartHeight;
    private static final int defaultWorldCubeFoldStartHeight = 15000;

    private static ModConfigSpec.ConfigValue<Integer> worldCubeCompressionFullHeight;
    private static final int defaultWorldCubeCompressionFullHeight = 2048;

    private static ModConfigSpec.ConfigValue<Integer> worldLodRadius;
    private static final int defaultWorldLodRadius = 4096;

    private static ModConfigSpec.ConfigValue<Integer> worldLodQuality;
    private static final int defaultWorldLodQuality = 3;

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

    public static int getCurrentPlanetRevealHeight() {
        int result = defaultCurrentPlanetRevealHeight;
        try {
            result = currentPlanetRevealHeight.get();
        } catch (Exception ignored) { }
        return Math.max(0, result);
    }

    public static int getWorldLodStartHeight() {
        int result = defaultWorldLodStartHeight;
        try {
            result = worldLodStartHeight.get();
        } catch (Exception ignored) { }
        return Math.max(128, result);
    }

    public static int getWorldLodFullHeight() {
        int result = defaultWorldLodFullHeight;
        try {
            result = worldLodFullHeight.get();
        } catch (Exception ignored) { }
        return Math.max(getWorldLodStartHeight() + 1, result);
    }


    public static int getWorldCubeFoldStartHeight() {
        int result = defaultWorldCubeFoldStartHeight;
        try {
            result = worldCubeFoldStartHeight.get();
        } catch (Exception ignored) { }
        return Math.max(getWorldLodFullHeight(), Math.min(getCurrentPlanetRevealHeight() - 1, result));
    }

    public static int getWorldCubeCompressionFullHeight() {
        int result = defaultWorldCubeCompressionFullHeight;
        try {
            result = worldCubeCompressionFullHeight.get();
        } catch (Exception ignored) { }
        return Math.max(Math.max(513, getWorldLodStartHeight() + 1), result);
    }

    public static int getWorldLodRadius() {
        int result = defaultWorldLodRadius;
        try {
            result = worldLodRadius.get();
        } catch (Exception ignored) { }
        return Math.max(512, result);
    }

    public static int getWorldLodQuality() {
        int result = defaultWorldLodQuality;
        try {
            result = worldLodQuality.get();
        } catch (Exception ignored) { }
        return Math.max(1, Math.min(3, result));
    }

    public static double getCelestialVisualScale() {
        double result = defaultCelestialVisualScale;
        try {
            result = celestialVisualScale.get();
        } catch (Exception ignored) { }
        return Math.max(0.25, Math.min(8.0, result));
    }

    public static final ModConfigSpec CONFIG_SPEC = buildConfig();

    private static ModConfigSpec buildConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        spaceShaderEnable = builder.define("EnableDynamicSpaceLighting", defaultSpaceShaderEnable);
        renderCurrentPlanet = builder.define("ShouldRenderCurrentPlanet", defaultRenderCurrentPlanet);
        celestialVisualScale = builder.defineInRange("CelestialVisualScale", defaultCelestialVisualScale, 0.25, 8.0);
        currentPlanetRevealHeight = builder
                .comment("Altitude where the finite cube-planet silhouette begins folding into view.")
                .defineInRange("CurrentPlanetRevealHeight", defaultCurrentPlanetRevealHeight, 0, 100000);
        worldLodStartHeight = builder
                .comment("Altitude where cached terrain LOD begins extending beyond vanilla chunks.")
                .defineInRange("WorldLodStartHeight", defaultWorldLodStartHeight, 128, 16000);
        worldLodFullHeight = builder
                .comment("Altitude where the flat-world LOD reaches full opacity.")
                .defineInRange("WorldLodFullHeight", defaultWorldLodFullHeight, 129, 20000);
        worldCubeFoldStartHeight = builder
                .comment("Altitude where the five remote cube-net regions begin folding around the current world face.")
                .defineInRange("WorldCubeFoldStartHeight", defaultWorldCubeFoldStartHeight, 1024, 19999);
        worldCubeCompressionFullHeight = builder
                .comment("Altitude where camera-relative distance compression fully prevents far-plane clipping.")
                .defineInRange("WorldCubeCompressionFullHeight", defaultWorldCubeCompressionFullHeight, 513, 20000);
        worldLodRadius = builder
                .comment("Maximum radius in blocks for the pre-space flat-world LOD field.")
                .defineInRange("WorldLodRadius", defaultWorldLodRadius, 512, 16384);
        worldLodQuality = builder
                .comment("World-to-planet LOD quality: 1=fast, 2=balanced, 3=high.")
                .defineInRange("WorldLodQuality", defaultWorldLodQuality, 1, 3);
        return builder.build();
    }
}
