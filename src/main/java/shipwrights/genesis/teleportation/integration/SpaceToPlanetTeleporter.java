package shipwrights.genesis.teleportation.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.networking.HyperspaceStatePacket;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.SpaceLevel;
import shipwrights.genesis.teleportation.ArrivalGate;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;
import shipwrights.genesis.teleportation.SpaceTravelManager;
import shipwrights.genesis.teleportation.impl.EntityTeleporter;

import java.util.ArrayList;

/**
 * Atmosphere entry: closing within the entry band of a rendered planet in the
 * space dimension drops the traveler straight into that planet's dimension —
 * no fade, no delay; the swap itself is kept cheap by pre-generating the
 * destination terrain during the approach.
 *
 * Ships travel as one unit (craft + seats + crew) via {@link SpaceTravelManager};
 * players standing on or seated in a ship are left to travel with it.
 */
public class SpaceToPlanetTeleporter {
	/** Widest entry band any body uses, for the approach pre-generation radius. */
	private static final double MAX_ENTRY_PADDING = SpaceTravelManager.PLANET_ENTRY_PADDING;
	/** How far out (relative to the planet's half-extent) terrain pre-generation starts. */
	private static final double APPROACH_RADIUS_FACTOR = 4.0;
	// Every five seconds was fine for warming a distant destination, but the
	// last stretch of an approach is crossed in less than one interval — so the
	// ground the traveller actually lands on was often still being generated
	// when they arrived. Checked often enough now that the final position is
	// always covered by a ticket placed before the entry fires.
	private static final int PREGEN_INTERVAL_TICKS = 20;

	private static final TicketType<BlockPos> APPROACH_TICKET =
			TicketType.create("genesis_planet_approach", Vec3i::compareTo, 400);

	private long pregenClock;

	private final boolean gameTest;

