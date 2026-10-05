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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private VantageServerConfig() {
    }

    public static int threads() {
        int n = THREADS.get();
        return n > 0 ? n : Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 4));
    }
}
