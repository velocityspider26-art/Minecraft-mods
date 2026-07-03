package net.mcreator.crustychunks.fluid;

import net.mcreator.crustychunks.init.CrustyChunksModBlocks;
import net.mcreator.crustychunks.init.CrustyChunksModFluidTypes;
import net.mcreator.crustychunks.init.CrustyChunksModFluids;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties;

public abstract class LiquidHydrogenFluid extends BaseFlowingFluid {
   public static final Properties PROPERTIES = new Properties(
         () -> (FluidType)CrustyChunksModFluidTypes.LIQUID_HYDROGEN_TYPE.get(),
         () -> (Fluid)CrustyChunksModFluids.LIQUID_HYDROGEN.get(),
         () -> (Fluid)CrustyChunksModFluids.FLOWING_LIQUID_HYDROGEN.get()
      )
      .explosionResistance(100.0F)
      .block(() -> (LiquidBlock)CrustyChunksModBlocks.LIQUID_HYDROGEN.get());

   private LiquidHydrogenFluid() {
      super(PROPERTIES);
   }

   public static class Flowing extends LiquidHydrogenFluid {
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

   public static class Source extends LiquidHydrogenFluid {
      public int getAmount(FluidState state) {
         return 8;
      }

      public boolean isSource(FluidState state) {
         return true;
      }
   }
}
