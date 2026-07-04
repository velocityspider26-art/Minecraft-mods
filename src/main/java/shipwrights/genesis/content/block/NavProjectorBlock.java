package shipwrights.genesis.content.block;

import com.mojang.serialization.MapCodec;

import shipwrights.genesis.content.blockentity.NavProjectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class NavProjectorBlock extends BaseEntityBlock {
    public static final MapCodec<NavProjectorBlock> CODEC = simpleCodec(NavProjectorBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public NavProjectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NavProjectorBlockEntity(pos, state);
    }
}
