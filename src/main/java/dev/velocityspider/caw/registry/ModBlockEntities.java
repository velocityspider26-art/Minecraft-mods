package dev.velocityspider.caw.registry;

import dev.velocityspider.caw.CreateAerialWarfare;
import dev.velocityspider.caw.blockentity.EngineNozzleBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CreateAerialWarfare.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineNozzleBlockEntity>> ENGINE_NOZZLE =
            BLOCK_ENTITIES.register(
                    "engine_nozzle",
                    () -> BlockEntityType.Builder.of(
                            EngineNozzleBlockEntity::new,
                            ModBlocks.ENGINE_NOZZLE.get()
                    ).build(null)
            );

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
