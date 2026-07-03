package com.example.examplemod.content.thruster;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Thruster tuning and plume-quality configuration. Kept separate from the block entity so values can
 * be adjusted without touching logic.
 */
public final class ThrusterConfig {
    public enum PlumeQuality { LOW, MEDIUM, HIGH, CINEMATIC }

    public static final ModConfigSpec SPEC;

    // Physics tuning
    public static final ModConfigSpec.DoubleValue BASE_THRUST;
    public static final ModConfigSpec.DoubleValue THRUST_RAMP_RATE;
    public static final ModConfigSpec.DoubleValue NOZZLE_OFFSET;

    // Plume visuals
    public static final ModConfigSpec.EnumValue<PlumeQuality> PLUME_QUALITY;
    public static final ModConfigSpec.BooleanValue ENABLE_HEAT_SHIMMER;
    public static final ModConfigSpec.BooleanValue ENABLE_SHOCK_DIAMONDS;
    public static final ModConfigSpec.IntValue MAX_THRUSTER_PARTICLES;
    public static final ModConfigSpec.DoubleValue PLUME_RENDER_DISTANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("physics");
        BASE_THRUST = b.comment("Base thrust impulse a single thruster applies at full throttle with a 1.0x fuel (Sable impulse units). Tune to taste.")
                .defineInRange("baseThrust", 60.0, 0.0, 100000.0);
        THRUST_RAMP_RATE = b.comment("How quickly thrust spools toward its target each tick (0..1). Prevents physics spikes.")
                .defineInRange("thrustRampRate", 0.15, 0.01, 1.0);
        NOZZLE_OFFSET = b.comment("How far along the thrust axis, from block centre, the force is applied (blocks).")
                .defineInRange("nozzleOffset", 0.5, 0.0, 4.0);
        b.pop();

        b.push("plume");
        PLUME_QUALITY = b.comment("Overall plume detail: LOW, MEDIUM, HIGH, CINEMATIC.")
                .defineEnum("plumeQuality", PlumeQuality.HIGH);
        ENABLE_HEAT_SHIMMER = b.comment("Render the faint outer heat-haze layer.")
                .define("enableHeatShimmer", true);
        ENABLE_SHOCK_DIAMONDS = b.comment("Render mach-diamond shock nodes for high-energy fuels.")
                .define("enableShockDiamonds", true);
        MAX_THRUSTER_PARTICLES = b.comment("Reserved: cap on particle spawns per thruster (mesh plume ignores this).")
                .defineInRange("maxThrusterParticles", 64, 0, 4096);
        PLUME_RENDER_DISTANCE = b.comment("Max distance (blocks) at which plumes render.")
                .defineInRange("plumeRenderDistance", 96.0, 8.0, 512.0);
        b.pop();

        SPEC = b.build();
    }

    private ThrusterConfig() {
    }

    /** Number of concentric mesh layers to draw for the current quality setting. */
    public static int layerCountForQuality() {
        return switch (PLUME_QUALITY.get()) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CINEMATIC -> 4;
        };
    }
}
