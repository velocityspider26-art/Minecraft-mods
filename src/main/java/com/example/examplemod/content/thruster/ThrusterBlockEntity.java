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
 * The thruster's server logic and the client-side state used to render the plume.
 *
 * <p>Server: resolves the fuel from the fluid in its tank ({@link ThrusterFuelManager}), gates thrust
 * on redstone, ramps thrust smoothly, consumes fuel at a rate scaled by burn-rate and throttle,
 * applies force to any Sable body via {@link ThrusterPhysicsHandler}, and publishes throttle
 * ({@link ThrusterBlock#POWER}) and fuel tier ({@link ThrusterBlock#PLUME}) through the block-state so
 * clients stay in sync without custom packets.</p>
 *
 * <p>Client: smooths the throttle for the plume and keeps a short history of the nozzle's world path so
 * the plume can trail/curve with the craft's motion.</p>
 */
public class ThrusterBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 8_000;   // 8 buckets
    private static final int BASE_BURN_MB = 2;        // baseline fuel per tick at full throttle
    private static final float CREATIVE_THRUST_MULT = 2.0f;

    private final FluidTank fuelTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return isFuelFluid(stack.getFluid());
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    // Server-authoritative ramped throttle in [0,1] (fuel multiplier applied separately for physics).
    private float serverThrottle = 0f;

    // Client-smoothed throttle for rendering.
    private float currentThrottle = 0f;
    private float prevThrottle = 0f;

    // Client-only ring buffer of recent nozzle samples (world pos + facing).
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

    /** Which fluids the tank will physically accept (lava + any fuel-mapped fluid, or kerosene). */
    public static boolean isFuelFluid(Fluid fluid) {
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA
                || fluid == ExampleMod.KEROSENE_FLUID.get() || fluid == ExampleMod.KEROSENE_FLOWING.get()) {
            return true;
        }
        ThrusterFuelManager mgr = ThrusterFuelManager.getInstance();
        return mgr != null && mgr.getFuel(fluid) != null;
    }

    public IFluidHandler getFuelTank() {
        return fuelTank;
    }

    /** Server tick: resolve fuel, gate on redstone, ramp thrust, consume fuel, apply physics. */
    public void serverTick() {
        if (level == null) {
            return;
        }
        boolean creative = isCreative();

        ThrusterFuelType fuel = null;
        if (!creative) {
            ThrusterFuelManager mgr = ThrusterFuelManager.getInstance();
            if (mgr != null && !fuelTank.getFluid().isEmpty()) {
                fuel = mgr.getFuel(fuelTank.getFluid().getFluid());
            }
        }
        boolean hasFuel = creative || fuel != null;
        int signal = creative ? 15 : level.getBestNeighborSignal(getBlockPos());

        float throttleTarget = (signal > 0 && hasFuel) ? (signal / 15f) : 0f;
        float ramp = (float) (double) ThrusterConfig.THRUST_RAMP_RATE.get();
        serverThrottle += (throttleTarget - serverThrottle) * ramp;
        if (serverThrottle < 0.001f && throttleTarget <= 0f) {
            serverThrottle = 0f;
        }

        // Consume fuel only while actually producing thrust.
        if (!creative && fuel != null && serverThrottle > 0.02f) {
            int burn = Math.max(1, Math.round(BASE_BURN_MB * fuel.burnRate() * (signal / 15f)));
            fuelTank.drain(burn, IFluidHandler.FluidAction.EXECUTE);
        }

        // Publish throttle (drives light + client plume) and fuel tier through the block-state.
        int power = Math.round(serverThrottle * 15f);
        if (getBlockState().getValue(ThrusterBlock.POWER) != power) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ThrusterBlock.POWER, power), 3);
        }
        PlumeType tier = fuel != null ? fuel.tier() : (creative ? PlumeType.EXOTIC : null);
        if (tier != null && getBlockState().getValue(ThrusterBlock.PLUME) != tier) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ThrusterBlock.PLUME, tier), 3);
        }

        // Apply force to any Sable physics object we are mounted on.
        float fuelMult = fuel != null ? fuel.thrustMultiplier() : (creative ? CREATIVE_THRUST_MULT : 0f);
        ThrusterPhysicsHandler.applyThrust(level, getBlockPos(), getFacing(), serverThrottle * fuelMult);
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
            return;
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

    /** Interpolates the nozzle sample at {@code targetTime}. Returns false if no history. */
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
        return state.hasProperty(ThrusterBlock.PLUME) ? state.getValue(ThrusterBlock.PLUME) : PlumeType.STANDARD;
    }

    public boolean isCreative() {
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("Throttle", serverThrottle);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
        serverThrottle = tag.getFloat("Throttle");
    }
}
