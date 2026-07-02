package com.example.examplemod.content.thruster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Stores the thruster's fuel and the smoothed throttle used to drive the plume mesh.
 *
 * <p>Fuel can be inserted two ways, like the Create thruster: piped into the exposed item-handler
 * capability (funnel/hopper/dropper), or by right-clicking with a fuel item. Inserted items are
 * burned down into a fuel buffer on the server; whether the thruster is currently burning is
 * published to the client through the {@link ThrusterBlock#LIT} block-state, so no extra sync is
 * needed. The throttle itself is smoothed client-side for a soft spool up/down.</p>
 */
public class ThrusterBlockEntity extends BlockEntity {
    public static final int FUEL_CAPACITY = 200_000;
    private static final int FUEL_BURN_PER_TICK = 1;

    /** Single input slot that only accepts valid fuel items. */
    private final ItemStackHandler fuelInput = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ThrusterBlock.getFuelValue(stack) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int fuel = 0;

    private float currentThrottle = 0f;
    private float prevThrottle = 0f;

    public ThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IItemHandler getFuelHandler() {
        return fuelInput;
    }

    /** Server tick: pull fuel from the input slot, burn it, and keep the LIT state in sync. */
    public void serverTick() {
        if (level == null) {
            return;
        }
        // Top up the fuel buffer from the input slot when it runs dry.
        if (fuel <= 0) {
            ItemStack in = fuelInput.getStackInSlot(0);
            int value = ThrusterBlock.getFuelValue(in);
            if (value > 0) {
                fuelInput.extractItem(0, 1, false);
                fuel += value;
                setChanged();
            }
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
        tag.putInt("Fuel", fuel);
        tag.put("FuelInput", fuelInput.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = tag.getInt("Fuel");
        if (tag.contains("FuelInput")) {
            fuelInput.deserializeNBT(registries, tag.getCompound("FuelInput"));
        }
    }
}
