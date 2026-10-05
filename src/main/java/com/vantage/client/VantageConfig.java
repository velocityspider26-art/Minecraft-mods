package com.vantage.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VantageConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Render distant terrain.")
            .define("enabled", true);

    public static final ModConfigSpec.IntValue RENDER_DISTANCE = BUILDER
            .comment("How far LOD terrain is drawn, in chunks (16 blocks).",
                    "Tip: lower the vanilla render distance (8-12) and raise this instead.")
            .defineInRange("renderDistance", 256, 16, 4096);

    public static final ModConfigSpec.DoubleValue DETAIL = BUILDER
            .comment("Largest size, in pixels, that one LOD voxel may appear on screen before a finer level is used.",
                    "Lower = sharper but more triangles; higher = faster.")
            .defineInRange("pixelsPerVoxel", 3.0, 0.5, 16.0);

    public static final ModConfigSpec.BooleanValue CAVE_CULLING = BUILDER
            .comment("Drop unlit caves and buried blocks from LOD data. They can never be seen from a distance,",
                    "and dropping them makes LODs several times cheaper to store and draw.")
            .define("caveCulling", true);

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
            .comment("Where distance fog starts, as a fraction of the LOD render distance.")
            .defineInRange("fogStart", 0.8, 0.0, 1.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

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
}
