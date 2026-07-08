package shipwrights.genesis.teleportation.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.teleportation.DimensionTravelTeleporter;
import shipwrights.genesis.teleportation.TravelDirection;
import shipwrights.genesis.teleportation.impl.EntityTeleporter;

import java.util.List;

import static shipwrights.genesis.teleportation.integration.Util.getSortedConstructs;

/**
 * Planet-side half of the travel loop.
 *
 * <p><b>Leaving a planet is seamless.</b> Climbing out of the atmosphere is <em>not</em> a dimension
 * change any more — the player and their craft stay in the overworld the whole way up. As they climb,
 * the sky/fog fade to black and the celestial renderer lifts the planet away below them (see
 * {@link VantagePoint#getObserverPosition()}), so Earth&nbsp;→&nbsp;lower atmosphere&nbsp;→&nbsp;upper
 * atmosphere&nbsp;→&nbsp;orbit&nbsp;→&nbsp;past the Moon happens with no portal, no loading screen and no
 * teleport at all.</p>
 *
 * <p>The <b>only</b> boundary this class enforces is <b>deep space</b>: once you have climbed clear past
 * the Moon — {@link GenesisCommonConfig#getDeepSpaceRadius()} blocks up, i.e. ~20,000 beyond the Moon's
 * ~10,000-block distance — the void hands off to the {@code subspace} deep-space dimension. Craft state
 * (structure, velocity, rotation, passengers, momentum) is carried through that single hop.</p>
 */
public class PlanetToSpaceTeleporter {
	/** Persistent-data tag holding the game time a player arrived in space (re-entry grace). */
	public static final String SPACE_ARRIVAL_TAG = "genesis_space_arrival_tick";

	private final boolean gameTest;

	public PlanetToSpaceTeleporter(boolean gameTest) {
		this.gameTest = gameTest;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) {
			if (gameTest || !serverLevel.getPlayers(u -> true, 1).isEmpty()) {
				tick(serverLevel);
			}
		}
	}

	private static void tick(ServerLevel level) {
		// Only runs on real bodies (a level with a celestial mapping) — never in the space dimensions.
		if (GenesisMod.isSpaceDimension(level) || GenesisMod.isSubSpaceDimension(level)) return;
		if (GenesisMod.getCelestialForLevel(level) == null) return;

		ServerLevel deepSpace = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.WORMHOLE_DIM));
		if (deepSpace == null) return;

		int deepSpaceHeight = GenesisCommonConfig.getDeepSpaceRadius();   // ~30,000 = 20,000 past the Moon
		int exitHeight = GenesisCommonConfig.getAtmosphereExitHeight();   // top of the breathable atmosphere

		// ---- constructs (Sable physics objects): only deep space moves them off this world ----
		for (AeronauticsConstruct construct : getSortedConstructs(level)) {
			if (construct.isRemoved()) continue;
			if (construct.positionInWorld().y() > deepSpaceHeight) {
				sendConstructToDeepSpace(level, deepSpace, construct);
			}
		}

		// ---- players ----
		for (ServerPlayer player : List.copyOf(level.players())) {
			if (player.isPassenger() || player.isRemoved()) continue;

			double y = player.getY();
			if (y > deepSpaceHeight) {
				sendPlayerToDeepSpace(player, deepSpace);
			} else if (y > exitHeight && level.getGameTime() % 40 == 0) {
				// Ambient orbital read-out; the trip itself is seamless so this is the only cue.
				long altPastMoon = (long) (y - MOON_DISTANCE);
				String msg = altPastMoon > 0
						? "Past the Moon — deep space in " + (deepSpaceHeight - (int) y) + " blocks"
						: "In orbit — Moon ahead at " + (MOON_DISTANCE - (int) y) + " blocks";
				player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.DARK_AQUA), true);
			}
		}
	}

	/** Distance (blocks) the Moon model sits from the surface — the reference point for "past the Moon". */
	private static final double MOON_DISTANCE = 10000.0;

	/**
	 * Teleports a player (and passengers) straight into the Great Unknown. No longer part of the normal
	 * ascent — retained for the {@code /genesis space} debug command only.
	 */
	public static boolean sendToSpace(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		if (GenesisMod.isSpaceDimension(level)) return false;

		ServerLevel spaceLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM));
		if (spaceLevel == null) return false;

		long ticks = GenesisMod.getTicks(level);
		Vector3d target;
		Quaterniond rotation;
		Vector3d pos = new Vector3d(player.getX(), player.getY(), player.getZ());
		if (VantagePoint.get(level, pos, ticks, 0f) instanceof VantagePoint.OnCelestial vantagePoint) {
			target = computeSpaceTarget(vantagePoint).add(0, 60, 0);
			rotation = vantagePoint.getCelestialRotation()
					.mul(vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()), new Quaterniond());
		} else {
			target = new Vector3d(player.getX() / 16.0, 320.0, player.getZ() / 16.0);
			rotation = new Quaterniond();
		}

		EntityTeleporter.teleportEntityAndPassengers(player, spaceLevel, new Vec3(target.x, target.y, target.z), rotation);
		player.getPersistentData().putLong(SPACE_ARRIVAL_TAG, spaceLevel.getGameTime());
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 60, 0, false, false, true));
		player.displayClientMessage(Component.literal("Entering the Great Unknown").withStyle(ChatFormatting.AQUA), true);
		return true;
	}

	/** Deep-space handoff for a free-flying player: preserves momentum, passengers and camera. */
	private static void sendPlayerToDeepSpace(ServerPlayer player, ServerLevel deepSpace) {
		if (GenesisMod.isSubSpaceDimension(player.serverLevel())) return;
		Vec3 carried = player.getDeltaMovement();
		Vector3d landing = new Vector3d(player.getX() / 16.0, 256.0, player.getZ() / 16.0);
		GenesisMod.LOGGER.info("Player {} crossed past the Moon into deep space at y={}",
				player.getGameProfile().getName(), (int) player.getY());
		EntityTeleporter.teleportEntityAndPassengers(player, deepSpace, new Vec3(landing.x, landing.y, landing.z), new Quaterniond());
		player.setDeltaMovement(carried);
		player.getPersistentData().putLong(SPACE_ARRIVAL_TAG, deepSpace.getGameTime());
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 60, 0, false, false, true));
		player.displayClientMessage(Component.literal("Crossing into deep space").withStyle(ChatFormatting.LIGHT_PURPLE), true);
		player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.25f, 1.5f);
	}

	/** Deep-space handoff for a Sable construct: structure/velocity/rotation/passengers carried by the teleporter. */
	private static void sendConstructToDeepSpace(ServerLevel level, ServerLevel deepSpace, AeronauticsConstruct construct) {
		var box = construct.worldBounds();
		Vector3d landing = new Vector3d((box.minX() + box.maxX()) / 32.0, 256.0, (box.minZ() + box.maxZ()) / 32.0);
		GenesisMod.LOGGER.info("Construct {} crossed past the Moon into deep space", construct.id());
		DimensionTravelTeleporter.teleportConstruct(construct, TravelDirection.PLANET_TO_SPACE, level, deepSpace, landing, new Quaterniond());
	}

	private static Vector3d computeSpaceTarget(VantagePoint.OnCelestial vantagePoint) {
		Vector3d targetPos = new Vector3d(0, vantagePoint.celestial().getActualSize() + 20, 0);
		vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()).transform(targetPos);
		vantagePoint.getCelestialRotation().transform(targetPos);
		targetPos.add(vantagePoint.getPosition());
		return targetPos;
	}
}
