package shipwrights.genesis.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class GenesisBushBlock extends BushBlock {
    public static final MapCodec<GenesisBushBlock> CODEC = simpleCodec(GenesisBushBlock::new);

    public GenesisBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }
}
