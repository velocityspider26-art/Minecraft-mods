package com.velocityspider.deathstar.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration for the size and heft of the summoned Death Star.
 *
 * <p>These values live on the server because the structure is generated and assembled server-side
 * before Sable takes over the physics simulation.
 */
public final class DeathStarConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue RADIUS;
    public static final ModConfigSpec.IntValue SHELL_THICKNESS;
    public static final ModConfigSpec.IntValue MAX_BLOCKS;
    public static final ModConfigSpec.IntValue DROP_HEIGHT;
    public static final ModConfigSpec.BooleanValue REACTOR_CORE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Death Star Ruins - structure & physics settings").push("deathstar");

        RADIUS = builder
                .comment(
                        "Outer radius of the Death Star in blocks. Diameter is roughly 2x this.",
                        "The block count scales with the square of the radius:",
                        "  radius 32 ~= 25,000 blocks, radius 45 ~= 50,000 blocks (heavy!).",
                        "Larger spheres are a *lot* more work for the physics engine.")
                .defineInRange("radius", 45, 8, 96);

        SHELL_THICKNESS = builder
                .comment("Thickness of the hull skin in blocks. 2 gives a solid-looking plated shell.")
                .defineInRange("shellThickness", 2, 1, 6);

        MAX_BLOCKS = builder
                .comment(
                        "Hard safety cap on how many blocks a single Death Star may contain.",
                        "If the configured radius would exceed this, the summon is refused so a typo",
                        "cannot lock up the server. Raise at your own (performance) risk.")
                .defineInRange("maxBlocks", 60_000, 100, 500_000);

        DROP_HEIGHT = builder
                .comment(
                        "How many blocks above the target the Death Star spawns before physics takes over.",
                        "A small drop makes it visibly settle; 0 spawns it already resting on the ground.")
                .defineInRange("dropHeight", 3, 0, 128);

        REACTOR_CORE = builder
                .comment("Place a small solid reactor core at the centre (mass anchor + looks less hollow).")
                .define("reactorCore", true);

        builder.pop();
        SPEC = builder.build();
    }

    private DeathStarConfig() {}
}
