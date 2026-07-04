package shipwrights.genesis.teleportation.integration;

import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.math.OBB;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.teleportation.DimensionTravelTeleporter;
import shipwrights.genesis.teleportation.TravelDirection;

import java.util.*;

import static shipwrights.genesis.teleportation.integration.Util.getSortedConstructs;

public class SpaceToPlanetTeleporter {
	private static final int LANDING_ACCURACY = 8; // Randomization range in chunks

	private final boolean gameTest;

	public SpaceToPlanetTeleporter(boolean gameTest) {
		this.gameTest = gameTest;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel serverLevel && GenesisMod.isSpaceDimension(serverLevel)) {
			if (gameTest || !serverLevel.getPlayers(u -> true, 1).isEmpty()) {
				tick(serverLevel);
			}
		}
	}

	private static void tick(ServerLevel level) {
		long ticks = GenesisMod.getTicks(level);
		Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);

		for (AeronauticsConstruct construct : getSortedConstructs(level)) {
			var box = construct.worldBounds();
			Vec3 shipCenter = new Vec3((box.minX() + box.maxX()) / 2, (box.minY() + box.maxY()) / 2, (box.minZ() + box.maxZ()) / 2);

			Celestial nearest = getNearestPlanet(construct, ticks, registry);
			if (nearest == null || construct.localBounds() == null) continue;

			if (!constructOverlapsCelestial(construct, nearest, ticks, registry)) continue;

			PlayerTeam team = level.getScoreboard().getPlayerTeam(registry.getResourceKey(nearest).orElseThrow().location().getPath());
			if (team != null) {
				Collection<String> teamShips = team.getPlayers();
				String slug = construct.name();
				if (slug == null || !teamShips.contains(slug)) {
					NeoForge.EVENT_BUS.post(new TeleportDisallowedEvent(construct, nearest));
					continue;
				}
			}

			ServerLevel targetLevel = getTargetLevel(level, nearest, registry);
			if (targetLevel == null) continue;

			Vector3d newPos = computePlanetTarget(level);
			Quaterniond rotation = getNewShipRot(shipCenter, nearest, ticks, registry);

			DimensionTravelTeleporter.teleportConstruct(construct, TravelDirection.SPACE_TO_PLANET, level, targetLevel, newPos, rotation);
		}
	}

	private static boolean constructOverlapsCelestial(AeronauticsConstruct construct, Celestial nearest, long ticks, Registry<Celestial> registry) {
		var b = construct.localBounds();
		OBB constructOBB = OBB.fromLocalBounds(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ(), construct.toWorldMatrix());
		return constructOBB.overlapsWith(nearest.getOBB(ticks, registry));
	}

	private static @Nullable ServerLevel getTargetLevel(ServerLevel level, Celestial nearest, Registry<Celestial> registry) {
		ResourceKey<Level> targetDimension = ResourceKey.create(
			net.minecraft.core.registries.Registries.DIMENSION,
			registry.getResourceKey(nearest).orElseThrow().location()
		);
        return level.getServer().getLevel(targetDimension);
	}

	private static Vector3d computePlanetTarget(ServerLevel level) {
		ChunkPos landingChunkPos = new ChunkPos(
			level.random.nextInt(LANDING_ACCURACY * 2 + 1) - LANDING_ACCURACY,
			level.random.nextInt(LANDING_ACCURACY * 2 + 1) - LANDING_ACCURACY
		);
		return new Vector3d(
			SectionPos.sectionToBlockCoord(landingChunkPos.x),
				GenesisCommonConfig.getAtmosphereEntryHeight(),
			SectionPos.sectionToBlockCoord(landingChunkPos.z)
		);
	}

	private static Quaterniond getNewShipRot(Vec3 shipCenter, Celestial nearest, long ticks, Registry<Celestial> registry) {
		Vector3dc planetPos = nearest.getPosition(ticks, registry);
		Vector3d directionToPlanet = new Vector3d(
			shipCenter.x - planetPos.x(),
			shipCenter.y - planetPos.y(),
			shipCenter.z - planetPos.z()
		).normalize();
		Quaterniond rotation = new Quaterniond().rotateTo(new Vector3d(0, 1, 0), directionToPlanet);
		nearest.getRotation(ticks, 0f, registry).mul(rotation, rotation).conjugate();
		return rotation;
	}

	@Nullable static Celestial getNearestPlanet(AeronauticsConstruct construct, long ticks, Registry<Celestial> registry) {
		var b = construct.localBounds();
		if (b != null) {
			OBB shipOBB = OBB.fromLocalBounds(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ(), construct.toWorldMatrix());

			Optional<Celestial> nearest = registry.stream()
							.filter(c -> c.type().isVisitable())
							.map(c -> Map.entry(c, c.getOBB(ticks, registry).distanceTo(shipOBB)))
							.min(Comparator.comparingDouble(Map.Entry::getValue))
							.map(Map.Entry::getKey);
			if (nearest.isPresent()) {
				return nearest.get();
			}
		}
		return null;
	}
}
