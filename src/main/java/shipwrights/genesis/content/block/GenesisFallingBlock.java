package shipwrights.genesis.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class GenesisFallingBlock extends FallingBlock {
    public static final MapCodec<GenesisFallingBlock> CODEC = simpleCodec(GenesisFallingBlock::new);

    public GenesisFallingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }
}
