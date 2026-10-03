package dev.velocityspider.caw.block;

import com.mojang.serialization.MapCodec;
import dev.velocityspider.caw.engine.EngineComponentType;
import dev.velocityspider.caw.engine.EngineTier;

public final class EngineFanBlock extends EngineComponentBlock {
    public static final MapCodec<EngineFanBlock> CODEC = simpleCodec(EngineFanBlock::new);

    public EngineFanBlock(Properties properties) {
        super(EngineComponentType.FAN, EngineTier.NORMAL, properties);
    }

    @Override
    public MapCodec<EngineFanBlock> codec() {
        return CODEC;
    }
}
