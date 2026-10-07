package com.velocityspider.crashphysics.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-world settings, stored in {@code serverconfig/crashphysics-server.toml}.
 */
public final class CrashConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // General
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue WORLD_DAMAGE;
    public static final ModConfigSpec.BooleanValue VEHICLE_DAMAGE;

    // Physics
    public static final ModConfigSpec.DoubleValue KG_PER_SABLE_MASS;
    public static final ModConfigSpec.DoubleValue STRENGTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue VEHICLE_STRENGTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue JOINT_STRENGTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MIN_IMPACT_SPEED;
    public static final ModConfigSpec.DoubleValue ELASTIC_TRAVEL;
    public static final ModConfigSpec.DoubleValue BREAK_DEPTH;
    public static final ModConfigSpec.DoubleValue PLOW_FRICTION;

    // Structure
    public static final ModConfigSpec.BooleanValue STRUCTURAL_FAILURE;
    public static final ModConfigSpec.DoubleValue STRUCTURAL_MIN_G;
    public static final ModConfigSpec.IntValue MAX_STRUCTURAL_BLOCKS;

    // Crew
    public static final ModConfigSpec.BooleanValue CREW_INJURY;
    public static final ModConfigSpec.DoubleValue SEATED_TOLERANCE_G;
    public static final ModConfigSpec.DoubleValue STANDING_TOLERANCE_G;
    public static final ModConfigSpec.DoubleValue CREW_DAMAGE_SCALE;
    public static final ModConfigSpec.BooleanValue THROW_LOOSE_CREW;

    // Debris
    public static final ModConfigSpec.BooleanValue EJECTA;
    public static final ModConfigSpec.DoubleValue EJECTA_CHANCE;
    public static final ModConfigSpec.DoubleValue EJECTA_SPEED_FACTOR;
    public static final ModConfigSpec.IntValue MAX_FLYING_DEBRIS_PER_TICK;
    public static final ModConfigSpec.DoubleValue VEHICLE_DEBRIS_CHANCE;
    public static final ModConfigSpec.DoubleValue DROP_CHANCE;
    public static final ModConfigSpec.BooleanValue COLLAPSE;
    public static final ModConfigSpec.IntValue MAX_COLLAPSE_BLOCKS;

    // Effects
    public static final ModConfigSpec.BooleanValue SKID_MARKS;
    public static final ModConfigSpec.BooleanValue FIRE_AND_EXPLOSIONS;
    public static final ModConfigSpec.DoubleValue SOUND_VOLUME;
    public static final ModConfigSpec.BooleanValue CAMERA_SHAKE;

    // Limits
    public static final ModConfigSpec.IntValue MAX_BLOCKS_BROKEN_PER_TICK;
    public static final ModConfigSpec.IntValue DAMAGE_MEMORY_SECONDS;
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("general");
        ENABLED = BUILDER
                .comment("Master switch for crash physics")
                .define("enabled", true);
        WORLD_DAMAGE = BUILDER
                .comment("Whether crashes can break terrain and buildings (craters, trenches, holes in walls)")
                .define("worldDamage", true);
        VEHICLE_DAMAGE = BUILDER
                .comment("Whether crashes can break the vehicles themselves")
                .define("vehicleDamage", true);
        BUILDER.pop();

        BUILDER.push("physics");
        KG_PER_SABLE_MASS = BUILDER
                .comment("How many kilograms one unit of Sable block mass represents. Sable gives planks 0.5, stone 2 and",
                        "iron blocks 4, so 1000 makes block masses match real material densities.")
                .defineInRange("kgPerSableMass", 1000.0, 1.0, 100000.0);
        STRENGTH_MULTIPLIER = BUILDER
                .comment("Multiplies the crush strength of every material. Lower = everything breaks more easily")
                .defineInRange("strengthMultiplier", 1.0, 0.001, 1000.0);
        VEHICLE_STRENGTH_MULTIPLIER = BUILDER
                .comment("Extra strength multiplier for blocks that are part of a vehicle")
                .defineInRange("vehicleStrengthMultiplier", 1.0, 0.001, 1000.0);
        JOINT_STRENGTH_MULTIPLIER = BUILDER
                .comment("Multiplies how much load the connections between vehicle blocks can carry before parts tear off")
                .defineInRange("jointStrengthMultiplier", 1.0, 0.001, 1000.0);
        MIN_IMPACT_SPEED = BUILDER
                .comment("Contacts closing slower than this [m/s] are never damaging")
                .defineInRange("minImpactSpeed", 1.5, 0.1, 100.0);
        ELASTIC_TRAVEL = BUILDER
                .comment("How far [m] a contact can flex before the weaker block starts to crush. Larger = more forgiving landings")
                .defineInRange("elasticTravel", 0.02, 0.001, 1.0);
        BREAK_DEPTH = BUILDER
                .comment("Fraction of a block's depth that has to be crushed before the block breaks.",
                        "0.35 means a block breaks once a dent reaches a third of the way through it")
                .defineInRange("breakDepth", 0.35, 0.05, 1.0);
        PLOW_FRICTION = BUILDER
                .comment("Friction coefficient between a vehicle and material it is ploughing through")
                .defineInRange("plowFriction", 0.6, 0.0, 2.0);
        BUILDER.pop();

        BUILDER.push("structure");
        STRUCTURAL_FAILURE = BUILDER
                .comment("Whether crash loads can tear parts (wings, tails, masts) off vehicles even if they did not hit anything")
                .define("structuralFailure", true);
        STRUCTURAL_MIN_G = BUILDER
                .comment("Crash decelerations below this [g] never trigger a structural check")
                .defineInRange("structuralMinG", 5.0, 0.5, 1000.0);
        MAX_STRUCTURAL_BLOCKS = BUILDER
                .comment("Vehicles with more blocks than this skip the structural check (performance)")
                .defineInRange("maxStructuralBlocks", 8000, 16, 1_000_000);
        BUILDER.pop();

        BUILDER.push("crew");
        CREW_INJURY = BUILDER
                .comment("Whether riders are hurt by crash g-forces")
                .define("crewInjury", true);
        SEATED_TOLERANCE_G = BUILDER
                .comment("Deceleration a seated rider survives without injury [g]")
                .defineInRange("seatedToleranceG", 25.0, 1.0, 1000.0);
        STANDING_TOLERANCE_G = BUILDER
                .comment("Deceleration a rider standing loose survives without injury [g]")
                .defineInRange("standingToleranceG", 10.0, 1.0, 1000.0);
        CREW_DAMAGE_SCALE = BUILDER
                .comment("Damage [half hearts] dealt at twice the tolerance; it grows faster beyond that")
                .defineInRange("crewDamageScale", 8.0, 0.0, 1000.0);
        THROW_LOOSE_CREW = BUILDER
                .comment("Whether riders who are not seated keep flying when the vehicle stops suddenly")
                .define("throwLooseCrew", true);
        BUILDER.pop();

        BUILDER.push("debris");
        EJECTA = BUILDER
                .comment("Whether dirt, sand and rock dug out by a crash is thrown out as falling blocks (builds a rim around craters)")
                .define("ejecta", true);
        EJECTA_CHANCE = BUILDER
                .comment("Chance that an excavated ground block is thrown out instead of being compacted")
                .defineInRange("ejectaChance", 0.55, 0.0, 1.0);
        EJECTA_SPEED_FACTOR = BUILDER
                .comment("Fraction of the penetration speed thrown material leaves with")
                .defineInRange("ejectaSpeedFactor", 0.35, 0.0, 2.0);
        MAX_FLYING_DEBRIS_PER_TICK = BUILDER
                .comment("Maximum falling-block entities (ejecta and wreckage) spawned per tick per dimension")
                .defineInRange("maxFlyingDebrisPerTick", 48, 0, 10000);
        VEHICLE_DEBRIS_CHANCE = BUILDER
                .comment("Chance that a vehicle block broken in a crash flies off as wreckage instead of being pulverised")
                .defineInRange("vehicleDebrisChance", 0.4, 0.0, 1.0);
        DROP_CHANCE = BUILDER
                .comment("Chance that a pulverised block still drops its item")
                .defineInRange("dropChance", 0.2, 0.0, 1.0);
        COLLAPSE = BUILDER
                .comment("Whether parts of buildings and trees that are cut loose by a crash fall down as physics objects")
                .define("collapse", true);
        MAX_COLLAPSE_BLOCKS = BUILDER
                .comment("The largest loose section [blocks] that can collapse. Anything bigger is assumed to be held up")
                .defineInRange("maxCollapseBlocks", 800, 1, 100_000);
        BUILDER.pop();

        BUILDER.push("effects");
        SKID_MARKS = BUILDER
                .comment("Whether vehicles sliding over grass, snow and dirt leave tracks")
                .define("skidMarks", true);
        FIRE_AND_EXPLOSIONS = BUILDER
                .comment("Whether destroyed burners, engines and other volatile blocks explode or start fires")
                .define("fireAndExplosions", true);
        SOUND_VOLUME = BUILDER
                .comment("Volume multiplier for crash sounds")
                .defineInRange("soundVolume", 1.0, 0.0, 10.0);
        CAMERA_SHAKE = BUILDER
                .comment("Whether big impacts shake the camera of nearby players (clients can turn it off for themselves too)")
                .define("cameraShake", true);
        BUILDER.pop();

        BUILDER.push("limits");
        MAX_BLOCKS_BROKEN_PER_TICK = BUILDER
                .comment("Maximum blocks broken by crashes per tick per dimension")
                .defineInRange("maxBlocksBrokenPerTick", 600, 1, 100_000);
        DAMAGE_MEMORY_SECONDS = BUILDER
                .comment("How long partial damage (cracks) is remembered on a block after its last hit [s]")
                .defineInRange("damageMemorySeconds", 90, 1, 86_400);
        DEBUG_LOGGING = BUILDER
                .comment("Logs every damaging impact (noisy)")
                .define("debugLogging", false);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private CrashConfig() {
    }

    /**
     * @return if the config has been loaded (server configs only exist while a world is running)
     */
    public static boolean isLoaded() {
        return SPEC.isLoaded();
    }
}
