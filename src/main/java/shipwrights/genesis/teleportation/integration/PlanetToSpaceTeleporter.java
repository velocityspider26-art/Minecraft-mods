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
		if (GenesisMod.isSpaceDimension(level)) return;

		ServerLevel spaceLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM));
		if (spaceLevel == null) return;

		Celestial body = GenesisMod.getCelestialForLevel(level);
		long ticks = GenesisMod.getTicks(level);

		if (body != null) {
			for (AeronauticsConstruct construct : getSortedConstructs(level)) {
				Vector3dc pos = construct.positionInWorld();
				if (pos.y() > GenesisCommonConfig.getAtmosphereExitHeight()) {
					if (VantagePoint.get(level, pos, ticks, 0f) instanceof VantagePoint.OnCelestial vantagePoint) {
						DimensionTravelTeleporter.teleportConstruct(
								construct,
								TravelDirection.PLANET_TO_SPACE,
								level,
								spaceLevel,
								computeSpaceTarget(vantagePoint),
								vantagePoint.getCelestialRotation().mul(vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()), new Quaterniond())
						);
					}
				}
			}
		}

		int exitHeight = GenesisCommonConfig.getAtmosphereExitHeight();

		// Free-flying players (not aboard a construct) always leave the atmosphere when high
		// enough — even if this level has no celestial mapping, they still reach space.
		for (ServerPlayer player : List.copyOf(level.players())) {
			if (player.isPassenger() || player.isRemoved()) continue;

			if (player.getY() > exitHeight) {
				sendToSpace(player);
			} else if (player.getY() > exitHeight * 0.55 && level.getGameTime() % 40 == 0) {
				// Climbing feedback so players know the transition is ahead of them.
				player.displayClientMessage(Component.literal(
								"Leaving the atmosphere... " + (int) player.getY() + " / " + exitHeight)
						.withStyle(ChatFormatting.DARK_AQUA), true);
			}
		}
	}

	/**
	 * Teleports a player (and passengers) into the Great Unknown from wherever they are.
	 * Uses the celestial vantage math when this level maps to a celestial; otherwise falls
	 * back to plain scaled coordinates so the trip always succeeds.
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
			// Arrive well clear of the celestial so re-entry doesn't immediately trigger.
			target = computeSpaceTarget(vantagePoint).add(0, 60, 0);
			rotation = vantagePoint.getCelestialRotation()
					.mul(vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()), new Quaterniond());
		} else {
			// No celestial mapping for this level — send them into the Great Unknown anyway.
			target = new Vector3d(player.getX() / 16.0, 320.0, player.getZ() / 16.0);
			rotation = new Quaterniond();
		}

		GenesisMod.LOGGER.info("Player {} left the atmosphere of {}; entering space at ({}, {}, {})",
				player.getGameProfile().getName(), level.dimension().location(),
				(int) target.x, (int) target.y, (int) target.z);

		EntityTeleporter.teleportEntityAndPassengers(player, spaceLevel, new Vec3(target.x, target.y, target.z), rotation);

		player.getPersistentData().putLong(SPACE_ARRIVAL_TAG, spaceLevel.getGameTime());
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 60, 0, false, false, true));
		player.displayClientMessage(Component.literal("Entering the Great Unknown").withStyle(ChatFormatting.AQUA), true);
		player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.25f, 1.5f);
		return true;
	}

	private static Vector3d computeSpaceTarget(VantagePoint.OnCelestial vantagePoint) {
		Vector3d targetPos = new Vector3d(0, vantagePoint.celestial().getActualSize() + 20, 0);
		vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()).transform(targetPos);
		vantagePoint.getCelestialRotation().transform(targetPos);
		targetPos.add(vantagePoint.getPosition());
		return targetPos;
    }
}
