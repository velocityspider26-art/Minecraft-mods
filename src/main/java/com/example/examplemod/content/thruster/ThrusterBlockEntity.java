package com.example.examplemod.content.thruster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds the smoothed "throttle" used to drive the plume mesh. Because the plume is client-only
 * eye-candy, the throttle is computed and smoothed entirely on the client from the block's
 * redstone signal - no network sync required.
 */
public class ThrusterBlockEntity extends BlockEntity {
    private float currentThrottle = 0f;
    private float prevThrottle = 0f;

    public ThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Client tick: ease the current throttle towards the target for a smooth spool up/down. */
    public void clientTick() {
        prevThrottle = currentThrottle;
        float target = getTargetThrottle();
        currentThrottle += (target - currentThrottle) * 0.18f;
        if (currentThrottle < 0.001f && target <= 0f) {
            currentThrottle = 0f;
        }
    }

    /** Target throttle in [0,1]. Regular thrusters are driven by their redstone input. */
    protected float getTargetThrottle() {
        if (level == null) {
            return 0f;
        }
        int signal = level.getBestNeighborSignal(getBlockPos());
        return Mth.clamp(signal / 15f, 0f, 1f);
    }

    /** Interpolated throttle for smooth rendering between ticks. */
    public float getThrottle(float partialTick) {
        return Mth.lerp(partialTick, prevThrottle, currentThrottle);
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(DirectionalBlock.FACING) ? state.getValue(DirectionalBlock.FACING) : Direction.DOWN;
    }

    public boolean isCreative() {
        return false;
    }
}
