package dev.velocityspider.caw.block;

import dev.velocityspider.caw.blockentity.EngineNozzleBlockEntity;
import dev.velocityspider.caw.engine.EngineComponentType;
import dev.velocityspider.caw.engine.EngineTier;
import dev.velocityspider.caw.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class EngineNozzleBlock extends EngineComponentBlock implements EntityBlock {
    public EngineNozzleBlock(Properties properties) {
        super(EngineComponentType.NOZZLE, EngineTier.NORMAL, properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EngineNozzleBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (level.isClientSide || type != ModBlockEntities.ENGINE_NOZZLE.get()) {
            return null;
        }

        return (BlockEntityTicker<T>) (BlockEntityTicker<EngineNozzleBlockEntity>)
                EngineNozzleBlockEntity::serverTick;
    }
}
