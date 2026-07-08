package shipwrights.genesis;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shipwrights.genesis.commands.GenesisCommandArguments;
import shipwrights.genesis.compat.aeronautics.AeronauticsCompat;
import shipwrights.genesis.compat.aeronautics.AeronauticsMovementHelper;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.time.GenesisTimeData;
import shipwrights.genesis.time.TimeTracker;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.content.block.GenesisBlocks;
import shipwrights.genesis.content.fluid.GenesisFluids;
import shipwrights.genesis.content.particle.GenesisParticles;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.transformProvider.BuiltinTransformProviders;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;
import shipwrights.genesis.teleportation.integration.PlanetToSpaceTeleporter;
import shipwrights.genesis.teleportation.integration.SpaceToPlanetTeleporter;
import shipwrights.genesis.tests.commands.GameTestCommands;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.regex.Pattern;

@Mod(GenesisMod.MOD_ID)
public final class GenesisMod {
    public static final String MOD_ID = "genesis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Vector3dc UP = new Vector3d(0.0, 1.0, 0.0);
    public static final Vector3dc EAST = new Vector3d(1.0, 0.0, 0.0);

    public static long clientTimeOffset = 0;

    public static final ResourceLocation SPACE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "great_unknown");
    public static final ResourceLocation WORMHOLE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "subspace");
    public static final ResourceLocation ASTEROID_RULE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "asteroid_block_surface_rule");
    public static ResourceLocation GENERIC_PLANET_ID = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet");

    public static final ResourceKey<Registry<Celestial>> CELESTIALS_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID, "celestials"));

    private static final Pattern SEAT_REGISTRY_NAME =
            Pattern.compile("(?<![a-z])(seat|chair)(?![a-z])", Pattern.CASE_INSENSITIVE);

    public GenesisMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, GenesisClientConfig.CONFIG_SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, GenesisCommonConfig.CONFIG_SPEC);

        // Register the celestials datapack registry
        modEventBus.addListener(GenesisMod::registerDataPackRegistries);

        // Register packet handlers
        modEventBus.addListener(GenesisNetworking::register);

        // Register celestial types
        BuiltinCelestialTypes.register();

        // Register celestial transform providers
        BuiltinTransformProviders.register();

        // Register fluids using Registrate (must be called before other registrations)
        GenesisFluids.init();

        GenesisCommandArguments.register(modEventBus);

        GenesisBlocks.BLOCKS.register(modEventBus);
        GenesisBlocks.MENU_TYPES.register(modEventBus);
        GenesisParticles.PARTICLE_TYPES.register(modEventBus);

        shipwrights.genesis.content.blockentity.GenesisBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(shipwrights.genesis.content.blockentity.GenesisBlockEntities::registerCapabilities);
        shipwrights.genesis.content.sound.GenesisSounds.SOUND_EVENTS.register(modEventBus);
        shipwrights.genesis.content.item.GenesisItems.ITEMS.register(modEventBus);
        shipwrights.genesis.content.item.GenesisCreativeTabs.register(modEventBus);

        boolean isGameTest = System.getProperty("neoforge.enabledGameTestNamespaces") != null;
        NeoForge.EVENT_BUS.register(new PlanetToSpaceTeleporter(isGameTest));
        NeoForge.EVENT_BUS.register(new SpaceToPlanetTeleporter(isGameTest));
        NeoForge.EVENT_BUS.register(new shipwrights.genesis.space.OrbitalGravityHandler(isGameTest));
        NeoForge.EVENT_BUS.register(new shipwrights.genesis.space.GalaxyGravityHandler(isGameTest));
        NeoForge.EVENT_BUS.addListener(shipwrights.genesis.command.GenesisCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.register(new shipwrights.genesis.content.fx.AtmosphereBurnEffects());
        NeoForge.EVENT_BUS.register(TimeTracker.class);

        if (isGameTest) {
            NeoForge.EVENT_BUS.addListener(GameTestCommands::onRegisterCommandsEvent);
        }

        if (!AeronauticsCompat.isLoaded()) {
            LOGGER.warn("Sable / Create Aeronautics not detected; Genesis vehicle features will be inert.");
        }
    }

    private static void registerDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CELESTIALS_KEY, Celestial.CODEC, Celestial.CODEC);
    }

    public static Registry<Celestial> getCelestialRegistry(Level level) {
        return level.registryAccess().registryOrThrow(CELESTIALS_KEY);
    }

    @Nullable public static Celestial getCelestialForLevel(Level level) {
        return getCelestialRegistry(level).get(level.dimension().location());
    }

    @SuppressWarnings("ConstantConditions")
    public static long getTicks(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            if (server == null || server.overworld() == null) return level.getGameTime();
            return level.getGameTime() + GenesisTimeData.getOrCreate(server).getTimeOffset();
        }
        return level.getGameTime() + clientTimeOffset;
    }

    public static float getPartialTick(Level level, RenderLevelStageEvent event) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) return 0f;
        return event.getPartialTick().getGameTimeDeltaPartialTick(false);
    }

    @Deprecated
    public static boolean isMiniScale(ResourceLocation dimensionLocation) {
        return dimensionLocation.equals(SPACE_DIM) || dimensionLocation.equals(WORMHOLE_DIM);
    }

    @Deprecated
    public static boolean isMiniScale(Level level) {
        return isMiniScale(level.dimension().location());
    }

    public static double getDimensionScale(Level level) {
        if (level == null) return 1.0;
        ResourceLocation dimension = level.dimension().location();
        if (dimension.equals(WORMHOLE_DIM) || dimension.equals(SPACE_DIM)) {
            return 1.0 / 16.0;
        } else {
            return 1.0;
        }
    }

    public static boolean shouldCancelVoidDamage(ResourceLocation dimensionLocation) {
        return dimensionLocation.equals(SPACE_DIM) || dimensionLocation.equals(WORMHOLE_DIM);
    }

    public static boolean shouldCancelVoidDamage(Level level) {
        return shouldCancelVoidDamage(level.dimension().location());
    }

    public static boolean isSpaceDimension(ResourceLocation dimensionLocation) {
        return dimensionLocation.equals(SPACE_DIM);
    }

    public static boolean isSpaceDimension(Level level) {
        return isSpaceDimension(level.dimension().location());
    }

    public static boolean isSubSpaceDimension(Level level) {
        return level.dimension().location().equals(WORMHOLE_DIM);
    }

    public static double getApparentSunAngle(double starUpDot, double starEastDot) {
        boolean sign = Math.signum(starUpDot) < 0;
        double starDot0To1 = (1 - starEastDot) / 4;
        return sign ? 1 - starDot0To1 : starDot0To1;
    }

    @ApiStatus.Internal
    public static void refreshEntityScaling(Entity entity, Level level) {
        try {
            ScaleData scaleData = ScaleTypes.BASE.getScaleData(entity);
            ScaleData explosionScaleData = ScaleTypes.EXPLOSIONS.getScaleData(entity);
            scaleData.setPersistence(true);
            explosionScaleData.setPersistence(true);
            if (isMiniScale(level)) {
                ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                // On Create Aeronautics, entities that are part of / riding a construct keep full
                // scale; loose entities in a mini-scale dimension are scaled down 16x. Seats and
                // projectiles are always kept full scale (they map to the vehicle frame).
                boolean onConstruct = AeronauticsMovementHelper.constructRiddenBy(entity) != null;
                if (
                        entity instanceof Projectile ||
                                !onConstruct ||
                                SEAT_REGISTRY_NAME.matcher(entityType.getPath()).find()
                ) {
                    scaleData.setScale(1 / 16f);
                    explosionScaleData.setScale(16f);
                }
                entity.setNoGravity(true);
            } else {
                scaleData.setScale(1f);
                explosionScaleData.setScale(1f);
                entity.setNoGravity(false);
            }
        } catch (Exception ignored) { /* not really sure what causes this, but I don't think it's critical */ }
    }
}
