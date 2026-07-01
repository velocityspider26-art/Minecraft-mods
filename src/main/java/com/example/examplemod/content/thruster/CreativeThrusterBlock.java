package com.example.examplemod.content.thruster;

import com.example.examplemod.ExampleMod;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Creative counterpart of {@link ThrusterBlock}. It always fires at full throttle (no fuel or
 * redstone required), which makes it convenient for showing off the plume mesh.
 */
public class CreativeThrusterBlock extends ThrusterBlock {
    public static final MapCodec<CreativeThrusterBlock> CODEC = simpleCodec(CreativeThrusterBlock::new);

    public CreativeThrusterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeThrusterBlockEntity(ExampleMod.CREATIVE_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? (lvl, pos, st, be) -> ((ThrusterBlockEntity) be).clientTick() : null;
    }
}
