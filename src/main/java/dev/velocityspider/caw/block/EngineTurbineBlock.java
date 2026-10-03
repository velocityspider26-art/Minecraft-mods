package dev.velocityspider.caw.block;

import com.mojang.serialization.MapCodec;
import dev.velocityspider.caw.engine.EngineComponentType;
import dev.velocityspider.caw.engine.EngineTier;

public final class EngineTurbineBlock extends EngineComponentBlock {
    public static final MapCodec<EngineTurbineBlock> CODEC = simpleCodec(EngineTurbineBlock::new);

    public EngineTurbineBlock(Properties properties) {
        super(EngineComponentType.TURBINE, EngineTier.NORMAL, properties);
    }

    @Override
    public MapCodec<EngineTurbineBlock> codec() {
        return CODEC;
    }
}
