package com.velocityspider.createjetengines.block;

import com.velocityspider.createjetengines.blockentity.JetModuleBlockEntity;
import com.velocityspider.createjetengines.engine.ModuleType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Every module except the combustion core: fan, compressor, afterburner and nozzle.
 *
 * <p>These carry a block entity purely so the renderer has somewhere to read animation state from;
 * they never apply force themselves. Only the core is a Sable actor, which is what keeps one chain
 * to exactly one force contribution.
 */
public class SimpleModuleBlock extends JetModuleBlock implements EntityBlock {

    private final ModuleType type;

    public SimpleModuleBlock(ModuleType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public ModuleType getModuleType() {
        return type;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JetModuleBlockEntity(pos, state);
    }
}
