package com.vantage.client.visual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/**
 * A one-block world used to ask block colour handlers for the tint of a block in a given biome.
 * Handlers that want more of the world than this get air and throw at worst; callers catch that.
 */
final class BiomeTintGetter implements BlockAndTintGetter {
    private final BlockState state;
    private final Holder<Biome> biome;

    BiomeTintGetter(BlockState state, Holder<Biome> biome) {
        this.state = state;
        this.biome = biome;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return 1.0f;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        throw new UnsupportedOperationException("No light in a tint query");
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return resolver.getColor(this.biome.value(), pos.getX(), pos.getZ());
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return pos.equals(BlockPos.ZERO) ? this.state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
        return 384;
    }

    @Override
    public int getMinBuildHeight() {
        return -64;
    }
}
