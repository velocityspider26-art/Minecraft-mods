package net.mcreator.crustychunks.init;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mcreator.crustychunks.world.inventory.ABGuiMenu;
import net.mcreator.crustychunks.world.inventory.BlastFurnaceGUIMenu;
import net.mcreator.crustychunks.world.inventory.BreederReactorControlGUIMenu;
import net.mcreator.crustychunks.world.inventory.ConveyorGUIMenu;
import net.mcreator.crustychunks.world.inventory.ConveyorSplitterGUIMenu;
import net.mcreator.crustychunks.world.inventory.CrusherGUIMenu;
import net.mcreator.crustychunks.world.inventory.EnergyDisplayMenu;
import net.mcreator.crustychunks.world.inventory.FireboxGUIMenu;
import net.mcreator.crustychunks.world.inventory.FissionBombGUIMenu;
import net.mcreator.crustychunks.world.inventory.FissionWarheadGUIMenu;
import net.mcreator.crustychunks.world.inventory.FoundryGUIMenu;
import net.mcreator.crustychunks.world.inventory.FusionBombGUIMenu;
import net.mcreator.crustychunks.world.inventory.MachineputGUI4xMenu;
import net.mcreator.crustychunks.world.inventory.MineralGrinderGUIMenu;
import net.mcreator.crustychunks.world.inventory.ProgressGUIMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CrustyChunksModMenus {
   public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.MENU, "crusty_chunks");
   public static final DeferredHolder<MenuType<?>, MenuType<BlastFurnaceGUIMenu>> BLAST_FURNACE_GUI = REGISTRY.register(
      "blast_furnace_gui", () -> IMenuTypeExtension.create(BlastFurnaceGUIMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<ConveyorGUIMenu>> CONVEYOR_GUI = REGISTRY.register(
      "conveyor_gui", () -> IMenuTypeExtension.create(ConveyorGUIMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<FoundryGUIMenu>> FOUNDRY_GUI = REGISTRY.register("foundry_gui", () -> IMenuTypeExtension.create(FoundryGUIMenu::new));
   public static final DeferredHolder<MenuType<?>, MenuType<FireboxGUIMenu>> FIREBOX_GUI = REGISTRY.register("firebox_gui", () -> IMenuTypeExtension.create(FireboxGUIMenu::new));
   public static final DeferredHolder<MenuType<?>, MenuType<MineralGrinderGUIMenu>> MINERAL_GRINDER_GUI = REGISTRY.register(
      "mineral_grinder_gui", () -> IMenuTypeExtension.create(MineralGrinderGUIMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<FissionBombGUIMenu>> FISSION_BOMB_GUI = REGISTRY.register(
      "fission_bomb_gui", () -> IMenuTypeExtension.create(FissionBombGUIMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<BreederReactorControlGUIMenu>> BREEDER_REACTOR_CONTROL_GUI = REGISTRY.register(
      "breeder_reactor_control_gui", () -> IMenuTypeExtension.create(BreederReactorControlGUIMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<FissionWarheadGUIMenu>> FISSION_WARHEAD_GUI = REGISTRY.register(
      "fission_warhead_gui", () -> IMenuTypeExtension.create(FissionWarheadGUIMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<EnergyDisplayMenu>> ENERGY_DISPLAY = REGISTRY.register(
      "energy_display", () -> IMenuTypeExtension.create(EnergyDisplayMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<ABGuiMenu>> AB_GUI = REGISTRY.register("ab_gui", () -> IMenuTypeExtension.create(ABGuiMenu::new));
   public static final DeferredHolder<MenuType<?>, MenuType<FusionBombGUIMenu>> FUSION_BOMB_GUI = REGISTRY.register(
      "fusion_bomb_gui", () -> IMenuTypeExtension.create(FusionBombGUIMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MachineputGUI4xMenu>> MACHINEPUT_GUI_4X = REGISTRY.register(
      "machineput_gui_4x", () -> IMenuTypeExtension.create(MachineputGUI4xMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<CrusherGUIMenu>> CRUSHER_GUI = REGISTRY.register("crusher_gui", () -> IMenuTypeExtension.create(CrusherGUIMenu::new));
   public static final DeferredHolder<MenuType<?>, MenuType<ConveyorSplitterGUIMenu>> CONVEYOR_SPLITTER_GUI = REGISTRY.register(
      "conveyor_splitter_gui", () -> IMenuTypeExtension.create(ConveyorSplitterGUIMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<ProgressGUIMenu>> PROGRESS_GUI = REGISTRY.register(
      "progress_gui", () -> IMenuTypeExtension.create(ProgressGUIMenu::new)
   );
}
