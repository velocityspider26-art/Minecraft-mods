package shipwrights.genesis.teleportation.integration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
		Celestial body = GenesisMod.getCelestialForLevel(level);
		ServerLevel spaceLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM));

		if (body == null || spaceLevel == null) {
			return;
		}

		long ticks = GenesisMod.getTicks(level);

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

		// Free-flying players (not aboard a construct) leave the atmosphere the same way ships do.
		for (ServerPlayer player : List.copyOf(level.players())) {
			if (player.isPassenger() || player.isRemoved()) continue;
			if (player.getY() <= GenesisCommonConfig.getAtmosphereExitHeight()) continue;

			Vector3d pos = new Vector3d(player.getX(), player.getY(), player.getZ());
			if (VantagePoint.get(level, pos, ticks, 0f) instanceof VantagePoint.OnCelestial vantagePoint) {
				Vector3d target = computeSpaceTarget(vantagePoint);
				Quaterniond rotation = vantagePoint.getCelestialRotation()
						.mul(vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()), new Quaterniond());
				GenesisMod.LOGGER.info("Player {} left the atmosphere of {}; entering space", player.getGameProfile().getName(), level.dimension().location());
				EntityTeleporter.teleportEntityAndPassengers(player, spaceLevel, new Vec3(target.x, target.y, target.z), rotation);
			}
		}
	}

	private static Vector3d computeSpaceTarget(VantagePoint.OnCelestial vantagePoint) {
		Vector3d targetPos = new Vector3d(0, vantagePoint.celestial().getActualSize() + 20, 0);
		vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()).transform(targetPos);
		vantagePoint.getCelestialRotation().transform(targetPos);
		targetPos.add(vantagePoint.getPosition());
		return targetPos;
    }
}
