package dev.velocityspider.caw.registry;

import dev.velocityspider.caw.CreateAerialWarfare;
import dev.velocityspider.caw.block.EngineCombustorBlock;
import dev.velocityspider.caw.block.EngineCompressorBlock;
import dev.velocityspider.caw.block.EngineFanBlock;
import dev.velocityspider.caw.block.EngineInletBlock;
import dev.velocityspider.caw.block.EngineNozzleBlock;
import dev.velocityspider.caw.block.EngineTurbineBlock;
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

    public static final DeferredBlock<EngineInletBlock> ENGINE_INLET =
            BLOCKS.registerBlock("engine_inlet", EngineInletBlock::new, engineProperties());

    public static final DeferredBlock<EngineFanBlock> ENGINE_FAN =
            BLOCKS.registerBlock("engine_fan", EngineFanBlock::new, engineProperties());

    public static final DeferredBlock<EngineCompressorBlock> ENGINE_COMPRESSOR =
            BLOCKS.registerBlock("engine_compressor", EngineCompressorBlock::new, engineProperties());

    public static final DeferredBlock<EngineCombustorBlock> ENGINE_COMBUSTOR =
            BLOCKS.registerBlock("engine_combustor", EngineCombustorBlock::new, engineProperties());

    public static final DeferredBlock<EngineTurbineBlock> ENGINE_TURBINE =
            BLOCKS.registerBlock("engine_turbine", EngineTurbineBlock::new, engineProperties());

    public static final DeferredBlock<EngineNozzleBlock> ENGINE_NOZZLE =
            BLOCKS.registerBlock("engine_nozzle", EngineNozzleBlock::new, engineProperties());

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
