package com.vantage;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Settings for the server side: what Vantage does for players joining this game or server. */
public final class VantageServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SERVE_TERRAIN = BUILDER
            .comment("Let players with Vantage see unexplored land in the distance: the server samples its world",
                    "generator for them (terrain shape and biomes only, no chunks are generated or saved).")
            .define("serveDistantTerrain", true);

    public static final ModConfigSpec.IntValue THREADS = BUILDER
            .comment("Threads answering distant-terrain requests. 0 = automatic (a quarter of the CPU cores).")
            .defineInRange("serverThreads", 0, 0, 16);

    public static final ModConfigSpec.IntValue MAX_PENDING = BUILDER
            .comment("Most distant-terrain requests one player may have waiting at a time; more are turned away",
                    "and asked again later.")
            .defineInRange("maxRequestsPerPlayer", 24, 1, 512);

    public static final ModConfigSpec.BooleanValue SERVE_DETAIL = BUILDER
            .comment("Also give players with Vantage the real terrain near them: trees, plants, rocks and snow, block for block what",
                    "the world has or will have there. Explored chunks are sent as they are; unexplored ones are made with",
                    "Minecraft's world generator in throwaway chunks (nothing is added to the world or the save). Takes some CPU",
                    "while players explore.")
            .define("serveDetailedTerrain", true);

    public static final ModConfigSpec.IntValue DETAIL_DISTANCE = BUILDER
            .comment("How far from a player, in blocks, the server sends real terrain.")
            .defineInRange("detailedTerrainDistance", 1024, 128, 4096);

    public static final ModConfigSpec.IntValue DETAIL_THREADS = BUILDER
            .comment("Threads making real terrain for players. 0 = automatic (one per eight CPU cores, at least one).")
            .defineInRange("detailedTerrainThreads", 0, 0, 16);

    public static final ModConfigSpec.IntValue DETAIL_CACHE_MB = BUILDER
            .comment("Memory, in MiB, for terrain made for players, so the next player asking for it gets it at once.")
            .defineInRange("detailedTerrainCacheMiB", 64, 0, 4096);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private VantageServerConfig() {
    }

    public static int detailThreads() {
        int n = DETAIL_THREADS.get();
        return n > 0 ? n : Math.max(1, Runtime.getRuntime().availableProcessors() / 8);
    }

    public static int threads() {
        int n = THREADS.get();
        return n > 0 ? n : Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 4));
    }
}
