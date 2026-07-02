package com.example.examplemod.content.thruster;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the creative thruster: always at full throttle, never burns fuel.
 */
public class CreativeThrusterBlockEntity extends ThrusterBlockEntity {
    public CreativeThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void serverTick() {
        // No fuel logic for the creative thruster.
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
