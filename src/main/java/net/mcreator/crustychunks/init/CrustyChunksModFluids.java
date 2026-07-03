package net.mcreator.crustychunks.init;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.fluid.ChlorineGasFluid;
import net.mcreator.crustychunks.fluid.CompressedAirFluid;
import net.mcreator.crustychunks.fluid.CrudeOilFluid;
import net.mcreator.crustychunks.fluid.DieselFluid;
import net.mcreator.crustychunks.fluid.HydrazineFluid;
import net.mcreator.crustychunks.fluid.KeroseneFluid;
import net.mcreator.crustychunks.fluid.LiquidHydrogenFluid;
import net.mcreator.crustychunks.fluid.LiquidOxygenFluid;
import net.mcreator.crustychunks.fluid.OilFluid;
import net.mcreator.crustychunks.fluid.PetroliumFluid;
import net.mcreator.crustychunks.fluid.SulfuricAcidFluid;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CrustyChunksModFluids {
   public static final DeferredRegister<Fluid> REGISTRY = DeferredRegister.create(BuiltInRegistries.FLUID, "crusty_chunks");
   public static final DeferredHolder<Fluid, FlowingFluid> CRUDE_OIL = REGISTRY.register("crude_oil", () -> new CrudeOilFluid.Source());
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_CRUDE_OIL = REGISTRY.register("flowing_crude_oil", () -> new CrudeOilFluid.Flowing());
   public static final DeferredHolder<Fluid, FlowingFluid> OIL = REGISTRY.register("oil", () -> new OilFluid.Source());
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_OIL = REGISTRY.register("flowing_oil", () -> new OilFluid.Flowing());
   public static final DeferredHolder<Fluid, FlowingFluid> DIESEL = REGISTRY.register("diesel", () -> new DieselFluid.Source());
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_DIESEL = REGISTRY.register("flowing_diesel", () -> new DieselFluid.Flowing());
   public static final DeferredHolder<Fluid, FlowingFluid> KEROSENE = REGISTRY.register("kerosene", () -> new KeroseneFluid.Source());
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_KEROSENE = REGISTRY.register("flowing_kerosene", () -> new KeroseneFluid.Flowing());
   public static final DeferredHolder<Fluid, FlowingFluid> PETROLIUM = REGISTRY.register("petrolium", () -> new PetroliumFluid.Source());
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_PETROLIUM = REGISTRY.register("flowing_petrolium", () -> new PetroliumFluid.Flowing());
   public static final DeferredHolder<Fluid, FlowingFluid> SULFURIC_ACID = REGISTRY.register("sulfuric_acid", () -> new SulfuricAcidFluid.Source());
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_SULFURIC_ACID = REGISTRY.register("flowing_sulfuric_acid", () -> new SulfuricAcidFluid.Flowing());
   public static final DeferredHolder<Fluid, FlowingFluid> COMPRESSED_AIR = REGISTRY.register("compressed_air", () -> new CompressedAirFluid.Source());
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_COMPRESSED_AIR = REGISTRY.register("flowing_compressed_air", () -> new CompressedAirFluid.Flowing());
   public static final DeferredHolder<Fluid, FlowingFluid> LIQUID_OXYGEN = REGISTRY.register("liquid_oxygen", () -> new LiquidOxygenFluid.Source());
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_LIQUID_OXYGEN = REGISTRY.register("flowing_liquid_oxygen", () -> new LiquidOxygenFluid.Flowing());
   public static final DeferredHolder<Fluid, FlowingFluid> LIQUID_HYDROGEN = REGISTRY.register("liquid_hydrogen", () -> new LiquidHydrogenFluid.Source());
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_LIQUID_HYDROGEN = REGISTRY.register(
      "flowing_liquid_hydrogen", () -> new LiquidHydrogenFluid.Flowing()
   );
   public static final DeferredHolder<Fluid, FlowingFluid> CHLORINE_GAS = REGISTRY.register("chlorine_gas", () -> new ChlorineGasFluid.Source());
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_CHLORINE_GAS = REGISTRY.register("flowing_chlorine_gas", () -> new ChlorineGasFluid.Flowing());
   public static final DeferredHolder<Fluid, FlowingFluid> HYDRAZINE = REGISTRY.register("hydrazine", () -> new HydrazineFluid.Source());
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_HYDRAZINE = REGISTRY.register("flowing_hydrazine", () -> new HydrazineFluid.Flowing());

   @EventBusSubscriber(
      bus = Bus.MOD,
      value = {Dist.CLIENT}
   )
   public static class FluidsClientSideHandler {
      @SubscribeEvent
      public static void clientSetup(FMLClientSetupEvent event) {
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.CRUDE_OIL.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.FLOWING_CRUDE_OIL.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.OIL.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.FLOWING_OIL.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.DIESEL.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.FLOWING_DIESEL.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.KEROSENE.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.FLOWING_KEROSENE.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.PETROLIUM.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.FLOWING_PETROLIUM.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.SULFURIC_ACID.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.FLOWING_SULFURIC_ACID.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.COMPRESSED_AIR.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.FLOWING_COMPRESSED_AIR.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.LIQUID_OXYGEN.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.FLOWING_LIQUID_OXYGEN.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.LIQUID_HYDROGEN.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.FLOWING_LIQUID_HYDROGEN.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.CHLORINE_GAS.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.FLOWING_CHLORINE_GAS.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.HYDRAZINE.get(), RenderType.translucent());
         ItemBlockRenderTypes.setRenderLayer((Fluid)CrustyChunksModFluids.FLOWING_HYDRAZINE.get(), RenderType.translucent());
      }
   }
}
