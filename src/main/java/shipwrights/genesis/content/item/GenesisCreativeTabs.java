package shipwrights.genesis.content.item;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import shipwrights.genesis.GenesisMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import shipwrights.genesis.content.block.datagen.BlockType;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class GenesisCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GenesisMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GENESIS_TAB = CREATIVE_MODE_TABS.register("genesis_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab.genesis_tab"))
            .icon(() -> new ItemStack(GenesisItems.NAV_PROJECTOR.get()))
            .displayItems((parameters, output) -> {
                output.accept(GenesisItems.NAV_PROJECTOR.get());
                output.accept(GenesisItems.RADAR_DISPLAY.get());
                output.accept(GenesisItems.PS5_CONSOLE.get());
                output.accept(GenesisItems.PS5_CONTROLLER.get());
                output.accept(GenesisItems.TULCITE_CATALYZER_BLOCK_ITEM.get());
                output.accept(GenesisItems.TULCITE_CHUNK.get());
                output.accept(GenesisItems.VOID_SHARD.get());
                output.accept(GenesisItems.ANORTHITE_CRYSTAL.get());
                output.accept(GenesisItems.VOID_ENGINE_INTERFACE.get());
                output.accept(GenesisItems.VOID_CORE_REFLECTOR_PANEL.get());
                output.accept(GenesisItems.VOID_FOCUS.get());
                output.accept(GenesisItems.VOID_ENGINE_VIEWPORT.get());
                output.accept(GenesisItems.VOID_ENGINE_FRAME.get());
                output.accept(GenesisItems.VOID_CORE.get());
                output.accept(GenesisItems.HYPERDRIVE_CLASS_6.get());
                output.accept(GenesisItems.HYPERDRIVE_CLASS_5.get());
                output.accept(GenesisItems.HYPERDRIVE_CLASS_4.get());
                output.accept(GenesisItems.HYPERDRIVE_CLASS_3.get());
                output.accept(GenesisItems.HYPERDRIVE_CLASS_2.get());
                output.accept(GenesisItems.HYPERDRIVE_CLASS_1.get());
                output.accept(GenesisItems.HYPERDRIVE_CLASS_0_5.get());
                output.accept(GenesisItems.FALKEN_HYPERDRIVE.get());
                output.accept(GenesisItems.ORBIT_GOGGLES.get());
                output.accept(GenesisItems.ORBITAL_STABILIZER.get());
                output.accept(GenesisItems.SPACE_HELMET.get());
                output.accept(GenesisItems.SPACE_CHESTPLATE.get());
                output.accept(GenesisItems.SPACE_LEGGINGS.get());
                output.accept(GenesisItems.SPACE_BOOTS.get());
                addPaintings(parameters, output);
            })
            .build());

    private static final Set<String> alreadyAdded = new HashSet<>();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GENESIS_NATURAL_TAB = CREATIVE_MODE_TABS.register("genesis_natural_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.genesis_natural_tab"))
                    .icon(() -> new ItemStack(GenesisItems.NULLSTONE.get()))
                    .displayItems((parameters, output) -> {
                        addItemGroup(output, GenesisItems.WARPSTONE);
                        addItemGroup(output, GenesisItems.TULCITE_ORE);
                        addItemGroup(output, GenesisItems.VERDITE_ORE);
                        addItemGroup(output, GenesisItems.VOID_CORE_ORE);
                        addItemGroup(output, GenesisItems.ANORTHITE);
                        addItemGroup(output, GenesisItems.NULLSTONE);
                        addItemGroup(output, GenesisItems.VOIDSTONE);
                        addItemGroup(output, GenesisItems.RIFTROCK);
                        addItemGroup(output, GenesisItems.ECHOSTONE);
                        addItemGroup(output, GenesisItems.PHASEROCK);
                        output.accept(GenesisItems.STELLAR_SAND.get());
                        output.accept(GenesisItems.LUNAR_DUST.get());
                        output.accept(GenesisItems.CRACKED_CYAN_SALT.get());
                        output.accept(GenesisItems.CYAN_SALT.get());
                        output.accept(GenesisItems.CRACKED_TURQUOISE_SALT.get());
                        output.accept(GenesisItems.TURQUOISE_SALT.get());
                        output.accept(GenesisItems.CRACKED_RED_SALT.get());
                        output.accept(GenesisItems.RED_SALT.get());
                        output.accept(GenesisItems.CRACKED_PALE_RED_SALT.get());
                        output.accept(GenesisItems.PALE_RED_SALT.get());
                        output.accept(GenesisItems.SALT.get());
                        output.accept(GenesisItems.CRACKED_SALT.get());
                        output.accept(GenesisItems.BRINE_TRUNK.get());
                        output.accept(GenesisItems.BRINE_FLOWER.get());
                        output.accept(GenesisItems.PETRIFIED_BUSH.get());
                        output.accept(GenesisItems.HALLOW_MOON_STONE.get());
                        output.accept(GenesisItems.MOON_STONE.get());
                        output.accept(GenesisItems.DARK_WAVY_MOON_SAND.get());
                        output.accept(GenesisItems.DARK_MOON_SAND.get());
                        output.accept(GenesisItems.MOON_SAND.get());
                        output.accept(GenesisItems.WAVY_MOON_SAND.get());
                        output.accept(GenesisItems.DARK_MOON_SAND.get());
                        output.accept(GenesisItems.DEAD_MOON_CORAL_BLOCK.get());
                        output.accept(GenesisItems.DEAD_MOON_CORAL_FAN.get());
                        output.accept(GenesisItems.DEAD_MOON_CORAL.get());

                        output.accept(GenesisItems.CHALCOPYRITE.get());
                        output.accept(GenesisItems.CHALCOPYRITE_GRAVEL.get());
                        output.accept(GenesisItems.CHALCOPYRITE_SAND.get());
                        output.accept(GenesisItems.CHALCOPYRITE_SPROUTS.get());
                        output.accept(GenesisItems.CHALCOPYRITE_BRAMBLE.get());
                        output.accept(GenesisItems.ROOTED_SMOLDERING_CHALCOPYRITE.get());
                        output.accept(GenesisItems.MALACHITE.get());
                        output.accept(GenesisItems.MALACHITE_GRAVEL.get());
                        output.accept(GenesisItems.MALACHITE_SAND.get());
                        output.accept(GenesisItems.MALACHITE_SPROUTS.get());
                        output.accept(GenesisItems.MALACHITE_BRAMBLE.get());
                        output.accept(GenesisItems.ROOTED_SMOLDERING_MALACHITE.get());
                        output.accept(GenesisItems.HEMATITE.get());
                        output.accept(GenesisItems.HEMATITE_GRAVEL.get());
                        output.accept(GenesisItems.HEMATITE_SAND.get());
                        output.accept(GenesisItems.HEMATITE_SPROUTS.get());
                        output.accept(GenesisItems.HEMATITE_BRAMBLE.get());
                        output.accept(GenesisItems.ROOTED_SMOLDERING_HEMATITE.get());
                        output.accept(GenesisItems.SMOLDERING_LILY.get());

                        output.accept(GenesisItems.WITHERING_WILLOW_LOG.get());
                        output.accept(GenesisItems.WITHERING_WILLOW_BRANCH.get());
                        output.accept(GenesisItems.WITHERING_WILLOW_LEAVES.get());
                        // Miasma bucket is added to this tab automatically by Registrate's
                        // creative-tab handler; adding it manually here double-registers it
                        // (NeoForge 1.21.1 throws on duplicate tab entries).

                        output.accept(GenesisItems.VERDITE_ORE.get());
                        output.accept(GenesisItems.VERDITE_CLUSTER.get());
                        output.accept(GenesisItems.VERDITE_CRYSTAL_BLOCK.get());

                        for (var item : GenesisItems.DYNAMIC_ITEMS.values()) {
                            if (alreadyAdded.contains(item.getId().getPath())) {
                                continue;
                            }
                            output.accept(item.get());
                        }
                    })
                    .build());

    private static void addItemGroup(CreativeModeTab.Output output, DeferredHolder<Item, ? extends Item> item) {
        output.accept(item.get());

        String name = item.getId().getPath();

        for (String suffix : BlockType.suffixes) {
            DeferredHolder<Item, ? extends Item> it = GenesisItems.DYNAMIC_ITEMS.get(name + suffix);
            if (it != null) {
                alreadyAdded.add(it.getId().getPath());
                output.accept(it.get());
            }
        }
    }


    private static void addPaintings(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        parameters.holders().lookup(Registries.PAINTING_VARIANT).ifPresent((arg2x) -> generatePresetPaintings(output, arg2x, (arg) -> arg.is(PaintingVariantTags.PLACEABLE), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }

    private static void generatePresetPaintings(CreativeModeTab.Output arg, HolderLookup.RegistryLookup<PaintingVariant> arg2, Predicate<Holder<PaintingVariant>> predicate, CreativeModeTab.TabVisibility arg3) {
        arg2.listElements().filter(predicate).sorted(PAINTING_COMPARATOR).forEach((arg3x) -> {
            if (arg3x.key().location().getNamespace().equals("genesis")) {
                ItemStack itemstack = new ItemStack(Items.PAINTING);
                CustomData.update(DataComponents.ENTITY_DATA, itemstack, tag ->
                        tag.putString("variant", arg3x.key().location().toString()));
                arg.accept(itemstack, arg3);
            }
        });
    }

    private static final Comparator<Holder<PaintingVariant>> PAINTING_COMPARATOR = Comparator.comparing(Holder::value, Comparator.comparingInt(PaintingVariant::area).thenComparing(PaintingVariant::width));


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
