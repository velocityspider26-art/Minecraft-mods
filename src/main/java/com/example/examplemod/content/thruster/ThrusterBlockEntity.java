package com.example.examplemod.content.thruster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores the thruster's fuel buffer and the smoothed throttle used to drive the plume mesh.
 *
 * <p>Fuel is burned on the server; whether the thruster is currently burning is published to the
 * client through the {@link ThrusterBlock#LIT} block-state, so the client needs no extra sync to
 * know when to draw the plume. The throttle itself is smoothed client-side for a soft spool up/down.</p>
 */
public class ThrusterBlockEntity extends BlockEntity {
    public static final int FUEL_CAPACITY = 200_000;
    private static final int FUEL_BURN_PER_TICK = 1;

    private int fuel = 0;

    private float currentThrottle = 0f;
    private float prevThrottle = 0f;

    public ThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Server tick: burn fuel and keep the LIT block-state in sync. */
    public void serverTick() {
        if (level == null) {
            return;
        }
        boolean lit = getBlockState().getValue(ThrusterBlock.LIT);
        if (fuel > 0) {
            fuel = Math.max(0, fuel - FUEL_BURN_PER_TICK);
            setChanged();
            if (!lit) {
                setLit(true);
            }
            if (fuel == 0) {
                setLit(false);
            }
        } else if (lit) {
            setLit(false);
        }
    }

    /** Client tick: ease the current throttle towards the target for a smooth spool up/down. */
    public void clientTick() {
        prevThrottle = currentThrottle;
        float target = getTargetThrottle();
        currentThrottle += (target - currentThrottle) * 0.12f;
        if (currentThrottle < 0.001f && target <= 0f) {
            currentThrottle = 0f;
        }
    }

    private void setLit(boolean value) {
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(ThrusterBlock.LIT) && state.getValue(ThrusterBlock.LIT) != value) {
                level.setBlock(getBlockPos(), state.setValue(ThrusterBlock.LIT, value), 3);
            }
        }
    }

    /** Target throttle in [0,1]. Regular thrusters fire while they have burning fuel. */
    protected float getTargetThrottle() {
        return getBlockState().hasProperty(ThrusterBlock.LIT) && getBlockState().getValue(ThrusterBlock.LIT) ? 1f : 0f;
    }

    /** Interpolated throttle for smooth rendering between ticks. */
    public float getThrottle(float partialTick) {
        return Mth.lerp(partialTick, prevThrottle, currentThrottle);
    }

    public void addFuel(int amount) {
        fuel = Math.min(FUEL_CAPACITY, fuel + amount);
        setChanged();
    }

    public int getFuel() {
        return fuel;
    }

    public boolean hasFuelRoom() {
        return fuel < FUEL_CAPACITY;
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(DirectionalBlock.FACING) ? state.getValue(DirectionalBlock.FACING) : Direction.NORTH;
    }

    public boolean isCreative() {
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fuel", fuel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = tag.getInt("Fuel");
    }
}
