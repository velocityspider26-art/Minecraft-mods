package com.velocityspider.crashphysics.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-player settings, stored in {@code config/crashphysics-client.toml}.
 */
public final class CrashClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CAMERA_SHAKE = BUILDER
            .comment("Whether nearby crashes shake your camera")
            .define("cameraShake", true);

    public static final ModConfigSpec.DoubleValue CAMERA_SHAKE_STRENGTH = BUILDER
            .comment("Camera shake strength multiplier")
            .defineInRange("cameraShakeStrength", 1.0, 0.0, 5.0);

    public static final ModConfigSpec.BooleanValue WATER_WAVES = BUILDER
            .comment("Whether splashes, ship wakes and anything moving through water make waves on the surface")
            .define("waterWaves", true);

    public static final ModConfigSpec.IntValue WAVE_RANGE = BUILDER
            .comment("How far from you water waves are simulated and drawn [blocks]")
            .defineInRange("waveRange", 96, 16, 256);

    public static final ModConfigSpec.DoubleValue WAVE_STRENGTH = BUILDER
            .comment("Wave height multiplier")
            .defineInRange("waveStrength", 1.0, 0.0, 4.0);

    public static final ModConfigSpec.DoubleValue PARTICLE_AMOUNT = BUILDER
            .comment("Multiplier for the number of crash particles (sparks, spray, dust, smoke)")
            .defineInRange("particleAmount", 1.0, 0.0, 4.0);

    public static final ModConfigSpec.BooleanValue SOUND_DELAY = BUILDER
            .comment("Whether far-away crashes are heard late, as sound takes time to travel (343 m/s)")
            .define("soundDelay", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CrashClientConfig() {
    }
}
