package com.velocityspider.createjetengines.registry;

import com.velocityspider.createjetengines.CreateJetEngines;
import com.velocityspider.createjetengines.block.CombustionCoreBlock;
import com.velocityspider.createjetengines.block.SimpleModuleBlock;
import com.velocityspider.createjetengines.engine.ModuleType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JetBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateJetEngines.MODID);

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(3.0F, 8.0F)
                .sound(SoundType.NETHERITE_BLOCK)
                .requiresCorrectToolForDrops()
                // The casings are octagonal shells, not solid cubes, so vanilla must not
                // treat them as occluding or cull the faces behind them.
                .noOcclusion();
    }

    public static final DeferredBlock<Block> FAN_MODULE =
            BLOCKS.register("fan_module", () -> new SimpleModuleBlock(ModuleType.FAN, metal()));

    public static final DeferredBlock<Block> COMPRESSOR_MODULE =
            BLOCKS.register("compressor_module", () -> new SimpleModuleBlock(ModuleType.COMPRESSOR, metal()));

    public static final DeferredBlock<Block> COMBUSTION_CORE =
            BLOCKS.register("combustion_core", () -> new CombustionCoreBlock(metal().lightLevel(state -> 3)));

    public static final DeferredBlock<Block> AFTERBURNER_MODULE =
            BLOCKS.register("afterburner_module", () -> new SimpleModuleBlock(ModuleType.AFTERBURNER, metal()));

    public static final DeferredBlock<Block> NOZZLE_MODULE =
            BLOCKS.register("nozzle_module", () -> new SimpleModuleBlock(ModuleType.NOZZLE, metal()));

    private JetBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
