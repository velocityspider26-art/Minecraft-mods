package com.example.examplemod;

import org.slf4j.Logger;

import com.example.examplemod.content.thruster.CreativeThrusterBlock;
import com.example.examplemod.content.thruster.CreativeThrusterBlockEntity;
import com.example.examplemod.content.thruster.ThrusterBlock;
import com.example.examplemod.content.thruster.ThrusterBlockEntity;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ExampleMod.MODID)
public class ExampleMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "examplemod";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold BlockEntityTypes which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    // --- Thruster blocks (plume-mesh exhaust instead of particles) ---
    // A directional thruster that burns fuel (lava or kerosene) to fire its plume.
    public static final DeferredBlock<ThrusterBlock> THRUSTER_BLOCK = BLOCKS.registerBlock("thruster",
            ThrusterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(ThrusterBlock.POWER)));
    public static final DeferredItem<BlockItem> THRUSTER_ITEM = ITEMS.registerSimpleBlockItem("thruster", THRUSTER_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThrusterBlockEntity>> THRUSTER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("thruster", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new ThrusterBlockEntity(ExampleMod.THRUSTER_BLOCK_ENTITY.get(), pos, state),
                    THRUSTER_BLOCK.get()).build(null));

    // A creative thruster that always fires at full throttle.
    public static final DeferredBlock<CreativeThrusterBlock> CREATIVE_THRUSTER_BLOCK = BLOCKS.registerBlock("creative_thruster",
            CreativeThrusterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0f).sound(SoundType.METAL).lightLevel(state -> 15));
    public static final DeferredItem<BlockItem> CREATIVE_THRUSTER_ITEM = ITEMS.registerSimpleBlockItem("creative_thruster", CREATIVE_THRUSTER_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeThrusterBlockEntity>> CREATIVE_THRUSTER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("creative_thruster", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new CreativeThrusterBlockEntity(ExampleMod.CREATIVE_THRUSTER_BLOCK_ENTITY.get(), pos, state),
                    CREATIVE_THRUSTER_BLOCK.get()).build(null));

    // --- Kerosene: a liquid thruster fuel you can store in a tank and pump into the thruster ---
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MODID);

    public static final DeferredHolder<FluidType, FluidType> KEROSENE_FLUID_TYPE = FLUID_TYPES.register("kerosene",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.examplemod.kerosene")
                    .density(820).viscosity(1200).canSwim(true).canDrown(false)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> KEROSENE_FLUID = FLUIDS.register("kerosene",
            () -> new BaseFlowingFluid.Source(ExampleMod.KEROSENE_FLUID_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> KEROSENE_FLOWING = FLUIDS.register("flowing_kerosene",
            () -> new BaseFlowingFluid.Flowing(ExampleMod.KEROSENE_FLUID_PROPERTIES));
    public static final DeferredBlock<LiquidBlock> KEROSENE_LIQUID_BLOCK = BLOCKS.register("kerosene",
            () -> new LiquidBlock(KEROSENE_FLUID.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE).replaceable().noCollission().strength(100f).noLootTable().liquid()));
    public static final DeferredItem<Item> KEROSENE_BUCKET = ITEMS.register("kerosene_bucket",
            () -> new BucketItem(KEROSENE_FLUID.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final BaseFlowingFluid.Properties KEROSENE_FLUID_PROPERTIES = new BaseFlowingFluid.Properties(
            KEROSENE_FLUID_TYPE, KEROSENE_FLUID, KEROSENE_FLOWING)
            .block(KEROSENE_LIQUID_BLOCK).bucket(KEROSENE_BUCKET)
            .slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(20);

    // Creates a new Block with the id "examplemod:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.examplemod")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(THRUSTER_ITEM.get());
                output.accept(CREATIVE_THRUSTER_ITEM.get());
                output.accept(KEROSENE_BUCKET.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Expose the thruster's fuel tank as a fluid-handler capability (Create pumps/pipes)
        modEventBus.addListener(this::registerCapabilities);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entity types get registered
        BLOCK_ENTITIES.register(modEventBus);
        // Register the kerosene fluid, its type, and its bucket
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Expose the regular thruster's fuel tank so Create pumps/pipes can fill it. The creative
        // thruster never burns anything, so it has no tank.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, THRUSTER_BLOCK_ENTITY.get(),
                (be, side) -> be.getFuelTank());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
