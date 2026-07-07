package shipwrights.genesis.teleportation.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.CubePlanetMapping;
import shipwrights.genesis.teleportation.DimensionTravelTeleporter;
import shipwrights.genesis.teleportation.TravelDirection;
import shipwrights.genesis.teleportation.impl.EntityTeleporter;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static shipwrights.genesis.teleportation.integration.Util.getSortedConstructs;

/**
 * Space-side half of the travel loop, run every tick in the Great Unknown.
 *
 * <p>Players and constructs are "captured" by a body's atmosphere when they come within its
 * approach radius (not when they clip inside it), and fall into that body's dimension — the Earth
 * via cube-face mapping so the side you approach decides where you land, the Moon via a surface
 * drop. Anyone who strays past {@code deepSpaceRadius} from Earth slips into deep space (subspace).
 * Every hop is a dimension change with a space dimension on one side, so the client's transition
 * screen hides the load and it never feels like a portal.</p>
 */
public class SpaceToPlanetTeleporter {

    private static final ResourceLocation EARTH_ID = ResourceLocation.parse("minecraft:overworld");

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

    private void tick(ServerLevel level) {
        long ticks = GenesisMod.getTicks(level);
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);

        Celestial earth = registry.get(EARTH_ID);
        Vector3dc earthPos = earth != null ? earth.getPosition(ticks, registry) : new Vector3d();
        double deepSpace = GenesisCommonConfig.getDeepSpaceRadius();

        // ---- constructs (Sable physics objects) ----
        for (AeronauticsConstruct construct : getSortedConstructs(level)) {
            if (construct.isRemoved() || construct.localBounds() == null) continue;
            var box = construct.worldBounds();
            Vector3d center = new Vector3d((box.minX() + box.maxX()) / 2, (box.minY() + box.maxY()) / 2, (box.minZ() + box.maxZ()) / 2);

            if (earth != null && center.distance(earthPos.x(), earthPos.y(), earthPos.z()) > deepSpace) {
                sendConstructToDeepSpace(level, construct);
                continue;
            }

            Celestial body = nearestWithinApproach(registry, ticks, center);
            if (body == null) continue;

            ResourceLocation bodyId = registry.getKey(body);
            ServerLevel target = dimensionFor(level, bodyId);
            if (target == null) continue;

            Vector3d landing = landingPosition(body, bodyId, ticks, registry, center);
            Quaterniond rotation = orientationToward(center, body, ticks, registry);
            DimensionTravelTeleporter.teleportConstruct(construct, TravelDirection.SPACE_TO_PLANET, level, target, landing, rotation);
        }

        // ---- free-flying players ----
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (player.isPassenger() || player.isRemoved()) continue;
            long arrival = player.getPersistentData().getLong(PlanetToSpaceTeleporter.SPACE_ARRIVAL_TAG);
            if (level.getGameTime() - arrival < 200) continue; // grace period after arriving in space

            Vector3d p = new Vector3d(player.getX(), player.getY(), player.getZ());

            if (earth != null && p.distance(earthPos.x(), earthPos.y(), earthPos.z()) > deepSpace) {
                sendPlayerToDeepSpace(player);
                continue;
            }

            Celestial body = nearestWithinApproach(registry, ticks, p);
            if (body == null) continue;

            ResourceLocation bodyId = registry.getKey(body);
            ServerLevel target = dimensionFor(level, bodyId);
            if (target == null) continue;

