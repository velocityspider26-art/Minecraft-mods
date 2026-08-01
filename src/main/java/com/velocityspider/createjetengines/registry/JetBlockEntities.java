package com.velocityspider.createjetengines.registry;

import com.velocityspider.createjetengines.CreateJetEngines;
import com.velocityspider.createjetengines.blockentity.CombustionCoreBlockEntity;
import com.velocityspider.createjetengines.blockentity.JetModuleBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JetBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateJetEngines.MODID);

    /** Shared type for fan, compressor, afterburner and nozzle. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JetModuleBlockEntity>> MODULE =
            BLOCK_ENTITIES.register("module", () -> BlockEntityType.Builder.of(
                    JetModuleBlockEntity::new,
                    JetBlocks.FAN_MODULE.get(),
                    JetBlocks.COMPRESSOR_MODULE.get(),
                    JetBlocks.AFTERBURNER_MODULE.get(),
                    JetBlocks.NOZZLE_MODULE.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionCoreBlockEntity>> COMBUSTION_CORE =
            BLOCK_ENTITIES.register("combustion_core", () -> BlockEntityType.Builder.of(
                    CombustionCoreBlockEntity::new,
                    JetBlocks.COMBUSTION_CORE.get()
            ).build(null));

    private JetBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
