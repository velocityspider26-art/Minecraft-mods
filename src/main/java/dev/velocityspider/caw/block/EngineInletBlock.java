package dev.velocityspider.caw.block;

import com.mojang.serialization.MapCodec;
import dev.velocityspider.caw.engine.EngineComponentType;
import dev.velocityspider.caw.engine.EngineTier;

public final class EngineInletBlock extends EngineComponentBlock {
    public static final MapCodec<EngineInletBlock> CODEC = simpleCodec(EngineInletBlock::new);

    public EngineInletBlock(Properties properties) {
        super(EngineComponentType.INLET, EngineTier.NORMAL, properties);
    }

    @Override
    public MapCodec<EngineInletBlock> codec() {
        return CODEC;
    }
}
