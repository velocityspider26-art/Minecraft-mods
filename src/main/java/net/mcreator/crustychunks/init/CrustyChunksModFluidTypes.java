package net.mcreator.crustychunks.init;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.mcreator.crustychunks.fluid.types.ChlorineGasFluidType;
import net.mcreator.crustychunks.fluid.types.CompressedAirFluidType;
import net.mcreator.crustychunks.fluid.types.CrudeOilFluidType;
import net.mcreator.crustychunks.fluid.types.DieselFluidType;
import net.mcreator.crustychunks.fluid.types.HydrazineFluidType;
import net.mcreator.crustychunks.fluid.types.KeroseneFluidType;
import net.mcreator.crustychunks.fluid.types.LiquidHydrogenFluidType;
import net.mcreator.crustychunks.fluid.types.LiquidOxygenFluidType;
import net.mcreator.crustychunks.fluid.types.OilFluidType;
import net.mcreator.crustychunks.fluid.types.PetroliumFluidType;
import net.mcreator.crustychunks.fluid.types.SulfuricAcidFluidType;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class CrustyChunksModFluidTypes {
   public static final DeferredRegister<FluidType> REGISTRY = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, "crusty_chunks");
   public static final DeferredHolder<FluidType, FluidType> CRUDE_OIL_TYPE = REGISTRY.register("crude_oil", () -> new CrudeOilFluidType());
   public static final DeferredHolder<FluidType, FluidType> OIL_TYPE = REGISTRY.register("oil", () -> new OilFluidType());
   public static final DeferredHolder<FluidType, FluidType> DIESEL_TYPE = REGISTRY.register("diesel", () -> new DieselFluidType());
   public static final DeferredHolder<FluidType, FluidType> KEROSENE_TYPE = REGISTRY.register("kerosene", () -> new KeroseneFluidType());
   public static final DeferredHolder<FluidType, FluidType> PETROLIUM_TYPE = REGISTRY.register("petrolium", () -> new PetroliumFluidType());
   public static final DeferredHolder<FluidType, FluidType> SULFURIC_ACID_TYPE = REGISTRY.register("sulfuric_acid", () -> new SulfuricAcidFluidType());
   public static final DeferredHolder<FluidType, FluidType> COMPRESSED_AIR_TYPE = REGISTRY.register("compressed_air", () -> new CompressedAirFluidType());
   public static final DeferredHolder<FluidType, FluidType> LIQUID_OXYGEN_TYPE = REGISTRY.register("liquid_oxygen", () -> new LiquidOxygenFluidType());
   public static final DeferredHolder<FluidType, FluidType> LIQUID_HYDROGEN_TYPE = REGISTRY.register("liquid_hydrogen", () -> new LiquidHydrogenFluidType());
   public static final DeferredHolder<FluidType, FluidType> CHLORINE_GAS_TYPE = REGISTRY.register("chlorine_gas", () -> new ChlorineGasFluidType());
   public static final DeferredHolder<FluidType, FluidType> HYDRAZINE_TYPE = REGISTRY.register("hydrazine", () -> new HydrazineFluidType());
}
