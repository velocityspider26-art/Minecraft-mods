package net.mcreator.crustychunks;

import java.util.Locale;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Safety, targeting and effect limits for Warium weapons.
 */
public final class WariumConfig {
	public enum EffectQuality {
		LOW, MEDIUM, HIGH, CINEMATIC
	}

	public static final ModConfigSpec SPEC;

	// --- projectiles ---
	public static final ModConfigSpec.IntValue PROJECTILE_LIFETIME_TICKS;
	public static final ModConfigSpec.IntValue MAX_ACTIVE_PROJECTILES;
	public static final ModConfigSpec.DoubleValue MAX_PROJECTILE_SPEED;
	public static final ModConfigSpec.BooleanValue INHERIT_VEHICLE_VELOCITY;
	public static final ModConfigSpec.IntValue SELF_COLLISION_GRACE_TICKS;

	// --- explosions ---
	public static final ModConfigSpec.DoubleValue MAX_EXPLOSION_RADIUS;
	public static final ModConfigSpec.BooleanValue GUN_EXPLOSIONS_BREAK_BLOCKS;

	// --- aeronautics ---
	public static final ModConfigSpec.BooleanValue AFFECT_PHYSICS_CONSTRUCTS;
	public static final ModConfigSpec.DoubleValue CONSTRUCT_IMPULSE_SCALE;

	// --- radar ---
	public static final ModConfigSpec.DoubleValue RADAR_RANGE;
	public static final ModConfigSpec.IntValue RADAR_SCAN_RATE;
	public static final ModConfigSpec.IntValue RADAR_LOCK_TIME;
	public static final ModConfigSpec.IntValue RADAR_MAX_TARGETS;
	public static final ModConfigSpec.IntValue RADAR_LOST_GRACE;
	public static final ModConfigSpec.BooleanValue RADAR_REQUIRES_LINE_OF_SIGHT;
	public static final ModConfigSpec.BooleanValue RADAR_CAN_TRACK_PLAYERS;
	public static final ModConfigSpec.BooleanValue RADAR_CAN_TRACK_AIRCRAFT;
	public static final ModConfigSpec.BooleanValue RADAR_CAN_TRACK_PROJECTILES;

	// --- laser designator ---
	public static final ModConfigSpec.DoubleValue LASER_RANGE;
	public static final ModConfigSpec.IntValue LASER_GUIDANCE_GRACE_TICKS;
	public static final ModConfigSpec.BooleanValue LASER_REQUIRES_LINE_OF_SIGHT;
	public static final ModConfigSpec.BooleanValue LASER_CAN_DESIGNATE_ENTITIES;
	public static final ModConfigSpec.BooleanValue LASER_CAN_DESIGNATE_BLOCKS;
	public static final ModConfigSpec.IntValue LASER_UPDATE_RATE;

	// --- missiles ---
	public static final ModConfigSpec.DoubleValue MISSILE_MAX_TURN_RATE;
	public static final ModConfigSpec.BooleanValue MISSILE_LAUNCH_UNGUIDED_WITHOUT_LOCK;

	// --- nuke effects ---
	public static final ModConfigSpec.EnumValue<EffectQuality> NUKE_EFFECTS_QUALITY;
	public static final ModConfigSpec.BooleanValue NUKE_SCREEN_SHAKE;
	public static final ModConfigSpec.BooleanValue NUKE_MUSHROOM_CLOUD;
	public static final ModConfigSpec.BooleanValue NUKE_SHOCKWAVE;
	public static final ModConfigSpec.IntValue MAX_NUKE_PARTICLES;
	public static final ModConfigSpec.IntValue MAX_NUKE_BLOCK_DESTRUCTION;
	public static final ModConfigSpec.DoubleValue NUKE_BLAST_RADIUS_MULTIPLIER;
	public static final ModConfigSpec.BooleanValue NUKE_RADIATION_EFFECTS;

