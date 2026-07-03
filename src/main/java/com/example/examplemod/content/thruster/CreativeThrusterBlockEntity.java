package com.example.examplemod.content.thruster;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the creative thruster: always at full throttle, needs no fuel, and still applies
 * physics force (handled in the base {@link ThrusterBlockEntity#serverTick()} via {@link #isCreative()}).
 */
public class CreativeThrusterBlockEntity extends ThrusterBlockEntity {
    public CreativeThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected float getTargetThrottle() {
        return 1f;
    }

    @Override
    public boolean isCreative() {
        return true;
    }
}
