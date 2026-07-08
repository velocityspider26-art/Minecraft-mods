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
 * Planet-side half of the travel loop: crossing the Kármán line takes you into real 3D space.
 *
 * <p>Climb past {@link GenesisCommonConfig#getAtmosphereExitHeight()} (the Kármán line) and you slip —
 * seamlessly, behind a frame-captured transition, with your momentum intact — into the
 * {@code great_unknown} space dimension: a true 3D void where the Sun, Earth and Moon sit at real
 * positions. There you actually fly <em>toward</em> the Moon and watch it grow, instead of climbing a
 * flat Y axis that never reaches it. The space-side {@link SpaceToPlanetTeleporter} then handles
 * arriving at a body (dropping you onto it) and the deep-space boundary out past the Moon.</p>
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

		ServerLevel spaceLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM));
		if (spaceLevel == null) return;

		int karman = GenesisCommonConfig.getAtmosphereExitHeight();   // the Kármán line

		// ---- constructs (Sable physics objects) cross the Kármán line into 3D space ----
		for (AeronauticsConstruct construct : getSortedConstructs(level)) {
			if (construct.isRemoved()) continue;
			if (construct.positionInWorld().y() > karman) {
				sendConstructToSpace(level, spaceLevel, construct);
			}
		}

		// ---- players ----
		for (ServerPlayer player : List.copyOf(level.players())) {
			if (player.isPassenger() || player.isRemoved()) continue;

			double y = player.getY();
			if (y > karman) {
				sendToSpace(player);
			} else if (y > karman * 0.6 && level.getGameTime() % 40 == 0) {
				player.displayClientMessage(Component.literal(
								"Approaching the Kármán line… " + (int) y + " / " + karman)
						.withStyle(ChatFormatting.DARK_AQUA), true);
			}
		}
	}

	/**
	 * Teleports a player (and passengers) into the Great Unknown, above the celestial they launched from,
	 * carrying their momentum so the climb continues seamlessly into 3D space. Also the {@code /genesis
	 * space} debug command.
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
			// Arrive just above the atmosphere, clear of the body, looking out into space.
			target = computeSpaceTarget(vantagePoint).add(0, 60, 0);
			rotation = vantagePoint.getCelestialRotation()
					.mul(vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()), new Quaterniond());
		} else {
			target = new Vector3d(player.getX() / 16.0, 320.0, player.getZ() / 16.0);
			rotation = new Quaterniond();
		}

		Vec3 carried = player.getDeltaMovement();
		EntityTeleporter.teleportEntityAndPassengers(player, spaceLevel, new Vec3(target.x, target.y, target.z), rotation);
		player.setDeltaMovement(carried); // keep momentum through the Kármán line
		player.getPersistentData().putLong(SPACE_ARRIVAL_TAG, spaceLevel.getGameTime());
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 60, 0, false, false, true));
		player.displayClientMessage(Component.literal("Crossing into space").withStyle(ChatFormatting.AQUA), true);
		player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.2f, 1.6f);
		return true;
	}

	/** Sends a construct across the Kármán line into 3D space, above the body, keeping its state. */
	private static void sendConstructToSpace(ServerLevel level, ServerLevel spaceLevel, AeronauticsConstruct construct) {
		long ticks = GenesisMod.getTicks(level);
		Vector3dc pos = construct.positionInWorld();
		Vector3d target;
		Quaterniond rotation;
		if (VantagePoint.get(level, new Vector3d(pos), ticks, 0f) instanceof VantagePoint.OnCelestial vantagePoint) {
			target = computeSpaceTarget(vantagePoint).add(0, 60, 0);
			rotation = vantagePoint.getCelestialRotation()
					.mul(vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()), new Quaterniond());
		} else {
			target = new Vector3d(pos.x() / 16.0, 320.0, pos.z() / 16.0);
			rotation = new Quaterniond();
		}
		GenesisMod.LOGGER.info("Construct {} crossed the Kármán line into space", construct.id());
		DimensionTravelTeleporter.teleportConstruct(construct, TravelDirection.PLANET_TO_SPACE, level, spaceLevel, target, rotation);
	}

	private static Vector3d computeSpaceTarget(VantagePoint.OnCelestial vantagePoint) {
		Vector3d targetPos = new Vector3d(0, vantagePoint.celestial().getActualSize() + 20, 0);
		vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()).transform(targetPos);
		vantagePoint.getCelestialRotation().transform(targetPos);
		targetPos.add(vantagePoint.getPosition());
		return targetPos;
	}
}
