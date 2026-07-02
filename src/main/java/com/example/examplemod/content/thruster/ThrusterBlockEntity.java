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
import org.joml.Vector3f;

/**
 * Stores the thruster's fuel tank, the redstone-driven throttle, and the smoothed motion used to
 * deform the plume mesh.
 *
 * <p>The thruster fires only when it has both fuel (a fluid: lava or kerosene) and a redstone signal.
 * The {@link ThrusterBlock#POWER} block-state mirrors the redstone level (1-15) while firing, so the
 * client renders 15 distinct plume intensities with no extra sync. Fuel is consumed faster at higher
 * throttle. The throttle is smoothed client-side for a soft spool up/down.</p>
 *
 * <p>For motion reactivity, the renderer feeds back the block's real world-space position each frame
 * (which already accounts for Valkyrien Skies ships / Create contraptions, since their transform is
 * baked into the render pose). We derive a smoothed velocity from it so the plume can trail and
 * deform as the craft moves - like a real exhaust.</p>
 */
public class ThrusterBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 8_000;   // 8 buckets
    private static final int MAX_BURN_MB = 6;        // fuel per tick at full throttle

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

    // Client-only motion tracking (world space), used to deform the plume.
    private double lastWorldX, lastWorldY, lastWorldZ;
    private float lastMotionTime;
    private boolean hasMotionSample;
    private float velX, velY, velZ;

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

    /** Server tick: gate on redstone + fuel, burn fuel scaled by throttle, publish POWER. */
    public void serverTick() {
        if (level == null) {
            return;
        }
        int signal = level.getBestNeighborSignal(getBlockPos());
        int power = 0;
        if (signal > 0 && !fuelTank.getFluid().isEmpty()) {
            int burn = Math.max(1, Math.round(MAX_BURN_MB * (signal / 15f)));
            FluidStack burned = fuelTank.drain(burn, IFluidHandler.FluidAction.EXECUTE);
            if (!burned.isEmpty()) {
                power = signal;
            }
        }
        if (getBlockState().getValue(ThrusterBlock.POWER) != power) {
            setPower(power);
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

    private void setPower(int value) {
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(ThrusterBlock.POWER) && state.getValue(ThrusterBlock.POWER) != value) {
                level.setBlock(getBlockPos(), state.setValue(ThrusterBlock.POWER, value), 3);
            }
        }
    }

    /** Target throttle in [0,1], from the redstone-driven power level. */
    protected float getTargetThrottle() {
        if (!getBlockState().hasProperty(ThrusterBlock.POWER)) {
            return 0f;
        }
        return getBlockState().getValue(ThrusterBlock.POWER) / 15f;
    }

    /** Interpolated throttle for smooth rendering between ticks. */
    public float getThrottle(float partialTick) {
        return Mth.lerp(partialTick, prevThrottle, currentThrottle);
    }

    /** Feeds the renderer's measured world position back in to compute a smoothed velocity. */
    public void updateRenderMotion(double wx, double wy, double wz, float timeTicks) {
        if (hasMotionSample) {
            float dt = timeTicks - lastMotionTime;
            if (dt > 0.0001f) {
                float ivx = (float) ((wx - lastWorldX) / dt);
                float ivy = (float) ((wy - lastWorldY) / dt);
                float ivz = (float) ((wz - lastWorldZ) / dt);
                float s = 0.25f;
                velX += (ivx - velX) * s;
                velY += (ivy - velY) * s;
                velZ += (ivz - velZ) * s;
            }
        }
        lastWorldX = wx;
        lastWorldY = wy;
        lastWorldZ = wz;
        lastMotionTime = timeTicks;
        hasMotionSample = true;
    }

    /** Smoothed world-space velocity in blocks/tick. */
    public Vector3f getSmoothedWorldVelocity() {
        return new Vector3f(velX, velY, velZ);
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
