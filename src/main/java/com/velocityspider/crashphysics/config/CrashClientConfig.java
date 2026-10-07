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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CrashClientConfig() {
    }
}
