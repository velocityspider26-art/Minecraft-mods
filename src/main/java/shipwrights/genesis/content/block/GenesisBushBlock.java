package shipwrights.genesis.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Concrete bush block (BushBlock is abstract in 1.21.1). */
public class GenesisBushBlock extends BushBlock {
    public static final MapCodec<GenesisBushBlock> CODEC = simpleCodec(GenesisBushBlock::new);

    public GenesisBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }
}
