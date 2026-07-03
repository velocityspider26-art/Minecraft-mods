package net.mcreator.crustychunks.utils;

import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Bridges the old Forge capability call-sites to the NeoForge 1.21.1 capability system.
 */
public final class WariumCaps {
	private WariumCaps() {
	}

	public static Optional<IItemHandler> itemHandler(BlockEntity be, Direction side) {
		if (be == null || be.getLevel() == null)
			return Optional.empty();
		return Optional.ofNullable(be.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), be.getBlockState(), be, side));
	}

	public static Optional<IItemHandler> itemHandler(Entity entity, Direction side) {
		if (entity == null)
			return Optional.empty();
		return Optional.ofNullable(entity.getCapability(Capabilities.ItemHandler.ENTITY, null));
	}

	public static Optional<IItemHandler> itemHandler(ItemStack stack, Direction side) {
		if (stack == null)
			return Optional.empty();
		return Optional.ofNullable(stack.getCapability(Capabilities.ItemHandler.ITEM));
	}

	public static Optional<IEnergyStorage> energy(BlockEntity be, Direction side) {
		if (be == null || be.getLevel() == null)
			return Optional.empty();
		return Optional.ofNullable(be.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, be.getBlockPos(), be.getBlockState(), be, side));
	}

	public static Optional<IEnergyStorage> energy(Entity entity, Direction side) {
		if (entity == null)
			return Optional.empty();
		return Optional.ofNullable(entity.getCapability(Capabilities.EnergyStorage.ENTITY, null));
	}

	public static Optional<IFluidHandler> fluid(BlockEntity be, Direction side) {
		if (be == null || be.getLevel() == null)
			return Optional.empty();
		return Optional.ofNullable(be.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), be.getBlockState(), be, side));
	}
}
