package net.mcreator.crustychunks.compat;

import net.mcreator.crustychunks.WariumConfig;
import net.mcreator.crustychunks.WariumConfig.EffectQuality;
import net.mcreator.crustychunks.network.ScreenShakeMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Integration facade between Warium's 1.3.0 explosion pipeline and the port's
 * config/safety/physics layers.
 *
 * <p>Since 1.3.0 the mod ships its own staged explosion system: the server runs
 * {@code WariumExplosionServerProcedure} / {@code WariumNuclearExplosionShapesProcedure}
 * for damage and scorching, then broadcasts a {@code ClientExplosionPacket}; each
 * client renders the fireball/shock-ring/mushroom particles locally
 * ({@code WariumExplosionClientProcedure}, {@code NuclearEffectProcedure}) plus the
 * sky-glare pass in {@code FireBallRenderProcedure}. This class no longer spawns
 * visuals itself - it feeds that pipeline with:</p>
 * <ul>
 *   <li>screen shake broadcast to nearby players (config gated),</li>
 *   <li>physics impulses for Create Aeronautics / Sable constructs,</li>
 *   <li>the configured blast radius multiplier,</li>
 *   <li>client particle budgets derived from the configured quality.</li>
 * </ul>
 *
 * <p>The {@link Profile} entry points are kept as a stable API: they trigger the
 * matching 1.3.0 detonation with a profile-appropriate power.</p>
 */
public final class WariumNukeEffects {
	/** Per-weapon detonation profile mapped onto the 1.3.0 explosion pipeline. */
	public enum Profile {
		SMALL_TACTICAL(25.0, true),
		STANDARD(50.0, true),
		HIGH_YIELD(100.0, true),
		DIRTY(45.0, true),
		THERMOBARIC(40.0, false);

		public final double power;
		public final boolean nuclear;

		Profile(double power, boolean nuclear) {
			this.power = power;
			this.nuclear = nuclear;
		}
	}

	private WariumNukeEffects() {
	}

	/** Triggers a full detonation (damage + staged visuals) through the 1.3.0 pipeline. */
	public static void play(LevelAccessor world, double x, double y, double z, Profile profile) {
		try {
			if (world == null || world.isClientSide() || profile == null)
				return;
			double power = profile.power * blastRadiusMultiplier();
			if (profile.nuclear)
				net.mcreator.crustychunks.procedures.NuclearExplosionExampleProcedure.execute(world, x, y, z, power);
			else
				net.mcreator.crustychunks.procedures.ExplosionExampleProcedure.execute(world, x, y, z, power);
		} catch (Throwable t) {
			WariumSafety.report("WariumNukeEffects.play", t);
		}
	}

	/**
	 * Server-side hook run for every 1.3.0 explosion: screen shake for nearby
	 * players and physics impulse for nearby constructs. Never throws.
	 */
	public static void onServerExplosion(LevelAccessor world, double x, double y, double z, double power, boolean nuclear) {
		try {
			if (!(world instanceof ServerLevel level))
				return;
			Vec3 center = new Vec3(x, y, z);
			double radius = Math.max(4.0, Math.sqrt(Math.max(0.0, power)) * (nuclear ? 24.0 : 8.0));
			if (WariumConfig.NUKE_SCREEN_SHAKE.get()) {
				float intensity = (float) Math.min(nuclear ? 2.5 : 1.2, 0.08 * Math.sqrt(Math.max(0.0, power)) * (nuclear ? 3.0 : 1.0));
				int duration = nuclear ? 90 : 30;
				double reachSqr = radius * 6.0 * (radius * 6.0);
				for (ServerPlayer player : level.players()) {
					if (player.distanceToSqr(x, y, z) <= reachSqr)
						PacketDistributor.sendToPlayer(player, new ScreenShakeMessage(intensity, duration));
				}
			}
			AeronauticsCompat.applyExplosionImpulse(level, center, radius, power * 120.0);
		} catch (Throwable t) {
			WariumSafety.report("WariumNukeEffects.onServerExplosion", t);
		}
	}

	/** Configured blast radius multiplier for nuclear detonations (1.0 default). */
	public static double blastRadiusMultiplier() {
		try {
			return WariumConfig.NUKE_BLAST_RADIUS_MULTIPLIER.get();
		} catch (Throwable t) {
			return 1.0;
		}
	}

	/** Quality-derived multiplier applied to client particle counts. */
	public static double clientQualityMultiplier() {
		try {
			EffectQuality q = WariumConfig.NUKE_EFFECTS_QUALITY.get();
			return switch (q) {
				case LOW -> 0.35;
				case MEDIUM -> 0.7;
				case HIGH -> 1.0;
				case CINEMATIC -> 1.6;
			};
		} catch (Throwable t) {
			return 1.0;
		}
	}

	/** Clamp a requested client particle count to the quality-scaled configured budget. */
	public static int clientParticleBudget(int requested) {
		try {
			int cap = WariumConfig.MAX_NUKE_PARTICLES.get();
			int scaled = (int) Math.round(requested * clientQualityMultiplier());
			return Math.max(1, Math.min(cap, scaled));
		} catch (Throwable t) {
			return Math.max(1, requested);
		}
	}

	public static boolean shockwaveEnabled() {
		try {
			return WariumConfig.NUKE_SHOCKWAVE.get();
		} catch (Throwable t) {
			return true;
		}
	}

	public static boolean mushroomEnabled() {
		try {
			return WariumConfig.NUKE_MUSHROOM_CLOUD.get();
		} catch (Throwable t) {
			return true;
		}
	}
}
