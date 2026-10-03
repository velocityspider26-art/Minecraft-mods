package dev.velocityspider.caw.registry;

import dev.velocityspider.caw.CreateAerialWarfare;
import dev.velocityspider.caw.block.EngineComponentBlock;
import dev.velocityspider.caw.block.EngineNozzleBlock;
import dev.velocityspider.caw.engine.EngineComponentType;
import dev.velocityspider.caw.engine.EngineTier;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateAerialWarfare.MOD_ID);

    private static BlockBehaviour.Properties engineProperties() {
        return BlockBehaviour.Properties.of()
                .strength(4.0f, 8.0f)
                .sound(SoundType.METAL);
    }

    public static final DeferredBlock<EngineComponentBlock> ENGINE_INLET =
            BLOCKS.registerBlock(
                    "engine_inlet",
                    props -> new EngineComponentBlock(EngineComponentType.INLET, EngineTier.NORMAL, props),
                    engineProperties()
            );

    public static final DeferredBlock<EngineComponentBlock> ENGINE_FAN =
            BLOCKS.registerBlock(
                    "engine_fan",
                    props -> new EngineComponentBlock(EngineComponentType.FAN, EngineTier.NORMAL, props),
                    engineProperties()
            );

    public static final DeferredBlock<EngineComponentBlock> ENGINE_COMPRESSOR =
            BLOCKS.registerBlock(
                    "engine_compressor",
                    props -> new EngineComponentBlock(EngineComponentType.COMPRESSOR, EngineTier.NORMAL, props),
                    engineProperties()
            );

    public static final DeferredBlock<EngineComponentBlock> ENGINE_COMBUSTOR =
            BLOCKS.registerBlock(
                    "engine_combustor",
                    props -> new EngineComponentBlock(EngineComponentType.COMBUSTOR, EngineTier.NORMAL, props),
                    engineProperties()
            );

    public static final DeferredBlock<EngineComponentBlock> ENGINE_TURBINE =
            BLOCKS.registerBlock(
                    "engine_turbine",
                    props -> new EngineComponentBlock(EngineComponentType.TURBINE, EngineTier.NORMAL, props),
                    engineProperties()
            );

    public static final DeferredBlock<EngineNozzleBlock> ENGINE_NOZZLE =
            BLOCKS.registerBlock(
                    "engine_nozzle",
                    EngineNozzleBlock::new,
                    engineProperties()
            );

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
