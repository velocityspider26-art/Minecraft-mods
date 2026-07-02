package com.example.examplemod.content.thruster;

import com.example.examplemod.ExampleMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Stores the thruster's fuel tank and the smoothed throttle used to drive the plume mesh.
 *
 * <p>Fuel is a fluid (lava or the mod's kerosene). Fill the tank by pumping fuel in with a Create
 * mechanical pump (the tank is exposed as a fluid-handler capability) or by right-clicking with a
 * filled bucket. While the tank has fuel the thruster burns it down and the {@link ThrusterBlock#LIT}
 * block-state is true, which drives both the plume and the block's light emission. The throttle is
 * smoothed client-side for a soft spool up/down.</p>
 */
public class ThrusterBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 8_000; // 8 buckets
    private static final int BURN_RATE_MB = 2;     // fuel consumed per tick while firing

    private final FluidTank fuelTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return isFuel(stack.getFluid());
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private float currentThrottle = 0f;
    private float prevThrottle = 0f;

    public ThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static boolean isFuel(Fluid fluid) {
        return fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA
                || fluid == ExampleMod.KEROSENE_FLUID.get() || fluid == ExampleMod.KEROSENE_FLOWING.get();
    }

    public IFluidHandler getFuelTank() {
        return fuelTank;
    }

    /** Server tick: burn fuel from the tank and keep the LIT block-state in sync. */
    public void serverTick() {
        if (level == null) {
            return;
        }
        FluidStack burned = fuelTank.drain(BURN_RATE_MB, IFluidHandler.FluidAction.EXECUTE);
        boolean firing = !burned.isEmpty();
        if (getBlockState().getValue(ThrusterBlock.LIT) != firing) {
            setLit(firing);
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

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(DirectionalBlock.FACING) ? state.getValue(DirectionalBlock.FACING) : Direction.NORTH;
    }

    public PlumeType getPlumeType() {
        BlockState state = getBlockState();
        return state.hasProperty(ThrusterBlock.PLUME) ? state.getValue(ThrusterBlock.PLUME) : PlumeType.KEROLOX;
    }

    public boolean isCreative() {
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
    }
}
