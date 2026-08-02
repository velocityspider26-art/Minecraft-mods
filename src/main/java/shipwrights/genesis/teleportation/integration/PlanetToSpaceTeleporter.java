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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Quaterniondc;
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
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.teleportation.ArrivalGate;
import shipwrights.genesis.teleportation.CubeNetSurfaceTransform;
import shipwrights.genesis.teleportation.SpaceTravelManager;
import shipwrights.genesis.teleportation.impl.EntityTeleporter;

import java.util.ArrayList;

/**
 * Atmosphere exit: climbing above a planet's atmosphere ceiling lifts the
 * traveler straight into the space dimension above their planet's celestial —
 * no fade, no delay; the space-side entry chunks are pre-generated while the
 * traveler climbs the final stretch so the swap is cheap.
 *
 * Ships travel as one unit (craft + seats + crew) via {@link SpaceTravelManager};
 * players aboard a ship are left to travel with it.
 */
public class PlanetToSpaceTeleporter {
	/** "Near space" buffer above the atmosphere ceiling before the dimension swap. */
	private static final double SPACE_TELEPORT_BUFFER = 1000.0;
	/** Blocks below the exit ceiling where space chunks start pre-generating. */
	private static final double PRE_EXIT_BAND = 60.0;
	private static final int PREGEN_INTERVAL_TICKS = 100;

	private static final TicketType<BlockPos> ASCENT_TICKET =
			TicketType.create("genesis_ascent", Vec3i::compareTo, 400);

	private long pregenClock;

	private final boolean gameTest;

