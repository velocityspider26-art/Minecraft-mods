package com.vantage.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VantageConfig {
    /** Largest LOD distance in chunks, also the cap when flying high (about 524 km). */
    public static final int MAX_RENDER_DISTANCE = 32768;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Render distant terrain.")
            .define("enabled", true);

    public static final ModConfigSpec.IntValue RENDER_DISTANCE = BUILDER
            .comment("How far LOD terrain is drawn, in chunks (16 blocks), when on the ground.",
                    "Higher up it reaches further on its own (see altitudeViewFactor).",
                    "Tip: lower the vanilla render distance (8-12) and raise this instead.")
            .defineInRange("renderDistance", 256, 16, MAX_RENDER_DISTANCE);

    public static final ModConfigSpec.DoubleValue DETAIL = BUILDER
            .comment("Largest size, in pixels, that one LOD voxel may appear on screen before a finer level is used.",
                    "Lower = sharper but more triangles; higher = faster.")
            .defineInRange("pixelsPerVoxel", 3.0, 0.5, 16.0);

    public static final ModConfigSpec.BooleanValue CAVE_CULLING = BUILDER
            .comment("Drop unlit caves and buried blocks from LOD data. They can never be seen from a distance,",
                    "and dropping them makes LODs several times cheaper to store and draw.")
            .define("caveCulling", true);

    public static final ModConfigSpec.BooleanValue DISTANT_GENERATION = BUILDER
            .comment("In singleplayer, fill land nobody has explored yet straight from the world generator",
                    "(terrain shape, biomes and tree cover; no buildings), so the ground never ends.",
                    "Explored chunks always replace it. It never creates chunks or changes your save.")
            .define("distantGeneration", true);

    public static final ModConfigSpec.BooleanValue DETAILED_GENERATION = BUILDER
            .comment("Show unexplored land near you as Minecraft's real world generator makes it, with the real trees,",
                    "plants, rocks and snow instead of an approximation. In singleplayer (and when hosting a LAN or",
                    "Essential game) your game makes it on background threads; on a server with Vantage, the server sends it.",
                    "Nothing is added to the save.")
            .define("detailedGeneration", true);

    public static final ModConfigSpec.IntValue DETAIL_DISTANCE = BUILDER
            .comment("How far from you (in blocks) unexplored land is made with the real world generator. Beyond this",
                    "the faster approximation is used; trees are too small to see that far anyway.")
            .defineInRange("detailDistance", 1024, 128, 16384);

    public static final ModConfigSpec.IntValue DETAIL_THREADS = BUILDER
            .comment("Threads for real-generator terrain. 0 picks a number from your CPU cores.")
            .defineInRange("detailThreads", 0, 0, 16);

    public static final ModConfigSpec.DoubleValue ALTITUDE_VIEW = BUILDER
            .comment("When flying high, LOD terrain reaches at least this many times your height above sea level,",
                    "so the ground stays visible all the way to the horizon. 0 = off.")
            .defineInRange("altitudeViewFactor", 8.0, 0.0, 64.0);

    public static final ModConfigSpec.IntValue HAZE_DISTANCE = BUILDER
            .comment("Haze: distance in blocks over which air at sea level hides about two thirds of the view.",
                    "Air thins out with height, so from high up you look down through very little haze.")
            .defineInRange("hazeDistance", 24000, 500, 10_000_000);

    public static final ModConfigSpec.IntValue ATMOSPHERE_HEIGHT = BUILDER
            .comment("Scale height of the atmosphere in blocks: air (and haze) gets about 2.7x thinner every this many",
                    "blocks above sea level.")
            .defineInRange("atmosphereHeight", 1200, 50, 1_000_000);

    public static final ModConfigSpec.BooleanValue SPACE_SKY = BUILDER
            .comment("High above the atmosphere (from about three atmosphere heights up) the sky fades to black and",
                    "the stars come out, while the ground below keeps its haze. Turn off if another mod draws the sky there.")
            .define("spaceSky", true);

    public static final ModConfigSpec.IntValue PLANET_RADIUS = BUILDER
            .comment("Bend distant terrain down like the surface of a planet with this radius in blocks, with a real",
                    "horizon past which terrain is hidden. 0 = flat like vanilla. Try 100000 for a big planet or",
                    "20000 for a small one; near you the bend is too small to notice.")
            .defineInRange("planetRadius", 0, 0, 100_000_000);

    public static final ModConfigSpec.BooleanValue IMPORT_SAVES = BUILDER
            .comment("In singleplayer, build LODs in the background from every chunk that has already been generated,",
                    "not just the ones you have loaded this session.")
            .define("importExistingChunks", true);

    public static final ModConfigSpec.IntValue WORKER_THREADS = BUILDER
            .comment("Background threads for building LODs. 0 = automatic.")
            .defineInRange("workerThreads", 0, 0, 32);

    public static final ModConfigSpec.IntValue UPLOAD_BUDGET_KB = BUILDER
            .comment("Most geometry uploaded to the GPU per frame, in KiB. Lower values avoid stutter on slow GPUs.")
            .defineInRange("uploadBudgetKiB", 4096, 256, 65536);

    public static final ModConfigSpec.IntValue GPU_MEMORY_MB = BUILDER
            .comment("Most GPU memory used for LOD geometry, in MiB.")
            .defineInRange("gpuMemoryMiB", 768, 64, 8192);

    public static final ModConfigSpec.IntValue CACHE_MB = BUILDER
            .comment("Most RAM used for the decoded LOD section cache, in MiB.")
            .defineInRange("ramCacheMiB", 256, 32, 4096);

    public static final ModConfigSpec.DoubleValue FOG_START = BUILDER
            .comment("Where terrain starts fading out towards the end of the LOD distance, as a fraction of it.")
            .defineInRange("fogStart", 0.8, 0.0, 1.0);

    /** Version of the settings' defaults this file has been brought up to; see {@link #migrate}. */
    private static final ModConfigSpec.IntValue VERSION = BUILDER
            .comment("Internal: which defaults this file has been updated to. Do not change.")
            .defineInRange("configVersion", 0, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * Moves settings still at an old default to the new one (players who chose a value keep it).
     * 2: clearer air (haze distance 12000 to 24000).
     */
    static void migrate() {
        int v = VERSION.get();
        if (v >= 2) {
            return;
        }
        if (HAZE_DISTANCE.get() == 12000) {
            HAZE_DISTANCE.set(24000);
        }
        VERSION.set(2);
        SPEC.save();
    }

    private VantageConfig() {
    }

    public static int workerThreads() {
        int n = WORKER_THREADS.get();
        if (n > 0) {
            return n;
        }
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(1, Math.min(6, cores / 2 - 1));
    }

    public static int detailThreads() {
        int n = DETAIL_THREADS.get();
        if (n > 0) {
            return n;
        }
        return Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 4));
    }
}
