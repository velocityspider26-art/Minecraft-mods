package shipwrights.genesis.content.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import shipwrights.genesis.GenesisMod;

/** Custom creatures and their registration. */
public final class GenesisEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, GenesisMod.MOD_ID);

    /** The moon's wildlife. */
    public static final DeferredHolder<EntityType<?>, EntityType<SpaceCritterEntity>> MOON_LURKER =
            ENTITY_TYPES.register("moon_lurker", () -> EntityType.Builder
                    .of(SpaceCritterEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 0.6f)
                    .clientTrackingRange(10)
                    .build("moon_lurker"));

    /** Mercury's wildlife — the same crawler, scorched a different colour. */
    public static final DeferredHolder<EntityType<?>, EntityType<SpaceCritterEntity>> CINDER_CRAWLER =
            ENTITY_TYPES.register("cinder_crawler", () -> EntityType.Builder
                    .of(SpaceCritterEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 0.6f)
                    .clientTrackingRange(10)
                    .build("cinder_crawler"));

    private GenesisEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener(GenesisEntities::registerAttributes);
        modBus.addListener(GenesisEntities::registerSpawnPlacements);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MOON_LURKER.get(), SpaceCritterEntity.createAttributes().build());
        event.put(CINDER_CRAWLER.get(), SpaceCritterEntity.createAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(MOON_LURKER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SpaceCritterEntity::canSpawnHere,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CINDER_CRAWLER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SpaceCritterEntity::canSpawnHere,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
