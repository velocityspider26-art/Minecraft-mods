package shipwrights.genesis.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Concrete falling block. In 1.21.1 {@link FallingBlock} is abstract (it requires a
 * {@code codec()}), so Genesis' sands/salts use this thin concrete subclass.
 */
public class GenesisFallingBlock extends FallingBlock {
    public static final MapCodec<GenesisFallingBlock> CODEC = simpleCodec(GenesisFallingBlock::new);

    public GenesisFallingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }
}
