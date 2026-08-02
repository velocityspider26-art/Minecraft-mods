package shipwrights.genesis.content.blockentity;

import net.minecraft.core.registries.Registries;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.block.GenesisBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import shipwrights.genesis.hyperspace.HyperdriveBlockEntity;

public class GenesisBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GenesisMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NavProjectorBlockEntity>> NAV_PROJECTOR =
        BLOCK_ENTITIES.register("nav_projector",
            () -> BlockEntityType.Builder.of(NavProjectorBlockEntity::new,
                GenesisBlocks.NAV_PROJECTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VoidEngineInterfaceBlockEntity>> VOID_ENGINE_INTERFACE =
        BLOCK_ENTITIES.register("void_engine_interface",
            () -> BlockEntityType.Builder.of(VoidEngineInterfaceBlockEntity::new,
                GenesisBlocks.VOID_ENGINE_INTERFACE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VoidCoreBlockEntity>> VOID_CORE =
        BLOCK_ENTITIES.register("void_core",
            () -> BlockEntityType.Builder.of(VoidCoreBlockEntity::new,
                GenesisBlocks.VOID_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TulciteCatalyzerBlockEntity>> TULCITE_CATALYZER_BLOCK_ENTITY = BLOCK_ENTITIES.register("tulcite_catalyzer_block",
            () -> BlockEntityType.Builder.of(TulciteCatalyzerBlockEntity::new, GenesisBlocks.TULCITE_CATALYZER_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadarDisplayBlockEntity>> RADAR_DISPLAY =
        BLOCK_ENTITIES.register("radar_display",
            () -> BlockEntityType.Builder.of(RadarDisplayBlockEntity::new,
                GenesisBlocks.RADAR_DISPLAY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HyperdriveBlockEntity>> HYPERDRIVE =
        BLOCK_ENTITIES.register("hyperdrive",
            () -> BlockEntityType.Builder.of(HyperdriveBlockEntity::new,
                GenesisBlocks.HYPERDRIVE.get(),
                GenesisBlocks.HYPERDRIVE_CLASS_6.get(),
                GenesisBlocks.HYPERDRIVE_CLASS_5.get(),
                GenesisBlocks.HYPERDRIVE_CLASS_4.get(),
                GenesisBlocks.HYPERDRIVE_CLASS_3.get(),
                GenesisBlocks.HYPERDRIVE_CLASS_2.get(),
                GenesisBlocks.HYPERDRIVE_CLASS_1.get(),
                GenesisBlocks.HYPERDRIVE_CLASS_0_5.get(),
                GenesisBlocks.FALKEN_HYPERDRIVE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OrbitalStabilizerBlockEntity>> ORBITAL_STABILIZER =
        BLOCK_ENTITIES.register("orbital_stabilizer",
            () -> BlockEntityType.Builder.of(OrbitalStabilizerBlockEntity::new,
                GenesisBlocks.ORBITAL_STABILIZER.get()).build(null));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                VOID_ENGINE_INTERFACE.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage()
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                TULCITE_CATALYZER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItems()
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                TULCITE_CATALYZER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getEnergyOutput()
        );
    }
}
