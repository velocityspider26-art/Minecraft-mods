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

    // ------------------------------------------------------------------
    // Planet voxel engine
    // ------------------------------------------------------------------

    private static ModConfigSpec.ConfigValue<Boolean> planetVoxelRenderer;
    private static final boolean defaultPlanetVoxelRenderer = true;

    private static ModConfigSpec.ConfigValue<Integer> worldCubeFoldFullHeight;
    private static final int defaultWorldCubeFoldFullHeight = 20480;

    private static ModConfigSpec.ConfigValue<Integer> planetVoxelCacheMegabytes;
    private static final int defaultPlanetVoxelCacheMegabytes = 1024;

    private static ModConfigSpec.ConfigValue<Integer> planetGpuCacheMegabytes;
    private static final int defaultPlanetGpuCacheMegabytes = 384;

    private static ModConfigSpec.ConfigValue<Integer> planetMeshWorkers;
    private static final int defaultPlanetMeshWorkers = 3;

    private static ModConfigSpec.ConfigValue<Integer> planetGpuUploadsPerFrame;
    private static final int defaultPlanetGpuUploadsPerFrame = 8;

    private static ModConfigSpec.ConfigValue<Boolean> planetDebugOverlay;
    private static final boolean defaultPlanetDebugOverlay = false;

    private static ModConfigSpec.ConfigValue<String> planetDebugMode;
    private static final String defaultPlanetDebugMode = "OFF";

    /** Master switch for the sparse voxel Earth. Disabling leaves only vanilla chunks. */
    public static boolean isPlanetVoxelRendererEnabled() {
        try {
            return planetVoxelRenderer.get();
        } catch (Exception ignored) {
            return defaultPlanetVoxelRenderer;
        }
    }

    /** Altitude at which the five remote faces have fully folded into a cube. */
    public static int getWorldCubeFoldFullHeight() {
        int result = defaultWorldCubeFoldFullHeight;
        try {
            result = worldCubeFoldFullHeight.get();
        } catch (Exception ignored) { }
        return Math.max(getWorldCubeFoldStartHeight() + 1, result);
    }

    public static long getPlanetVoxelCacheBytes() {
        int megabytes = defaultPlanetVoxelCacheMegabytes;
        try {
            megabytes = planetVoxelCacheMegabytes.get();
        } catch (Exception ignored) { }
        return Math.max(128L, Math.min(4096L, megabytes)) * 1024L * 1024L;
    }

    public static long getPlanetGpuCacheBytes() {
        int megabytes = defaultPlanetGpuCacheMegabytes;
        try {
            megabytes = planetGpuCacheMegabytes.get();
        } catch (Exception ignored) { }
        return Math.max(64L, Math.min(2048L, megabytes)) * 1024L * 1024L;
    }

    public static int getPlanetMeshWorkers() {
        int result = defaultPlanetMeshWorkers;
        try {
            result = planetMeshWorkers.get();
        } catch (Exception ignored) { }
        return Math.max(1, Math.min(4, result));
    }

    public static int getPlanetGpuUploadsPerFrame() {
        int result = defaultPlanetGpuUploadsPerFrame;
        try {
            result = planetGpuUploadsPerFrame.get();
        } catch (Exception ignored) { }
        return Math.max(1, Math.min(64, result));
    }

    public static boolean isPlanetDebugOverlayEnabled() {
        try {
            return planetDebugOverlay.get();
        } catch (Exception ignored) {
            return defaultPlanetDebugOverlay;
        }
    }

    public static String getPlanetDebugMode() {
        try {
            return planetDebugMode.get();
        } catch (Exception ignored) {
            return defaultPlanetDebugMode;
        }
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

        builder.comment("Genesis planet voxel engine — the world's own 3D LOD chunks, reprojected onto six cube faces.")
                .push("PlanetVoxelEngine");
        planetVoxelRenderer = builder
                .comment("Render the orbital Earth from the sparse voxel pyramid. Disabling leaves only vanilla chunks.")
                .define("EnablePlanetVoxelRenderer", defaultPlanetVoxelRenderer);
        worldCubeFoldFullHeight = builder
                .comment("Altitude where the five remote cube-net regions have fully folded into a cube.")
                .defineInRange("WorldCubeFoldFullHeight", defaultWorldCubeFoldFullHeight, 1025, 100000);
        planetVoxelCacheMegabytes = builder
                .comment("Client voxel brick cache budget, in MiB.")
                .defineInRange("VoxelCacheMegabytes", defaultPlanetVoxelCacheMegabytes, 128, 4096);
        planetGpuCacheMegabytes = builder
                .comment("Client GPU mesh cache budget, in MiB.")
                .defineInRange("GpuCacheMegabytes", defaultPlanetGpuCacheMegabytes, 64, 2048);
        planetMeshWorkers = builder
                .comment("Background CPU meshing threads.")
                .defineInRange("MeshWorkerThreads", defaultPlanetMeshWorkers, 1, 4);
        planetGpuUploadsPerFrame = builder
                .comment("Maximum brick meshes uploaded to the GPU per frame.")
                .defineInRange("GpuUploadsPerFrame", defaultPlanetGpuUploadsPerFrame, 1, 64);
        planetDebugOverlay = builder
                .comment("Show the planet voxel engine diagnostics overlay.")
                .define("DebugOverlay", defaultPlanetDebugOverlay);
        planetDebugMode = builder
                .comment("Debug rendering: OFF, LOD, AUTHORITY, BRICK_BOUNDS, SEAMS, FOLD, MISSING.")
                .define("DebugRenderMode", defaultPlanetDebugMode);
        builder.pop();
        return builder.build();
    }
}
