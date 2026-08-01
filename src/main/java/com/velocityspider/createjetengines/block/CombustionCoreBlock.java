package com.velocityspider.createjetengines.block;

import com.velocityspider.createjetengines.blockentity.CombustionCoreBlockEntity;
import com.velocityspider.createjetengines.engine.ModuleType;
import com.velocityspider.createjetengines.registry.JetBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The combustion core: the authoritative brain and the single Sable propulsion actor of a chain.
 */
public class CombustionCoreBlock extends JetModuleBlock implements EntityBlock {

    public CombustionCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ModuleType getModuleType() {
        return ModuleType.COMBUSTION;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CombustionCoreBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != JetBlockEntities.COMBUSTION_CORE.get()) {
            return null;
        }
        return level.isClientSide()
                ? (lvl, pos, st, be) -> ((CombustionCoreBlockEntity) be).clientTick()
                : (lvl, pos, st, be) -> ((CombustionCoreBlockEntity) be).serverTick();
    }
}