	public SpaceToPlanetTeleporter(boolean gameTest) {
		this.gameTest = gameTest;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel level && GenesisMod.isSpaceDimension(level)) {
			tick(level);
		}
	}

	private void tick(ServerLevel level) {
		Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
		long ticks = GenesisMod.getTicks(level);
		pregenClock++;

		tickPlayers(level, registry, ticks);
		tickShips(level, registry, ticks);
	}

	private void tickPlayers(ServerLevel level, Registry<Celestial> registry, long ticks) {
		for (ServerPlayer player : new ArrayList<>(level.getPlayers(p -> true))) {
			if (player.isSpectator()) {
				continue;
			}

			// Already committed to a crossing and waiting on the destination.
			// The entry test must not run again: the outside-to-inside
			// transition that produced this crossing has been consumed, so
			// re-testing can only lose it or duplicate it.
			if (ArrivalGate.isHeld(player.getUUID())) {
				continue;
			}

			// A player aboard a ship enters planets with the whole travel unit,
			// measured at the ship's position (a seated player's own coordinates
			// are shadow-region plot values and must never drive the entry test).
			ServerSubLevel ship = SpaceTravelManager.shipCarrying(level, player);
			if (ship != null) {
				if (ArrivalGate.isHeld(ship.getUniqueId())) {
					continue;
				}
				Vector3d shipPosition = SpaceLevel.toCelestialSpace(level, ship.logicalPose().position());
				EnteredCelestial shipEntered = findEnteredSwept(registry, ticks, ship.getUniqueId(), shipPosition);
				// Only a ship that CROSSED IN from outside enters. A ship just
				// assembled inside the zone has never been outside, so it stays.
				boolean shipCrossed = SpaceTravelManager.crossedIntoEntryZone(
						ship.getUniqueId(), shipEntered != null);
				if (shipEntered != null && shipCrossed) {
					ServerLevel planetLevel = destinationLevel(level, registry, shipEntered);
					if (planetLevel != null) {
						Vec3 target = computePlanetTarget(shipEntered.localPoint(), shipEntered.celestial(), planetLevel);
						GenesisMod.LOGGER.info("[ENTRY] Ship {} aboard {} entering {}",
								ship.getUniqueId(), player.getGameProfile().getName(),
								planetLevel.dimension().location());
						holdShipCrossing(level, ship, planetLevel, target);
					}
				}
				continue;
			}

			Vector3d position = SpaceLevel.toCelestialSpace(level, player.position());
			pregenApproach(level, registry, ticks, position);

			EnteredCelestial entered = findEnteredSwept(registry, ticks, player.getUUID(), position);
			// Approach diagnostic: every ~4s, report how close the nearest
			// landable body is and whether you are in its entry zone yet — so a
			// "flew at the planet and nothing happened" can be traced to reach,
			// crossing, or suppression.
			if (ticks % 80 == 0) {
				logApproach(registry, ticks, position, player);
			}
			// Crossing IN from outside is the whole guard. Arrivals are marked as
			// already-inside, so a fresh arrival cannot re-trigger entry until it
			// has genuinely left and come back. A separate distance-based
			// suppression used to sit here as well, but it keyed off the last
			// arrival anywhere — so landing near one body silently blocked entry
			// to every body, which is why flying into Mercury did nothing.
			boolean crossed = SpaceTravelManager.crossedIntoEntryZone(player.getUUID(), entered != null);
			if (entered == null || !crossed) {
				continue;
			}
			ServerLevel planetLevel = destinationLevel(level, registry, entered);
			if (planetLevel == null) {
				GenesisMod.LOGGER.warn("[ENTRY] {} reached {} but it has no dimension to land in",
						player.getGameProfile().getName(), registry.getResourceKey(entered.celestial()).map(k -> k.location().toString()).orElse("?"));
				continue;
			}

			Vec3 target = computePlanetTarget(entered.localPoint(), entered.celestial(), planetLevel);

			// Safety net: carry the whole ship if aboard-detection missed one the
			// player is actually on (large deck / seat off the ship's centre).
			dev.ryanhcode.sable.sublevel.ServerSubLevel nearbyShip =
					SpaceTravelManager.nearestShip(level, player.position(), 96.0);
			if (nearbyShip != null) {
				Vector3d shipCelestial = SpaceLevel.toCelestialSpace(level, nearbyShip.logicalPose().position());
				EnteredCelestial shipEntered = findEnteredCelestial(registry, ticks, shipCelestial, 0.0);
				Vec3 shipTarget = shipEntered != null
						? computePlanetTarget(shipEntered.localPoint(), shipEntered.celestial(), planetLevel)
						: target;
				GenesisMod.LOGGER.info("[ENTRY] Solo player {} was on ship {} (radius net) entering {}",
						player.getGameProfile().getName(), nearbyShip.getUniqueId(), planetLevel.dimension().location());
				holdShipCrossing(level, nearbyShip, planetLevel, shipTarget);
				continue;
			}

			GenesisMod.LOGGER.info("[ENTRY] {} entering {} solo", player.getGameProfile().getName(),
					planetLevel.dimension().location());
			SpaceTravelManager.logShips(level, player.position(), "ENTRY-SOLO");

			holdPlayerCrossing(level, player, planetLevel, target);
		}
	}

	/**
	 * Commits a solo player's crossing, to fire once the planet has terrain to
	 * receive them. Everything the crossing needs is looked up again when it
	 * runs: several ticks can pass first, and a player can log out inside that
	 * window.
	 */
	private static void holdPlayerCrossing(ServerLevel level, ServerPlayer player, ServerLevel planetLevel, Vec3 target) {
		java.util.UUID travellerId = player.getUUID();
		MinecraftServer server = level.getServer();
		ArrivalGate.hold(travellerId, planetLevel, target, () -> {
			ServerPlayer arriving = server.getPlayerList().getPlayer(travellerId);
			if (arriving == null || arriving.level() != level) {
				return;
			}
			Vec3 velocity = arriving.getDeltaMovement();
			EntityTeleporter.teleportEntityAndPassengers(arriving, planetLevel, target);
			arriving.setDeltaMovement(velocity);
			arriving.resetFallDistance();
			GenesisMod.refreshEntityScaling(arriving, planetLevel);
		});
	}

	/**
	 * Commits a craft's crossing, to fire once the planet has terrain to receive
	 * it. The craft is re-resolved by id when the crossing runs, because Sable
	 * can unload a sub-level on any tick and moving a stale handle would strand
	 * its crew.
	 */
	private static void holdShipCrossing(ServerLevel level, ServerSubLevel ship, ServerLevel planetLevel, Vec3 target) {
		java.util.UUID shipId = ship.getUniqueId();
		ResourceKey<net.minecraft.world.level.Level> originKey = level.dimension();
		MinecraftServer server = level.getServer();
		ArrivalGate.hold(shipId, planetLevel, target, () -> {
			ServerLevel origin = server.getLevel(originKey);
			ServerSubLevel current = origin == null ? null : SpaceTravelManager.getShip(origin, shipId);
			if (current == null) {
				GenesisMod.LOGGER.warn("[ENTRY] craft {} left {} before its crossing fired",
						shipId, originKey.location());
				return;
			}
			for (ServerPlayer crew : SpaceTravelManager.crewPlayers(origin, current)) {
				GenesisNetworking.sendToPlayer(crew, HyperspaceStatePacket.loadingHint());
			}
			// Crew first, ship a moment later — see transferShipAfterCrew.
			SpaceTravelManager.transferShipAfterCrew(current, planetLevel, target);
		});
	}

	/** Sable ships that sink into a planet's entry band drop into its dimension as one unit. */
	private void tickShips(ServerLevel level, Registry<Celestial> registry, long ticks) {
		ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
		if (container == null) {
			return;
		}

		for (ServerSubLevel ship : new ArrayList<>(container.getAllSubLevels())) {
			if (ArrivalGate.isHeld(ship.getUniqueId())) {
				continue; // crossing committed; waiting on the destination
			}
			Vector3dc shipPos = ship.logicalPose().position();
			Vector3d position = SpaceLevel.toCelestialSpace(level, shipPos);
			pregenApproach(level, registry, ticks, position);

			EnteredCelestial entered = findEnteredSwept(registry, ticks, ship.getUniqueId(), position);
			boolean crossed = SpaceTravelManager.crossedIntoEntryZone(ship.getUniqueId(), entered != null);
			if (entered == null || !crossed) {
				continue;
			}
			ServerLevel planetLevel = destinationLevel(level, registry, entered);
			if (planetLevel == null) {
				continue;
			}

			Vec3 target = computePlanetTarget(entered.localPoint(), entered.celestial(), planetLevel);
			holdShipCrossing(level, ship, planetLevel, target);
		}
	}

	/** Starts async terrain generation on the destination while a traveler closes in. */
	private void pregenApproach(ServerLevel level, Registry<Celestial> registry, long ticks, Vector3dc position) {
		if (pregenClock % PREGEN_INTERVAL_TICKS != 0) {
			return;
		}

		for (Celestial celestial : registry) {
			if (!celestial.type().isVisitable()) {
				continue;
			}

			Vector3dc celestialPos = celestial.getPosition(ticks, registry);
			double halfExtent = celestial.getActualSize() / 2.0;
			double entryRadius = halfExtent + MAX_ENTRY_PADDING;
			double distance = position.distance(celestialPos);
			if (distance > entryRadius * APPROACH_RADIUS_FACTOR) {
				continue;
			}
			// Widen the pre-generated area as the traveller closes in. Far out
			// this is speculative and a small patch is enough; near the entry
			// band they are about to be standing on it, and a landing that
			// straddles the edge of a narrow patch still stutters.
			//
			// The top of this range used to be 8, which is 289 chunks — queued
			// afresh every second at a centre that moves the whole way in. That
			// is a sustained generation load for chunks nobody ends up standing
			// in, and it competes with the arrival chunks that actually matter.
			// Four covers the 5x5 the arrival gate waits on with a chunk of
			// margin either side, which is what the landing needs and no more.
			double closeness = 1.0 - net.minecraft.util.Mth.clamp(
					(distance - entryRadius) / (entryRadius * (APPROACH_RADIUS_FACTOR - 1.0)), 0.0, 1.0);
			int ticketRadius = 2 + (int) Math.round(closeness * 2.0);

			ServerLevel planetLevel = level.getServer().getLevel(ResourceKey.create(
					Registries.DIMENSION, registry.getResourceKey(celestial).orElseThrow().location()));
			if (planetLevel == null || planetLevel == level) {
				continue;
			}

			// Predict the landing column. The ray from the planet's centre out
			// through the traveller leaves the entry cube at the face they are
			// about to sink through, and that crossing point maps to the surface
			// exactly the way a real entry does — so pre-generation and entry
			// name the same chunks.
			Vector3d local = toLocalCelestialPoint(celestial, registry, ticks, position);
			double cubeHalf = halfExtent + SpaceTravelManager.entryPadding(celestial.getActualSize());
			double reach = Math.max(Math.abs(local.x), Math.max(Math.abs(local.y), Math.abs(local.z)));
			if (reach > 1.0E-6) {
				local.mul(cubeHalf / reach);
			}
			CubeNetSurfaceTransform.SurfacePoint column = surfaceColumn(celestial, local);
			ChunkPos center = new ChunkPos(BlockPos.containing(column.worldX(), 0.0, column.worldZ()));
			planetLevel.getChunkSource().addRegionTicket(APPROACH_TICKET, center, ticketRadius, center.getWorldPosition());
		}
	}

	private static ServerLevel destinationLevel(ServerLevel level, Registry<Celestial> registry, EnteredCelestial entered) {
		ServerLevel planetLevel = level.getServer().getLevel(ResourceKey.create(
				Registries.DIMENSION, registry.getResourceKey(entered.celestial()).orElseThrow().location()));
		return planetLevel == level ? null : planetLevel;
	}

	/**
	 * Maps a point in the celestial's rotating local frame into one of the six
	 * cube-net regions in the planet world.
	 *
	 * <p>The ray is projected onto the padded entry cube, the dominant axis
	 * selects the face, and the face's texture-oriented axes become the region's
	 * world X/Z. The up face remains centred on the origin, preserving the old
	 * mapping for existing saves, while side and bottom approaches now retain the
	 * dimension that the old X/Z-only projection discarded.</p>
	 */
	private static CubeNetSurfaceTransform.SurfacePoint surfaceColumn(
			Celestial celestial, Vector3dc localPoint) {
		CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
				celestial.getActualSize(), SpaceTravelManager.entryPadding(celestial.getActualSize()));
		return transform.localToWorld(localPoint.x(), localPoint.y(), localPoint.z());
	}

	private static Vec3 computePlanetTarget(Vector3dc localEntryPoint, Celestial celestial, ServerLevel planetLevel) {
		CubeNetSurfaceTransform.SurfacePoint column = surfaceColumn(celestial, localEntryPoint);
		// High above the build cap on purpose: reentry is a real fall through
		// the planet's upper atmosphere. Scaled to the world's own atmosphere, so
		// you drop in low over an airless world instead of falling 19k blocks.
		double y = AtmosphereHeights.arrivalHeight(celestial);
		GenesisMod.LOGGER.info("[CUBENET] entry through {} -> surface ({}, {})",
				column.face(), Math.round(column.worldX()), Math.round(column.worldZ()));
		return new Vec3(column.worldX(), y, column.worldZ());
	}

	/**
	 * CUBE entry test in the celestial's rotating local frame: which face you
	 * sink through determines where on the planet you arrive (the local entry
	 * point maps to surface coordinates). Arrivals from planets and hyperspace
	 * are placed on a ring at 1.5x the cube half-extent
	 * ({@link SpaceTravelManager#arrivalRingRadius}), which clears the cube
	 * even at its corners, so entering and leaving can never ping-pong.
	 */
	/** Logs the nearest landable body and how the entry test sees it right now. */
	private static void logApproach(Registry<Celestial> registry, long ticks, Vector3dc position, ServerPlayer player) {
		Celestial nearest = null;
		double nearestDist = Double.MAX_VALUE;
		for (Celestial celestial : registry) {
			if (!celestial.type().isVisitable()) {
				continue;
			}
			double d = position.distance(celestial.getPosition(ticks, registry));
			if (d < nearestDist) {
				nearestDist = d;
				nearest = celestial;
			}
		}
		if (nearest == null) {
			return;
		}
		double zone = nearest.getActualSize() / 2.0 + SpaceTravelManager.entryPadding(nearest.getActualSize());
		String name = registry.getResourceKey(nearest).map(k -> k.location().toString()).orElse("?");
		GenesisMod.LOGGER.info("[APPROACH] {} nearest {} dist={} entryZone={} (inside={})",
				player.getGameProfile().getName(), name, (long) nearestDist, (long) zone, nearestDist <= zone);
	}

	/** Last sampled position per traveller, for the swept entry test. */
	private static final java.util.Map<java.util.UUID, Vector3d> LAST_POSITIONS =
			new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * Entry test along the path travelled since last tick, not just at the point
	 * you happen to occupy now.
	 *
	 * <p>A body's entry band is only as thick as its padding — 50 blocks around
	 * something Mercury-sized — and a craft at speed covers far more than that
	 * between ticks, so a point test simply misses: you pass clean through the
	 * cube and nothing happens. Sampling along the segment means the crossing is
	 * detected however fast you were going.</p>
	 */
	private static EnteredCelestial findEnteredSwept(Registry<Celestial> registry, long ticks,
			java.util.UUID travellerId, Vector3dc position) {
		Vector3d previous = travellerId == null ? null : LAST_POSITIONS.get(travellerId);
		if (travellerId != null) {
			LAST_POSITIONS.put(travellerId, new Vector3d(position));
		}

		EnteredCelestial direct = findEnteredCelestial(registry, ticks, position, 0.0);
		if (direct != null || previous == null) {
			return direct;
		}

		// Walk the segment finely enough that the thinnest band cannot be
		// stepped over, but cap the work for very long jumps.
		double distance = previous.distance(position);
		if (distance < 1.0E-3) {
			return null;
		}
		int samples = (int) Math.min(64, Math.max(1, distance / 25.0));
		Vector3d sample = new Vector3d();
		for (int i = 1; i < samples; i++) {
			double t = i / (double) samples;
			sample.set(previous).lerp(position, t);
			EnteredCelestial hit = findEnteredCelestial(registry, ticks, sample, 0.0);
			if (hit != null) {
				return hit;
			}
		}
		return null;
	}

	private static EnteredCelestial findEnteredCelestial(Registry<Celestial> registry, long ticks, Vector3dc position, double padding) {
		EnteredCelestial nearest = null;
		double nearestDistanceSquared = Double.MAX_VALUE;

		for (Celestial celestial : registry) {
			if (!celestial.type().isVisitable()) {
				continue;
			}

			Vector3d local = toLocalCelestialPoint(celestial, registry, ticks, position);
			double halfExtent = celestial.getActualSize() / 2.0
					+ SpaceTravelManager.entryPadding(celestial.getActualSize()) + padding;
			if (
					Math.abs(local.x) <= halfExtent &&
					Math.abs(local.y) <= halfExtent &&
					Math.abs(local.z) <= halfExtent
			) {
				double distanceSquared = position.distanceSquared(celestial.getPosition(ticks, registry));
				if (distanceSquared < nearestDistanceSquared) {
					nearestDistanceSquared = distanceSquared;
					nearest = new EnteredCelestial(celestial, local);
				}
			}
		}

		return nearest;
	}

	private static Vector3d toLocalCelestialPoint(Celestial celestial, Registry<Celestial> registry, long ticks, Vector3dc position) {
		Vector3d local = new Vector3d(position).sub(celestial.getPosition(ticks, registry));
		celestial.getRotation(ticks, 0.0f, registry).conjugate(new Quaterniond()).transform(local);
		return local;
	}

	private record EnteredCelestial(Celestial celestial, Vector3dc localPoint) {}
}
