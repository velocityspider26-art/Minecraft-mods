package net.mcreator.crustychunks;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Safety and integration limits for Warium weapons.
 */
public final class WariumConfig {
	public static final ModConfigSpec SPEC;

	public static final ModConfigSpec.IntValue PROJECTILE_LIFETIME_TICKS;
	public static final ModConfigSpec.IntValue MAX_ACTIVE_PROJECTILES;
	public static final ModConfigSpec.DoubleValue MAX_PROJECTILE_SPEED;
	public static final ModConfigSpec.DoubleValue MAX_EXPLOSION_RADIUS;
	public static final ModConfigSpec.BooleanValue GUN_EXPLOSIONS_BREAK_BLOCKS;
	public static final ModConfigSpec.BooleanValue AFFECT_PHYSICS_CONSTRUCTS;
	public static final ModConfigSpec.DoubleValue CONSTRUCT_IMPULSE_SCALE;
	public static final ModConfigSpec.BooleanValue INHERIT_VEHICLE_VELOCITY;

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
		SPEC = b.build();
	}

	private WariumConfig() {
	}
}
