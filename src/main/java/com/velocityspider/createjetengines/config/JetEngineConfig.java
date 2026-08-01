package com.velocityspider.createjetengines.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Server-authoritative tuning for the thrust model.
 *
 * <p>All physics-relevant numbers live here so the whole model can be retuned without a rebuild.
 * Thrust is expressed in Sable force units per module; the physics body's own mass decides the
 * resulting acceleration, so nothing here assumes anything about aircraft mass.
 */
public final class JetEngineConfig {

    public static final ModConfigSpec SPEC;
    public static final JetEngineConfig INSTANCE;

    public final ModConfigSpec.DoubleValue baseThrust;
    public final ModConfigSpec.DoubleValue maxForcePerEngine;
    public final ModConfigSpec.DoubleValue compressorStageMultiplier;
    public final ModConfigSpec.IntValue maxCompressorStages;
    public final ModConfigSpec.DoubleValue afterburnerMultiplier;
    public final ModConfigSpec.DoubleValue afterburnerMinSpool;
    public final ModConfigSpec.DoubleValue spoolUpRate;
    public final ModConfigSpec.DoubleValue spoolDownRate;
    public final ModConfigSpec.DoubleValue idleSpool;
    public final ModConfigSpec.DoubleValue throttleSlewRate;
    public final ModConfigSpec.DoubleValue obstructedIntakeEfficiency;
    public final ModConfigSpec.DoubleValue obstructedExhaustEfficiency;
    public final ModConfigSpec.BooleanValue useAtmosphericEfficiency;
    public final ModConfigSpec.DoubleValue minAtmosphericEfficiency;
    public final ModConfigSpec.BooleanValue useRamEfficiency;
    public final ModConfigSpec.DoubleValue maxAirspeed;
    public final ModConfigSpec.DoubleValue fuelBurnPerTick;
    public final ModConfigSpec.BooleanValue logPropulsionDebug;

    private JetEngineConfig(ModConfigSpec.Builder b) {
        b.comment("Thrust model").push("thrust");

        baseThrust = b.comment(
                        "Base dry thrust of a minimal valid engine (fan + 1 compressor + core + nozzle),",
                        "in Sable force units, at full throttle and full spool.")
                .defineInRange("baseThrust", 2400.0D, 0.0D, 1_000_000.0D);

        maxForcePerEngine = b.comment(
                        "Hard clamp on the force a single engine may apply in one physics step.",
                        "This is a safety rail against physics blow-ups; leave it generous but finite.")
                .defineInRange("maxForcePerEngine", 40_000.0D, 1.0D, 10_000_000.0D);

        compressorStageMultiplier = b.comment(
                        "Thrust gain per compressor stage, with diminishing returns.",
                        "Multiplier = 1 + m*(1 - 1/stages) style curve; see JetThrustModel.")
                .defineInRange("compressorStageMultiplier", 0.55D, 0.0D, 4.0D);

        maxCompressorStages = b.comment("Maximum usable compressor stages in one chain.")
                .defineInRange("maxCompressorStages", 4, 1, 8);

        afterburnerMultiplier = b.comment("Thrust multiplier while the afterburner is lit.")
                .defineInRange("afterburnerMultiplier", 1.6D, 1.0D, 5.0D);

        afterburnerMinSpool = b.comment("Dry spool fraction the engine must reach before the afterburner can light.")
                .defineInRange("afterburnerMinSpool", 0.80D, 0.0D, 1.0D);

        b.pop();
        b.comment("Spool behaviour").push("spool");

        spoolUpRate = b.comment("Spool fraction gained per tick at full throttle demand.")
                .defineInRange("spoolUpRate", 0.012D, 0.0001D, 1.0D);

        spoolDownRate = b.comment("Spool fraction lost per tick with no throttle demand.")
                .defineInRange("spoolDownRate", 0.008D, 0.0001D, 1.0D);

        idleSpool = b.comment("Spool fraction the engine settles at while running but at zero throttle.")
                .defineInRange("idleSpool", 0.18D, 0.0D, 1.0D);

        throttleSlewRate = b.comment("Maximum throttle change per tick; smooths abrupt redstone changes.")
                .defineInRange("throttleSlewRate", 0.05D, 0.001D, 1.0D);

        b.pop();
        b.comment("Efficiency").push("efficiency");

        obstructedIntakeEfficiency = b.comment("Thrust multiplier when the intake is blocked.")
                .defineInRange("obstructedIntakeEfficiency", 0.15D, 0.0D, 1.0D);

        obstructedExhaustEfficiency = b.comment("Thrust multiplier when the nozzle exhaust is blocked.")
                .defineInRange("obstructedExhaustEfficiency", 0.0D, 0.0D, 1.0D);

        useAtmosphericEfficiency = b.comment(
                        "Scale thrust by Sable's dimension air pressure at the engine's world position.",
                        "A jet needs air; this makes high-altitude and low-pressure dimensions weaker.")
                .define("useAtmosphericEfficiency", true);

        minAtmosphericEfficiency = b.comment("Floor for the atmospheric term so engines never hard-cut to zero.")
                .defineInRange("minAtmosphericEfficiency", 0.05D, 0.0D, 1.0D);

        useRamEfficiency = b.comment(
                        "Reduce thrust as the aircraft approaches the engine's exhaust velocity,",
                        "which gives a natural top speed instead of unbounded acceleration.")
                .define("useRamEfficiency", true);

        maxAirspeed = b.comment("Airspeed (blocks/second) at which dry thrust falls to zero.")
                .defineInRange("maxAirspeed", 120.0D, 1.0D, 10_000.0D);

        b.pop();
        b.comment("Misc").push("misc");

        fuelBurnPerTick = b.comment("Reserved for future fuel integration; currently informational only.")
                .defineInRange("fuelBurnPerTick", 0.0D, 0.0D, 1000.0D);

        logPropulsionDebug = b.comment(
                        "Log actor registration, sublevel id, force vector, application point and throttle",
                        "once per second per engine. Use this when diagnosing propulsion problems.")
                .define("logPropulsionDebug", false);

        b.pop();
    }

    static {
        Pair<JetEngineConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(JetEngineConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }
}
