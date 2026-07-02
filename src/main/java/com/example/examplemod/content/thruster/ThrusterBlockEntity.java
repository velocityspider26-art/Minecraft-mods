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
 * Stores the thruster's fuel tank, the redstone-driven throttle, and the smoothed motion used to
 * deform the plume mesh.
 *
 * <p>The thruster fires only when it has both fuel (a fluid: lava or kerosene) and a redstone signal.
 * The {@link ThrusterBlock#POWER} block-state mirrors the redstone level (1-15) while firing, so the
 * client renders 15 distinct plume intensities with no extra sync. Fuel is consumed faster at higher
 * throttle. The throttle is smoothed client-side for a soft spool up/down.</p>
 *
 * <p>For motion reactivity, the renderer feeds back the nozzle's real world-space position and facing
 * each frame (which already accounts for Valkyrien Skies ships / Create contraptions, since their
 * transform is baked into the render pose). We keep a short history of those samples so the plume can
 * be drawn through the path the nozzle actually traced - trailing when the craft translates and
 * curving at the tail when it turns, like a real exhaust.</p>
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

    // Client-only ring buffer of recent nozzle samples (world-space position + facing), newest at
    // HEAD. Used to draw the plume through the path the nozzle actually traced.
    private static final int HIST = 96;
    private final double[] histX = new double[HIST];
    private final double[] histY = new double[HIST];
    private final double[] histZ = new double[HIST];
    private final float[] histDX = new float[HIST];
    private final float[] histDY = new float[HIST];
    private final float[] histDZ = new float[HIST];
    private final float[] histT = new float[HIST];
    private int histHead = 0;
    private int histSize = 0;

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

    /** Records the current nozzle world position + facing (called once per frame by the renderer). */
    public void pushNozzleSample(float time, double px, double py, double pz, float dx, float dy, float dz) {
        if (histSize > 0 && time <= histT[histHead]) {
            return; // no time progress (e.g. paused); keep existing history
        }
        histHead = (histHead + 1) % HIST;
        if (histSize < HIST) {
            histSize++;
        }
        histX[histHead] = px;
        histY[histHead] = py;
        histZ[histHead] = pz;
        histDX[histHead] = dx;
        histDY[histHead] = dy;
        histDZ[histHead] = dz;
        histT[histHead] = time;
    }

    /**
     * Interpolates the nozzle sample at {@code targetTime}, writing world position into {@code outPos}
     * and facing into {@code outDir}. Clamps to the ends of the history. Returns false if no history.
     */
    public boolean sampleNozzle(float targetTime, double[] outPos, float[] outDir) {
        if (histSize == 0) {
            return false;
        }
        if (targetTime >= histT[histHead]) {
            copySample(histHead, outPos, outDir);
            return true;
        }
        int oldest = ((histHead - (histSize - 1)) % HIST + HIST) % HIST;
        if (targetTime <= histT[oldest]) {
            copySample(oldest, outPos, outDir);
            return true;
        }
        for (int k = 0; k < histSize - 1; k++) {
            int newer = ((histHead - k) % HIST + HIST) % HIST;
            int older = ((histHead - k - 1) % HIST + HIST) % HIST;
            if (targetTime <= histT[newer] && targetTime >= histT[older]) {
                float span = histT[newer] - histT[older];
                float f = span > 1.0e-5f ? (targetTime - histT[older]) / span : 0f;
                outPos[0] = Mth.lerp(f, histX[older], histX[newer]);
                outPos[1] = Mth.lerp(f, histY[older], histY[newer]);
                outPos[2] = Mth.lerp(f, histZ[older], histZ[newer]);
                float ndx = Mth.lerp(f, histDX[older], histDX[newer]);
                float ndy = Mth.lerp(f, histDY[older], histDY[newer]);
                float ndz = Mth.lerp(f, histDZ[older], histDZ[newer]);
                float len = Mth.sqrt(ndx * ndx + ndy * ndy + ndz * ndz);
                if (len > 1.0e-5f) {
                    ndx /= len;
                    ndy /= len;
                    ndz /= len;
                }
                outDir[0] = ndx;
                outDir[1] = ndy;
                outDir[2] = ndz;
                return true;
            }
        }
        copySample(histHead, outPos, outDir);
        return true;
    }

    private void copySample(int i, double[] outPos, float[] outDir) {
        outPos[0] = histX[i];
        outPos[1] = histY[i];
        outPos[2] = histZ[i];
        outDir[0] = histDX[i];
        outDir[1] = histDY[i];
        outDir[2] = histDZ[i];
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
