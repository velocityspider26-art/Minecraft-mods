package net.mcreator.crustychunks.compat;

import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.WariumConfig;
import net.mcreator.crustychunks.WariumConfig.EffectQuality;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.mcreator.crustychunks.network.ScreenShakeMessage;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Upgraded, staged, cinematic nuke visuals. This is the VISUAL layer only:
 * damage, block destruction and fallout gameplay stay in the existing explosion
 * procedures (server authority). Here we schedule layered particle "stages" over
 * several ticks so a detonation reads as flash -> fireball -> shockwave -> dust
 * wall -> mushroom column -> rolling smoke -> settling, instead of one flat
 * puff. Particles are emitted with {@link ServerLevel#sendParticles}, which the
 * server distance-culls per client, and every stage is capped and scaled by the
 * configured quality so it cannot destroy FPS or run on a client that is far
 * away. A separate client-only screen-shake payload is broadcast to nearby
 * players.
 */
public final class WariumNukeEffects {
	/** Per-weapon visual profile. */
	public enum Profile {
		//        fireball flash  smoke  dust  shake  mushroom  fireCol        smokeCol
		SMALL_TACTICAL(4.0, 1.0, 1.0, 0.8, 2.0, 0.7, "huge_fireball", "huge_smoke"),
		STANDARD(7.0, 1.4, 1.6, 1.4, 3.5, 1.0, "huge_fireball", "huge_smoke"),
		HIGH_YIELD(11.0, 1.8, 2.4, 2.2, 6.0, 1.6, "huge_static_fireball", "ground_huge_smoke"),
		DIRTY(6.0, 1.2, 3.0, 2.6, 3.0, 1.1, "fusion_fireball", "gas_cloud"),
		THERMOBARIC(9.0, 1.5, 2.0, 3.2, 4.5, 0.4, "fusion_fireball", "huge_smoke");

		final double fireball;
		final double flash;
		final double smoke;
		final double dust;
		final double shake;
		final double mushroom;
		final String fireParticle;
		final String smokeParticle;

		Profile(double fireball, double flash, double smoke, double dust, double shake, double mushroom, String fireParticle, String smokeParticle) {
			this.fireball = fireball;
			this.flash = flash;
			this.smoke = smoke;
			this.dust = dust;
			this.shake = shake;
			this.mushroom = mushroom;
			this.fireParticle = fireParticle;
			this.smokeParticle = smokeParticle;
		}
	}

	private WariumNukeEffects() {
	}

	/** Kicks off the staged visual sequence for a detonation. Safe on dedicated servers (no client classes). */
	public static void play(net.minecraft.world.level.LevelAccessor levelAccessor, double x, double y, double z, Profile profile) {
		if (!(levelAccessor instanceof Level level))
			return;
		if (!(level instanceof ServerLevel server) || profile == null)
			return;
		double qMul = qualityMultiplier();
		if (qMul <= 0.0)
			return;
		Vec3 center = new Vec3(x, y, z);

		// Stage 0 (tick 0): blinding flash + ignition core + layered boom.
		stageFlash(server, center, profile, qMul);
		playSound(server, center, "fissionblast", 20.0F, 0.8F);
		playSound(server, center, "farblast", 30.0F, 0.7F);
		broadcastShake(server, center, profile);

		// Stage 1 (tick ~2): expanding fireball.
		schedule(2, () -> stageFireball(server, center, profile, qMul));
		// Stage 2 (tick ~4): ground shockwave ring + dust wall.
		if (WariumConfig.NUKE_SHOCKWAVE.get())
			schedule(4, () -> stageShockwave(server, center, profile, qMul));
		schedule(6, () -> stageDustWall(server, center, profile, qMul));
		// Stage 3 (tick ~10-16): rising mushroom column.
		if (WariumConfig.NUKE_MUSHROOM_CLOUD.get()) {
			schedule(10, () -> stageMushroomStem(server, center, profile, qMul));
			schedule(16, () -> stageMushroomCap(server, center, profile, qMul));
		}
		// Stage 4 (tick ~28): rolling smoke + delayed rumble.
		schedule(28, () -> {
			stageSmokeColumn(server, center, profile, qMul);
			playSound(server, center, "rumble", 40.0F, 0.6F);
		});
		// Stage 5 (tick ~50): settling ash / lingering fallout.
		if (WariumConfig.NUKE_RADIATION_EFFECTS.get())
			schedule(50, () -> stageFallout(server, center, profile, qMul));
	}

	// ---- stages ----

	private static void stageFlash(ServerLevel level, Vec3 c, Profile p, double q) {
		int n = cap((int) (60 * p.flash * q));
		level.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
		level.sendParticles(named(p.fireParticle, ParticleTypes.EXPLOSION_EMITTER), c.x, c.y, c.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, n, p.fireball * 0.3, p.fireball * 0.3, p.fireball * 0.3, 0.02);
	}

	private static void stageFireball(ServerLevel level, Vec3 c, Profile p, double q) {
		int n = cap((int) (120 * p.fireball * q));
		ParticleOptions fire = named(p.fireParticle, ParticleTypes.FLAME);
		level.sendParticles(fire, c.x, c.y + p.fireball * 0.3, c.z, n, p.fireball * 0.5, p.fireball * 0.4, p.fireball * 0.5, 0.05);
		level.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, cap((int) (30 * q)), p.fireball * 0.4, p.fireball * 0.4, p.fireball * 0.4, 0.1);
	}

	private static void stageShockwave(ServerLevel level, Vec3 c, Profile p, double q) {
		ParticleOptions wave = named("shock_wave", ParticleTypes.EXPLOSION);
		int steps = cap((int) (72 * q));
		double r = p.fireball;
		for (int i = 0; i < steps; i++) {
			double a = (Math.PI * 2 * i) / steps;
			double px = c.x + Math.cos(a) * r;
			double pz = c.z + Math.sin(a) * r;
			level.sendParticles(wave, px, c.y + 0.5, pz, 1, 0, 0, 0, 0.0);
		}
	}

	private static void stageDustWall(ServerLevel level, Vec3 c, Profile p, double q) {
		ParticleOptions dust = named("dust_wave", ParticleTypes.CAMPFIRE_COSY_SMOKE);
		int n = cap((int) (150 * p.dust * q));
		level.sendParticles(dust, c.x, c.y + 1.0, c.z, n, p.fireball * 1.2 * p.dust, 1.0, p.fireball * 1.2 * p.dust, 0.08);
	}

	private static void stageMushroomStem(ServerLevel level, Vec3 c, Profile p, double q) {
		ParticleOptions smoke = named(p.smokeParticle, ParticleTypes.LARGE_SMOKE);
		int layers = cap((int) (14 * p.mushroom * q));
		for (int i = 0; i < layers; i++) {
			double h = c.y + i * 1.6 * p.mushroom;
			level.sendParticles(smoke, c.x, h, c.z, cap((int) (10 * q)), p.mushroom * 0.6, 0.3, p.mushroom * 0.6, 0.02);
		}
	}

	private static void stageMushroomCap(ServerLevel level, Vec3 c, Profile p, double q) {
		ParticleOptions smoke = named(p.smokeParticle, ParticleTypes.LARGE_SMOKE);
		double capY = c.y + 14.0 * p.mushroom;
		int steps = cap((int) (90 * p.mushroom * q));
		double r = 4.0 * p.mushroom;
		for (int i = 0; i < steps; i++) {
			double a = (Math.PI * 2 * i) / steps;
			double rr = r * (0.6 + 0.4 * ((i % 5) / 5.0));
			level.sendParticles(smoke, c.x + Math.cos(a) * rr, capY + Math.sin(a * 3) * 1.5, c.z + Math.sin(a) * rr, 1, 0.2, 0.2, 0.2, 0.01);
		}
	}

	private static void stageSmokeColumn(ServerLevel level, Vec3 c, Profile p, double q) {
		ParticleOptions smoke = named(p.smokeParticle, ParticleTypes.LARGE_SMOKE);
		int n = cap((int) (120 * p.smoke * q));
		level.sendParticles(smoke, c.x, c.y + 6.0 * p.mushroom, c.z, n, p.fireball, 6.0 * p.mushroom, p.fireball, 0.04);
	}

	private static void stageFallout(ServerLevel level, Vec3 c, Profile p, double q) {
		ParticleOptions ash = p == Profile.DIRTY ? named("gas_cloud", ParticleTypes.ASH) : ParticleTypes.WHITE_ASH;
		int n = cap((int) (120 * p.smoke * q));
		level.sendParticles(ash, c.x, c.y + 4.0, c.z, n, p.fireball * 1.5, 3.0, p.fireball * 1.5, 0.02);
	}

	// ---- helpers ----

	private static void broadcastShake(ServerLevel level, Vec3 c, Profile p) {
		if (!WariumConfig.NUKE_SCREEN_SHAKE.get())
			return;
		float intensity = (float) p.shake;
		double radius = 40.0 + p.fireball * 6.0;
		for (var player : level.players()) {
			double dist = player.position().distanceTo(c);
			if (dist > radius)
				continue;
			float scaled = (float) (intensity * (1.0 - dist / radius));
			if (scaled > 0.02F)
				PacketDistributor.sendToPlayer(player, new ScreenShakeMessage(scaled, 30));
		}
	}

	private static void playSound(ServerLevel level, Vec3 c, String name, float volume, float pitch) {
		SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("crusty_chunks", name));
		if (event != null)
			level.playSound(null, c.x, c.y, c.z, event, SoundSource.BLOCKS, volume, pitch);
	}

	private static ParticleOptions named(String path, ParticleOptions fallback) {
		try {
			var type = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.fromNamespaceAndPath("crusty_chunks", path));
			if (type instanceof SimpleParticleType simple)
				return simple;
		} catch (Throwable ignored) {
		}
		return fallback;
	}

	private static int cap(int n) {
		return Math.max(0, Math.min(n, WariumConfig.MAX_NUKE_PARTICLES.get()));
	}

	private static double qualityMultiplier() {
		EffectQuality q = WariumConfig.NUKE_EFFECTS_QUALITY.get();
		return switch (q) {
			case LOW -> 0.35;
			case MEDIUM -> 0.65;
			case HIGH -> 1.0;
			case CINEMATIC -> 1.6;
		};
	}

	private static void schedule(int tick, Runnable action) {
		CrustyChunksMod.queueServerWork(tick, () -> {
			try {
				action.run();
			} catch (Throwable t) {
				WariumSafety.report("WariumNukeEffects.stage", t);
			}
		});
	}
}
