package dev.velocityspider.caw.block;

import com.mojang.serialization.MapCodec;
import dev.velocityspider.caw.engine.EngineComponentType;
import dev.velocityspider.caw.engine.EngineTier;

public final class EngineCompressorBlock extends EngineComponentBlock {
    public static final MapCodec<EngineCompressorBlock> CODEC = simpleCodec(EngineCompressorBlock::new);

    public EngineCompressorBlock(Properties properties) {
        super(EngineComponentType.COMPRESSOR, EngineTier.NORMAL, properties);
    }

    @Override
    public MapCodec<EngineCompressorBlock> codec() {
        return CODEC;
    }
}
