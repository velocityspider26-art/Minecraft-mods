package net.mcreator.crustychunks.fluid;

import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModFluidTypes;
import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties;

public abstract class KeroseneFluid extends BaseFlowingFluid {
   public static final Properties PROPERTIES = new Properties(
         () -> (FluidType)CrustyChunksModFluidTypes.KEROSENE_TYPE.get(),
         () -> (Fluid)CrustyChunksModFluids.KEROSENE.get(),
         () -> (Fluid)CrustyChunksModFluids.FLOWING_KEROSENE.get()
      )
      .explosionResistance(100.0F)
      .tickRate(4)
      .bucket(() -> (Item)CrustyChunksModItems.KEROSENE_BUCKET.get())
      .block(() -> (LiquidBlock)CrustyChunksModBlocks.KEROSENE.get());

   private KeroseneFluid() {
      super(PROPERTIES);
   }

   public static class Flowing extends KeroseneFluid {
      protected void createFluidStateDefinition(Builder<Fluid, FluidState> builder) {
         super.createFluidStateDefinition(builder);
         builder.add(new Property[]{LEVEL});
      }

      public int getAmount(FluidState state) {
         return (Integer)state.getValue(LEVEL);
      }

      public boolean isSource(FluidState state) {
         return false;
      }
   }

   public static class Source extends KeroseneFluid {
      public int getAmount(FluidState state) {
         return 8;
      }

      public boolean isSource(FluidState state) {
         return true;
      }
   }
}
