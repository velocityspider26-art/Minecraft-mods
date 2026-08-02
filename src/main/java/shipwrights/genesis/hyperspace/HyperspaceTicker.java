package shipwrights.genesis.hyperspace;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.networking.HyperspaceStatePacket;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.teleportation.SpaceTravelManager;
import shipwrights.genesis.teleportation.impl.EntityTeleporter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Drives hyperdrive jumps. Hyperspace is a real, traversable dimension
 * ({@code genesis:subspace}) that maps 1:32 onto celestial space — a jump is
 * physical travel:
 *
 * <pre>origin (spool) -> subspace (fly to the destination's coordinates) -> space</pre>
 *
 * There is no transit timer. The ship autopilots toward the destination's
 * compressed position while the crew is free to walk around; arrival triggers
 * by distance, and the jump drops out in the SPACE dimension on a standoff
 * ring outside the destination — the final approach is flown for real.
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class HyperspaceTicker {
    public static final ResourceKey<Level> SUBSPACE_KEY =
            ResourceKey.create(Registries.DIMENSION, GenesisMod.WORMHOLE_DIM);

    private static final double MAX_CRUISE_VELOCITY = 5.5;
    private static final int SUBSPACE_ENTRY_MIN_Y = 72;
    private static final int SUBSPACE_ENTRY_MAX_Y = 288;
    private static final double SUBSPACE_CRUISE_ALTITUDE = 144.0;

    /**
     * Autopilot cruise speed in m/s for a class-1 drive; actual speed is this
     * divided by the drive class (class 6 crawls, class 0.5 doubles it, and
     * the Falken's class 0.1 does 300 m/s). Time in hyperspace is therefore
     * distance / class speed — bigger jumps genuinely take longer.
     */
    private static final double CLASS_1_CRUISE_SPEED = 12.0;
    /** Cruise speed for shipless jumps, blocks per tick (also scaled by class). */
    private static final double PLAYER_CRUISE_SPEED = 1.6;
    /** Horizontal distance (subspace blocks) at which arrival triggers. */
    private static final double ARRIVAL_RADIUS = 40.0;

    /** Ticks before a teleport at which the client is told to flash to white. */
    private static final int FLASH_LEAD_TICKS = 10;
    /** Length of the exiting phase: flash rises, then the drop-out fires. */
    private static final int EXIT_TICKS = 12;
    /** Chunk ticket that pre-generates jump destinations; times out on its own. */
    private static final TicketType<BlockPos> PREGEN_TICKET =
            TicketType.create("genesis_hyperspace", Vec3i::compareTo, 400);
    private static final int PREGEN_RADIUS = 3;

    private HyperspaceTicker() {
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLevelTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        for (ServerPlayer player : level.getPlayers(HyperspaceData::isActive)) {
            tickPlayer(level, player);
        }
    }

    private static void tickPlayer(ServerLevel level, ServerPlayer player) {
        int ticks = HyperspaceData.advance(player);
        switch (HyperspaceData.phase(player)) {
            case HyperspaceData.PHASE_SPOOL -> tickSpool(level, player, ticks);
            case HyperspaceData.PHASE_TRANSIT -> tickTransit(level, player, ticks);
            case HyperspaceData.PHASE_EXITING -> tickExiting(level, player, ticks);
            default -> tickCruise(level, player, ticks);
        }
    }

    // --- Free cruise (/hyperspace enter debug mode) ---

    private static void tickCruise(ServerLevel level, ServerPlayer player, int ticks) {
        int durationTicks = HyperspaceData.durationTicks(player);
        if (durationTicks > 0 && ticks >= durationTicks) {
            HyperspaceData.exit(player);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.65f, 1.45f);
            return;
        }

        double speed = HyperspaceData.speed(player);
        Vec3 look = player.getLookAngle();
        Vec3 targetVelocity = look.scale(speed);
        Vec3 blendedVelocity = player.getDeltaMovement().scale(0.82).add(targetVelocity.scale(0.18));
        player.setDeltaMovement(limit(blendedVelocity, MAX_CRUISE_VELOCITY));
        player.resetFallDistance();
        applyTravelEffects(player);

        if (ticks == 1) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
    }

    // --- Phase 1: spooling up in the origin dimension ---

    private static void tickSpool(ServerLevel level, ServerPlayer player, int ticks) {
        HyperspaceDestination destination = HyperspaceData.destination(player);
        if (destination == null) {
            cancel(level, player, "Hyperspace failed: destination lost");
            return;
        }

        player.resetFallDistance();
        applyTravelEffects(player);

        int durationTicks = HyperspaceData.durationTicks(player);
        MinecraftServer server = level.getServer();

        if (ticks == 1) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 0.65f);
            // Start generating the subspace entry chunks now so the swap is cheap.
            ServerLevel subspace = server.getLevel(SUBSPACE_KEY);
            if (subspace != null) {
                Vec3 entry = subspaceEntry(level, player, subspace);
                pregenerate(subspace, entry.x, entry.z);
            }
        }

        // Flash to white shortly before the teleport so the swap hitch is hidden.
        if (ticks == durationTicks - FLASH_LEAD_TICKS) {
            Vec3 tunnelBearing = destinationBearing(level, player, destination);
            for (ServerPlayer member : jumpCrew(level, player)) {
                GenesisNetworking.sendToPlayer(member, HyperspaceStatePacket.enterFlash(30, tunnelBearing));
                GenesisNetworking.sendToPlayer(member, HyperspaceStatePacket.loadingHint());
            }
        }

        if (ticks >= durationTicks) {
            enterHyperspace(level, player, destination);
        }
    }

    private static void enterHyperspace(ServerLevel originLevel, ServerPlayer player, HyperspaceDestination destination) {
        MinecraftServer server = originLevel.getServer();
        ServerLevel subspace = server.getLevel(SUBSPACE_KEY);
        if (subspace == null) {
            cancel(originLevel, player, "Hyperspace failed: subspace dimension missing");
            return;
        }
        if (server.getLevel(destination.dimension()) == null) {
            cancel(originLevel, player, "Hyperspace failed: space dimension missing");
            return;
        }

        BlockPos source = HyperspaceData.sourceBlock(player);
        ServerSubLevel ship = source != null ? SpaceTravelManager.findShipContaining(originLevel, source) : null;

        Entity root = player.getRootVehicle();
        Vec3 anchor = ship != null ? toVec3(ship.logicalPose().position()) : root.position();
        Vec3 entry = subspaceEntry(originLevel, player, subspace);

        UUID shipId = null;
        if (ship != null) {
            if (SpaceTravelManager.transferShip(ship, subspace, entry, travelOrientation(ship, destination, subspace, entry))) {
                shipId = ship.getUniqueId();
            } else {
                GenesisMod.LOGGER.warn("Hyperdrive jump continues without ship (transfer failed)");
            }
        }

        // transferShip's reconcileCrew carries the player into subspace on the
        // ship. Only move separately when there was no ship.
        if (shipId == null && player.level() != subspace) {
            Entity arrivedRoot = EntityTeleporter.teleportEntityAndPassengers(root, subspace, entry, new Quaterniond());
            if (arrivedRoot != null) {
                arrivedRoot.setDeltaMovement(Vec3.ZERO);
                arrivedRoot.resetFallDistance();
            }
        }

        HyperspaceData.beginTransit(player, shipId);
        subspace.playSound(null, BlockPos.containing(entry), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.55f);
    }

    /**
     * Maps the traveler's position into hyperspace coordinates. Everything is
     * expressed in the celestial (space) frame first — a planet position becomes
     * "the planet's celestial, offset by your surface position at 1:16" — then
     * compressed by the subspace coordinate scale. This is what makes hyperspace
     * an actual map of the solar system rather than an arbitrary void.
     */
    private static Vec3 subspaceEntry(ServerLevel originLevel, ServerPlayer player, ServerLevel subspace) {
        ServerSubLevel ship = jumpShip(originLevel, player);
        Entity root = player.getRootVehicle();
        Vec3 anchor = ship != null ? toVec3(ship.logicalPose().position()) : root.position();

        Vec3 spacePos;
        if (GenesisMod.isSpaceDimension(originLevel)) {
            spacePos = anchor;
        } else {
            Celestial originCelestial = GenesisMod.getCelestialForLevel(originLevel);
            if (originCelestial != null) {
                Vector3dc center = originCelestial.getPosition(GenesisMod.getTicks(originLevel),
                        GenesisMod.getCelestialRegistry(originLevel));
                spacePos = new Vec3(
                        center.x() + anchor.x * SpaceTravelManager.SPACE_BLOCKS_PER_PLANET_BLOCK,
                        anchor.y,
                        center.z() + anchor.z * SpaceTravelManager.SPACE_BLOCKS_PER_PLANET_BLOCK);
            } else {
                spacePos = anchor;
            }
        }

        double compression = subspace.dimensionType().coordinateScale();
        double entryY = Mth.clamp(anchor.y, SUBSPACE_ENTRY_MIN_Y, SUBSPACE_ENTRY_MAX_Y);
        return new Vec3(spacePos.x / compression, entryY, spacePos.z / compression);
    }

    // --- Phase 2: flying through hyperspace, no timer ---

    private static void tickTransit(ServerLevel level, ServerPlayer player, int ticks) {
        HyperspaceDestination destination = HyperspaceData.destination(player);

        // Left subspace by other means (death, commands, void engine) — jump is over.
        if (level.dimension() != SUBSPACE_KEY) {
            HyperspaceData.exit(player);
            GenesisNetworking.sendToPlayer(player, HyperspaceStatePacket.cancel(25));
            return;
        }

        player.resetFallDistance();
        applyTravelEffects(player);

        ServerSubLevel ship = jumpShip(level, player);
        Vec3 anchor = ship != null ? toVec3(ship.logicalPose().position()) : player.position();

        Vec3 target = destination != null ? destination.subspaceTarget(level) : null;
        if (target == null) {
            // No destination to fly to: drop out where we are.
            beginExit(level, player, ship);
            return;
        }

        double dx = target.x - anchor.x;
        double dz = target.z - anchor.z;
        double distance = Math.sqrt(dx * dx + dz * dz);

        // Stop OUTSIDE the destination's footprint, not at its center. A big
        // body (the sun) has a large subspace radius; arriving within a fixed
        // 40 blocks would drop the ship inside it, and the exit direction would
        // be degenerate — that's the "teleported somewhere random" bug.
        double arrivalRadius = ARRIVAL_RADIUS;
        Celestial destCelestial = destination.celestial(level);
        if (destCelestial != null) {
            double compression = level.dimensionType().coordinateScale();
            double subspaceRadius = (destCelestial.getActualSize() / 2.0) / compression;
            arrivalRadius = Math.max(ARRIVAL_RADIUS, subspaceRadius * 1.5);
        }

        if (distance <= arrivalRadius) {
            beginExit(level, player, ship);
            return;
        }

        autopilot(level, player, ship, anchor, dx / distance, dz / distance);

        // Distance + ETA readout in planet-scale kilometers.
        if (ticks % 40 == 0) {
            double compression = level.dimensionType().coordinateScale();
            double surfaceBlocks = distance * compression / SpaceTravelManager.SPACE_BLOCKS_PER_PLANET_BLOCK;
            double cruiseSpeed = CLASS_1_CRUISE_SPEED / HyperspaceData.driveClass(player);
            int etaSeconds = (int) Math.ceil(distance / cruiseSpeed);
            player.displayClientMessage(Component.literal(String.format(
                    "%s — %.1f km — %d:%02d", destination.displayName(), surfaceBlocks / 1000.0,
                    etaSeconds / 60, etaSeconds % 60)), true);
        }

        // Warm up the space-side arrival chunks during the final approach.
        if (distance < 120.0 && ticks % 100 == 0) {
            ServerLevel spaceLevel = level.getServer().getLevel(destination.dimension());
            if (spaceLevel != null) {
                double compression = level.dimensionType().coordinateScale();
                Vec3 arrival = destination.resolveArrival(spaceLevel, anchor.x * compression, anchor.z * compression);
                pregenerate(spaceLevel, arrival.x, arrival.z);
            }
        }
    }

    /**
     * Flies the travel unit toward the destination with no input needed —
     * the crew can leave the helm and walk around. Velocity converges hard on
     * the cruise vector so propellers, gravity quirks, or collisions can't
     * stall the jump.
     */
    private static void autopilot(ServerLevel level, ServerPlayer player, ServerSubLevel ship,
                                  Vec3 anchor, double dirX, double dirZ) {
        double cruiseSpeed = CLASS_1_CRUISE_SPEED / HyperspaceData.driveClass(player);

        if (ship != null) {
            RigidBodyHandle handle = RigidBodyHandle.of(ship);
            if (handle == null || !handle.isValid()) {
                return;
            }
            Vector3d velocity = handle.getLinearVelocity(new Vector3d());
            double targetY = Mth.clamp((SUBSPACE_CRUISE_ALTITUDE - anchor.y) * 0.05, -3.0, 3.0);
            Vector3d desired = new Vector3d(dirX * cruiseSpeed, targetY, dirZ * cruiseSpeed);
            Vector3d dv = desired.sub(velocity, new Vector3d()).mul(0.3);
            Vector3d angular = handle.getAngularVelocity(new Vector3d()).mul(-0.05);
            handle.addLinearAndAngularVelocity(dv, angular);
            return;
        }

        double playerSpeed = PLAYER_CRUISE_SPEED * Math.min(4.0, 1.0 / HyperspaceData.driveClass(player) * 2.0);
        Vec3 desired = new Vec3(dirX * playerSpeed,
                (SUBSPACE_CRUISE_ALTITUDE - player.getY()) * 0.004, dirZ * playerSpeed);
        Vec3 blended = player.getDeltaMovement().scale(0.7).add(desired.scale(0.3));
        player.setDeltaMovement(limit(blended, Math.max(MAX_CRUISE_VELOCITY, playerSpeed)));
    }

    // --- Phase 3: arrival — flash up, then drop out into space ---

    private static void beginExit(ServerLevel level, ServerPlayer player, ServerSubLevel ship) {
        HyperspaceData.beginExiting(player);
        List<ServerPlayer> crew = ship != null
                ? SpaceTravelManager.crewPlayers(level, ship)
                : List.of(player);
        for (ServerPlayer member : crew) {
            GenesisNetworking.sendToPlayer(member, HyperspaceStatePacket.postTunnel(45));
            GenesisNetworking.sendToPlayer(member, HyperspaceStatePacket.loadingHint());
        }
        if (!crew.contains(player)) {
            GenesisNetworking.sendToPlayer(player, HyperspaceStatePacket.postTunnel(45));
        }
    }

    private static void tickExiting(ServerLevel level, ServerPlayer player, int ticks) {
        if (level.dimension() != SUBSPACE_KEY) {
            HyperspaceData.exit(player);
            return;
        }

        player.resetFallDistance();
        applyTravelEffects(player);

        if (ticks < EXIT_TICKS) {
            return;
        }

        HyperspaceDestination destination = HyperspaceData.destination(player);
        ServerLevel spaceLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM));
        if (spaceLevel == null) {
            cancel(level, player, "Hyperspace failed: space dimension missing");
            return;
        }

        Entity root = player.getRootVehicle();
        ServerSubLevel ship = jumpShip(level, player);
        // Never anchor on a shadow-region coordinate: a seated player's raw
        // position is a plot coord (~640k), and using it lands the jump at
        // 640k*32 ≈ 20 million blocks out. Prefer the ship's visible pose;
        // if there's genuinely no ship, use the player's own position only if
        // it looks real, else the target's mapped coords.
        Vec3 exitAnchor;
        if (ship != null) {
            exitAnchor = toVec3(ship.logicalPose().position());
        } else if (Math.abs(root.getX()) < 100000.0 && Math.abs(root.getZ()) < 100000.0) {
            exitAnchor = root.position();
        } else {
            Vec3 subTarget = destination != null ? destination.subspaceTarget(level) : null;
            exitAnchor = subTarget != null ? subTarget : new Vec3(0, 96, 0);
        }

        // Decompress back into the celestial frame.
        double compression = level.dimensionType().coordinateScale();
        double spaceX = exitAnchor.x * compression;
        double spaceZ = exitAnchor.z * compression;
        // Never land at a raw (possibly shadow-region) coordinate. With a
        // destination, resolveArrival clamps near the celestial; without one,
        // clamp the manual drop-out to a sane magnitude.
        Vec3 arrival = destination != null
                ? destination.resolveArrival(spaceLevel, spaceX, spaceZ)
                : new Vec3(Mth.clamp(spaceX, -60000, 60000), 96.0, Mth.clamp(spaceZ, -60000, 60000));

        boolean shipMoved = ship != null && SpaceTravelManager.transferShip(ship, spaceLevel, arrival);
        if (ship != null && !shipMoved) {
            GenesisMod.LOGGER.warn("Hyperdrive exit continues without ship (transfer failed)");
        }
        // Arm the entry buffer so the drop-out near the destination doesn't
        // instantly get pulled into it.
        SpaceTravelManager.recordSpaceArrival(player.getUUID(), arrival);
        if (shipMoved) {
            SpaceTravelManager.recordSpaceArrival(ship.getUniqueId(), arrival);
        }

        HyperspaceData.exit(player);

        // transferShip's reconcileCrew already carried the player onto the ship
        // at the arrival. Only teleport separately when there was no ship to
        // carry them, and never using a shadow-region root position.
        if (!shipMoved && player.level() != spaceLevel) {
            Entity arrivedRoot = EntityTeleporter.teleportEntityAndPassengers(root, spaceLevel, arrival, new Quaterniond());
            if (arrivedRoot != null) {
                arrivedRoot.setDeltaMovement(Vec3.ZERO);
                arrivedRoot.resetFallDistance();
            }
        }

        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 140, 0, true, false));
        spaceLevel.playSound(null, BlockPos.containing(arrival), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    /** Manual bail-out: drop out of hyperspace at the current position. */
    public static void requestManualExit(ServerLevel level, ServerPlayer player) {
        if (level.dimension() != SUBSPACE_KEY || !HyperspaceData.isActive(player)) {
            return;
        }
        HyperspaceData.clearDestination(player);
        ServerSubLevel ship = jumpShip(level, player);
        beginExit(level, player, ship);
    }

    // --- Helpers ---

    /**
     * The orientation a ship arrives in hyperspace with. A ship's true "forward"
     * is unknowable from geometry alone, but a moving Create craft is flying
     * along its nose — so we take the ship's horizontal velocity as forward and
     * yaw the ship (preserving pitch/roll) so that heading points down the
     * tunnel at the destination. A stationary ship keeps its orientation (no
     * snap) since there's no motion to define forward.
     */
    private static org.joml.Quaterniondc travelOrientation(ServerSubLevel ship,
                                                           HyperspaceDestination destination,
                                                           ServerLevel subspace, Vec3 entry) {
        Vec3 target = destination.subspaceTarget(subspace);
        if (target == null) {
            return null;
        }
        double tx = target.x - entry.x;
        double tz = target.z - entry.z;
        double tLen = Math.sqrt(tx * tx + tz * tz);
        if (tLen < 1.0E-3) {
            return null;
        }

        RigidBodyHandle handle = RigidBodyHandle.of(ship);
        if (handle == null || !handle.isValid()) {
            return null;
        }
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());
        double vLen = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (vLen < 0.05) {
            return null; // parked: don't snap
        }

        Vector3d forward = new Vector3d(velocity.x / vLen, 0.0, velocity.z / vLen);
        Vector3d targetDir = new Vector3d(tx / tLen, 0.0, tz / tLen);
        // Yaw-only delta from current heading to the tunnel direction.
        org.joml.Quaterniond yawDelta = new org.joml.Quaterniond().rotateTo(forward, targetDir);
        return yawDelta.mul(new org.joml.Quaterniond(ship.logicalPose().orientation()));
    }

    /**
     * Keeps the traveler's offset from the ship only when it is a sane
     * deck-scale distance. A player who was riding a plot seat has raw
     * coordinates in the shadow region (~640k blocks out) — blindly applying
     * that offset is how people ended up in deep space or below the void.
     */
    private static Vec3 safeRelativeArrival(Vec3 base, Vec3 rootPosition, Vec3 anchor) {
        Vec3 offset = rootPosition.subtract(anchor);
        if (offset.length() < 128.0) {
            return base.add(offset);
        }
        return base.add(0.0, 2.0, 0.0);
    }

    private static ServerSubLevel jumpShip(ServerLevel level, ServerPlayer player) {
        // Most reliable first: the ship whose plot physically contains the
        // seated player. The stored shipId can go stale across the transfer,
        // and the source block is an origin-dimension plot coord that won't
        // resolve in subspace — so falling through to those left ship == null
        // and dumped the player at their shadow-region seat position.
        ServerSubLevel ship = SpaceTravelManager.shipCarrying(level, player);
        if (ship != null) {
            return ship;
        }
        ship = SpaceTravelManager.getShip(level, HyperspaceData.shipId(player));
        if (ship == null) {
            BlockPos source = HyperspaceData.sourceBlock(player);
            ship = source != null ? SpaceTravelManager.findShipContaining(level, source) : null;
        }
        return ship;
    }

    private static List<ServerPlayer> jumpCrew(ServerLevel level, ServerPlayer pilot) {
        List<ServerPlayer> crew = new ArrayList<>();
        ServerSubLevel ship = jumpShip(level, pilot);
        if (ship != null) {
            crew.addAll(SpaceTravelManager.crewPlayers(level, ship));
        }
        if (!crew.contains(pilot)) {
            crew.add(pilot);
        }
        return crew;
    }

    private static Vec3 destinationBearing(ServerLevel originLevel, ServerPlayer player,
                                           HyperspaceDestination destination) {
        ServerLevel subspace = originLevel.getServer().getLevel(SUBSPACE_KEY);
        if (subspace != null) {
            Vec3 entry = subspaceEntry(originLevel, player, subspace);
            Vec3 target = destination.subspaceTarget(subspace);
            if (target != null) {
                Vec3 bearing = new Vec3(target.x - entry.x, 0.0, target.z - entry.z);
                if (bearing.lengthSqr() > 1.0E-6) {
                    return bearing.normalize();
                }
            }
        }
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        return horizontal.lengthSqr() > 1.0E-6 ? horizontal.normalize() : new Vec3(0.0, 0.0, 1.0);
    }

    /** Queues async chunk generation around the given position (nearest chunks first). */
    private static void pregenerate(ServerLevel level, double x, double z) {
        ChunkPos center = new ChunkPos(BlockPos.containing(x, 0, z));
        level.getChunkSource().addRegionTicket(PREGEN_TICKET, center, PREGEN_RADIUS, center.getWorldPosition());
    }

    private static void cancel(ServerLevel level, ServerPlayer player, String message) {
        HyperspaceData.exit(player);
        GenesisNetworking.sendToPlayer(player, HyperspaceStatePacket.cancel(35));
        player.displayClientMessage(Component.literal(message), true);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 0.8f);
    }

    private static void applyTravelEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false));
    }

    private static Vec3 toVec3(Vector3dc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }

    private static Vec3 limit(Vec3 velocity, double maxLength) {
        double length = velocity.length();
        if (length <= maxLength || length < 1.0E-6) {
            return velocity;
        }

        return velocity.scale(maxLength / length);
    }
}