            Vector3d landing = landingPosition(body, bodyId, ticks, registry, p);
            Quaterniond rotation = orientationToward(p, body, ticks, registry);
            enterAtmosphere(player, target, landing, rotation, bodyId);
        }
    }

    /** Nearest visitable body whose atmosphere (radius + approach margin) already contains {@code point}. */
    private static @Nullable Celestial nearestWithinApproach(Registry<Celestial> registry, long ticks, Vector3dc point) {
        return registry.stream()
                .filter(c -> c.type().isVisitable())
                .filter(c -> {
                    Vector3dc bp = c.getPosition(ticks, registry);
                    double approach = isEarth(registry.getKey(c))
                            ? GenesisCommonConfig.getEarthApproachRadius()
                            : GenesisCommonConfig.getMoonApproachRadius();
                    return point.distance(bp.x(), bp.y(), bp.z()) <= c.getActualSize() * 0.5 + approach;
                })
                .min(Comparator.comparingDouble(c -> {
                    Vector3dc bp = c.getPosition(ticks, registry);
                    return point.distance(bp.x(), bp.y(), bp.z());
                }))
                .orElse(null);
    }

    /** Landing coordinate: cube-face mapped for the Earth, a plain surface drop otherwise. */
    private static Vector3d landingPosition(Celestial body, ResourceLocation bodyId, long ticks,
                                            Registry<Celestial> registry, Vector3dc approachPoint) {
        int y = GenesisCommonConfig.getAtmosphereEntryHeight();
        if (isEarth(bodyId)) {
            Vector3dc bodyPos = body.getPosition(ticks, registry);
            Vector3d dirWorld = new Vector3d(approachPoint).sub(bodyPos);
            // into the planet's local frame so the cube faces are axis-aligned
            Quaterniond rot = new Quaterniond(body.getRotation(ticks, 0f, registry)).conjugate();
            Vector3d dirLocal = new Vector3d(dirWorld).rotate(rot);
            double[] xz = CubePlanetMapping.landingXZ(dirLocal,
                    GenesisCommonConfig.getCubeFaceRegionSpacing(),
                    GenesisCommonConfig.getCubeFaceRegionSpacing() * 0.2);
            return new Vector3d(xz[0], y, xz[1]);
        }
        // Moon (and any other body): drop near its dimension origin.
        return new Vector3d(0, y, 0);
    }

    private static Quaterniond orientationToward(Vector3dc from, Celestial body, long ticks, Registry<Celestial> registry) {
        Vector3dc bp = body.getPosition(ticks, registry);
        Vector3d down = new Vector3d(from).sub(bp);
        if (down.lengthSquared() < 1.0e-9) return new Quaterniond();
        down.normalize();
        Quaterniond rotation = new Quaterniond().rotateTo(new Vector3d(0, 1, 0), down);
        body.getRotation(ticks, 0f, registry).mul(rotation, rotation).conjugate();
        return rotation;
    }

    private static void enterAtmosphere(ServerPlayer player, ServerLevel target, Vector3d landing,
                                        Quaterniondc rotation, ResourceLocation bodyId) {
        Vec3 carried = player.getDeltaMovement();
        GenesisMod.LOGGER.info("Player {} entered the atmosphere of {}", player.getGameProfile().getName(), target.dimension().location());
        EntityTeleporter.teleportEntityAndPassengers(player, target, new Vec3(landing.x, landing.y, landing.z), rotation);
        player.setDeltaMovement(carried); // preserve momentum through the transition
        player.hurtMarked = true;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 300, 0, false, false, true));
        player.displayClientMessage(Component.literal(isEarth(bodyId) ? "Entering the atmosphere" : "Descending to the surface")
                .withStyle(ChatFormatting.GOLD), true);
    }

    private static void sendPlayerToDeepSpace(ServerPlayer player) {
        ServerLevel subspace = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.WORMHOLE_DIM));
        if (subspace == null) return;
        Vec3 carried = player.getDeltaMovement();
        Vector3d landing = new Vector3d(player.getX() / 16.0, 256.0, player.getZ() / 16.0);
        GenesisMod.LOGGER.info("Player {} crossed into deep space", player.getGameProfile().getName());
        EntityTeleporter.teleportEntityAndPassengers(player, subspace, new Vec3(landing.x, landing.y, landing.z), new Quaterniond());
        player.setDeltaMovement(carried);
        player.getPersistentData().putLong(PlanetToSpaceTeleporter.SPACE_ARRIVAL_TAG, subspace.getGameTime());
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 60, 0, false, false, true));
        player.displayClientMessage(Component.literal("Crossing into deep space").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private static void sendConstructToDeepSpace(ServerLevel level, AeronauticsConstruct construct) {
        ServerLevel subspace = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.WORMHOLE_DIM));
        if (subspace == null) return;
        var box = construct.worldBounds();
        Vector3d landing = new Vector3d((box.minX() + box.maxX()) / 32.0, 256.0, (box.minZ() + box.maxZ()) / 32.0);
        DimensionTravelTeleporter.teleportConstruct(construct, TravelDirection.SPACE_TO_PLANET, level, subspace, landing, new Quaterniond());
    }

    private static @Nullable ServerLevel dimensionFor(ServerLevel level, @Nullable ResourceLocation bodyId) {
        if (bodyId == null) return null;
        return level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, bodyId));
    }

    private static boolean isEarth(@Nullable ResourceLocation bodyId) {
        return EARTH_ID.equals(bodyId);
    }

    /**
     * Public entry point for {@code /genesis land}: drop a player from space into the nearest
     * visitable body's atmosphere (Earth via cube-face mapping), overworld fallback.
     */
    public static boolean sendToNearestPlanet(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!GenesisMod.isSpaceDimension(level)) return false;

        long ticks = GenesisMod.getTicks(level);
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        Vector3d p = new Vector3d(player.getX(), player.getY(), player.getZ());

        Optional<Celestial> nearest = registry.stream()
                .filter(c -> c.type().isVisitable())
                .min(Comparator.comparingDouble(c -> {
                    Vector3dc bp = c.getPosition(ticks, registry);
                    return p.distance(bp.x(), bp.y(), bp.z());
                }));

        Celestial body = nearest.orElse(null);
        ResourceLocation bodyId = body != null ? registry.getKey(body) : null;
        ServerLevel target = dimensionFor(level, bodyId);
        Quaterniond rotation = body != null ? orientationToward(p, body, ticks, registry) : new Quaterniond();
        Vector3d landing;
        if (target == null || body == null) {
            target = level.getServer().overworld();
            landing = new Vector3d(0, GenesisCommonConfig.getAtmosphereEntryHeight(), 0);
        } else {
            landing = landingPosition(body, bodyId, ticks, registry, p);
        }
        enterAtmosphere(player, target, landing, rotation, bodyId);
        return true;
    }
}