	public PlanetToSpaceTeleporter(boolean gameTest) {
		this.gameTest = gameTest;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel level) {
			tick(level);
		}
	}

	private void tick(ServerLevel level) {
		if (GenesisMod.isSpaceDimension(level) || GenesisMod.isSubSpaceDimension(level)) {
			return;
		}

		Celestial celestial = GenesisMod.getCelestialForLevel(level);
		if (celestial == null) {
			return;
		}

		ServerLevel spaceLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM));
		if (spaceLevel == null) {
			return;
		}

		pregenClock++;

		// Space begins at this world's own atmosphere ceiling — far lower on an
		// airless world like Mercury than on Earth — plus a 1,000-block "near
		// space" buffer so the swap never snaps the instant you cross.
		double exitHeight = AtmosphereHeights.exitHeight(celestial) + SPACE_TELEPORT_BUFFER;

		tickPlayers(level, spaceLevel, celestial, exitHeight);
		tickShips(level, spaceLevel, celestial, exitHeight);
	}

	private void tickPlayers(ServerLevel level, ServerLevel spaceLevel, Celestial celestial, double exitHeight) {
		for (ServerPlayer player : new ArrayList<>(level.getPlayers(p -> true))) {
			if (player.isSpectator()) {
				continue;
			}

			// A player crossing the boundary while aboard a ship launches the
			// whole travel unit. Waiting for the ship's own crossing is a trap:
			// if the player teleported alone first, the origin dimension stops
			// ticking (no players left) and the ship hangs mid-air forever.
			dev.ryanhcode.sable.sublevel.ServerSubLevel ship = SpaceTravelManager.shipCarrying(level, player);
			if (ship != null) {
				if (ArrivalGate.isHeld(ship.getUniqueId())) {
					continue; // crossing committed; waiting on the destination
				}
				Vector3dc shipPos = ship.logicalPose().position();
				double referenceY = Math.max(shipPos.y(), player.getY());
				if (referenceY >= exitHeight) {
					Vec3 target = computeSpaceTarget(level, celestial, shipPos.x(), shipPos.z());
					GenesisMod.LOGGER.info("[LAUNCH] Ship {} aboard {} crossing to space at y={}",
							ship.getUniqueId(), player.getGameProfile().getName(), (long) referenceY);
					holdShipCrossing(level, ship, spaceLevel, target);
				} else if (referenceY > exitHeight - PRE_EXIT_BAND) {
					// The ship is what crosses, so warm the chunks IT will arrive
					// in — a player standing off-centre on a large deck names a
					// different column, and that is not where anyone lands.
					pregenAscent(level, spaceLevel, celestial, shipPos.x(), shipPos.z());
				}
				continue;
			}

			if (ArrivalGate.isHeld(player.getUUID())) {
				continue;
			}

			if (player.getY() < exitHeight) {
				if (player.getY() > exitHeight - PRE_EXIT_BAND) {
					pregenAscent(level, spaceLevel, celestial, player.getX(), player.getZ());
				}
				continue;
			}

			// Last-ditch safety net: if aboard-detection missed a ship the player
			// is actually standing on (large decks put crew well off the ship's
			// centre), carry the whole ship instead of teleporting the player
			// alone and stranding it. This is exactly the "physics object doesn't
			// come with you" case.
			ServerSubLevel nearbyShip = SpaceTravelManager.nearestShip(level, player.position(), 96.0);
			if (nearbyShip != null) {
				if (ArrivalGate.isHeld(nearbyShip.getUniqueId())) {
					continue;
				}
				Vector3dc shipPos = nearbyShip.logicalPose().position();
				Vec3 shipTarget = computeSpaceTarget(level, celestial, shipPos.x(), shipPos.z());
				GenesisMod.LOGGER.info("[LAUNCH] Solo player {} was on ship {} (radius net) -> carrying whole ship",
						player.getGameProfile().getName(), nearbyShip.getUniqueId());
				holdShipCrossing(level, nearbyShip, spaceLevel, shipTarget);
				continue;
			}

			Vec3 target = computeSpaceTarget(level, celestial, player.getX(), player.getZ());
			GenesisMod.LOGGER.info("[LAUNCH] Player {} crossed to space solo at y={}",
					player.getGameProfile().getName(), (long) player.getY());
			SpaceTravelManager.logShips(level, player.position(), "LAUNCH-SOLO");
			holdPlayerCrossing(level, player, spaceLevel, target);
		}
	}

	/**
	 * Commits a solo player's launch, to fire once space has terrain to receive
	 * them. Re-resolved when it runs — a hold can outlive the player's session.
	 */
	private static void holdPlayerCrossing(ServerLevel level, ServerPlayer player, ServerLevel spaceLevel, Vec3 target) {
		java.util.UUID travellerId = player.getUUID();
		MinecraftServer server = level.getServer();
		ArrivalGate.hold(travellerId, spaceLevel, target, () -> {
			ServerPlayer launching = server.getPlayerList().getPlayer(travellerId);
			if (launching == null || launching.level() != level) {
				return;
			}
			Vec3 velocity = launching.getDeltaMovement();
			EntityTeleporter.teleportEntityAndPassengers(launching, spaceLevel, target);
			SpaceTravelManager.recordSpaceArrival(travellerId, target);
			launching.setDeltaMovement(velocity);
			launching.resetFallDistance();
			GenesisMod.refreshEntityScaling(launching, spaceLevel);
		});
	}

	/**
	 * Commits a craft's launch, to fire once space has terrain to receive it.
	 * The craft is re-resolved by id when the crossing runs, because Sable can
	 * unload a sub-level on any tick and moving a stale handle would strand its
	 * crew.
	 */
	private static void holdShipCrossing(ServerLevel level, ServerSubLevel ship, ServerLevel spaceLevel, Vec3 target) {
		java.util.UUID shipId = ship.getUniqueId();
		ResourceKey<Level> originKey = level.dimension();
		MinecraftServer server = level.getServer();
		ArrivalGate.hold(shipId, spaceLevel, target, () -> {
			ServerLevel origin = server.getLevel(originKey);
			ServerSubLevel current = origin == null ? null : SpaceTravelManager.getShip(origin, shipId);
			if (current == null) {
				GenesisMod.LOGGER.warn("[LAUNCH] craft {} left {} before its crossing fired",
						shipId, originKey.location());
				return;
			}
			for (ServerPlayer crew : SpaceTravelManager.crewPlayers(origin, current)) {
				GenesisNetworking.sendToPlayer(crew, HyperspaceStatePacket.loadingHint());
				SpaceTravelManager.recordSpaceArrival(crew.getUUID(), target);
			}
			SpaceTravelManager.recordSpaceArrival(shipId, target);
			// Crew first, ship a moment later — see transferShipAfterCrew.
			SpaceTravelManager.transferShipAfterCrew(current, spaceLevel, target);
		});
	}

	/** Sable ships that climb above the atmosphere launch into orbit as one unit. */
	private void tickShips(ServerLevel level, ServerLevel spaceLevel, Celestial celestial, double exitHeight) {
		ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
		if (container == null) {
			return;
		}

		for (ServerSubLevel ship : new ArrayList<>(container.getAllSubLevels())) {
			if (ArrivalGate.isHeld(ship.getUniqueId())) {
				continue; // crossing committed; waiting on the destination
			}
			Vector3dc shipPos = ship.logicalPose().position();

			if (shipPos.y() < exitHeight) {
				if (shipPos.y() > exitHeight - PRE_EXIT_BAND) {
					pregenAscent(level, spaceLevel, celestial, shipPos.x(), shipPos.z());
				}
				continue;
			}

			Vec3 target = computeSpaceTarget(level, celestial, shipPos.x(), shipPos.z());
			holdShipCrossing(level, ship, spaceLevel, target);
		}
	}

	/**
	 * Pre-generates the space-side entry chunks while a traveler climbs the last
	 * stretch.
	 *
	 * <p>The target comes from the same cube-net inverse as the real launch, so
	 * pre-generation and transfer cannot disagree about which face of the body
	 * receives the traveler. The height matters too: the arrival sits at a real
	 * y in space, and a region ticket around y=0 claims a different column of
	 * chunks than the one the traveler lands in.</p>
	 */
	private void pregenAscent(ServerLevel level, ServerLevel spaceLevel, Celestial celestial, double x, double z) {
		if (pregenClock % PREGEN_INTERVAL_TICKS != 0) {
			return;
		}

		Vec3 target = computeSpaceTarget(level, celestial, x, z);
		ArrivalGate.prepare(spaceLevel, target);
		ChunkPos center = new ChunkPos(BlockPos.containing(target.x, target.y, target.z));
		spaceLevel.getChunkSource().addRegionTicket(ASCENT_TICKET, center, 3, center.getWorldPosition());
	}

    // Package-private — accessed by tests
	static Quaterniondc computeSpaceRotation(Quaterniondc vantageRotation, Quaterniondc shipRotation) {
		return new Quaterniond(vantageRotation).mul(shipRotation, new Quaterniond());
	}

	private static Vector3d computeSpaceTarget(VantagePoint.OnCelestial vantagePoint) {
		Vector3d targetPos = new Vector3d(0, vantagePoint.celestial().getActualSize() + 20, 0);
		vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()).transform(targetPos);
		vantagePoint.getCelestialRotation().transform(targetPos);
		targetPos.add(vantagePoint.getPosition());
		return targetPos;
    }

	/**
	 * Inverts the cube-net surface transform and places the traveler just off
	 * the matching face of the rotating celestial.
	 *
	 * <p>No per-player memory participates. The planet-world X/Z identifies a
	 * face region and texture-oriented coordinates inside it; those reconstruct
	 * the exact point on the padded entry cube. Normalising that direction to the
	 * exit standoff gives an exact entry/exit round trip even after relogging or
	 * changing ships.</p>
	 */
	private static Vec3 computeSpaceTarget(ServerLevel level, Celestial celestial, double x, double z) {
		Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
		long ticks = GenesisMod.getTicks(level);

		CubeNetSurfaceTransform transform = new CubeNetSurfaceTransform(
				celestial.getActualSize(), SpaceTravelManager.entryPadding(celestial.getActualSize()));
		CubeNetSurfaceTransform.LocalPoint shell = transform.worldToLocal(x, z);
		Vector3d local = new Vector3d(shell.x(), shell.y(), shell.z());
		if (local.lengthSquared() < 1.0E-9) {
			local.set(0.0, 1.0, 0.0);
		}

		double crossingHeight = AtmosphereHeights.exitHeight(celestial) + SPACE_TELEPORT_BUFFER;
		double exitRadius = scaleMatchedExitRadius(transform, crossingHeight, level.getSeaLevel());
		local.normalize(exitRadius);
		celestial.getRotation(ticks, 0.0f, registry).transform(local);

		Vector3d center = new Vector3d(celestial.getPosition(ticks, registry));
		Vec3 target = new Vec3(center.x + local.x, center.y + local.y, center.z + local.z);
		GenesisMod.LOGGER.info("[CUBENET] launch from {}{} surface ({}, {}) -> local ({}, {}, {}), radius={}",
				shell.face(), shell.outsideNetFallback() ? " [legacy fallback]" : "",
				Math.round(x), Math.round(z),
				Math.round(shell.x()), Math.round(shell.y()), Math.round(shell.z()),
				Math.round(exitRadius));
		return target;
	}

	/**
	 * Converts the planet-side crossing altitude into the equivalent distance
	 * from the celestial centre in the 1:16 space frame. This keeps the planet's
	 * apparent size continuous across the seamless room handoff. The padded
	 * entry shell remains the hard minimum so a launch can never immediately
	 * trigger re-entry.
	 */
	static double scaleMatchedExitRadius(CubeNetSurfaceTransform transform, double crossingHeight, double seaLevel) {
		double altitudeAboveSurface = Math.max(0.0, crossingHeight - seaLevel);
		double scaleMatched = transform.halfExtent()
				+ altitudeAboveSurface * SpaceTravelManager.SPACE_BLOCKS_PER_PLANET_BLOCK;
		double legacyClearance = transform.halfExtent() + SpaceTravelManager.ATMOSPHERE_EXIT_CLEARANCE;
		return Math.max(transform.entryRadius() + 1.0, Math.max(legacyClearance, scaleMatched));
	}

}
