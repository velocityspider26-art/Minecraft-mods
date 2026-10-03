package dev.velocityspider.caw.block;

import com.mojang.serialization.MapCodec;
import dev.velocityspider.caw.engine.EngineComponentType;
import dev.velocityspider.caw.engine.EngineTier;

public final class EngineCombustorBlock extends EngineComponentBlock {
    public static final MapCodec<EngineCombustorBlock> CODEC = simpleCodec(EngineCombustorBlock::new);

    public EngineCombustorBlock(Properties properties) {
        super(EngineComponentType.COMBUSTOR, EngineTier.NORMAL, properties);
    }

    @Override
    public MapCodec<EngineCombustorBlock> codec() {
        return CODEC;
    }
}
