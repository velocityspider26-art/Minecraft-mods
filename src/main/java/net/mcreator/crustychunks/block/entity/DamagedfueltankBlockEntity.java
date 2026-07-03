package net.mcreator.crustychunks.block.entity;

import java.util.function.Predicate;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.mcreator.crustychunks.init.CrustyChunksModBlockEntities;
import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

public class DamagedfueltankBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
   private NonNullList<ItemStack> stacks = NonNullList.withSize(0, ItemStack.EMPTY);
   private final SidedInvWrapper handler = new SidedInvWrapper(this, null);
   private final FluidTank fluidTank = new FluidTank(1000, fs -> {
      if (fs.getFluid() == CrustyChunksModFluids.OIL.get()) {
         return true;
      } else if (fs.getFluid() == CrustyChunksModFluids.FLOWING_OIL.get()) {
         return true;
      } else if (fs.getFluid() == CrustyChunksModFluids.DIESEL.get()) {
         return true;
      } else if (fs.getFluid() == CrustyChunksModFluids.FLOWING_DIESEL.get()) {
         return true;
      } else if (fs.getFluid() == CrustyChunksModFluids.KEROSENE.get()) {
         return true;
      } else if (fs.getFluid() == CrustyChunksModFluids.FLOWING_KEROSENE.get()) {
         return true;
      } else if (fs.getFluid() == CrustyChunksModFluids.PETROLIUM.get()) {
         return true;
      } else if (fs.getFluid() == CrustyChunksModFluids.FLOWING_PETROLIUM.get()) {
         return true;
      } else if (fs.getFluid() == CrustyChunksModFluids.LIQUID_HYDROGEN.get()) {
         return true;
      } else {
         return fs.getFluid() == CrustyChunksModFluids.HYDRAZINE.get() ? true : fs.getFluid() == CrustyChunksModFluids.LIQUID_OXYGEN.get();
      }
   }) {
      protected void onContentsChanged() {
         super.onContentsChanged();
         DamagedfueltankBlockEntity.this.setChanged();
         DamagedfueltankBlockEntity.this.level
            .sendBlockUpdated(
               DamagedfueltankBlockEntity.this.worldPosition,
               DamagedfueltankBlockEntity.this.level.getBlockState(DamagedfueltankBlockEntity.this.worldPosition),
               DamagedfueltankBlockEntity.this.level.getBlockState(DamagedfueltankBlockEntity.this.worldPosition),
               2
            );
      }
   };

   public DamagedfueltankBlockEntity(BlockPos position, BlockState state) {
      super((BlockEntityType)CrustyChunksModBlockEntities.DAMAGEDFUELTANK.get(), position, state);
   }

   public void loadAdditional(CompoundTag compound, net.minecraft.core.HolderLookup.Provider lookupProvider) {
      super.loadAdditional(compound, lookupProvider);
      if (!this.tryLoadLootTable(compound)) {
         this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      }

      ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
      if (compound.get("fluidTank") instanceof CompoundTag compoundTag) {
         this.fluidTank.readFromNBT(lookupProvider, compoundTag);
      }
   }

   public void saveAdditional(CompoundTag compound, net.minecraft.core.HolderLookup.Provider lookupProvider) {
      super.saveAdditional(compound, lookupProvider);
      if (!this.trySaveLootTable(compound)) {
         ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
      }

      compound.put("fluidTank", this.fluidTank.writeToNBT(lookupProvider, new CompoundTag()));
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookupProvider) {
      return this.saveWithFullMetadata(lookupProvider);
   }

   public int getContainerSize() {
      return this.stacks.size();
   }

   public boolean isEmpty() {
      for (ItemStack itemstack : this.stacks) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public Component getDefaultName() {
      return Component.literal("damagedfueltank");
   }

   public int getMaxStackSize() {
      return 64;
   }

   public AbstractContainerMenu createMenu(int id, Inventory inventory) {
      return ChestMenu.threeRows(id, inventory);
   }

   public Component getDisplayName() {
      return Component.literal("Damaged Fuel Tank Module");
   }

   protected NonNullList<ItemStack> getItems() {
      return this.stacks;
   }

   protected void setItems(NonNullList<ItemStack> stacks) {
      this.stacks = stacks;
   }

   public boolean canPlaceItem(int index, ItemStack stack) {
      return true;
   }

   public int[] getSlotsForFace(Direction side) {
      return IntStream.range(0, this.getContainerSize()).toArray();
   }

   public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
      return this.canPlaceItem(index, stack);
   }

   public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
      return true;
   }

   public SidedInvWrapper getItemHandler() {
      return this.handler;
   }

   public net.neoforged.neoforge.fluids.capability.templates.FluidTank getFluidTank() {
      return this.fluidTank;
   }
}
