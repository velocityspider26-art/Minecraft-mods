package net.mcreator.crustychunks.compat;

import net.mcreator.crustychunks.WariumConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Central entry point for every explosion Warium creates. Applies the
 * configured radius cap and block-damage policy, then transfers impulse to
 * nearby Sable / Create: Aeronautics constructs.
 */
public final class WariumExplosions {
	private WariumExplosions() {
	}

	public static void explode(Level level, Entity source, double x, double y, double z, float radius, Level.ExplosionInteraction interaction) {
		explode(level, source, x, y, z, radius, false, interaction);
	}

	public static void explode(Level level, Entity source, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction interaction) {
		if (level.isClientSide())
			return;
		float clamped = (float) Math.min(radius, WariumConfig.MAX_EXPLOSION_RADIUS.get());
		Level.ExplosionInteraction effective = WariumConfig.GUN_EXPLOSIONS_BREAK_BLOCKS.get() ? interaction : Level.ExplosionInteraction.NONE;
		level.explode(source, x, y, z, clamped, fire, effective);
		AeronauticsCompat.applyExplosionImpulse(level, new Vec3(x, y, z), clamped * 2.0, clamped * 250.0);
	}
}
