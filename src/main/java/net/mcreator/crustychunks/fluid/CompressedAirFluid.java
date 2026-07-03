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

public abstract class CompressedAirFluid extends BaseFlowingFluid {
   public static final Properties PROPERTIES = new Properties(
         () -> (FluidType)CrustyChunksModFluidTypes.COMPRESSED_AIR_TYPE.get(),
         () -> (Fluid)CrustyChunksModFluids.COMPRESSED_AIR.get(),
         () -> (Fluid)CrustyChunksModFluids.FLOWING_COMPRESSED_AIR.get()
      )
      .explosionResistance(100.0F)
      .block(() -> (LiquidBlock)CrustyChunksModBlocks.COMPRESSED_AIR.get());

   private CompressedAirFluid() {
      super(PROPERTIES);
   }

   public static class Flowing extends CompressedAirFluid {
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

   public static class Source extends CompressedAirFluid {
      public int getAmount(FluidState state) {
         return 8;
      }

      public boolean isSource(FluidState state) {
         return true;
      }
   }
}
