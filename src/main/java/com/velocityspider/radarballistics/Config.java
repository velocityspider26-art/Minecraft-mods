package com.velocityspider.radarballistics;

import com.velocityspider.radarballistics.ballistics.BallisticSolver;
import com.velocityspider.radarballistics.ballistics.ProjectileProfile;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for the ballistics fire-control engine.
 *
 * <p>The projectile-model values (muzzle speed, gravity, drag) describe the shell the solver
 * should aim; tune them to match the Create: Big Cannons projectile you fire. The advanced
 * values control the solver's convergence and rarely need changing.</p>
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_AUTO_TRACK_LEAD = BUILDER
            .comment("Master switch for the leading/ballistics correction on auto-tracking cannons.",
                    "When false the tracking behaves like vanilla (aims at the target's current position).")
            .define("enableAutoTrackLead", true);

    public static final ModConfigSpec.DoubleValue MUZZLE_SPEED = BUILDER
            .comment("Projectile muzzle speed in blocks per tick (20 ticks = 1 second).",
                    "Match this to the shell your cannon fires; more propellant charges = faster shell.")
            .defineInRange("projectile.muzzleSpeedBlocksPerTick", 8.0, 0.01, 100.0);

    public static final ModConfigSpec.DoubleValue GRAVITY = BUILDER
            .comment("Downward acceleration applied to the projectile each tick, in blocks/tick^2.")
            .defineInRange("projectile.gravityBlocksPerTickSquared", 0.05, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue DRAG = BUILDER
            .comment("Per-tick velocity multiplier modelling air resistance. 1.0 = no drag.")
            .defineInRange("projectile.dragPerTick", 0.99, 0.5, 1.0);

    public static final ModConfigSpec.BooleanValue LEAD_MOVING_TARGETS = BUILDER
            .comment("Predict target motion (velocity + acceleration) when aiming.",
                    "This is the core fix: aim where the target WILL be, not where it is now.")
            .define("leadMovingTargets", true);

    public static final ModConfigSpec.IntValue MAX_ITERATIONS = BUILDER
            .comment("[Advanced] Maximum lead/time-of-flight refinement iterations before giving up.")
            .defineInRange("solver.maxIterations", 48, 4, 500);

    public static final ModConfigSpec.DoubleValue TIME_TOLERANCE = BUILDER
            .comment("[Advanced] Convergence tolerance on the time-of-flight estimate, in ticks.")
            .defineInRange("solver.timeToleranceTicks", 0.25, 0.001, 5.0);

    public static final ModConfigSpec.IntValue MAX_FLIGHT_TICKS = BUILDER
            .comment("[Advanced] Hard cap on simulated shell flight time; also the out-of-range guard.")
            .defineInRange("solver.maxFlightTicks", 20 * 60, 20, 20 * 600);

    public static final ModConfigSpec.DoubleValue LEAD_DAMPING = BUILDER
            .comment("[Advanced] Relaxation factor for the lead fixed-point iteration in (0,1].",
                    "Lower is more stable but converges slower.")
            .defineInRange("solver.leadDamping", 0.6, 0.05, 1.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    /** Builds the projectile flight model from the current config values. */
    public static ProjectileProfile projectileProfile() {
        return new ProjectileProfile(MUZZLE_SPEED.get(), GRAVITY.get(), DRAG.get());
    }

    /** Builds a solver configured from the current advanced config values. */
    public static BallisticSolver solver() {
        return new BallisticSolver(
                MAX_ITERATIONS.get(),
                TIME_TOLERANCE.get(),
                MAX_FLIGHT_TICKS.get(),
                LEAD_DAMPING.get());
    }
}