	static {
		ModConfigSpec.Builder b = new ModConfigSpec.Builder();

		b.push("projectiles");
		PROJECTILE_LIFETIME_TICKS = b.comment("Maximum lifetime of Warium projectiles in ticks before they are discarded.")
				.defineInRange("projectileLifetimeTicks", 1200, 100, 72000);
		MAX_ACTIVE_PROJECTILES = b.comment("Maximum number of Warium projectiles allowed per level. Additional spawns are discarded.")
				.defineInRange("maxActiveProjectiles", 512, 32, 8192);
		MAX_PROJECTILE_SPEED = b.comment("Maximum projectile speed in blocks per tick. Faster projectiles are clamped.")
				.defineInRange("maxProjectileSpeed", 20.0D, 4.0D, 1000.0D);
		INHERIT_VEHICLE_VELOCITY = b.comment("Whether projectiles inherit the velocity of the shooter's vehicle or physics construct.")
				.define("inheritVehicleVelocity", true);
		SELF_COLLISION_GRACE_TICKS = b.comment("Ticks after firing during which a projectile ignores its own launch platform / shooter.")
				.defineInRange("selfCollisionGraceTicks", 6, 0, 100);
		b.pop();

		b.push("explosions");
		MAX_EXPLOSION_RADIUS = b.comment("Hard cap for the radius of any single Warium explosion.")
				.defineInRange("maxExplosionRadius", 100.0D, 1.0D, 500.0D);
		GUN_EXPLOSIONS_BREAK_BLOCKS = b.comment("Whether Warium weapon explosions can destroy blocks. When false, explosions still damage entities.")
				.define("gunExplosionsBreakBlocks", true);
		b.pop();

		b.push("aeronautics");
		AFFECT_PHYSICS_CONSTRUCTS = b.comment("Whether Warium explosions and recoil apply forces to Sable / Create: Aeronautics physics constructs.")
				.define("affectPhysicsConstructs", true);
		CONSTRUCT_IMPULSE_SCALE = b.comment("Scale factor for forces applied to physics constructs by explosions and recoil.")
				.defineInRange("constructImpulseScale", 1.0D, 0.0D, 100.0D);
		b.pop();

		b.push("radar");
		RADAR_RANGE = b.comment("Maximum radar detection range in blocks.").defineInRange("radarRange", 300.0D, 16.0D, 4096.0D);
		RADAR_SCAN_RATE = b.comment("Ticks between radar scans.").defineInRange("radarScanRate", 10, 1, 200);
		RADAR_LOCK_TIME = b.comment("Ticks a target must be tracked before a hard lock is achieved.").defineInRange("radarLockTime", 40, 0, 600);
		RADAR_MAX_TARGETS = b.comment("Maximum number of simultaneous tracked targets per radar.").defineInRange("radarMaxTargets", 16, 1, 128);
		RADAR_LOST_GRACE = b.comment("Ticks a lock is retained after the target is no longer visible before it breaks.").defineInRange("radarLostGraceTicks", 40, 0, 600);
		RADAR_REQUIRES_LINE_OF_SIGHT = b.comment("Whether radar requires unobstructed line of sight to lock.").define("radarRequiresLineOfSight", false);
		RADAR_CAN_TRACK_PLAYERS = b.define("radarCanTrackPlayers", true);
		RADAR_CAN_TRACK_AIRCRAFT = b.define("radarCanTrackAircraft", true);
		RADAR_CAN_TRACK_PROJECTILES = b.define("radarCanTrackProjectiles", false);
		b.pop();

		b.push("laserDesignator");
		LASER_RANGE = b.comment("Maximum laser designation range in blocks.").defineInRange("laserDesignatorRange", 256.0D, 16.0D, 2048.0D);
		LASER_GUIDANCE_GRACE_TICKS = b.comment("Ticks a laser-guided missile keeps its last designation after the laser is lost.").defineInRange("laserGuidanceGraceTicks", 30, 0, 600);
		LASER_REQUIRES_LINE_OF_SIGHT = b.define("laserRequiresLineOfSight", true);
		LASER_CAN_DESIGNATE_ENTITIES = b.define("laserCanDesignateEntities", true);
		LASER_CAN_DESIGNATE_BLOCKS = b.define("laserCanDesignateBlocks", true);
		LASER_UPDATE_RATE = b.comment("Ticks between laser designation refreshes while held.").defineInRange("laserUpdateRate", 2, 1, 40);
		b.pop();

		b.push("missiles");
		MISSILE_MAX_TURN_RATE = b.comment("Maximum guidance turn rate in radians per tick.").defineInRange("missileMaxTurnRate", 0.18D, 0.01D, 3.14159D);
		MISSILE_LAUNCH_UNGUIDED_WITHOUT_LOCK = b.comment("If true, guided missiles launch unguided when no valid lock/designation exists; if false they refuse guidance.")
				.define("missileLaunchUnguidedWithoutLock", true);
		b.pop();

		b.push("nukeEffects");
		NUKE_EFFECTS_QUALITY = b.comment("Visual quality of nuke effects: LOW, MEDIUM, HIGH, CINEMATIC.").defineEnum("nukeEffectsQuality", EffectQuality.HIGH);
		NUKE_SCREEN_SHAKE = b.define("enableNukeScreenShake", true);
		NUKE_MUSHROOM_CLOUD = b.define("enableNukeMushroomCloud", true);
		NUKE_SHOCKWAVE = b.define("enableNukeShockwave", true);
		MAX_NUKE_PARTICLES = b.comment("Maximum particles a single nuke stage may spawn on the client.").defineInRange("maxNukeParticles", 2000, 100, 20000);
		MAX_NUKE_BLOCK_DESTRUCTION = b.comment("Advisory cap on blocks a nuke may destroy (enforced by explosion radius cap).").defineInRange("maxNukeBlockDestruction", 200000, 0, 5000000);
		NUKE_BLAST_RADIUS_MULTIPLIER = b.comment("Multiplier applied to nuke blast radii.").defineInRange("nukeBlastRadiusMultiplier", 1.0D, 0.1D, 10.0D);
		NUKE_RADIATION_EFFECTS = b.define("nukeRadiationEffects", true);
		b.pop();

		SPEC = b.build();
	}

	private WariumConfig() {
	}

	public static String qualityName() {
		return NUKE_EFFECTS_QUALITY.get().name().toLowerCase(Locale.ROOT);
	}
}
